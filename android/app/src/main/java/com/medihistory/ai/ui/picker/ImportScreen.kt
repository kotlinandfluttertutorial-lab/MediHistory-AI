package com.medihistory.ai.ui.picker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.components.LoadingIndicator

/**
 * Bottom-sheet style screen that offers three import paths for a prescription document:
 * 1. Android Photo Picker (gallery image)
 * 2. System file picker (PDF)
 * 3. Camera (navigates to CameraScreen via [onOpenCamera])
 *
 * On successful import the OCR worker is enqueued and [onFileImported] is called with
 * the new prescription ID so the caller navigates directly to the OCR review screen.
 */
@Composable
fun ImportScreen(
    onFileImported: (prescriptionId: String) -> Unit,
    onDismiss: () -> Unit,
    onOpenCamera: (() -> Unit)? = null,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Photo Picker — images only; no READ_MEDIA_IMAGES permission needed on API 33+
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // System file picker — restricted to PDF MIME type
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    // React to terminal states
    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportState.Success -> {
                if (s.prescriptionId.isNotEmpty()) {
                    onFileImported(s.prescriptionId)
                }
            }
            is ImportState.Error -> snackbarHostState.showSnackbar(s.message)
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add Prescription",
                style = MaterialTheme.typography.titleLarge,
            )

            // Option 1: Photo Picker (gallery)
            ImportOptionCard(
                icon = Icons.Outlined.Photo,
                title = "Choose from gallery",
                description = "Select a photo of your prescription",
            ) {
                imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            // Option 2: PDF via system file picker
            ImportOptionCard(
                icon = Icons.Outlined.PictureAsPdf,
                title = "Import PDF",
                description = "Select a PDF prescription",
            ) {
                filePickerLauncher.launch("application/pdf")
            }

            // Option 3: Camera — navigate to CameraScreen
            ImportOptionCard(
                icon = Icons.Outlined.CameraAlt,
                title = "Take a photo",
                description = "Capture a new photo of your prescription",
            ) {
                onOpenCamera?.invoke() ?: onDismiss()
            }

            if (importState is ImportState.Processing) {
                LoadingIndicator()
            }
        }
    }
}

/**
 * Single import option card with an icon, title, and description.
 *
 * Minimum touch target is enforced by [ElevatedCard] which fills the full width.
 */
@Composable
private fun ImportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative; title provides the label
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
