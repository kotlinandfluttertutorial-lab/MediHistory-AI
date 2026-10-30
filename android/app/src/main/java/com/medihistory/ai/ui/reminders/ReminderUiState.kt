package com.medihistory.ai.ui.reminders

import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Reminder

/**
 * UI state for [ReminderScreen].
 *
 * [reminders] is the list of reminders for the current patient.
 * [availableMedicines] is the list of medicines shown in the medicine-picker dialog.
 * [showMedicinePicker] controls visibility of the medicine-selection dialog.
 * [isLoading] is true while the initial load is in progress.
 * [error] is set on repository failures and cleared on the next successful load.
 *
 * Privacy: do not log any field of this state — it contains patient health data.
 */
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    val availableMedicines: List<Medicine> = emptyList(),
    val showMedicinePicker: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)
