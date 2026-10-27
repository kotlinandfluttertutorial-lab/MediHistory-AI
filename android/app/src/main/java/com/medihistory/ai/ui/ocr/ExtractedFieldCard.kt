package com.medihistory.ai.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.medihistory.ai.data.ocr.ReviewState
import com.medihistory.ai.ui.components.ConfidenceBadge
import com.medihistory.ai.ui.theme.extendedColors
import kotlin.math.roundToInt

/** Confidence threshold below which a field is highlighted as low-confidence. */
private const val LOW_CONFIDENCE_THRESHOLD = 0.70f

/**
 * Card displaying a single OCR-extracted field and its review controls.
 *
 * Medical-safety contract:
 * - Amber border + tint is shown when the field is PENDING_REVIEW AND confidence < 0.70.
 *   State is conveyed by BOTH colour AND an icon + text label — never colour alone.
 * - [ConfidenceBadge] is always visible regardless of review state.
 * - Review state chip always shows BOTH colour AND text label.
 * - Fields with null rawValue show a "Not extracted" chip — no review actions offered.
 * - contentDescription announces fieldName, displayValue, confidence %, and status for
 *   TalkBack users.
 * - All interactive controls have a minimum touch target of 48×48 dp (WCAG 2.5.5).
 *
 * @param field             The field state to display.
 * @param onConfirm         Called when the patient taps "Confirm" (PENDING → REVIEWED).
 * @param onEdit            Called when the patient taps "Edit" (opens edit mode).
 * @param onReject          Called when the patient taps "Reject" (PENDING → REJECTED).
 * @param onUnreview        Called when the patient taps "Un-review" (REVIEWED/REJECTED → PENDING).
 * @param onEditTextChange  Called on every keystroke while editing.
 * @param onSaveEdit        Called when the patient taps "Save" in edit mode.
 */
@Composable
fun ExtractedFieldCard(
    field: FieldReviewState,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onReject: () -> Unit,
    onUnreview: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isLowConfidencePending =
        field.reviewState == ReviewState.PENDING_REVIEW && field.confidence < LOW_CONFIDENCE_THRESHOLD

    val cardDescription = buildString {
        append("${field.fieldName}: ${field.displayValue}. ")
        append("Confidence ${(field.confidence * 100).roundToInt()}%. ")
        append("Status: ${field.reviewState.name.replace('_', ' ')}")
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLowConfidencePending) {
                    Modifier.border(
                        width  = 2.dp,
                        color  = MaterialTheme.extendedColors.warning,
                        shape  = CardDefaults.elevatedShape,
                    )
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = cardDescription },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLowConfidencePending) {
                        Modifier.background(
                            color = MaterialTheme.extendedColors.warningContainer.copy(alpha = 0.18f),
                            shape = CardDefaults.elevatedShape,
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
        ) {
            // ── Field name ────────────────────────────────────────────────────
            Text(
                text  = field.fieldName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // ── Low-confidence warning banner (colour + icon + text) ──────────
            if (isLowConfidencePending) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Warning,
                        contentDescription = null,  // announced via card contentDescription
                        tint               = MaterialTheme.extendedColors.warning,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = "Low confidence — please verify",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.extendedColors.warning,
                    )
                }
            }

            // ── Display value ─────────────────────────────────────────────────
            val valueStyle = if (field.patientValue == null && field.rawValue != null) {
                // rawValue shown but not yet confirmed — italic to indicate unverified
                MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
            } else {
                MaterialTheme.typography.bodyMedium
            }
            Text(
                text  = field.displayValue,
                style = valueStyle,
            )

            Spacer(Modifier.height(8.dp))

            // ── Confidence badge (always shown) ───────────────────────────────
            ConfidenceBadge(confidence = field.confidence)

            Spacer(Modifier.height(8.dp))

            // ── Review state chip ─────────────────────────────────────────────
            if (field.rawValue == null) {
                // Field was absent from the source document — no review required.
                NotExtractedChip()
            } else {
                ReviewStateChip(reviewState = field.reviewState)
            }

            // ── Edit mode ─────────────────────────────────────────────────────
            if (field.isEditing) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = field.editText,
                    onValueChange = onEditTextChange,
                    label         = { Text("Edit value") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick  = onCancelEdit,    // cancels without saving
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick  = onSaveEdit,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Text("Save")
                    }
                }
            }

            // ── PENDING_REVIEW actions ────────────────────────────────────────
            if (!field.isEditing && field.reviewState == ReviewState.PENDING_REVIEW && field.rawValue != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick  = onConfirm,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text("Confirm") }
                    TextButton(
                        onClick  = onEdit,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text("Edit") }
                    TextButton(
                        onClick  = onReject,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                    ) { Text("Reject") }
                }
            }

            // ── REVIEWED un-review action ─────────────────────────────────────
            if (field.reviewState == ReviewState.REVIEWED) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick  = onUnreview,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(
                        text  = "Un-review",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── REJECTED un-reject action ─────────────────────────────────────
            if (field.reviewState == ReviewState.REJECTED) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick  = onUnreview,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Text(
                        text  = "Un-reject",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private chip helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Chip shown for fields absent from the source document.
 * Medical-safety: no review actions are available for absent fields.
 */
@Composable
private fun NotExtractedChip() {
    SuggestionChip(
        onClick = {},
        label   = { Text("Not extracted") },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor     = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    )
}

/**
 * Chip that conveys [ReviewState] using BOTH colour AND text label.
 * Medical-safety: state must never be conveyed by colour alone.
 *
 * - REVIEWED     → green container + "Reviewed"
 * - PENDING_REVIEW → amber container + "Pending review"
 * - REJECTED     → error container + "Rejected"
 */
@Composable
private fun ReviewStateChip(reviewState: ReviewState) {
    val (containerColor, labelColor, label) = when (reviewState) {
        ReviewState.REVIEWED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Reviewed",
        )
        ReviewState.PENDING_REVIEW -> Triple(
            MaterialTheme.extendedColors.warningContainer,
            MaterialTheme.extendedColors.onWarningContainer,
            "Pending review",
        )
        ReviewState.REJECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Rejected",
        )
    }

    SuggestionChip(
        onClick = {},
        label   = { Text(label) },
        colors  = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor     = labelColor,
        ),
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    )
}
