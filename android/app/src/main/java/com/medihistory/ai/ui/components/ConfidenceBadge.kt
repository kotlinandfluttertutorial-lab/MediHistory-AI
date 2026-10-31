package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.theme.extendedColors
import kotlin.math.roundToInt

/**
 * Returns the human-readable confidence level string for a given [confidence] value.
 *
 * This is a pure function exposed at package level so it can be tested directly in unit tests
 * without a Compose test rule.
 *
 * - ≥ 0.70 → "High"
 * - 0.55–0.69 → "Medium"
 * - < 0.55 → "Low"
 */
internal fun getConfidenceLevel(confidence: Float): String =
    when {
        confidence >= 0.70f -> "High"
        confidence >= 0.55f -> "Medium"
        else -> "Low"
    }

/**
 * A chip that displays an OCR field's [confidence] score as both a percentage and a level label.
 *
 * Accessibility contract: state is conveyed by text label, never by colour alone.
 * [contentDescription] announces both the percentage and the level for TalkBack users.
 *
 * Touch target is enforced to a minimum of 48×48 dp as required by WCAG 2.5.5.
 *
 * @param confidence Float in [0.0, 1.0] representing OCR confidence for this field.
 */
@Composable
fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier,
) {
    val pct = (confidence * 100).roundToInt()
    val level = getConfidenceLevel(confidence)
    val displayText = "$pct% \u2022 $level"

    val containerColor =
        when (level) {
            "High" -> MaterialTheme.colorScheme.tertiaryContainer
            "Medium" -> MaterialTheme.extendedColors.warningContainer
            else -> MaterialTheme.colorScheme.errorContainer
        }

    val labelColor =
        when (level) {
            "High" -> MaterialTheme.colorScheme.onTertiaryContainer
            "Medium" -> MaterialTheme.extendedColors.onWarningContainer
            else -> MaterialTheme.colorScheme.onErrorContainer
        }

    AssistChip(
        onClick = {},
        label = { Text(text = displayText, color = labelColor) },
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor),
        modifier =
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .semantics { contentDescription = "Confidence: $pct%, $level" },
    )
}
