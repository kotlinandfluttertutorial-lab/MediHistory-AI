package com.medihistory.ai.ui.history

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

/**
 * Medical history screen — top-level destination for feature F06.
 *
 * Medical safety:
 * - The disclaimer card is ALWAYS visible and cannot be dismissed.
 * - Every section from [HistoryCompiler] links back to its source prescription.
 * - AI-generated sections are visually labelled with an amber "AI-generated" chip.
 * - This screen never presents clinical advice, diagnoses, or treatment recommendations.
 *
 * Privacy: [onViewPrescription] receives only an ID — no prescription data is passed.
 *
 * Accessibility: all interactive elements have a minimum 48×48 dp touch target.
 *
 * @param patientId           The patient whose history is compiled.
 * @param onViewPrescription  Called with a prescription ID when the patient taps a source chip.
 * @param onBack              Called when the back button is tapped.
 * @param viewModel           Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalHistoryScreen(
    patientId: String,
    onViewPrescription: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicalHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Trigger initial compile when the screen first appears.
    LaunchedEffect(patientId) {
        viewModel.generateHistory(patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Medical History") },
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
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh(patientId) },
                        modifier = Modifier.semantics {
                            contentDescription = "Refresh history"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.sections.isNotEmpty() && !uiState.isGenerating) {
                FloatingActionButton(
                    onClick = { viewModel.refresh(patientId) },
                    modifier = Modifier.semantics {
                        contentDescription = "Refresh history"
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ----------------------------------------------------------------
            // Disclaimer card — ALWAYS visible, non-dismissible
            // ----------------------------------------------------------------
            DisclaimerCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ----------------------------------------------------------------
            // Generation progress
            // ----------------------------------------------------------------
            if (uiState.isGenerating) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { uiState.generationProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Compiling records" },
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Compiling your records…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ----------------------------------------------------------------
            // Content
            // ----------------------------------------------------------------
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Loading medical history",
                    )
                }

                uiState.error != null -> {
                    EmptyStateView(
                        message = uiState.error ?: "An error occurred.",
                        actionLabel = "Retry",
                        onAction = { viewModel.refresh(patientId) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.sections.isEmpty() && !uiState.isGenerating -> {
                    EmptyStateView(
                        message = "No history yet",
                        actionLabel = "Generate history",
                        onAction = { viewModel.generateHistory(patientId) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                uiState.sections.isNotEmpty() -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        items(
                            items = uiState.sections,
                            key = { section -> section.type.name },
                        ) { section ->
                            HistorySectionView(
                                section = section,
                                onViewSource = onViewPrescription,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Non-dismissible disclaimer card.
 *
 * Medical safety requirement: this card must be visible on every state of
 * [MedicalHistoryScreen]. It must not be hidden, collapsed, or made dismissible.
 */
@Composable
private fun DisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null, // decorative — disclaimer text conveys meaning
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "This is a patient-compiled summary. It is not a clinical report. " +
                    "Please verify all information with your doctor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics {
                    contentDescription =
                        "Disclaimer: This is a patient-compiled summary. " +
                        "It is not a clinical report. " +
                        "Please verify all information with your doctor."
                },
            )
        }
    }
}
