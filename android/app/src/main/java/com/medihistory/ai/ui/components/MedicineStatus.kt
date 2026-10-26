package com.medihistory.ai.ui.components

/**
 * Patient-assigned status for a medicine in the medicine explorer and prescription detail views.
 *
 * Status is ALWAYS patient-assigned. The app never infers or fabricates a status value.
 */
enum class MedicineStatus {
    CURRENT,
    PAST,
    UNKNOWN,
}
