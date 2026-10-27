package com.medihistory.ai.data.ocr

/**
 * The result of an OCR extraction pass on a single prescription document.
 *
 * Medical-safety invariants:
 * - [fields] contains one [ExtractedField] per detected field. Fields absent from the
 *   source document are represented with [ExtractedField.rawValue] == null — they are
 *   NEVER fabricated or inferred.
 * - [errorMessage] is non-null only when the OCR service failed entirely. Partial
 *   low-confidence results are still a success; confidence is on each [ExtractedField].
 *
 * Privacy: Never log [fields] contents, document paths, or patient identifiers.
 */
data class OcrResult(
    /** ID of the [com.medihistory.ai.data.model.Prescription] this result belongs to. */
    val prescriptionId: String,
    /** All fields extracted (or found absent) in the source document. */
    val fields: List<ExtractedField>,
    /** Aggregate confidence across all extracted fields, in [0.0, 1.0]. */
    val overallConfidence: Float,
    /**
     * Non-null when the OCR service could not process the document at all.
     * Null on a successful (even low-confidence) extraction.
     * Never contains prescription text, patient names, or document content.
     */
    val errorMessage: String? = null,
) {
    /** True when the OCR service completed without a fatal error. */
    val isSuccess: Boolean get() = errorMessage == null

    /**
     * Number of fields that still require patient review.
     * Only counts fields where [ExtractedField.rawValue] is non-null (i.e. the field
     * was actually found in the document) and the state is still [ReviewState.PENDING_REVIEW].
     * Fields with null rawValue are absent — there is nothing for the patient to review.
     */
    val pendingReviewCount: Int
        get() = fields.count {
            it.reviewState == ReviewState.PENDING_REVIEW && it.rawValue != null
        }
}
