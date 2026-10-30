package com.medihistory.ai.data.privacy

import android.content.Context
import com.medihistory.ai.data.database.MediHistoryDatabase
import com.medihistory.ai.data.security.SecurePreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Permanently deletes all data belonging to a patient from the device.
 *
 * Privacy invariants:
 * - NEVER silently succeeds — always returns a [DeletionResult] with counts.
 * - Steps are performed in order: Room data → files → preferences.
 * - Callers must navigate to login/onboarding AFTER this function completes,
 *   leaving no residual patient data in memory.
 * - Never logs patient data, medicine names, or prescription contents.
 *
 * Security invariants:
 * - Preference clear removes ALL sensitive values including the access token.
 * - Prescription files are deleted from [Context.getFilesDir]/prescriptions/.
 * - After this call the patient has no data on the device; the app must
 *   return to the onboarding / login screen.
 */
object DataDeletion {
    /**
     * Permanently deletes all data for [patientId].
     *
     * Steps:
     * 1. Collect counts of Room records to be deleted (for the result).
     * 2. Delete the Patient row — Room cascade deletes all child records
     *    (prescriptions, medicines, extracted fields, doctors, duplicate alerts).
     * 3. Delete all files in [Context.getFilesDir]/prescriptions/.
     * 4. Clear [SecurePreferences].
     *
     * @param context           Application context — used only to resolve filesDir.
     * @param patientId         The patient whose data is being erased.
     * @param database          Room database instance.
     * @param securePreferences Encrypted preference store to clear.
     * @return [DeletionResult] with counts of deleted items.
     */
    suspend fun deleteAllData(
        context: Context,
        patientId: String,
        database: MediHistoryDatabase,
        securePreferences: SecurePreferencesStore,
    ): DeletionResult =
        withContext(Dispatchers.IO) {
            // ── Step 1: Count records before deletion ─────────────────────────────
            val prescriptions =
                database.prescriptionDao()
                    .observeByPatient(patientId)
                    .first()
            val prescriptionCount = prescriptions.size

            val medicineCount =
                database.medicineDao()
                    .observeByPatient(patientId)
                    .first()
                    .size

            // ── Step 2: Delete Room data ─────────────────────────────────────────
            // Delete all prescriptions for the patient — cascade deletes
            // extracted_fields and medicines automatically via FK CASCADE.
            for (prescription in prescriptions) {
                database.prescriptionDao().delete(prescription)
            }

            // Delete all doctors for the patient
            val doctors =
                database.doctorDao()
                    .observeByPatient(patientId)
                    .first()
            for (doctor in doctors) {
                database.doctorDao().delete(doctor)
            }

            // Delete all duplicate alerts for the patient
            database.duplicateAlertDao().deleteByPatient(patientId)

            // Delete the patient record itself
            val patient = database.patientDao().getById(patientId)
            if (patient != null) {
                database.patientDao().delete(patient)
            }

            // ── Step 3: Delete prescription document files ────────────────────────
            val prescriptionsDir = File(context.filesDir, "prescriptions")
            var filesDeleted = 0
            if (prescriptionsDir.exists() && prescriptionsDir.isDirectory) {
                val files = prescriptionsDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.delete()) {
                        filesDeleted++
                    }
                }
            }

            // ── Step 4: Clear encrypted preferences ──────────────────────────────
            securePreferences.clear()

            DeletionResult(
                prescriptionsDeleted = prescriptionCount,
                medicinesDeleted = medicineCount,
                filesDeleted = filesDeleted,
                preferencesCleared = true,
            )
        }

    /**
     * Result of a [deleteAllData] call.
     *
     * All counts reflect the items that existed at the time deletion was initiated.
     * [preferencesCleared] is always true on successful completion.
     */
    data class DeletionResult(
        /** Number of prescription records removed from the database. */
        val prescriptionsDeleted: Int,
        /** Number of medicine records removed from the database. */
        val medicinesDeleted: Int,
        /** Number of document files deleted from app-private storage. */
        val filesDeleted: Int,
        /** True when [SecurePreferences.clear] completed without error. */
        val preferencesCleared: Boolean,
    )
}
