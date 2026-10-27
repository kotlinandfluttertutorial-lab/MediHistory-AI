package com.medihistory.ai.ui.newdoctor

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * New Doctor Mode screen — feature F07.
 *
 * Allows the patient to select prescriptions for inclusion in a PDF summary, consent to
 * sharing, and then share the PDF via the Android share sheet.
 *
 * Medical safety and privacy requirements enforced here:
 * - [ConsentDialog] is ALWAYS shown before PDF generation begins. No sharing happens without it.
 * - Only records selected by the patient are included in the PDF.
 * - The share sheet is opened only after explicit patient consent.
 * - No background upload. The share sheet is the only mechanism.
 * - The PDF is written to app-private cache and shared via FileProvider URI.
 *
 * @param patientId Identifies which patient's records to load. Required; never null in production.
 * @param onBack    Called when the patient navigates back.
 * @param viewModel Injected by Hilt; can be overridden in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDoctorModeScreen(
    patientId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewDoctorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showConsentDialog by remember { mutableStateOf(false) }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }

    // Load records when the screen appears
    LaunchedEffect(patientId) {
        viewModel.loadRecords(patientId)
    }

    // Once the PDF is ready and consent has been confirmed, open the share sheet
    LaunchedEffect(pdfUri) {
        val uri = pdfUri ?: return@LaunchedEffect
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Medical Summary — MediHistory AI",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Medical Summary"))
        pdfUri = null // reset so the effect doesn't re-fire
    }

    if (showConsentDialog) {
        ConsentDialog(
            selectedCount = uiState.selectedCount,
            onConfirm = {
                showConsentDialog = false
                // Patient has explicitly consented — generate PDF and deliver share sheet
                viewModel.generatePdfAndShare(context, patientId) { uri ->
                    pdfUri = uri
                }
            },
            onDismiss = {
                showConsentDialog = false
                // Patient cancelled — nothing is shared
            },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "New Doctor Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            // Sticky bottom bar — always visible so the action is never off-screen
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { showConsentDialog = true },
                        enabled = uiState.canProceed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = if (uiState.canProceed) {
                                    "Prepare summary with ${uiState.selectedCount} prescriptions selected"
                                } else {
                                    "Prepare summary, disabled, select at least one prescription"
                                }
                            },
                    ) {
                        Text(text = "Prepare summary")
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentDescription = "Loading prescriptions",
                )
            }

            uiState.error != null -> {
                EmptyStateView(
                    message = uiState.error ?: "An error occurred.",
                    actionLabel = "Retry",
                    onAction = { viewModel.loadRecords(patientId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            uiState.records.isEmpty() -> {
                EmptyStateView(
                    message = "No reviewed prescriptions found",
                    actionLabel = "Go back",
                    onAction = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    // Explanation card
                    item {
                        ExplanationCard()
                    }

                    // Select all / Deselect all row
                    item {
                        SelectionControlRow(
                            onSelectAll = { viewModel.onSelectAll() },
                            onDeselectAll = { viewModel.onDeselectAll() },
                        )
                    }

                    // Selectable record cards
                    items(
                        items = uiState.records,
                        key = { record -> record.prescriptionId },
                    ) { record ->
                        RecordSelectionCard(
                            record = record,
                            onToggle = { viewModel.onToggleRecord(record.prescriptionId) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Bottom spacer so the last card isn't hidden behind the bottom bar
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------
// Private composables
// -----------------------------------------------------------------------

/**
 * Explanation card shown at the top of the record list.
 *
 * Provides context so the patient understands what "New Doctor Mode" does
 * before they make any selections.
 */
@Composable
private fun ExplanationCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.MedicalServices,
                contentDescription = null, // decorative; card text is descriptive
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Prepare a summary for a new doctor appointment. " +
                    "Select the prescriptions you want to include.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * Row with "Select all" and "Deselect all" text buttons.
 */
@Composable
private fun SelectionControlRow(
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        TextButton(onClick = onSelectAll) {
            Text(text = "Select all")
        }
        TextButton(onClick = onDeselectAll) {
            Text(text = "Deselect all")
        }
    }
}

/**
 * A selectable card for a single [RecordSelection].
 *
 * Tapping anywhere on the card toggles the selection.
 *
 * Accessibility: the card has a merged content description that announces
 * the selection state and all key fields to TalkBack.
 */
@Composable
private fun RecordSelectionCard(
    record: RecordSelection,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val medLabel = if (record.medicineCount == 1) "1 medicine" else "${record.medicineCount} medicines"
    val selectionLabel = if (record.isSelected) "Selected" else "Not selected"
    val cardContentDescription =
        "Prescription from ${record.doctorName}, " +
            "dated ${record.prescriptionDate}, " +
            "$medLabel. $selectionLabel. Tap to toggle."

    Card(
        onClick = onToggle,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = cardContentDescription
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Checkbox indicator — decorative; full description is on the card root
            Icon(
                imageVector = if (record.isSelected) {
                    Icons.Outlined.CheckBox
                } else {
                    Icons.Outlined.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = if (record.isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.doctorName,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = record.prescriptionDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = medLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
