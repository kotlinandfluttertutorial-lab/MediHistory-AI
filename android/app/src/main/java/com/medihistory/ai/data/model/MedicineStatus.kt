package com.medihistory.ai.data.model

/**
 * Patient-assigned status for a medicine in the data layer.
 *
 * This enum lives in the data layer and is persisted to the Room database.
 * The corresponding UI enum [com.medihistory.ai.ui.components.MedicineStatus] mirrors
 * these values for display purposes; they are kept in separate packages intentionally.
 *
 * Status is ALWAYS patient-assigned. The app never infers or fabricates a status value.
 */
enum class MedicineStatus {
    CURRENT,
    PAST,
    UNKNOWN,
}
