package com.medihistory.ai.ui.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty

/**
 * Screen that lists all doctors belonging to [patientId].
 *
 * Tapping a doctor navigates to [DoctorEditScreen] via [onDoctorClick].
 * The FAB navigates to [DoctorEditScreen] in add mode via [onAddClick].
 * Delete triggers a confirmation dialog before removal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorListScreen(
    patientId: String,
    onAddClick: () -> Unit,
    onDoctorClick: (Doctor) -> Unit,
    viewModel: DoctorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        viewModel.loadDoctors(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Doctors") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.semantics { contentDescription = "Add doctor" },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
            }
        },
    ) { paddingValues ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .semantics { contentDescription = "Loading doctors" },
                    )
                }

                uiState.doctors.isEmpty() -> {
                    Text(
                        text = "No doctors added yet. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                    )
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.doctors, key = { it.id }) { doctor ->
                            DoctorListItem(
                                doctor = doctor,
                                onClick = { onDoctorClick(doctor) },
                                onDeleteClick = { viewModel.onDeleteClicked(doctor) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                )
            }
        }
    }

    // Delete confirmation dialog
    uiState.showDeleteConfirm?.let { doctor ->
        DeleteDoctorDialog(
            doctorName = doctor.name,
            onConfirm = { viewModel.onDeleteConfirmed(doctor, patientId) },
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
}

@Composable
private fun DoctorListItem(
    doctor: Doctor,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = "Edit ${doctor.name}",
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .size(40.dp)
                    .padding(end = 8.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = doctor.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = doctor.displaySpecialty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!doctor.clinicName.isNullOrBlank()) {
                Text(
                    text = doctor.clinicName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = onDeleteClick,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Delete ${doctor.name}" },
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DeleteDoctorDialog(
    doctorName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove doctor?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remove $doctorName from your doctors list?")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Prescriptions linked to this doctor will show Doctor removed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Display helpers
// ---------------------------------------------------------------------------

private fun Doctor.displaySpecialty(): String =
    if (specialty == DoctorSpecialty.OTHER && !specialtyOther.isNullOrBlank()) {
        specialtyOther
    } else {
        specialty.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
