package com.medihistory.ai.ui.reminders

import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.data.reminders.ReminderSuggestion

/**
 * UI state for [ReminderScreen].
 *
 * [reminders] is the list of reminders for the current patient.
 * [availableMedicines] is the list of medicines shown in the medicine-picker dialog.
 * [showMedicinePicker] controls visibility of the medicine-selection dialog.
 * [isLoading] is true while the initial load is in progress.
 * [error] is set on repository failures and cleared on the next successful load.
 * [pendingReminderMedicine] is the medicine for which the advisor dialog is shown (F16).
 * [reminderSuggestion] is the suggested reminder times from [ReminderAdvisorEngine] (F16).
 * [showReminderAdvisor] controls visibility of the reminder advisor dialog (F16).
 *
 * Privacy: do not log any field of this state — it contains patient health data.
 */
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    val availableMedicines: List<Medicine> = emptyList(),
    val showMedicinePicker: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** The medicine for which the advisor is currently showing time suggestions (F16). */
    val pendingReminderMedicine: Medicine? = null,
    /** Suggested reminder times from [ReminderAdvisorEngine]. Null when no match. */
    val reminderSuggestion: ReminderSuggestion? = null,
    /** Controls visibility of the reminder advisor dialog (F16). */
    val showReminderAdvisor: Boolean = false,
)
