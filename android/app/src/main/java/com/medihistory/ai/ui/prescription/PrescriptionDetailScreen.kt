package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.components.StatusBadge
import com.medihistory.ai.ui.components.OcrStatus as UiOcrStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Prescription detail screen — displays all stored metadata for a single prescription.
 *
 * Shows a document preview placeholder, doctor info, prescription date, OCR status badge,
 * and an "Extract text" button when OCR has not yet been attempted or failed.
 *
 * @param prescriptionId The ID of the prescription to display.
 * @param patientId      The ID of the owning patient (used to scope the repository query).
 * @param onBack         Called when the user taps the back arrow.
 * @param viewModel      Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionDetailScreen(
    prescriptionId: String,
    patientId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrescriptionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(prescriptionId, patientId) {
        viewModel.load(prescriptionId, patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "Prescription") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentDescription = "Loading prescription",
                )
            }

            uiState.error != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    Text(
                        text = uiState.error ?: "An error occurred.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            else -> {
                val prescription = uiState.prescription!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Document preview placeholder
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .semantics { contentDescription = "Document preview placeholder" },
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Document preview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Doctor info
                    Text(
                        text = "Doctor",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = prescription.doctorId ?: "Not specified",
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Prescription date
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val dateText = prescription.prescriptionDate?.let { epochMs ->
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(Date(epochMs))
                    } ?: "Not specified"
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OCR status badge
                    Text(
                        text = "OCR Status",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(status = prescription.ocrStatus.toUiOcrStatus())

                    // Notes
                    if (!prescription.notes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = prescription.notes,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    // Extract text button — visible only when OCR not yet done or failed
                    if (prescription.ocrStatus == OcrStatus.PENDING ||
                        prescription.ocrStatus == OcrStatus.ERROR
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { /* OCR trigger — implemented in a future ticket */ },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Extract text")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Maps the data-layer [OcrStatus] to the UI-layer [UiOcrStatus] for [StatusBadge].
 */
private fun OcrStatus.toUiOcrStatus(): UiOcrStatus = when (this) {
    OcrStatus.PENDING      -> UiOcrStatus.PENDING
    OcrStatus.PROCESSING   -> UiOcrStatus.PROCESSING
    OcrStatus.NEEDS_REVIEW -> UiOcrStatus.NEEDS_REVIEW
    OcrStatus.REVIEWED     -> UiOcrStatus.REVIEWED
    OcrStatus.ERROR        -> UiOcrStatus.ERROR
}
