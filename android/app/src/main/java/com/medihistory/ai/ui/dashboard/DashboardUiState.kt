package com.medihistory.ai.ui.dashboard

import com.medihistory.ai.ui.prescription.PrescriptionSummary

/**
 * UI state for [DashboardScreen].
 *
 * All counts default to 0 so the screen renders correctly before data loads.
 *
 * @param prescriptionCount    Total prescriptions for the current patient.
 * @param doctorCount          Total doctors recorded for the current patient.
 * @param medicineCount        Total medicines recorded for the current patient.
 * @param pendingAlertCount    Unreviewed potential duplicate alerts.
 * @param pendingOcrCount      Prescriptions with OCR status PENDING or NEEDS_REVIEW.
 * @param recentPrescriptions  The 3 most recent prescriptions shown in "Recent activity".
 * @param isLoading            True while any data is being fetched.
 * @param error                Non-null when a fetch error occurred.
 */
data class DashboardUiState(
    val prescriptionCount: Int = 0,
    val doctorCount: Int = 0,
    val medicineCount: Int = 0,
    val pendingAlertCount: Int = 0,
    val pendingOcrCount: Int = 0,
    val recentPrescriptions: List<PrescriptionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
