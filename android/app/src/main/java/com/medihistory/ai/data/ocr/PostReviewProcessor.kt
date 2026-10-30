package com.medihistory.ai.data.ocr

import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.duplicate.DuplicateExplainerEngine
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.DuplicateAlertRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.duplicate.DuplicateDetector
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject

/**
 * Reads the patient-confirmed values from reviewed [ExtractedFieldEntity] records and
 * applies them to the downstream tables:
 *
 *  - `doctor_name`       → look up or create a [Doctor] row; link via [Prescription.doctorId]
 *  - `prescription_date` → parse and set [Prescription.prescriptionDate]
 *  - `medicine_name`     → create [Medicine] rows (with dose/frequency where present)
 *
 * Medical-safety invariants:
 * - Only fields in [ReviewState.REVIEWED] with a non-null patientValue are used.
 * - REJECTED or PENDING_REVIEW fields are ignored entirely — never inferred.
 * - Doctor specialty defaults to [DoctorSpecialty.OTHER] (unknown) — never guessed.
 * - Medicine status is always [MedicineStatus.UNKNOWN] — always patient-assigned.
 *
 * Privacy: field values are never logged.
 */
class PostReviewProcessor @Inject constructor(
    private val extractedFieldDao: ExtractedFieldDao,
    private val prescriptionDao: PrescriptionDao,
    private val doctorRepository: DoctorRepository,
    private val medicineRepository: MedicineRepository,
    private val duplicateAlertRepository: DuplicateAlertRepository,
    private val duplicateExplainerEngine: DuplicateExplainerEngine,
) : PostReviewProcessorInterface {

    /**
     * Runs the processor for a completed review session.
     *
     * @param prescriptionId  The prescription whose fields were just reviewed.
     * @param patientId       Required for all DAO/repository operations (security scope).
     */
    override suspend fun process(prescriptionId: String, patientId: String) {
        val prescription = prescriptionDao.getById(prescriptionId, patientId) ?: return

        // Snapshot the current extracted fields (Flow emits immediately for Room).
        val allFields: List<ExtractedFieldEntity> =
            extractedFieldDao.observeByPrescription(prescriptionId).first()

        // Only REVIEWED fields with a non-null patientValue are acted on.
        val reviewed: Map<String, ExtractedFieldEntity> = allFields
            .filter { field -> field.reviewState == ReviewState.REVIEWED && field.patientValue != null }
            .associateBy { field -> field.fieldName }

        // ── Doctor ────────────────────────────────────────────────────────────
        val doctorNameValue: String? = reviewed["doctor_name"]?.patientValue?.trim()
            ?.takeIf { it.isNotBlank() }

        val resolvedDoctorId: String? = if (doctorNameValue != null) {
            findOrCreateDoctor(name = doctorNameValue, patientId = patientId)
        } else {
            prescription.doctorId // keep whatever was already set
        }

        // ── Prescription date ─────────────────────────────────────────────────
        val dateValue: String? = reviewed["prescription_date"]?.patientValue?.trim()
        val parsedDate: LocalDate? = dateValue?.let { parseDate(it) }

        // ── Update prescription row if anything changed ───────────────────────
        val updatedPrescription = prescription.copy(
            doctorId = resolvedDoctorId ?: prescription.doctorId,
            prescriptionDate = parsedDate ?: prescription.prescriptionDate,
            updatedAt = System.currentTimeMillis(),
        )
        if (updatedPrescription != prescription) {
            prescriptionDao.update(updatedPrescription)
        }

        // ── Medicines ─────────────────────────────────────────────────────────
        // Collect all medicine-name fields (medicine_name, medicine_name_2, …) in order.
        val medicineNameEntries: List<Map.Entry<String, ExtractedFieldEntity>> = reviewed.entries
            .filter { entry ->
                entry.key == "medicine_name" || entry.key.startsWith("medicine_name_")
            }
            .sortedBy { entry -> entry.key }

        val medicines: List<Medicine> = medicineNameEntries.mapNotNull { entry ->
            val name: String = entry.value.patientValue?.trim()
                ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            // Match dose/frequency fields with the same suffix (e.g. _2 for medicine_name_2).
            val suffix: String = entry.key.removePrefix("medicine_name")
            val dose: String? = reviewed["dose$suffix"]?.patientValue?.trim()
                ?.takeIf { it.isNotBlank() }
            val frequency: String? = reviewed["frequency$suffix"]?.patientValue?.trim()
                ?.takeIf { it.isNotBlank() }

            val dosageInstructions: String? = listOfNotNull(dose, frequency)
                .joinToString(", ")
                .ifBlank { null }

            Medicine(
                id = UUID.randomUUID().toString(),
                prescriptionId = prescriptionId,
                patientId = patientId,
                medicineName = name,
                dosageInstructions = dosageInstructions,
                prescribingDoctorId = resolvedDoctorId,
                prescriptionDate = parsedDate ?: prescription.prescriptionDate,
                // Status is always UNKNOWN — the patient assigns CURRENT/PAST explicitly.
                status = MedicineStatus.UNKNOWN,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        }

        if (medicines.isNotEmpty()) {
            medicineRepository.insertAll(medicines)

            // Run duplicate detection only when new medicines were added this review.
            // Load all patient medicines (including the ones just inserted) and detect pairs.
            val allMedicines = medicineRepository.observeByPatient(patientId).first()
            val candidates = DuplicateDetector.detect(allMedicines)

            // Load existing alerts so we can skip pairs that are already recorded.
            // Deduplication is on (medicineAId, medicineBId) — the UUID primary key
            // is always fresh, so OnConflictStrategy.IGNORE on the DAO is not sufficient.
            val existingAlerts = duplicateAlertRepository.observeByPatient(patientId).first()
            val existingPairs: Set<Pair<String, String>> = existingAlerts
                .map { alert -> Pair(alert.medicineAId, alert.medicineBId) }
                .toSet()

            for (candidate in candidates) {
                // Skip if this exact pair (in either order) was already inserted.
                val pairFwd = Pair(candidate.medicineAId, candidate.medicineBId)
                val pairRev = Pair(candidate.medicineBId, candidate.medicineAId)
                if (pairFwd in existingPairs || pairRev in existingPairs) continue

                val alert = DuplicateAlert(
                    id = UUID.randomUUID().toString(),
                    patientId = patientId,
                    medicineAId = candidate.medicineAId,
                    medicineBId = candidate.medicineBId,
                    matchingIngredient = candidate.matchingIngredient,
                    matchType = candidate.matchType,
                    confidence = candidate.confidence,
                    combinationNote = candidate.combinationNote,
                    createdAt = System.currentTimeMillis(),
                )
                duplicateAlertRepository.insert(alert)

                // Generate and persist the plain-language explanation (F13).
                // Look up the two Medicine entities by their IDs (already in allMedicines).
                val medA = allMedicines.find { it.id == candidate.medicineAId }
                val medB = allMedicines.find { it.id == candidate.medicineBId }
                if (medA != null && medB != null) {
                    val explanation = duplicateExplainerEngine.explain(alert, medA, medB)
                    duplicateAlertRepository.updateExplanation(alert.id, patientId, explanation)
                }
            }        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds an existing doctor by exact name (case-insensitive) for this patient,
     * or creates a new one. Returns the doctor ID.
     */
    private suspend fun findOrCreateDoctor(name: String, patientId: String): String {
        val doctors: List<Doctor> = doctorRepository.observeByPatient(patientId).first()
        val existing: Doctor? = doctors.firstOrNull { doctor ->
            doctor.name.equals(name, ignoreCase = true)
        }
        if (existing != null) return existing.id

        val newDoctor = Doctor(
            id = UUID.randomUUID().toString(),
            patientId = patientId,
            name = name,
            // Specialty is UNKNOWN until the patient explicitly sets it — never inferred.
            specialty = DoctorSpecialty.OTHER,
            specialtyOther = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        doctorRepository.insert(newDoctor)
        return newDoctor.id
    }

    /**
     * Tries several common date formats used on Indian prescriptions.
     * Returns null if none match — absent dates stay null, never fabricated.
     */
    private fun parseDate(raw: String): LocalDate? {
        val formats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,                    // 2025-07-01
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),           // 01/07/2025
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),           // 01-07-2025
            DateTimeFormatter.ofPattern("d MMM yyyy"),           // 1 Jul 2025
            DateTimeFormatter.ofPattern("dd MMM yyyy"),          // 01 Jul 2025
            DateTimeFormatter.ofPattern("MMMM d, yyyy"),         // July 1, 2025
            DateTimeFormatter.ofPattern("d/M/yyyy"),             // 1/7/2025
        )
        return formats.firstNotNullOfOrNull { fmt ->
            try {
                LocalDate.parse(raw.trim(), fmt)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
