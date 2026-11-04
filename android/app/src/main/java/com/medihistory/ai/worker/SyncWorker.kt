package com.medihistory.ai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.medihistory.ai.data.dao.DoctorDao
import com.medihistory.ai.data.dao.MedicineDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.dao.SyncQueueDao
import com.medihistory.ai.data.network.MediHistoryApiService
import com.medihistory.ai.data.network.dto.SyncDoctorDto
import com.medihistory.ai.data.network.dto.SyncDoctorsRequest
import com.medihistory.ai.data.network.dto.SyncMedicineDto
import com.medihistory.ai.data.network.dto.SyncMedicinesRequest
import com.medihistory.ai.data.network.dto.SyncPrescriptionDto
import com.medihistory.ai.data.network.dto.SyncPrescriptionsRequest
import com.medihistory.ai.data.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that pushes dirty local Room entities to the FastAPI backend.
 *
 * Runs every 15 minutes when the device has network connectivity.
 * Uses the sync_queue table as an outbox: each new/updated entity gets an entry,
 * this worker reads the queue, calls the backend upsert endpoints, and removes
 * successfully synced rows.
 *
 * On 401 (token expired): returns [Result.failure] — the session has ended.
 * On 4xx/5xx other than 401: increments retry count; drops after 5 failures.
 * On network error: returns [Result.retry] with exponential backoff.
 *
 * Privacy / security:
 * - Entity IDs and field values are NEVER logged.
 * - The bearer token is added by [AuthInterceptor] — never handled here.
 * - Cross-patient data cannot be sent because the backend scopes all upserts to
 *   the authenticated patient's JWT claim.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val prescriptionDao: PrescriptionDao,
    private val doctorDao: DoctorDao,
    private val medicineDao: MedicineDao,
    private val apiService: MediHistoryApiService,
    private val sessionManager: SessionManager,
) : CoroutineWorker(context, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "medihistory_sync"
        const val MAX_RETRIES = 5

        /**
         * Enqueues the periodic sync job.
         * Call this from [MainActivity] after successful authentication.
         * [ExistingPeriodicWorkPolicy.KEEP] means a running job is never replaced.
         */
        fun schedule(workManager: WorkManager) {
            val request: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        val patientId = sessionManager.getPatientId()

        // Drop exhausted entries before processing
        syncQueueDao.dropExhausted(MAX_RETRIES)

        val pending = syncQueueDao.getPending(patientId)
        if (pending.isEmpty()) return Result.success()

        // Group by entity type for batched API calls
        val prescriptionIds = pending.filter { it.entityType == "PRESCRIPTION" }
        val doctorIds       = pending.filter { it.entityType == "DOCTOR" }
        val medicineIds     = pending.filter { it.entityType == "MEDICINE" }

        var anyRetry = false

        // ── Prescriptions ─────────────────────────────────────────────────────
        if (prescriptionIds.isNotEmpty()) {
            val prescriptions = prescriptionIds.mapNotNull { entry ->
                prescriptionDao.getById(entry.entityId, patientId)?.let { p ->
                    SyncPrescriptionDto(
                        id               = p.id,
                        doctorId         = p.doctorId,
                        prescriptionDate = p.prescriptionDate?.toString(),
                        notes            = p.notes,
                        ocrStatus        = p.ocrStatus.name,
                        createdAt        = p.createdAt,
                        updatedAt        = p.updatedAt,
                    )
                }
            }
            if (prescriptions.isNotEmpty()) {
                val response = runCatching {
                    apiService.syncPrescriptions(SyncPrescriptionsRequest(prescriptions))
                }.getOrNull()

                when {
                    response == null -> anyRetry = true   // network error
                    response.code() == 401 -> return Result.failure()
                    response.isSuccessful -> prescriptionIds.forEach { syncQueueDao.delete(it.id) }
                    else -> { prescriptionIds.forEach { syncQueueDao.incrementRetry(it.id) }; anyRetry = true }
                }
            } else {
                // Entities no longer exist — remove stale queue entries
                prescriptionIds.forEach { syncQueueDao.delete(it.id) }
            }
        }

        // ── Doctors ───────────────────────────────────────────────────────────
        if (doctorIds.isNotEmpty()) {
            val doctors = doctorIds.mapNotNull { entry ->
                doctorDao.getById(entry.entityId, patientId)?.let { d ->
                    SyncDoctorDto(
                        id             = d.id,
                        name           = d.name,
                        specialty      = d.specialty.name,
                        specialtyOther = d.specialtyOther,
                        clinicName     = d.clinicName,
                        createdAt      = d.createdAt,
                        updatedAt      = d.updatedAt,
                    )
                }
            }
            if (doctors.isNotEmpty()) {
                val response = runCatching {
                    apiService.syncDoctors(SyncDoctorsRequest(doctors))
                }.getOrNull()

                when {
                    response == null -> anyRetry = true
                    response.code() == 401 -> return Result.failure()
                    response.isSuccessful -> doctorIds.forEach { syncQueueDao.delete(it.id) }
                    else -> { doctorIds.forEach { syncQueueDao.incrementRetry(it.id) }; anyRetry = true }
                }
            } else {
                doctorIds.forEach { syncQueueDao.delete(it.id) }
            }
        }

        // ── Medicines ─────────────────────────────────────────────────────────
        if (medicineIds.isNotEmpty()) {
            val medicines = medicineIds.mapNotNull { entry ->
                medicineDao.getById(entry.entityId, patientId)?.let { m ->
                    SyncMedicineDto(
                        id                  = m.id,
                        prescriptionId      = m.prescriptionId,
                        medicineName        = m.medicineName,
                        activeIngredient    = m.activeIngredient,
                        strength            = m.strength,
                        dosageInstructions  = m.dosageInstructions,
                        status              = m.status.name,
                        createdAt           = m.createdAt,
                        updatedAt           = m.updatedAt,
                    )
                }
            }
            if (medicines.isNotEmpty()) {
                val response = runCatching {
                    apiService.syncMedicines(SyncMedicinesRequest(medicines))
                }.getOrNull()

                when {
                    response == null -> anyRetry = true
                    response.code() == 401 -> return Result.failure()
                    response.isSuccessful -> medicineIds.forEach { syncQueueDao.delete(it.id) }
                    else -> { medicineIds.forEach { syncQueueDao.incrementRetry(it.id) }; anyRetry = true }
                }
            } else {
                medicineIds.forEach { syncQueueDao.delete(it.id) }
            }
        }

        return if (anyRetry) Result.retry() else Result.success()
    }
}
