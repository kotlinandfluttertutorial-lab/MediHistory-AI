package com.medihistory.ai.ui.dashboard

import com.medihistory.ai.ui.prescription.PrescriptionSummary

/**
 * UI state for [DashboardScreen].
 *
 * @param visitSummary  Pre-visit summary from [PrepareForVisitAgent] (F12). Null until generated.
 * @param isVisitSummaryLoading True while the agent is running.
 */
data class DashboardUiState(
    // Greeting section
    val patientName: String = "there",
    val currentDate: String = "",
    val syncStatus: String = "MediHistory AI care records sync is active",
    // Stats
    val prescriptionCount: Int = 0,
    val doctorCount: Int = 0,
    val medicineCount: Int = 0,
    val pendingAlertCount: Int = 0,
    val pendingOcrCount: Int = 0,
    val recentPrescriptions: List<PrescriptionSummary> = emptyList(),
    // Visit preparation (F12)
    val visitSummary: com.medihistory.ai.data.ai.VisitSummary? = null,
    val isVisitSummaryLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
