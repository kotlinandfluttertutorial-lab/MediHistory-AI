package com.medihistory.ai.data.history

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription

/**
 * Compiles a medical history from reviewed records.
 *
 * Design rules:
 * 1. DETERMINISTIC compilation — same input always produces the same output.
 * 2. Uses ONLY [OcrStatus.REVIEWED] prescriptions.
 * 3. NEVER invents data. Every entry carries at least one [SourceReference] pointing
 *    to the prescription it was compiled from.
 * 4. Separate from any AI summarisation — no AI model is called here.
 * 5. Never includes diagnoses, treatment recommendations, or clinical history.
 *
 * Medical safety: this object must never fabricate, infer, or imply:
 * - a diagnosis
 * - dosage instructions not present in the source record
 * - allergy information
 * - medicine status (status is patient-assigned only)
 * - any clinical recommendation
 */
object HistoryCompiler {

    /**
     * Compiles the three deterministic sections (medicines, doctors, timeline)
     * plus an AI_SUMMARY stub section.
     *
     * [prescriptions] is the full list for a patient; only [OcrStatus.REVIEWED] items
     * are used. [medicines] and [doctors] are filtered by the reviewed prescription IDs.
     */
    fun compile(
        prescriptions: List<Prescription>,
        medicines: List<Medicine>,
        doctors: List<Doctor>,
    ): List<HistorySection> {
        val reviewed = prescriptions
            .filter { it.ocrStatus == OcrStatus.REVIEWED }
            .sortedWith(
                compareByDescending<Prescription> { it.prescriptionDate }
                    .thenByDescending { it.createdAt },
            )

        val reviewedIds = reviewed.map { it.id }.toSet()

        // Only include medicines linked to a reviewed prescription.
        val reviewedMedicines = medicines
            .filter { it.prescriptionId in reviewedIds }
            .sortedBy { it.medicineName }

        // Doctors referenced by a reviewed prescription.
        val reviewedDoctorIds = reviewed.mapNotNull { it.doctorId }.toSet()
        val reviewedDoctors = doctors
            .filter { it.id in reviewedDoctorIds }
            .sortedBy { it.name }

        return listOf(
            compileMedicinesSection(reviewedMedicines, reviewed),
            compileDoctorsSection(reviewedDoctors, reviewed),
            compileTimelineSection(reviewed),
            compileAiSummaryStub(),
        )
    }

    // -------------------------------------------------------------------------
    // Section compilers
    // -------------------------------------------------------------------------

    /**
     * Builds the active medicines section from medicines not marked [MedicineStatus.PAST].
     *
     * Each entry text is derived only from fields present in the source record.
     * Null fields are omitted rather than inferred.
     */
    private fun compileMedicinesSection(
        medicines: List<Medicine>,
        reviewed: List<Prescription>,
    ): HistorySection {
        val activeMedicines = medicines.filter { it.status != MedicineStatus.PAST }

        val entries = activeMedicines.map { medicine ->
            val parts = mutableListOf<String>()
            parts += medicine.medicineName
            if (medicine.strength != null) parts += medicine.strength
            if (medicine.formulation != null) parts += "(${medicine.formulation})"
            if (medicine.dosageInstructions != null) parts += "— ${medicine.dosageInstructions}"
            if (medicine.status != MedicineStatus.UNKNOWN) {
                parts += "[${medicine.status.name.lowercase().replaceFirstChar { it.uppercase() }}]"
            }

            val contentType = when {
                reviewed.any { it.id == medicine.prescriptionId } -> ContentType.DOCUMENTED_FACT
                else -> ContentType.PATIENT_REPORTED
            }

            HistoryEntry(
                text = parts.joinToString(" "),
                contentType = contentType,
                sourceReferences = listOf(
                    SourceReference(
                        prescriptionId = medicine.prescriptionId,
                        fieldName = "medicineName",
                    ),
                ),
            )
        }

        return HistorySection(
            type = SectionType.MEDICINES,
            title = "Active Medicines",
            entries = entries,
        )
    }

    /**
     * Builds the doctors section.
     *
     * Each entry lists the doctor's name, specialty, and clinic (if present).
     * The source reference points to the most recent reviewed prescription
     * that references this doctor.
     */
    private fun compileDoctorsSection(
        doctors: List<Doctor>,
        reviewed: List<Prescription>,
    ): HistorySection {
        val entries = doctors.map { doctor ->
            val specialtyLabel = when {
                doctor.specialty.name == "OTHER" && doctor.specialtyOther != null ->
                    doctor.specialtyOther
                else ->
                    doctor.specialty.name
                        .lowercase()
                        .replace('_', ' ')
                        .replaceFirstChar { it.uppercase() }
            }

            val parts = mutableListOf(doctor.name, "— $specialtyLabel")
            if (doctor.clinicName != null) parts += "(${doctor.clinicName})"

            // Source: most recent reviewed prescription referencing this doctor.
            val sourcePrescription = reviewed
                .filter { it.doctorId == doctor.id }
                .maxByOrNull { it.createdAt }

            val sourceRefs = listOfNotNull(
                sourcePrescription?.let {
                    SourceReference(prescriptionId = it.id, fieldName = "doctorId")
                },
            )

            HistoryEntry(
                text = parts.joinToString(" "),
                contentType = ContentType.DOCUMENTED_FACT,
                sourceReferences = sourceRefs,
            )
        }

        return HistorySection(
            type = SectionType.DOCTORS,
            title = "Doctors",
            entries = entries,
        )
    }

    /**
     * Builds the chronological timeline from reviewed prescriptions.
     *
     * Each entry is a prescription date (when present) and its associated doctor.
     * Missing dates are omitted — never fabricated.
     */
    private fun compileTimelineSection(
        reviewed: List<Prescription>,
    ): HistorySection {
        val entries = reviewed.mapNotNull { prescription ->
            val datePart = prescription.prescriptionDate?.toString() ?: return@mapNotNull null
            val parts = mutableListOf<String>("Prescription: $datePart")
            if (prescription.notes != null) parts += "— ${prescription.notes}"

            HistoryEntry(
                text = parts.joinToString(" "),
                contentType = ContentType.DOCUMENTED_FACT,
                sourceReferences = listOf(
                    SourceReference(
                        prescriptionId = prescription.id,
                        fieldName = "prescriptionDate",
                    ),
                ),
            )
        }

        return HistorySection(
            type = SectionType.TIMELINE,
            title = "Prescription Timeline",
            entries = entries,
        )
    }

    /**
     * Returns a stub AI_SUMMARY section.
     *
     * Full AI summarisation requires a configured AI provider. Until that is available,
     * this stub informs the patient that the deterministic sections above were compiled
     * successfully.
     *
     * Medical safety: this stub MUST NOT contain clinical advice, diagnoses, treatment
     * recommendations, or any fabricated health information.
     */
    private fun compileAiSummaryStub(): HistorySection {
        val stubEntry = HistoryEntry(
            text = "AI summary generation requires a configured AI provider. " +
                "Reviewed records compiled above.",
            contentType = ContentType.DOCUMENTED_FACT,
            sourceReferences = emptyList(),
        )
        return HistorySection(
            type = SectionType.AI_SUMMARY,
            title = "AI Summary",
            entries = listOf(stubEntry),
            isAiGenerated = true,
        )
    }
}
