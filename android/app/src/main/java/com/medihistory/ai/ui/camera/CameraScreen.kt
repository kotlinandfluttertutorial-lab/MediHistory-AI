package com.medihistory.ai.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageConfirmed: (filePath: String, sha256: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val captureState by viewModel.captureState.collectAsStateWithLifecycle()
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    when {
        !cameraPermissionState.status.isGranted -> {
            PermissionDeniedContent(
                isPermanentlyDenied = !cameraPermissionState.status.shouldShowRationale,
                onDismiss = onDismiss,
            )
        }
        captureState is CaptureState.Preview -> {
            val previewState = captureState as CaptureState.Preview
            CaptureConfirmScreen(
                tempFilePath = previewState.tempFilePath,
                onConfirm = {
                    val result = viewModel.onConfirm()
                    if (result != null) onImageConfirmed(result.first, result.second)
                },
                onRetake = { viewModel.onRetake() },
            )
        }
        captureState is CaptureState.Error -> {
            val errorState = captureState as CaptureState.Error
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(errorState.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.reset() }) { Text("Try again") }
                }
            }
        }
        else -> {
            CameraPreviewContent(
                onImageCaptured = { path -> viewModel.onImageCaptured(path) },
            )
        }
    }
}
