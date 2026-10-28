package com.medihistory.ai.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.PrimaryButton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
private val DOB_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * Screen for creating or editing a patient profile.
 *
 * On a successful save [onSaveSuccess] is called so the caller can navigate back.
 * [onNavigateUp] is called when the back arrow is tapped.
 *
 * @param patientId Non-null when editing an existing patient; null for create mode.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatientProfileScreen(
    patientId: String?,
    onSaveSuccess: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        if (patientId != null) viewModel.loadPatient(patientId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (patientId == null) "Add Patient" else "Edit Patient") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.semantics { contentDescription = "Navigate up" },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Name ──────────────────────────────────────────────────
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Full name *") },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Patient name" },
                )

                // ── Date of birth ─────────────────────────────────────────
                DobField(
                    dateOfBirth = uiState.dateOfBirth,
                    onDateSelected = viewModel::onDateOfBirthChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Blood group ───────────────────────────────────────────
                BloodGroupDropdown(
                    selected = uiState.bloodGroup,
                    onSelect = viewModel::onBloodGroupChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Allergies ─────────────────────────────────────────────
                AllergyInput(
                    allergies = uiState.allergies,
                    onAllergyAdded = viewModel::onAllergyAdded,
                    onAllergyRemoved = viewModel::onAllergyRemoved,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Generic error ─────────────────────────────────────────
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // ── Save ──────────────────────────────────────────────────
                PrimaryButton(
                    text = "Save",
                    onClick = { viewModel.onSaveClicked(patientId) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Date of birth field + DatePickerDialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DobField(
    dateOfBirth: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val displayText = dateOfBirth?.format(DOB_FORMATTER) ?: ""

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date of birth") },
            placeholder = { Text("Tap to select") },
            trailingIcon = {
                if (dateOfBirth != null) {
                    IconButton(onClick = { onDateSelected(null) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear date of birth",
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Date of birth" },
        )
        // Transparent overlay captures taps on the read-only text field
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { showPicker = true },
        )
    }

    if (showPicker) {
        val initialMillis = dateOfBirth
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = pickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val selected = Instant.ofEpochMilli(selectedMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onDateSelected(selected)
                        }
                        showPicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

// ---------------------------------------------------------------------------
// Blood group dropdown
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BloodGroupDropdown(
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Blood group") },
            placeholder = { Text("Select") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse blood group menu" else "Expand blood group menu",
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .semantics { contentDescription = "Blood group selector" },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            // "None" option to clear the selection
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            BLOOD_GROUPS.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group) },
                    onClick = {
                        onSelect(group)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Allergy chip input
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergyInput(
    allergies: List<String>,
    onAllergyAdded: (String) -> Unit,
    onAllergyRemoved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var allergyText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = allergyText,
                onValueChange = { allergyText = it },
                label = { Text("Add allergy") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Allergy input" },
            )
            TextButton(
                onClick = {
                    onAllergyAdded(allergyText)
                    allergyText = ""
                },
                modifier = Modifier.semantics { contentDescription = "Add allergy button" },
            ) {
                Text("Add")
            }
        }

        if (allergies.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allergies.forEach { allergy ->
                    AssistChip(
                        onClick = { onAllergyRemoved(allergy) },
                        label = { Text(allergy) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $allergy",
                            )
                        },
                    )
                }
            }
        }
    }
}
