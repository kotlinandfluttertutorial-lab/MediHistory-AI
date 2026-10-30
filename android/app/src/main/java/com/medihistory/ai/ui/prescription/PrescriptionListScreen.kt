package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Prescription list screen — top-level destination for feature F02.
 *
 * Displays all prescriptions as a scrollable list of [PrescriptionCard] items. While data is
 * loading, shows a [LoadingIndicator]. On error, shows an error message with a retry action.
 * When the list is empty, shows [EmptyStateView].
 *
 * @param onPrescriptionClick Called with the prescription ID when a card is tapped.
 * @param onAddPrescription   Called when the "Add Prescription" FAB is tapped.
 * @param viewModel           Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionListScreen(
    onPrescriptionClick: (String) -> Unit,
    onAddPrescription: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrescriptionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            MediumTopAppBar(
                title = { Text(text = "Prescriptions") },
                actions = {
                    IconButton(onClick = { /* TODO: open search */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search prescriptions",
                        )
                    }
                    IconButton(onClick = { /* TODO: open sort */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = "Sort prescriptions",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Add Prescription") },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null, // label already describes the action
                    )
                },
                onClick = onAddPrescription,
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
                    contentDescription = "Loading prescriptions",
                )
            }

            uiState.error != null -> {
                EmptyStateView(
                    message = uiState.error ?: "An error occurred.",
                    actionLabel = "Retry",
                    onAction = { viewModel.loadPrescriptions() },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }

            uiState.prescriptions.isEmpty() -> {
                EmptyStateView(
                    message = "No prescriptions yet",
                    actionLabel = "Add your first prescription",
                    onAction = onAddPrescription,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    // Doctor-grouped layout matching Figma SCR-02-RX-LIST design
                    uiState.doctorGroups.forEach { group ->
                        item(key = "header-${group.doctorName}") {
                            DoctorGroupHeader(
                                group = group,
                                onToggle = { viewModel.onToggleGroup(group.doctorName) },
                            )
                        }
                        if (group.isExpanded) {
                            items(
                                items = group.prescriptions,
                                key = { rx -> rx.id },
                            ) { prescription ->
                                PrescriptionCard(
                                    prescription = prescription,
                                    onClick = { onPrescriptionClick(prescription.id) },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                        }
                        item(key = "divider-${group.doctorName}") {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
