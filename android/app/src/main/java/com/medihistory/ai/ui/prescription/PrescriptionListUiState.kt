package com.medihistory.ai.ui.prescription

/**
 * UI state for the prescription list screen.
 *
 * @param prescriptions Ordered list of prescriptions to display. Empty until loaded.
 * @param isLoading     True while the initial data load is in progress.
 * @param error         Non-null when an error occurred during load; null otherwise.
 */
data class PrescriptionListUiState(
    val prescriptions: List<PrescriptionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
