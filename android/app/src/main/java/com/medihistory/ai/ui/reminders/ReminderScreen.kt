package com.medihistory.ai.ui.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Reminders screen — top-level destination for feature F08 (Reminders and adherence tracking).
 *
 * Shows all medication reminders for [patientId]. Each reminder card displays the medicine
 * name, scheduled time, active toggle, and Taken/Skipped quick-action buttons.
 *
 * Safety invariants:
 * - Prescribed instructions ([Reminder.prescribedInstructions]) are labelled "Prescribed:"
 *   and are always visually separated from the dose recording buttons.
 * - Dose recording (Taken / Skipped) is patient-reported and never shown as a prescription fact.
 * - Reminders are opt-in. The empty state directs the patient to the medicine detail screen
 *   where they can create reminders explicitly.
 * - Every reminder can be cancelled (deleted) from this screen.
 *
 * Accessibility:
 * - All interactive elements have a minimum 48×48 dp touch target via Button/Switch defaults.
 * - contentDescription is set on the active toggle Switch.
 * - The screen title is announced by TalkBack via the TopAppBar.
 *
 * @param patientId  The patient whose reminders are shown.
 * @param viewModel  Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    patientId: String,
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        viewModel.loadReminders(patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = "Reminders") })
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentDescription = "Loading reminders",
                )
            }

            uiState.error != null -> {
                EmptyStateView(
                    message = uiState.error ?: "An error occurred.",
                    actionLabel = "Retry",
                    onAction = { viewModel.loadReminders(patientId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.reminders.isEmpty() -> {
                EmptyStateView(
                    message = "No reminders set",
                    actionLabel = "Add reminders from the medicine detail screen.",
                    onAction = { /* Navigation to medicine detail is triggered by the caller */ },
                    icon = Icons.Outlined.NotificationsOff,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                ) {
                    items(
                        items = uiState.reminders,
                        key = { reminder -> reminder.id },
                    ) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onToggleActive = { /* Active toggle — wired in a follow-up ticket */ },
                            onRecordDose = { doseRecord ->
                                viewModel.recordDose(
                                    reminderId = reminder.id,
                                    patientId = patientId,
                                    doseRecord = doseRecord,
                                )
                            },
                            onDelete = { viewModel.deleteReminder(reminder) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card representing a single [Reminder].
 *
 * Safety layout:
 * - "Prescribed:" section is visually distinct and labelled — never mixed with dose buttons.
 * - Dose buttons (Taken / Skipped) are patient-action only; their labels make clear they
 *   record what the patient did, not what was prescribed.
 *
 * Accessibility: the active toggle Switch has a contentDescription including the medicine name.
 *
 * @param reminder       The reminder to display.
 * @param onToggleActive Called when the patient toggles the active switch.
 * @param onRecordDose   Called with [DoseRecord.TAKEN] or [DoseRecord.SKIPPED] when tapped.
 * @param onDelete       Called when the patient removes the reminder.
 * @param modifier       Modifier applied to the card.
 */
@Composable
fun ReminderCard(
    reminder: Reminder,
    onToggleActive: (Boolean) -> Unit,
    onRecordDose: (DoseRecord) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row: medicine name + active toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reminder.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.semantics {
                        contentDescription =
                            if (reminder.isActive) "Reminder active for ${reminder.medicineName}"
                            else "Reminder inactive for ${reminder.medicineName}"
                    },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scheduled time
            Text(
                text = "At ${reminder.reminderTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Prescribed instructions — always labelled and visually separated.
            // NEVER shown as a patient-reported dose record.
            if (!reminder.prescribedInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Prescribed:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = reminder.prescribedInstructions,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dose recording buttons — patient-reported only
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onRecordDose(DoseRecord.TAKEN) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = "Mark dose as taken for ${reminder.medicineName}"
                        },
                ) {
                    Text(text = "Taken")
                }
                OutlinedButton(
                    onClick = { onRecordDose(DoseRecord.SKIPPED) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription =
                                "Mark dose as skipped for ${reminder.medicineName}"
                        },
                ) {
                    Text(text = "Skipped")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cancel / delete reminder
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Cancel reminder for ${reminder.medicineName}"
                    },
            ) {
                Text(text = "Cancel reminder")
            }
        }
    }
}
