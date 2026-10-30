package com.medihistory.ai.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.components.PrimaryButton
import com.medihistory.ai.ui.components.SecondaryButton

@Composable
internal fun CaptureConfirmScreen(
    tempFilePath: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Image preview placeholder (real image rendering requires coil, not yet added)
        Box(
            modifier =
                Modifier.weight(1f).fillMaxWidth()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Image captured. Tap \"Use this photo\" to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SecondaryButton(text = "Retake", onClick = onRetake, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Use this photo", onClick = onConfirm, modifier = Modifier.weight(1f))
        }
    }
}
