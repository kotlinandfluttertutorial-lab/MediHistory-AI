package com.medihistory.ai.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.components.SecondaryButton

/**
 * Shown when the camera permission has been denied.
 *
 * If [isPermanentlyDenied] is true the system dialog will not appear again, so
 * the primary action directs the patient to Settings instead of re-requesting.
 */
@Composable
internal fun PermissionDeniedContent(
    isPermanentlyDenied: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    if (isPermanentlyDenied) {
                        "Camera access has been permanently denied. Please enable it in Settings to capture prescription photos."
                    } else {
                        "MediHistory AI needs camera access to capture prescription photos."
                    },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(
                text = "Dismiss",
                onClick = onDismiss,
            )
        }
    }
}
