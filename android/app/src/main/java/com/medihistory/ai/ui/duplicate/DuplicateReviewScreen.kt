package com.medihistory.ai.ui.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.medihistory.ai.ui.theme.extendedColors

/**
 * Screen for reviewing potential duplicate medicine alerts — feature F05.
 *
 * ## Medical safety
 * - Every alert is labelled "Potential duplicate" — never "Duplicate medication" or any
 *   clinical term.
 * - Alerts are informational only; they do not block any patient action.
 * - Reviewed alerts remain visible (not deleted, not hidden by default).
 * - "Mark as Reviewed" requires confirmation with the mandatory disclaimer dialog.
 *
 * ## Accessibility
 * - All interactive elements have a minimum 48×48 dp touch target.
 * - Summary banner is a live region so TalkBack announces updates.
 *
 * Privacy: [onViewPrescription] receives only a prescription ID — no medicine data is
 * passed outside this screen.
 *
 * @param patientId           The ID of the patient whose alerts are displayed.
 * @param onViewPrescription  Called with a prescription ID when the patient taps a source link.
 * @param onBack              Called when the patient taps the back navigation icon.
 * @param viewModel           Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateReviewScreen(
    patientId: String,
    onViewPrescription: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuplicateReviewViewModel = hiltViewModel(),
) {
    // Trigger the load whenever patientId changes.
    LaunchedEffect(patientId) {
        viewModel.loadAlerts(patientId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Potential Duplicates") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Navigate back"
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
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
            // ---- Top summary banner ----
            if (uiState.unreviewedCount > 0) {
                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics {
                                contentDescription =
                                    "${uiState.unreviewedCount} alert(s) need your review"
                            },
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.extendedColors.warningContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.extendedColors.warning,
                        )
                        Text(
                            text = "${uiState.unreviewedCount} alert(s) need your review",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.extendedColors.onWarningContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            // ---- Show-reviewed filter row ----
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = viewModel::onToggleShowReviewed,
                    modifier =
                        Modifier.semantics {
                            contentDescription =
                                if (uiState.showReviewedAlerts) {
                                    "Hide reviewed alerts"
                                } else {
                                    "Show reviewed alerts"
                                }
                        },
                ) {
                    if (uiState.showReviewedAlerts) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                        )
                        Text(
                            text = "Hide reviewed",
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    } else {
                        Text(text = "Show reviewed")
                    }
                }
            }

            // ---- Content area ----
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Loading duplicate alerts",
                    )
                }

                uiState.error != null -> {
                    EmptyStateView(
                        message = uiState.error ?: "An error occurred.",
                        actionLabel = "Retry",
                        onAction = { viewModel.loadAlerts(patientId) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.alerts.isEmpty() -> {
                    EmptyStateView(
                        message = "No potential duplicate alerts",
                        actionLabel = "View medicines",
                        onAction = { /* Caller handles navigation */ },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        items(
                            items = uiState.alerts,
                            key = { item -> item.alert.id },
                        ) { item ->
                            DuplicateAlertCard(
                                item = item,
                                onToggleExpand = { viewModel.onToggleExpanded(item.alert.id) },
                                onMarkReviewed = { viewModel.onMarkReviewed(item.alert.id) },
                                onViewPrescriptionA = {
                                    item.prescriptionAId?.let { onViewPrescription(it) }
                                },
                                onViewPrescriptionB = {
                                    item.prescriptionBId?.let { onViewPrescription(it) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
