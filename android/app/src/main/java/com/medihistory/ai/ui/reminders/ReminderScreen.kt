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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.data.model.DoseRecord
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Reminder
import com.medihistory.ai.data.reminders.ReminderSuggestion
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.components.PrimaryButton
import com.medihistory.ai.ui.components.SecondaryButton
import com.medihistory.ai.ui.theme.extendedColors
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Reminders screen — top-level destination for feature F08 (Reminders and adherence tracking).
 *
 * Layout follows Figma SCR-08-REMINDERS with three distinct sections:
 * 1. **Today's Medicines** — dose cards for reminders due today with Taken/Skipped buttons.
 * 2. **All Reminders** — the full list of configured reminders with toggle and cancel.
 * 3. An extended FAB to set up a new reminder.
 *
 * Safety invariants:
 * - Prescribed instructions are labelled "Prescribed:" and are always visually separated
 *   from dose recording buttons.
 * - Dose recording (Taken / Skipped) is patient-reported and never shown as a prescription fact.
 * - Reminders are opt-in. The empty state directs the patient to the medicine detail screen.
 *
 * Accessibility:
 * - All interactive elements have a minimum 48×48 dp touch target via [PrimaryButton]/[SecondaryButton]/Switch.
 * - The active toggle Switch carries a contentDescription.
 *
 * @param patientId  The patient whose reminders are shown.
 * @param modifier   Modifier applied to the Scaffold.
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
            TopAppBar(
                title = { Text(text = "Reminders") },
                actions = {
                    IconButton(onClick = { /* Add reminder — future ticket */ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add reminder")
                    }
                    IconButton(onClick = { /* Options */ }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Set Up Reminder") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                onClick = { viewModel.onShowMedicinePicker() },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier =
                        Modifier
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
                    modifier =
                        Modifier
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
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }

            else -> {
                ReminderContent(
                    reminders = uiState.reminders,
                    patientId = patientId,
                    viewModel = viewModel,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }
        }
    }

    // ── Medicine picker dialog ─────────────────────────────────────────────
    // Shown when the patient taps "Set Up Reminder" FAB.
    if (uiState.showMedicinePicker) {
        MedicinePickerDialog(
            medicines = uiState.availableMedicines,
            onMedicineSelected = { medicine -> viewModel.onMedicineSelected(medicine, patientId) },
            onDismiss = viewModel::onDismissMedicinePicker,
        )
    }

    // ── Reminder advisor dialog (F16) ─────────────────────────────────────
    val pendingMedicine = uiState.pendingReminderMedicine
    if (uiState.showReminderAdvisor && pendingMedicine != null) {
        ReminderAdvisorDialog(
            medicine = pendingMedicine,
            suggestion = uiState.reminderSuggestion,
            onConfirm = { times ->
                viewModel.onConfirmReminderTimes(
                    medicine = pendingMedicine,
                    patientId = patientId,
                    times = times,
                )
            },
            onDismiss = viewModel::onDismissReminderAdvisor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Medicine picker dialog
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Simple medicine selection dialog shown before creating a reminder.
 *
 * If no medicines exist yet, shows guidance to add prescriptions first.
 * Tapping a medicine creates a reminder with a default time of 08:00.
 *
 * Privacy: medicine names are not logged anywhere in this composable.
 */
@Composable
private fun MedicinePickerDialog(
    medicines: List<Medicine>,
    onMedicineSelected: (Medicine) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a medicine") },
        text = {
            if (medicines.isEmpty()) {
                Text(
                    text = "No medicines found. Add a prescription first and review the extracted fields to populate your medicine list.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn {
                    items(items = medicines, key = { it.id }) { medicine ->
                        TextButton(
                            onClick = { onMedicineSelected(medicine) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        contentDescription = "Set reminder for ${medicine.medicineName}"
                                    },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                Text(
                                    text = medicine.medicineName,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (!medicine.dosageInstructions.isNullOrBlank()) {
                                    Text(
                                        text = medicine.dosageInstructions,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reminder advisor dialog (F16)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dialog that shows AI-suggested reminder times based on the medicine's dosage instructions.
 *
 * Medical safety:
 * - The suggested times are advisory only — clearly labelled as suggestions.
 * - The patient must tap "Set reminders" to confirm. Nothing is created automatically.
 * - A disclaimer is always shown at the bottom.
 *
 * @param medicine   The medicine the patient is setting a reminder for.
 * @param suggestion The suggested times from [ReminderAdvisorEngine], or null if none match.
 * @param onConfirm  Called with the patient-confirmed list of [LocalTime] values.
 * @param onDismiss  Called when the patient cancels or dismisses the dialog.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReminderAdvisorDialog(
    medicine: Medicine,
    suggestion: ReminderSuggestion?,
    onConfirm: (List<LocalTime>) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialTimes = suggestion?.times ?: listOf(LocalTime.of(8, 0))
    var times by rememberSaveable { mutableStateOf(initialTimes) }
    var editingIndex by rememberSaveable { mutableStateOf(-1) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    // Time picker sub-dialog for the slot at editingIndex
    if (editingIndex >= 0 && editingIndex < times.size) {
        val current = times[editingIndex]
        val timePickerState =
            rememberTimePickerState(
                initialHour = current.hour,
                initialMinute = current.minute,
                is24Hour = true,
            )
        AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            title = { Text("Set reminder time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val updated = times.toMutableList()
                    updated[editingIndex] = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    times = updated
                    editingIndex = -1
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = -1 }) { Text("Cancel") }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggested reminder times") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Medicine: ${medicine.medicineName}", style = MaterialTheme.typography.bodyMedium)
                if (!medicine.dosageInstructions.isNullOrBlank()) {
                    Text(
                        text = "Prescribed: ${medicine.dosageInstructions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (suggestion != null) "Tap a time to edit it:" else "Set your reminder time:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                times.forEachIndexed { idx, time ->
                    Card(
                        onClick = { editingIndex = idx },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "Reminder time ${time.format(formatter)}, tap to edit"
                                },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = time.format(formatter),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text("Edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "AI-generated � always verify timing with your doctor or pharmacist.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(times) }) { Text("Set ${times.size} reminder(s)") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 3-section content layout
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ReminderContent(
    reminders: List<Reminder>,
    patientId: String,
    viewModel: ReminderViewModel,
    modifier: Modifier = Modifier,
) {
    // "Today's medicines" section — all active reminders with dose buttons
    // (for MVP, treat all active reminders as due today; day-level filtering is future work)
    val todayReminders = reminders.filter { it.isActive }
    val allReminders = reminders

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        // Disclaimer banner — always shown (Figma requirement)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.extendedColors.warningContainer,
                    ),
            ) {
                Text(
                    text = "Reminders are based on your prescription instructions. Always verify timing with your doctor or pharmacist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // ── Section 1: Today's Medicines ─────────────────────────────────────
        item {
            SectionHeader(title = "Today's Medicines")
        }

        if (todayReminders.isEmpty()) {
            item {
                Text(
                    text = "No active reminders for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        } else {
            items(
                items = todayReminders,
                key = { reminder -> "today_${reminder.id}" },
            ) { reminder ->
                TodayDoseCard(
                    reminder = reminder,
                    onRecordDose = { doseRecord ->
                        viewModel.recordDose(
                            reminderId = reminder.id,
                            patientId = patientId,
                            doseRecord = doseRecord,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Section 2: All Reminders ──────────────────────────────────────────
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "All Reminders")
        }

        items(
            items = allReminders,
            key = { reminder -> reminder.id },
        ) { reminder ->
            ReminderCard(
                reminder = reminder,
                onToggleActive = { viewModel.toggleActive(reminder) },
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

        // Bottom spacer so the last card isn't hidden behind the FAB
        item { Spacer(modifier = Modifier.height(88.dp)) }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Today's dose card — compact, dose-action focused
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Compact dose card for the "Today's Medicines" section.
 *
 * Shows medicine name, scheduled time, prescribed instructions (if any), and
 * Taken/Skipped action buttons using [PrimaryButton] and [SecondaryButton].
 *
 * Safety layout: "Prescribed:" section is always visually distinct.
 */
@Composable
private fun TodayDoseCard(
    reminder: Reminder,
    onRecordDose: (DoseRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                Text(
                    text = "At ${reminder.reminderTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!reminder.prescribedInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PrimaryButton(
                    text = "Taken",
                    onClick = { onRecordDose(DoseRecord.TAKEN) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Mark dose as taken for ${reminder.medicineName}"
                            },
                )
                SecondaryButton(
                    text = "Skipped",
                    onClick = { onRecordDose(DoseRecord.SKIPPED) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Mark dose as skipped for ${reminder.medicineName}"
                            },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Full reminder card — used in "All Reminders" section
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card representing a single [Reminder] in the "All Reminders" section.
 *
 * Safety layout:
 * - "Prescribed:" section is visually distinct and labelled — never mixed with dose buttons.
 * - Dose buttons use [PrimaryButton] and [SecondaryButton] for consistent 48 dp touch target.
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
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (reminder.isActive) {
                                    "Reminder active for ${reminder.medicineName}"
                                } else {
                                    "Reminder inactive for ${reminder.medicineName}"
                                }
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
                PrimaryButton(
                    text = "Taken",
                    onClick = { onRecordDose(DoseRecord.TAKEN) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Mark dose as taken for ${reminder.medicineName}"
                            },
                )
                SecondaryButton(
                    text = "Skipped",
                    onClick = { onRecordDose(DoseRecord.SKIPPED) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription =
                                    "Mark dose as skipped for ${reminder.medicineName}"
                            },
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cancel / delete reminder
            SecondaryButton(
                text = "Cancel reminder",
                onClick = onDelete,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Cancel reminder for ${reminder.medicineName}"
                        },
            )
        }
    }
}
