package com.medihistory.ai.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Theme preview – Light", showBackground = true)
@Composable
fun ThemePreviewLight() {
    MediHistoryTheme(darkTheme = false, dynamicColor = false) {
        ThemePreviewContent(themeName = "Light")
    }
}

@Preview(
    name = "Theme preview – Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun ThemePreviewDark() {
    MediHistoryTheme(darkTheme = true, dynamicColor = false) {
        ThemePreviewContent(themeName = "Dark")
    }
}

@Composable
private fun ThemePreviewContent(themeName: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card showing theme name and colour swatches
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MediHistory AI — $themeName Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Colour swatches: primary, secondary, error, warning
                    Row {
                        ColorSwatch(
                            color = MaterialTheme.colorScheme.primary,
                            label = "Primary",
                        )
                        ColorSwatch(
                            color = MaterialTheme.colorScheme.secondary,
                            label = "Secondary",
                        )
                        ColorSwatch(
                            color = MaterialTheme.colorScheme.error,
                            label = "Error",
                        )
                        ColorSwatch(
                            color = MaterialTheme.extendedColors.warning,
                            label = "Warning",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // OCR confidence score badge (synthetic value)
                    Text(
                        text = "OCR confidence: 92 %",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // OCR review badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.extendedColors.warningContainer,
                                shape = MaterialTheme.shapes.extraSmall,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "Pending review",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.extendedColors.onWarningContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: androidx.compose.ui.graphics.Color, label: String) {
    Column(
        modifier = Modifier.padding(end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = color, shape = MaterialTheme.shapes.small),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
