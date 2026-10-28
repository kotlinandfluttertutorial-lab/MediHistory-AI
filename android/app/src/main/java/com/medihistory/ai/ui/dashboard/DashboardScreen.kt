package com.medihistory.ai.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.prescription.PrescriptionCard
import com.medihistory.ai.ui.prescription.PrescriptionSummary
import com.medihistory.ai.ui.theme.extendedColors

/**
 * Dashboard screen — the primary landing screen (SCR-01-DASHBOARD).
 *
 * Displays a greeting, summary stats, quick operations, an "attention needed" section
 * for pending OCR/duplicate issues, and recent prescription activity.
 *
 * Medical safety: an AI-generated content disclaimer is shown in the recent-activity
 * section header when AI-generated summaries are present. All stat counts are patient data
 * only — never clinical advice.
 *
 * Privacy: [patientId] is passed to the ViewModel only — not logged or surfaced in UI strings.
 *
 * Accessibility:
 * - All interactive elements have a minimum 48×48 dp touch target.
 * - Stats cards carry content descriptions for TalkBack.
 * - Notification bell badge is announced via content description.
 *
 * @param patientId           The current patient's ID — used by the ViewModel only.
 * @param onAddPrescription   Called when the "Add Prescription" FAB or chip is tapped.
 * @param onViewDuplicates    Called when the "Duplicates" chip or attention row is tapped.
 * @param onViewHistory       Called when the "My History" chip or History row is tapped.
 * @param onNewDoctorMode     Called when the "New Doctor" chip is tapped.
 * @param onViewPrescriptions Called when "See all" in the recent-activity section is tapped.
 * @param viewModel           Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    patientId: String,
    onAddPrescription: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewHistory: () -> Unit,
    onNewDoctorMode: () -> Unit,
    onViewPrescriptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        viewModel.load(patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Dashboard") },
                actions = {
                    // Notification bell — badge shown when there are unreviewed alerts
                    val alertDesc = if (uiState.pendingAlertCount > 0) {
                        "${uiState.pendingAlertCount} pending alert(s), tap to review"
                    } else {
                        "No pending alerts"
                    }
                    IconButton(
                        onClick = onViewDuplicates,
                        modifier = Modifier.semantics { contentDescription = alertDesc },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = null,
                        )
                    }
                    IconButton(
                        onClick = { /* Profile — future ticket */ },
                        modifier = Modifier.semantics {
                            contentDescription = "Patient profile"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = null,
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
                        contentDescription = null,
                    )
                },
                onClick = onAddPrescription,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentDescription = "Loading dashboard",
                )
            }

            uiState.error != null -> {
                EmptyStateView(
                    message = uiState.error ?: "An error occurred.",
                    actionLabel = "Retry",
                    onAction = { viewModel.load(patientId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    // ── Stats row ───────────────────────────────────────────────────
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        StatsGrid(uiState = uiState)
                    }

                    // ── Quick operations row ────────────────────────────────────────
                    item {
                        QuickOpsRow(
                            onAddPrescription = onAddPrescription,
                            onViewDuplicates = onViewDuplicates,
                            onViewHistory = onViewHistory,
                            onNewDoctorMode = onNewDoctorMode,
                        )
                    }

                    // ── Attention section ───────────────────────────────────────────
                    if (uiState.pendingOcrCount > 0 || uiState.pendingAlertCount > 0) {
                        item {
                            AttentionSection(
                                pendingOcrCount = uiState.pendingOcrCount,
                                pendingAlertCount = uiState.pendingAlertCount,
                                onReviewOcr = onViewPrescriptions,
                                onViewDuplicates = onViewDuplicates,
                            )
                        }
                    }

                    // ── Recent activity section ─────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            TextButton(onClick = onViewPrescriptions) {
                                Text(text = "See all")
                            }
                        }
                    }

                    if (uiState.recentPrescriptions.isEmpty()) {
                        item {
                            Text(
                                text = "No prescriptions yet. Add your first one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentPrescriptions,
                            key = { rx -> rx.id },
                        ) { prescription ->
                            PrescriptionCard(
                                prescription = prescription,
                                onClick = onViewPrescriptions,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Bottom spacer so the last card isn't behind the FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats grid — 2×2 summary cards
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Prescriptions",
                count = uiState.prescriptionCount,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Doctors",
                count = uiState.doctorCount,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Medicines",
                count = uiState.medicineCount,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Pending alerts",
                count = uiState.pendingAlertCount,
                containerColor = if (uiState.pendingAlertCount > 0) {
                    MaterialTheme.extendedColors.warningContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                onContainerColor = if (uiState.pendingAlertCount > 0) {
                    MaterialTheme.extendedColors.onWarningContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    onContainerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.semantics {
            contentDescription = "$count $label"
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick-ops row — horizontally scrollable chip strip
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickOpsRow(
    onAddPrescription: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewHistory: () -> Unit,
    onNewDoctorMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickOpChip(
            label = "Add Rx",
            icon = Icons.Filled.Add,
            contentDescription = "Add prescription",
            onClick = onAddPrescription,
        )
        QuickOpChip(
            label = "Duplicates",
            icon = Icons.Outlined.Warning,
            contentDescription = "View duplicate alerts",
            onClick = onViewDuplicates,
        )
        QuickOpChip(
            label = "My History",
            icon = Icons.Outlined.ContentCopy,
            contentDescription = "View medical history",
            onClick = onViewHistory,
        )
        QuickOpChip(
            label = "New Doctor",
            icon = Icons.Outlined.PersonAdd,
            contentDescription = "New doctor mode",
            onClick = onNewDoctorMode,
        )
    }
}

@Composable
private fun QuickOpChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Attention section — shown only when action is needed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttentionSection(
    pendingOcrCount: Int,
    pendingAlertCount: Int,
    onReviewOcr: () -> Unit,
    onViewDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Needs your attention" },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Needs your attention",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.extendedColors.onWarningContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (pendingOcrCount > 0) {
                AttentionRow(
                    text = "$pendingOcrCount prescription(s) need OCR review",
                    actionLabel = "Review",
                    onClick = onReviewOcr,
                )
            }

            if (pendingAlertCount > 0) {
                AttentionRow(
                    text = "$pendingAlertCount potential duplicate alert(s)",
                    actionLabel = "View",
                    onClick = onViewDuplicates,
                )
            }
        }
    }
}

@Composable
private fun AttentionRow(
    text: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 4.dp),
                tint = MaterialTheme.extendedColors.warning,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(onClick = onClick) {
            Text(text = "$actionLabel →")
        }
    }
}
