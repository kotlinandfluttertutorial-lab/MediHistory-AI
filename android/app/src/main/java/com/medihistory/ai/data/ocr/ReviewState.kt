package com.medihistory.ai.data.ocr

/**
 * Review lifecycle state for a single OCR-extracted field.
 *
 * Valid transitions (enforced in ValidationUtils):
 *   PENDING_REVIEW → REVIEWED
 *   PENDING_REVIEW → REJECTED
 *   REVIEWED       → PENDING_REVIEW  (patient un-reviews)
 *   REJECTED       → PENDING_REVIEW  (patient un-rejects)
 *
 * No other transitions are valid.
 */
enum class ReviewState {
    PENDING_REVIEW,
    REVIEWED,
    REJECTED,
}
