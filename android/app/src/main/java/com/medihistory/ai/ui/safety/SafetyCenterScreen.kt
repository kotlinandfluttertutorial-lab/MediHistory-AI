package com.medihistory.ai.ui.safety

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Safety Center screen — Feature F05 / MA-42.
 *
 * Presents two informational panels to the patient:
 *
 * 1. **Duplicate alerts** (always available): shows how many potential duplicate
 *    active-ingredient alerts need review and allows navigation to [onViewDuplicates].
 *    This panel relies on deterministic local matching — it does not require any
 *    external reference source.
 *
 * 2. **Interaction check** (gated on a reference source): displays the current
 *    [InteractionCheckStatus]. When no source is configured, an amber information card
 *    explicitly states that checking is unavailable and directs the patient to a doctor
 *    or pharmacist.
 *
 * Medical safety invariants enforced by this screen:
 * - The word "safe" never appears in any UI text.
 * - [InteractionCheckStatus.NotConfigured] is presented as "checking not available", NOT
 *   as "no interactions found".
 * - Every [InteractionResult] card shows [InteractionResult.reviewWarning].
 * - The duplicate-alerts section is always visible regardless of check availability.
 *
 * Accessibility:
 * - All interactive elements have a minimum 48×48 dp touch target.
 * - Informational cards have content descriptions for TalkBack.
 *
 * Privacy: [patientId] is passed to the ViewModel only — not logged or surfaced in UI.
 *
 * @param patientId        The current patient's ID. Passed to the ViewModel for data scoping.
 * @param onViewDuplicates Called when the patient taps "Review duplicates".
 * @param onBack           Called when the patient taps the back arrow.
 * @param viewModel        Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyCenterScreen(
    patientId: String,
    onViewDuplicates: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SafetyCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) {
        viewModel.load(patientId)
        viewModel.checkInteractions(patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Safety Center") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Back"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->

        if (uiState.isLoading) {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentDescription = "Loading safety information",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // ── Section 1: Duplicate alerts ──────────────────────────────────
                item {
                    DuplicateAlertsSection(
                        unreviewedCount = uiState.unreviewedDuplicateCount,
                        onViewDuplicates = onViewDuplicates,
                    )
                }

                // ── Section 2: Interaction check ─────────────────────────────────
                item {
                    InteractionCheckSection(
                        status = uiState.interactionCheckStatus,
                        onRetry = { viewModel.checkInteractions(patientId) },
                    )
                }

                // ── Interaction results list ──────────────────────────────────────
                if (uiState.interactionCheckStatus is InteractionCheckStatus.Results) {
                    val results =
                        (uiState.interactionCheckStatus as InteractionCheckStatus.Results).interactions
                    if (results.isNotEmpty()) {
                        items(items = results, key = { r -> "${r.medicineAName}_${r.medicineBName}" }) { result ->
                            InteractionResultCard(result = result)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Duplicate alerts section — always available regardless of reference source.
 *
 * Shows how many potential duplicate alerts need review. A zero count is shown
 * as "No pending alerts" — never as a clinical safety statement.
 */
@Composable
private fun DuplicateAlertsSection(
    unreviewedCount: Int,
    onViewDuplicates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Potential duplicate alerts section" },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Potential Duplicate Alerts",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val countText = if (unreviewedCount == 0) {
                "No pending alerts"
            } else {
                "$unreviewedCount potential duplicate alert${if (unreviewedCount == 1) "" else "s"} need${if (unreviewedCount == 1) "s" else ""} review"
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { contentDescription = countText },
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Based on active ingredients in your reviewed prescriptions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            ElevatedButton(
                onClick = onViewDuplicates,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Review duplicate alerts" },
            ) {
                Text(text = "Review duplicates")
            }
        }
    }
}

/**
 * Interaction check section.
 *
 * Renders the appropriate UI for each [InteractionCheckStatus]:
 * - [InteractionCheckStatus.NotConfigured]: amber info card explaining unavailability
 *   and directing the patient to a doctor or pharmacist. NEVER implies safety.
 * - [InteractionCheckStatus.Checking]: loading indicator.
 * - [InteractionCheckStatus.Results]: header card (individual results rendered outside).
 * - [InteractionCheckStatus.Error]: error card with retry action.
 */
@Composable
private fun InteractionCheckSection(
    status: InteractionCheckStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        is InteractionCheckStatus.NotConfigured -> {
            NotConfiguredCard(modifier = modifier)
        }

        is InteractionCheckStatus.Checking -> {
            LoadingIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentDescription = "Checking for interactions",
            )
        }

        is InteractionCheckStatus.Results -> {
            val results = status.interactions
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Interaction check results: ${results.size} item${if (results.size == 1) "" else "s"}"
                    },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Interaction Check",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (results.isEmpty()) {
                        // Empty results ≠ safety confirmation.
                        Text(
                            text = "Checking could not be completed. Please consult your doctor or pharmacist.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            text = "${results.size} item${if (results.size == 1) "" else "s"} to review with your healthcare provider.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        is InteractionCheckStatus.Error -> {
            ElevatedCard(
                modifier = modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Interaction check error" },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Checking could not be completed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please consult your doctor or pharmacist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Retry interaction check" },
                    ) {
                        Text(text = "Try again")
                    }
                }
            }
        }
    }
}

/**
 * Amber information card shown when no verified medicine reference source is configured.
 *
 * Medical safety: explicitly states that unavailability does NOT mean medicines are
 * approved for combined use, and directs the patient to a doctor or pharmacist.
 * The word "safe" does not appear in this composable.
 */
@Composable
private fun NotConfiguredCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Interaction checking not available" },
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Interaction checking not available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Medication interaction checks require a verified medicine reference source. No source has been configured.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Medical safety statement — the word "safe" is deliberately absent.
            Text(
                text = "This does not mean your medicines are approved for combined use. Please consult your doctor or pharmacist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            Spacer(modifier = Modifier.height(12.dp))

            ElevatedButton(
                onClick = {
                    // Open phone dialer. If no phone intent is available, the OS handles
                    // gracefully — no crash. An information dialog could be shown instead
                    // once emergency contact support is implemented.
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Consult your doctor or pharmacist" },
            ) {
                Text(text = "Consult your doctor or pharmacist")
            }
        }
    }
}

/**
 * Card showing a single [InteractionResult].
 *
 * Medical safety: always shows [InteractionResult.reviewWarning]. Severity label
 * is displayed as-is — the caller ([SafetyCenterViewModel]) ensures it never
 * contains "safe" or "no interaction".
 */
@Composable
private fun InteractionResultCard(
    result: InteractionResult,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Interaction between ${result.medicineAName} and ${result.medicineBName}: ${result.severity}"
            },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${result.medicineAName} + ${result.medicineBName}",
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.severity,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Review warning — always displayed, never omitted.
            Text(
                text = result.reviewWarning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
