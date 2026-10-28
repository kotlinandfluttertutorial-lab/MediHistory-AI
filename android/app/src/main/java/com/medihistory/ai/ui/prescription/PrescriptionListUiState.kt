package com.medihistory.ai.ui.prescription

/**
 * A doctor's prescriptions grouped together for display.
 *
 * @param doctorName   Display name of the prescribing doctor.
 * @param specialty    Doctor's specialty (shown as a chip).
 * @param prescriptions  All prescriptions from this doctor.
 * @param isExpanded   Whether the group's prescription cards are visible.
 */
data class DoctorGroup(
    val doctorName: String,
    val specialty: String = "",
    val prescriptions: List<PrescriptionSummary>,
    val isExpanded: Boolean = true,
)

/**
 * UI state for the prescription list screen.
 *
 * @param prescriptions  Flat ordered list (kept for backward compat with tests).
 * @param doctorGroups   Prescriptions grouped by doctor for the grouped layout.
 * @param isLoading      True while the initial data load is in progress.
 * @param error          Non-null when an error occurred during load; null otherwise.
 */
data class PrescriptionListUiState(
    val prescriptions: List<PrescriptionSummary> = emptyList(),
    val doctorGroups: List<DoctorGroup> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
