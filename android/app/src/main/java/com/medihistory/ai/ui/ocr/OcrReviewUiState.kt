package com.medihistory.ai.ui.ocr

import com.medihistory.ai.data.ocr.ReviewState

/**
 * UI representation of a single OCR-extracted field during the review flow.
 *
 * Medical-safety invariants:
 * - [rawValue] is the original OCR output and MUST NEVER be changed by any user action.
 * - [patientValue] is set only when the patient explicitly confirms or edits the field.
 * - [displayValue] derives from [patientValue] first, then [rawValue], then "Not extracted".
 *   It NEVER fabricates a value — if both are null the field was absent from the source.
 * - [reviewState] must follow the state machine:
 *     PENDING_REVIEW → REVIEWED or REJECTED
 *     REVIEWED       → PENDING_REVIEW  (un-review)
 *     REJECTED       → PENDING_REVIEW  (un-reject)
 *
 * Privacy: [rawValue] and [patientValue] must never be logged.
 */
data class FieldReviewState(
    val fieldId: String,
    val fieldName: String,
    /** Original OCR output — NEVER changed by any patient action. */
    val rawValue: String?,
    /** Value explicitly confirmed or edited by the patient. Null until reviewed. */
    val patientValue: String?,
    /**
     * The value displayed to the patient.
     * Priority: patientValue → rawValue → "Not extracted".
     * NEVER fabricated — absent means absent.
     */
    val displayValue: String,
    /** OCR confidence in [0.0, 1.0]. Fields below 0.70 are highlighted amber. */
    val confidence: Float,
    val reviewState: ReviewState,
    /** True while the patient is actively editing this field. */
    val isEditing: Boolean = false,
    /** Buffer for the in-progress edit text. Seeded from patientValue or rawValue. */
    val editText: String = patientValue ?: rawValue ?: "",
)

/**
 * Overall UI state for [OcrReviewScreen].
 *
 * [allReviewed] becomes true once every field that has a [FieldReviewState.rawValue]
 * has been moved out of [ReviewState.PENDING_REVIEW]. When true, the prescription's
 * [com.medihistory.ai.data.model.OcrStatus] is updated to REVIEWED in the ViewModel.
 */
data class OcrReviewUiState(
    val prescriptionId: String = "",
    val doctorName: String = "",
    val fields: List<FieldReviewState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val allReviewed: Boolean = false,
) {
    /**
     * Number of fields that are still [ReviewState.PENDING_REVIEW] and have a
     * non-null [FieldReviewState.rawValue] (fields with null rawValue need no review).
     */
    val pendingCount: Int
        get() = fields.count { it.reviewState == ReviewState.PENDING_REVIEW && it.rawValue != null }
}
