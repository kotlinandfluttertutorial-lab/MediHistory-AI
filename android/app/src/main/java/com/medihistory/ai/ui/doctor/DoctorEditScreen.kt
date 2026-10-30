package com.medihistory.ai.ui.doctor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.ui.components.PrimaryButton

/**
 * Screen for adding a new doctor.
 *
 * On a successful save [onSaveSuccess] is called so the caller can navigate away.
 * [onNavigateUp] is called when the back arrow is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorEditScreen(
    patientId: String,
    onSaveSuccess: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: DoctorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaveSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Doctor") },
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Name field
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Doctor name *") },
                    singleLine = true,
                    isError = uiState.error?.contains("name", ignoreCase = true) == true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Doctor name" },
                )

                // Specialty dropdown
                SpecialtyDropdown(
                    selected = uiState.specialty,
                    onSelect = viewModel::onSpecialtyChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                // OTHER specialty free-text — only visible when OTHER is selected
                AnimatedVisibility(visible = uiState.specialty == DoctorSpecialty.OTHER) {
                    OutlinedTextField(
                        value = uiState.specialtyOther,
                        onValueChange = viewModel::onSpecialtyOtherChange,
                        label = { Text("Describe specialty *") },
                        singleLine = true,
                        isError = uiState.error?.contains("specialty", ignoreCase = true) == true,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Describe specialty" },
                    )
                }

                // Clinic name field
                OutlinedTextField(
                    value = uiState.clinicName,
                    onValueChange = viewModel::onClinicNameChange,
                    label = { Text("Clinic / Hospital name") },
                    singleLine = true,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Clinic name" },
                )

                // Validation error
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Save button
                PrimaryButton(
                    text = "Save",
                    onClick = { viewModel.onSaveClicked(patientId) },
                    enabled = !uiState.isLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Specialty dropdown
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpecialtyDropdown(
    selected: DoctorSpecialty,
    onSelect: (DoctorSpecialty) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.displayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Specialty *") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse specialty menu" else "Expand specialty menu",
                )
            },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .semantics { contentDescription = "Specialty selector" },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DoctorSpecialty.entries.forEach { specialty ->
                DropdownMenuItem(
                    text = { Text(specialty.displayName()) },
                    onClick = {
                        onSelect(specialty)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun DoctorSpecialty.displayName(): String = name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
