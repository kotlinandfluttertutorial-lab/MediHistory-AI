package com.medihistory.ai.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.dao.PrescriptionDao
import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.network.MediHistoryApiService
import com.medihistory.ai.data.ocr.OcrService
import com.medihistory.ai.data.ocr.ReviewState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager job that runs OCR on a prescription document and persists the results.
 *
 * Medical-safety invariants:
 * - NEVER log [documentPath], [prescriptionId], or any extracted field values.
 * - All extracted fields are persisted with [ReviewState.PENDING_REVIEW]; the patient
 *   must review every field before it is treated as verified.
 * - On any failure, [OcrStatus.ERROR] is set so the UI can prompt the patient to retry.
 *
 * Security: [patientId] is required input — it scopes the [PrescriptionDao.updateOcrStatus]
 * call so that a rogue job cannot update records belonging to another patient.
 *
 * Inject via [buildRequest]; do not instantiate directly.
 */
@HiltWorker
class OcrWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val ocrService: OcrService,
        private val prescriptionDao: PrescriptionDao,
        private val extractedFieldDao: ExtractedFieldDao,
        /** Backend API — null when BACKEND_BASE_URL is blank or NetworkModule not configured. */
        private val apiService: MediHistoryApiService?,
    ) : CoroutineWorker(context, params) {
        companion object {
            const val KEY_PRESCRIPTION_ID = "prescription_id"
            const val KEY_PATIENT_ID = "patient_id"
            const val KEY_DOCUMENT_PATH = "document_path"
            const val KEY_MIME_TYPE = "mime_type"

            /**
             * Builds a [OneTimeWorkRequest] for this worker.
             *
             * @param prescriptionId ID of the prescription entity to process.
             * @param patientId ID of the patient who owns this prescription (for DAO scoping).
             * @param documentPath Absolute path to the document in app-private storage.
             * @param mimeType MIME type of the document file.
             */
            fun buildRequest(
                prescriptionId: String,
                patientId: String,
                documentPath: String,
                mimeType: String,
            ): OneTimeWorkRequest =
                OneTimeWorkRequestBuilder<OcrWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setInputData(
                        workDataOf(
                            KEY_PRESCRIPTION_ID to prescriptionId,
                            KEY_PATIENT_ID to patientId,
                            KEY_DOCUMENT_PATH to documentPath,
                            KEY_MIME_TYPE to mimeType,
                        ),
                    )
                    .setConstraints(Constraints(requiresStorageNotLow = true))
                    .build()
        }

        override suspend fun doWork(): Result {
            val prescriptionId = inputData.getString(KEY_PRESCRIPTION_ID) ?: return Result.failure()
            val patientId = inputData.getString(KEY_PATIENT_ID) ?: return Result.failure()
            val documentPath = inputData.getString(KEY_DOCUMENT_PATH) ?: return Result.failure()
            val mimeType = inputData.getString(KEY_MIME_TYPE) ?: return Result.failure()

            // Mark as processing before calling the service.
            // NEVER log prescriptionId, documentPath, or patientId here.
            prescriptionDao.updateOcrStatus(prescriptionId, patientId, OcrStatus.PROCESSING)

            return try {
                // ── Backend OCR (backend-first, on-device fallback) ─────────────
                // Try the backend server first — Gemini key lives in the server env,
                // not in the APK. Falls back to on-device ML Kit on 503 or network error.
                val backendFields = tryBackendOcr(prescriptionId)
                val extractedFields: List<ExtractedFieldEntity>
                val newStatus: OcrStatus

                if (backendFields != null) {
                    extractedFields = backendFields
                    newStatus = if (backendFields.any { it.rawValue != null }) {
                        OcrStatus.NEEDS_REVIEW
                    } else {
                        OcrStatus.REVIEWED
                    }
                } else {
                    // Fallback: on-device ML Kit
                    val result = ocrService.extract(documentPath, mimeType, prescriptionId)
                    if (!result.isSuccess) {
                        prescriptionDao.updateOcrStatus(prescriptionId, patientId, OcrStatus.ERROR)
                        return Result.failure()
                    }
                    extractedFields = result.fields.map { field ->
                        ExtractedFieldEntity(
                            id = "$prescriptionId-${field.fieldName}",
                            prescriptionId = prescriptionId,
                            fieldName = field.fieldName,
                            rawValue = field.rawValue,
                            patientValue = null,
                            confidence = field.confidence,
                            reviewState = ReviewState.PENDING_REVIEW,
                            sourceReference = field.sourceReference,
                        )
                    }
                    newStatus = if (result.pendingReviewCount > 0) OcrStatus.NEEDS_REVIEW
                    else OcrStatus.REVIEWED
                }

                extractedFieldDao.insertAll(extractedFields)
                prescriptionDao.updateOcrStatus(prescriptionId, patientId, newStatus)
                Result.success()
            } catch (e: Exception) {
                // Catch-all: record the failure. NEVER log exception message if it could
                // contain document content, medicine names, or patient identifiers.
                prescriptionDao.updateOcrStatus(prescriptionId, patientId, OcrStatus.ERROR)
                Result.failure()
            }
        }

        /**
         * Attempts to run OCR via the backend server.
         * Returns mapped [ExtractedFieldEntity] rows on HTTP 200, or null on 503/error.
         * Privacy: field values are transmitted over HTTPS; nothing is logged here.
         */
        private suspend fun tryBackendOcr(prescriptionId: String): List<ExtractedFieldEntity>? {
            val service = apiService ?: return null
            return try {
                val response = service.getOcrFields(prescriptionId)
                if (!response.isSuccessful || response.code() == 503) return null
                val body = response.body() ?: return null
                body.fields.map { field ->
                    ExtractedFieldEntity(
                        id            = "$prescriptionId-${field.fieldName}",
                        prescriptionId = prescriptionId,
                        fieldName     = field.fieldName,
                        rawValue      = field.rawValue,
                        patientValue  = null,
                        confidence    = field.confidence,
                        reviewState   = ReviewState.PENDING_REVIEW,
                        sourceReference = field.sourceReference,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                null // network unavailable — fall back to on-device OCR
            }
        }
    }
