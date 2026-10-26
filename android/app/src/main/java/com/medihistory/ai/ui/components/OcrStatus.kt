package com.medihistory.ai.ui.components

/**
 * Represents the OCR processing and review lifecycle state of a prescription field or document.
 *
 * State transitions are enforced separately in the review state machine (ValidationUtils).
 * This enum drives the visual presentation of [StatusBadge].
 */
enum class OcrStatus {
    PENDING,
    PROCESSING,
    NEEDS_REVIEW,
    REVIEWED,
    ERROR,
}
