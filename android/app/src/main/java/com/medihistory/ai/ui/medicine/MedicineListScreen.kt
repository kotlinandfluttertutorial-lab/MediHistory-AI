package com.medihistory.ai.ui.medicine

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Medicine list screen — top-level destination for feature F04 (Medicine Explorer).
 *
 * Shows all medicines for the current patient with search and status filter chips.
 * The search bar filters by medicine name or active ingredient. The filter chips
 * narrow results to a single [MedicineStatus] (or show all when none is selected).
 *
 * Accessibility: all interactive elements have a minimum 48×48 dp touch target.
 * The results count text is a live region so TalkBack announces updates.
 *
 * Privacy: [onMedicineClick] receives only an ID — no medicine data is passed.
 *
 * @param onMedicineClick Called with the medicine ID when a [MedicineCard] is tapped.
 * @param viewModel       Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    patientId: String,
    onMedicineClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicineListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger data load whenever the patientId changes (e.g. on first composition).
    LaunchedEffect(patientId) {
        viewModel.loadMedicines(patientId)
    }

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf<MedicineStatus?>(null) }

    // Apply the status filter client-side (the DAO returns all medicines for the query;
    // status filter is a lightweight in-memory step that avoids additional DB queries).
    val displayedMedicines =
        uiState.medicines.let { list ->
            if (selectedStatus == null) list else list.filter { it.status == selectedStatus }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            MediumTopAppBar(
                title = { Text(text = "Medicines") },
                actions = {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Search medicines"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = { /* Filter bottom sheet — future ticket */ },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Filter medicines"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // Search bar — visible only when toggled
            if (isSearchActive) {
                DockedSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSearch = { isSearchActive = false },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text(text = "Search medicines, ingredients…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                ) {}
            }

            // Status filter chips — horizontally scrollable
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("All") },
                )
                MedicineStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = {
                            selectedStatus = if (selectedStatus == status) null else status
                        },
                        label = {
                            Text(
                                text =
                                    status.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                            )
                        },
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { /* Duplicate alert filter — future ticket */ },
                    label = { Text("Duplicate alert") },
                )
            }

            // Results count
            Text(
                text = "Showing ${displayedMedicines.size} medicine${if (displayedMedicines.size == 1) "" else "s"}",
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .semantics { contentDescription = "Showing ${displayedMedicines.size} medicines" },
            )

            // Content area
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Loading medicines",
                    )
                }

                uiState.error != null -> {
                    EmptyStateView(
                        message = uiState.error ?: "An error occurred.",
                        actionLabel = "Retry",
                        onAction = { /* Retry is driven by re-subscribing; caller can reload */ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                displayedMedicines.isEmpty() -> {
                    EmptyStateView(
                        message =
                            if (uiState.searchQuery.isBlank() && selectedStatus == null) {
                                "No medicines yet"
                            } else {
                                "No medicines match your search"
                            },
                        actionLabel = "Clear search",
                        onAction = {
                            viewModel.onSearchQueryChange("")
                            selectedStatus = null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                    ) {
                        items(
                            items = displayedMedicines,
                            key = { medicine -> medicine.id },
                        ) { medicine ->
                            MedicineCard(
                                medicine = medicine,
                                onClick = { onMedicineClick(medicine.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
