package com.medihistory.ai.ui.duplicate

import com.medihistory.ai.data.model.DuplicateAlert

/**
 * Represents a single duplicate alert enriched with medicine display names and
 * prescription source IDs for the review screen.
 *
 * Medical safety: this item type never claims that the combination is safe or approved.
 * Labels must always use "Potential duplicate" — never "Duplicate medication" or a
 * clinical term.
 *
 * Privacy: do not log any field of this class — it contains patient medicine data.
 *
 * @param alert           The underlying [DuplicateAlert] entity from Room.
 * @param medicineAName   Display name of the first medicine in the pair.
 * @param medicineBName   Display name of the second medicine in the pair.
 * @param prescriptionAId FK to the source prescription for medicine A. Null if not linked.
 * @param prescriptionBId FK to the source prescription for medicine B. Null if not linked.
 * @param isExpanded      Whether the card is currently expanded to show details.
 */
data class DuplicateAlertItem(
    val alert: DuplicateAlert,
    val medicineAName: String,
    val medicineBName: String,
    val prescriptionAId: String?,
    val prescriptionBId: String?,
    val isExpanded: Boolean = false,
)

/**
 * UI state for [DuplicateReviewScreen].
 *
 * [alerts] is the full list of alert items for the current patient, potentially filtered
 * by [showReviewedAlerts].
 * [unreviewedCount] is derived — the count of alerts the patient has not yet seen.
 *
 * Privacy: do not log any field — it contains patient medicine data.
 */
data class DuplicateReviewUiState(
    val alerts: List<DuplicateAlertItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showReviewedAlerts: Boolean = true,
) {
    /** Number of alerts the patient has not yet reviewed. */
    val unreviewedCount: Int get() = alerts.count { !it.alert.isReviewed }
}
