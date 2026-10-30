package com.medihistory.ai.data.ocr

/**
 * Interface for [PostReviewProcessor] — extracted so tests can inject a no-op without
 * depending on Room DAOs and repositories.
 */
interface PostReviewProcessorInterface {
    suspend fun process(
        prescriptionId: String,
        patientId: String,
    )
}
