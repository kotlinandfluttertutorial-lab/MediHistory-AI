package com.medihistory.ai.ui.duplicate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.ui.theme.extendedColors

/**
 * Card composable for a single [DuplicateAlertItem] on the Duplicate Review screen.
 *
 * ## Medical safety
 * - The card title always uses "Potential duplicate" — NEVER "Duplicate medication" or
 *   any clinical term.
 * - The expanded view shows the required disclaimer banner: "Potential duplicate — please
 *   review with your doctor or pharmacist."
 * - "Mark as Reviewed" shows a confirmation dialog with the mandatory disclaimer before
 *   the action is committed.
 * - Reviewed alerts remain visible — they are never deleted or hidden by this composable.
 * - No action in this composable blocks the patient from recording medicines.
 *
 * ## Accessibility
 * - The card surface has a full [contentDescription] that TalkBack reads as one phrase.
 * - All interactive elements have a minimum 48×48 dp touch target.
 * - State (reviewed/unreviewed) is conveyed by both text and colour.
 *
 * @param item                The enriched alert item to display.
 * @param onToggleExpand      Called when the patient taps the expand/collapse chevron.
 * @param onMarkReviewed      Called after the patient confirms the disclaimer dialog.
 * @param onViewPrescriptionA Called when the patient taps "View prescription" for medicine A.
 * @param onViewPrescriptionB Called when the patient taps "View prescription" for medicine B.
 */
@Composable
fun DuplicateAlertCard(
    item: DuplicateAlertItem,
    onToggleExpand: () -> Unit,
    onMarkReviewed: () -> Unit,
    onViewPrescriptionA: () -> Unit,
    onViewPrescriptionB: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDisclaimerDialog by rememberSaveable { mutableStateOf(false) }
    var showExplanation by rememberSaveable { mutableStateOf(false) }

    val statusLabel = if (item.alert.isReviewed) "reviewed" else "unreviewed"
    val cardContentDescription =
        "Potential duplicate alert: ${item.medicineAName} and ${item.medicineBName}. " +
            "Matching ingredient: ${item.alert.matchingIngredient}. " +
            "Status: $statusLabel."

    ElevatedCard(
        onClick = onToggleExpand,
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = cardContentDescription },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ---- Collapsed header ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Warning icon — decorative; meaning conveyed by text
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.extendedColors.warning,
                    modifier = Modifier.size(24.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Potential duplicate: ${item.medicineAName} and ${item.medicineBName}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Matching ingredient: ${item.alert.matchingIngredient}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expand / collapse chevron
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector =
                            if (item.isExpanded) {
                                Icons.Outlined.ExpandLess
                            } else {
                                Icons.Outlined.ExpandMore
                            },
                        contentDescription = if (item.isExpanded) "Collapse alert" else "Expand alert",
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Chips row ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Match type chip
                val matchLabel =
                    when (item.alert.matchType) {
                        MatchType.EXACT -> "Exact match"
                        MatchType.PARTIAL -> "Partial match"
                    }
                SuggestionChip(
                    onClick = {},
                    label = { Text(text = matchLabel) },
                    colors =
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.extendedColors.warningContainer,
                            labelColor = MaterialTheme.extendedColors.onWarningContainer,
                        ),
                )

                // Reviewed badge — only shown when reviewed
                if (item.alert.isReviewed) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "Reviewed",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        },
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                    )
                }
            }

            // ---- Expanded details ----
            AnimatedVisibility(
                visible = item.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Medicine A source card
                    MedicineSideCard(
                        label = "Medicine A",
                        medicineName = item.medicineAName,
                        prescriptionId = item.prescriptionAId,
                        onViewPrescription = onViewPrescriptionA,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Medicine B source card
                    MedicineSideCard(
                        label = "Medicine B",
                        medicineName = item.medicineBName,
                        prescriptionId = item.prescriptionBId,
                        onViewPrescription = onViewPrescriptionB,
                    )

                    // Combination note — only for PARTIAL matches
                    if (item.alert.matchType == MatchType.PARTIAL &&
                        item.alert.combinationNote != null
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.alert.combinationNote,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Required disclaimer banner
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.extendedColors.warningContainer,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.extendedColors.warning,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Potential duplicate — please review with your doctor or pharmacist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extendedColors.onWarningContainer,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Explain this alert (F13) ───────────────────────────────────────
                    if (item.alert.explanation != null) {
                        TextButton(
                            onClick = { showExplanation = !showExplanation },
                            modifier =
                                Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .semantics {
                                        contentDescription =
                                            if (showExplanation) {
                                                "Hide explanation for this alert"
                                            } else {
                                                "Explain this alert"
                                            }
                                    },
                        ) {
                            Text(text = if (showExplanation) "Hide explanation" else "Explain this alert")
                        }

                        AnimatedVisibility(
                            visible = showExplanation,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item.alert.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = "AI-generated — always verify with your doctor or pharmacist",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                    colors =
                                        SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .sizeIn(minHeight = 48.dp)
                                            .semantics {
                                                contentDescription =
                                                    "AI-generated — always verify with your doctor or pharmacist"
                                            },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Mark as Reviewed / already reviewed
                    if (!item.alert.isReviewed) {
                        Button(
                            onClick = { showDisclaimerDialog = true },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .sizeIn(minHeight = 48.dp),
                        ) {
                            Text(text = "Mark as Reviewed")
                        }
                    } else {
                        Text(
                            text = "You have reviewed this alert.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // ---- Disclaimer confirmation dialog ----
    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text(text = "Mark as reviewed?") },
            text = {
                Text(
                    text =
                        "Marking as reviewed means you have seen this alert. " +
                            "It does not mean this combination has been approved by a doctor.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDisclaimerDialog = false
                        onMarkReviewed()
                    },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text(text = "I understand, mark as reviewed")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDisclaimerDialog = false },
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Private helper composable
// ---------------------------------------------------------------------------

/**
 * A compact outlined card showing one side (medicine A or B) of a duplicate pair,
 * with a source-link button when a prescription ID is available.
 *
 * @param label           Heading label ("Medicine A" or "Medicine B").
 * @param medicineName    Display name of the medicine.
 * @param prescriptionId  Nullable FK to the source prescription. Button hidden if null.
 * @param onViewPrescription Called when the patient taps the source link button.
 */
@Composable
private fun MedicineSideCard(
    label: String,
    medicineName: String,
    prescriptionId: String?,
    onViewPrescription: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = medicineName,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (prescriptionId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ElevatedButton(
                    onClick = onViewPrescription,
                    modifier = Modifier.sizeIn(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "View prescription")
                }
            }
        }
    }
}
