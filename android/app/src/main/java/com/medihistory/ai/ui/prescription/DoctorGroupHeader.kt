package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Collapsible section header for a doctor's prescription group (SCR-02-RX-LIST).
 *
 * Shows doctor initials avatar, name, specialty chip, prescription count, and
 * an expand/collapse chevron. Tapping the header toggles [group.isExpanded].
 *
 * Accessibility: the entire row has a content description for TalkBack that
 * includes the doctor name, count, and expanded/collapsed state.
 */
@Composable
fun DoctorGroupHeader(
    group: DoctorGroup,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateLabel = if (group.isExpanded) "expanded" else "collapsed"
    val actionLabel = if (group.isExpanded) "collapse" else "expand"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .semantics {
                contentDescription =
                    "${group.doctorName}, ${group.prescriptions.size} prescription(s), $stateLabel. Double-tap to $actionLabel."
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Doctor initials avatar circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val initials = group.doctorName
                    .split(" ")
                    .filter { it.isNotBlank() && it.first().isLetter() }
                    .takeLast(2)
                    .joinToString("") { it.first().uppercase() }
                Text(
                    text = initials.ifEmpty { "?" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.doctorName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (group.specialty.isNotEmpty()) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = group.specialty,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            modifier = Modifier.height(24.dp),
                        )
                    }
                    Text(
                        text = "${group.prescriptions.size} prescription${if (group.prescriptions.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Icon(
                imageVector = if (group.isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
