package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.theme.extendedColors

/**
 * A chip that communicates [OcrStatus] via BOTH a text label AND a distinct icon + colour.
 *
 * Accessibility contract: state is always conveyed by text, never by colour alone.
 * The [contentDescription] is set on the chip so that TalkBack announces the full status string.
 *
 * Touch target is enforced to a minimum of 48×48 dp as required by WCAG 2.5.5.
 */
@Composable
fun StatusBadge(
    status: OcrStatus,
    modifier: Modifier = Modifier,
) {
    val label: String
    val icon: @Composable () -> Unit
    val containerColor =
        when (status) {
            OcrStatus.PENDING -> {
                label = "Pending"
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
                MaterialTheme.colorScheme.surfaceVariant
            }
            OcrStatus.PROCESSING -> {
                label = "Extracting\u2026"
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
                MaterialTheme.colorScheme.primaryContainer
            }
            OcrStatus.NEEDS_REVIEW -> {
                label = "Needs review"
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
                MaterialTheme.extendedColors.warningContainer
            }
            OcrStatus.REVIEWED -> {
                label = "Reviewed"
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
                MaterialTheme.colorScheme.secondaryContainer
            }
            OcrStatus.ERROR -> {
                label = "Failed"
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Error,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                }
                MaterialTheme.colorScheme.errorContainer
            }
        }

    val labelColor =
        when (status) {
            OcrStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
            OcrStatus.PROCESSING -> MaterialTheme.colorScheme.onPrimaryContainer
            OcrStatus.NEEDS_REVIEW -> MaterialTheme.extendedColors.onWarningContainer
            OcrStatus.REVIEWED -> MaterialTheme.colorScheme.onSecondaryContainer
            OcrStatus.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }

    AssistChip(
        onClick = {},
        label = { Text(text = label, color = labelColor) },
        leadingIcon = icon,
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "OCR status: $label" },
    )
}
