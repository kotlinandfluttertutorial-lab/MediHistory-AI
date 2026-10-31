package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Prescription list screen — top-level destination for feature F02.
 *
 * Displays all prescriptions grouped by doctor. Supports:
 * - Live search (filters by doctor name, specialty, or date)
 * - Sort (newest-first / oldest-first) via a bottom sheet
 * - Doctor group expand/collapse
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
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            MediumTopAppBar(
                title = { Text(text = "Prescriptions") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onToggleSearch() },
                        modifier = Modifier.semantics { contentDescription = "Search prescriptions" },
                    ) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    }
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.semantics { contentDescription = "Sort prescriptions" },
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Sort, contentDescription = null)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Add Prescription") },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                onClick = onAddPrescription,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // ── Search bar ────────────────────────────────────────────────────
            if (uiState.isSearchActive) {
                DockedSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { },
                    active = false,
                    onActiveChange = { if (!it) viewModel.onToggleSearch() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search by doctor, specialty, or date…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                ) {}
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Loading prescriptions",
                    )
                }

                uiState.error != null -> {
                    EmptyStateView(
                        message = uiState.error ?: "An error occurred.",
                        actionLabel = "Retry",
                        onAction = { viewModel.loadPrescriptions() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.prescriptions.isEmpty() -> {
                    EmptyStateView(
                        message = "No prescriptions yet",
                        actionLabel = "Add your first prescription",
                        onAction = onAddPrescription,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.doctorGroups.isEmpty() && uiState.searchQuery.isNotBlank() -> {
                    EmptyStateView(
                        message = "No results for \"${uiState.searchQuery}\"",
                        actionLabel = "Clear search",
                        onAction = { viewModel.onSearchQueryChange("") },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
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

    // ── Sort bottom sheet ─────────────────────────────────────────────────────
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sortSheetState,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sort by",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(
                    onClick = {
                        viewModel.onSortNewestFirst()
                        showSortSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Newest first") }
                TextButton(
                    onClick = {
                        viewModel.onSortOldestFirst()
                        showSortSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Oldest first") }
                TextButton(
                    onClick = { showSortSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancel") }
            }
        }
    }
}
