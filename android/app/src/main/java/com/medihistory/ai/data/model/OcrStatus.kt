package com.medihistory.ai.data.model

/**
 * OCR processing and review lifecycle state for a prescription document.
 *
 * Stored in Room as a TEXT column via [com.medihistory.ai.data.database.Converters].
 * Valid state transitions are enforced in ValidationUtils (not here).
 */
enum class OcrStatus {
    /** Document has been added but OCR has not been queued yet. */
    PENDING,
    /** OCR WorkManager job is running. */
    PROCESSING,
    /** OCR completed; one or more fields require patient review. */
    NEEDS_REVIEW,
    /** All extracted fields have been reviewed by the patient. */
    REVIEWED,
    /** OCR service returned an error or the job failed. */
    ERROR,
}
