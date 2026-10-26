package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.theme.extendedColors

/**
 * A chip that communicates [MedicineStatus] via BOTH a text label AND a distinct colour.
 *
 * Accessibility contract: status is always conveyed by the visible text label, not by colour alone.
 * [contentDescription] is set so TalkBack announces the full medication status string.
 *
 * Touch target is enforced to a minimum of 48×48 dp as required by WCAG 2.5.5.
 */
@Composable
fun MedicineStatusChip(
    status: MedicineStatus,
    modifier: Modifier = Modifier,
) {
    val label = when (status) {
        MedicineStatus.CURRENT -> "Current"
        MedicineStatus.PAST    -> "Past"
        MedicineStatus.UNKNOWN -> "Unknown"
    }

    val containerColor = when (status) {
        MedicineStatus.CURRENT -> MaterialTheme.extendedColors.statusCurrent
        MedicineStatus.PAST    -> MaterialTheme.extendedColors.statusPast
        MedicineStatus.UNKNOWN -> MaterialTheme.extendedColors.statusUnknown
    }

    val labelColor = when (status) {
        MedicineStatus.CURRENT -> MaterialTheme.extendedColors.onStatusCurrent
        MedicineStatus.PAST    -> MaterialTheme.extendedColors.onStatusPast
        MedicineStatus.UNKNOWN -> MaterialTheme.extendedColors.onStatusUnknown
    }

    SuggestionChip(
        onClick = {},
        label = { Text(text = label, color = labelColor) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor),
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = "Medication status: $label" },
    )
}
