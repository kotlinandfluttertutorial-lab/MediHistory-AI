package com.medihistory.ai.data.model

/**
 * OCR processing and review lifecycle state for a prescription document.
 *
 * This data-layer enum is separate from [com.medihistory.ai.ui.components.OcrStatus] so that
 * the data layer has no dependency on UI code. The UI layer maps between the two by name.
 *
 * Valid state transitions are enforced in ValidationUtils (not here).
 * Stored in Room as a TEXT column via [com.medihistory.ai.data.database.Converters].
 */
enum class OcrStatus {
    PENDING,
    PROCESSING,
    NEEDS_REVIEW,
    REVIEWED,
    ERROR,
}
