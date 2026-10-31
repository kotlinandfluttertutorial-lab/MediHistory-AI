package com.medihistory.ai.data.privacy

import android.content.Context
import com.medihistory.ai.data.database.MediHistoryDatabase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Exports all data owned by a patient to a JSON file in app-private cache storage.
 *
 * Privacy invariants:
 * - Output file is written to [Context.getCacheDir] — app-private, not accessible to
 *   other apps directly. The caller must use a FileProvider to share the file.
 * - Never logs patient data, medicine names, prescription text, or document paths.
 * - The exported file contains only data the patient themselves entered or confirmed.
 * - [patientId] is included in the file for record tracing but no external PII is added.
 *
 * JSON structure:
 * ```json
 * {
 *   "patientId": "...",
 *   "exportedAt": "ISO-8601 timestamp",
 *   "prescriptions": [...],
 *   "medicines": [...],
 *   "doctors": [...],
 *   "reminders": []
 * }
 * ```
 *
 * SYNTHETIC DATA NOTE: In tests, use synthetic patientId values (e.g. "test-patient-alpha").
 * No real patient PII must appear in test fixtures.
 */
object DataExporter {
    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC)

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_INSTANT

    /**
     * Exports all patient data as a JSON file to [Context.getCacheDir].
     *
     * @param context   Application context — used only to resolve cacheDir.
     * @param patientId The patient whose data is being exported.
     * @param database  Room database to query.
     * @return The exported file in app-private cache storage.
     *
     * The returned [File] must be shared via a FileProvider URI, not exposed directly.
     */
    suspend fun export(
        context: Context,
        patientId: String,
        database: MediHistoryDatabase,
    ): File {
        val now = Instant.now()
        val timestamp = timestampFormatter.format(now)
        val exportedAt = isoFormatter.format(now)

        val prescriptions =
            database.prescriptionDao()
                .observeByPatient(patientId)
                .first()

        val medicines =
            database.medicineDao()
                .observeByPatient(patientId)
                .first()

        val doctors =
            database.doctorDao()
                .observeByPatient(patientId)
                .first()

        val prescriptionsArray =
            JSONArray().apply {
                for (prescription in prescriptions) {
                    put(
                        JSONObject().apply {
                            put("id", prescription.id)
                            put("patientId", prescription.patientId)
                            put("doctorId", prescription.doctorId ?: JSONObject.NULL)
                            put("prescriptionDate", prescription.prescriptionDate?.toString() ?: JSONObject.NULL)
                            put("notes", prescription.notes ?: JSONObject.NULL)
                            put("documentMimeType", prescription.documentMimeType ?: JSONObject.NULL)
                            put("documentHashSha256", prescription.documentHashSha256 ?: JSONObject.NULL)
                            put("ocrStatus", prescription.ocrStatus.name)
                            put("createdAt", prescription.createdAt)
                            put("updatedAt", prescription.updatedAt)
                            // documentPath intentionally excluded — internal path is not useful externally
                        },
                    )
                }
            }

        val medicinesArray =
            JSONArray().apply {
                for (medicine in medicines) {
                    put(
                        JSONObject().apply {
                            put("id", medicine.id)
                            put("prescriptionId", medicine.prescriptionId)
                            put("patientId", medicine.patientId)
                            put("medicineName", medicine.medicineName)
                            put("activeIngredient", medicine.activeIngredient ?: JSONObject.NULL)
                            put("brandName", medicine.brandName ?: JSONObject.NULL)
                            put("strength", medicine.strength ?: JSONObject.NULL)
                            put("formulation", medicine.formulation ?: JSONObject.NULL)
                            put("dosageInstructions", medicine.dosageInstructions ?: JSONObject.NULL)
                            put("prescribingDoctorId", medicine.prescribingDoctorId ?: JSONObject.NULL)
                            put("prescriptionDate", medicine.prescriptionDate?.toString() ?: JSONObject.NULL)
                            put("status", medicine.status.name)
                            put("createdAt", medicine.createdAt)
                            put("updatedAt", medicine.updatedAt)
                        },
                    )
                }
            }

        val doctorsArray =
            JSONArray().apply {
                for (doctor in doctors) {
                    put(
                        JSONObject().apply {
                            put("id", doctor.id)
                            put("patientId", doctor.patientId)
                            put("name", doctor.name)
                            put("specialty", doctor.specialty.name)
                            put("specialtyOther", doctor.specialtyOther ?: JSONObject.NULL)
                            put("clinicName", doctor.clinicName ?: JSONObject.NULL)
                            put("contactNotes", doctor.contactNotes ?: JSONObject.NULL)
                            put("createdAt", doctor.createdAt)
                            put("updatedAt", doctor.updatedAt)
                        },
                    )
                }
            }

        val exportJson =
            JSONObject().apply {
                put("patientId", patientId)
                put("exportedAt", exportedAt)
                put("prescriptions", prescriptionsArray)
                put("medicines", medicinesArray)
                put("doctors", doctorsArray)
                put("reminders", JSONArray()) // placeholder — reminders not yet implemented
            }

        // Write to app-private cache — never write to shared external storage
        val outputFile = File(context.cacheDir, "mha_export_${patientId}_$timestamp.json")
        outputFile.writeText(exportJson.toString(2))

        return outputFile
    }
}
