package com.medihistory.ai.ui.privacy

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Screen for data export, cloud backup status, and two-step data deletion.
 *
 * Privacy and security invariants enforced here:
 * - Export uses FileProvider URI — never exposes the raw file path to other apps.
 * - Deletion requires two separate confirmations. Step 2 requires typing "DELETE".
 * - Step 1 dialog explicitly states cloud backup retention limitations.
 * - After deletion [onDataDeleted] is called — the caller navigates to onboarding.
 * - No patient data content appears in any dialog body or log.
 *
 * @param patientId     ID of the current patient — passed to export and delete operations.
 * @param onDataDeleted Called after deletion succeeds — caller must navigate to login.
 * @param onBack        Called when the user taps the back navigation arrow.
 * @param viewModel     Hilt-injected [PrivacyViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    patientId: String,
    onDataDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Navigate out after successful deletion
    LaunchedEffect(uiState.deletionResult) {
        if (uiState.deletionResult != null) {
            onDataDeleted()
        }
    }

    // Launch share sheet when export file is ready
    LaunchedEffect(uiState.exportFile) {
        val file = uiState.exportFile ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MediHistory AI — My Medical Records Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share my records export"))
        viewModel.onExportFileConsumed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy and Data") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (uiState.isDeleting) {
                // Overlay spinner during deletion
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = "Deleting data" },
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // ── Export section ─────────────────────────────────────────────────
                    ExportCard(
                        isExporting = uiState.isExporting,
                        onExportClicked = { viewModel.onExportClicked(patientId) },
                    )

                    // ── Cloud backup status ────────────────────────────────────────────
                    CloudBackupCard()

                    // ── Consent and sharing ────────────────────────────────────────────
                    ConsentCard(onManageConsent = { /* TODO: navigate to consent management */ })

                    // ── Deletion section ───────────────────────────────────────────────
                    DeleteCard(
                        onDeleteClicked = { viewModel.onDeleteStep1Clicked() },
                    )

                    // ── Error display ──────────────────────────────────────────────────
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }

    // ── Delete Step 1 Dialog ───────────────────────────────────────────────────
    if (uiState.showDeleteStep1) {
        DeleteStep1Dialog(
            retentionExplanation = viewModel.cloudBackupRetentionExplanation,
            onContinue = { viewModel.onDeleteStep2Triggered() },
            onCancel = { viewModel.onDeleteCancelled() },
        )
    }

    // ── Delete Step 2 Dialog ───────────────────────────────────────────────────
    if (uiState.showDeleteStep2) {
        DeleteStep2Dialog(
            onConfirm = { viewModel.onDeleteStep2Confirmed(patientId) },
            onCancel = { viewModel.onDeleteCancelled() },
        )
    }
}

// ── Cards ──────────────────────────────────────────────────────────────────────

@Composable
private fun ExportCard(
    isExporting: Boolean,
    onExportClicked: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Export your data",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Download a copy of all your prescriptions, medicines, and medical records as a JSON file.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onExportClicked,
                enabled = !isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Export my data" },
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .height(18.dp)
                            .padding(end = 8.dp),
                    )
                }
                Text(if (isExporting) "Exporting…" else "Export my data")
            }
        }
    }
}

@Composable
private fun CloudBackupCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Cloud backup",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Cloud backup is OFF by default. Your data stays on this device only.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "To enable cloud backup, contact support or check app settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConsentCard(onManageConsent: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Consent and sharing",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "No active sharing consents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onManageConsent,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Manage consent" },
            ) {
                Text(text = "Manage consent")
            }
        }
    }
}

@Composable
private fun DeleteCard(onDeleteClicked: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Delete all data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "This will permanently delete all prescriptions, medicines, records, and reminders from this device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onDeleteClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Delete all my data" },
            ) {
                Text("Delete all my data")
            }
        }
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteStep1Dialog(
    retentionExplanation: String,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete all your data?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This will permanently delete:")
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("• All prescriptions and their documents")
                    Text("• All medicine records")
                    Text("• All doctor records")
                    Text("• All reminders and adherence data")
                    Text("• All duplicate alerts")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = retentionExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                modifier = Modifier.semantics { contentDescription = "I understand, continue to final confirmation" },
            ) {
                Text("I understand, continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun DeleteStep2Dialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var confirmationText by rememberSaveable { mutableStateOf("") }
    val isConfirmed = confirmationText == "DELETE"

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Final confirmation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Type DELETE to confirm permanent data deletion.")
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text("Type DELETE") },
                    singleLine = true,
                    isError = confirmationText.isNotEmpty() && !isConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Deletion confirmation input" },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.semantics { contentDescription = "Delete permanently" },
            ) {
                Text("Delete permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}
