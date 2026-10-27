package com.medihistory.ai.data.ocr

/**
 * A single field extracted from a prescription document by the OCR service.
 *
 * Medical-safety invariants:
 * - [rawValue] is null when the field was NOT found in the source document.
 *   A null rawValue must NEVER be replaced with a fabricated or inferred value.
 * - [patientValue] is set only when the patient explicitly confirms or edits the field.
 * - [confidence] is in [0.0, 1.0]. Fields below the configured threshold must be
 *   visually highlighted and require patient review before use.
 * - [reviewState] starts as [ReviewState.PENDING_REVIEW] for every extracted field.
 * - [sourceReference] links this field back to its location in the source document
 *   (e.g. page number, bounding-box region). Never null-checked away — store as-is.
 *
 * Privacy: Never log [rawValue] or [patientValue] in Logcat or server logs.
 */
data class ExtractedField(
    /** Name of the field (e.g. "doctor_name", "medicine_name", "dose"). */
    val fieldName: String,
    /**
     * The raw text extracted by OCR from the source document.
     * Null means the field was absent from the document — do not infer a value.
     */
    val rawValue: String?,
    /**
     * The value confirmed or edited by the patient during the review flow.
     * Null until the patient has reviewed and saved this field.
     */
    val patientValue: String?,
    /**
     * OCR confidence for this field, in the range [0.0, 1.0].
     * 0.0 means no confidence (field absent or unreadable).
     * 1.0 means the OCR engine is fully confident.
     */
    val confidence: Float,
    /** Current review state. Starts as [ReviewState.PENDING_REVIEW] after OCR. */
    val reviewState: ReviewState = ReviewState.PENDING_REVIEW,
    /** Page or region reference in the source document, for traceability. */
    val sourceReference: String? = null,
) {
    init {
        require(confidence in 0f..1f) {
            "confidence must be in range 0.0–1.0 but was $confidence"
        }
    }
}
