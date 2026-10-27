package com.medihistory.ai.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.data.history.ContentType
import com.medihistory.ai.data.history.HistoryEntry
import com.medihistory.ai.data.history.HistorySection
import com.medihistory.ai.data.history.SectionType
import com.medihistory.ai.data.history.SourceReference

/**
 * Renders a single [HistorySection] within the medical history screen.
 *
 * Layout:
 * - Section title row with a [SectionType]-specific icon.
 * - If [HistorySection.isAiGenerated], an amber "AI-generated" chip appears before entries.
 * - Each [HistoryEntry] shows a provenance icon, body text, and clickable source chips.
 *
 * Accessibility:
 * - All clickable source chips have contentDescription for TalkBack.
 * - Icons are either decorative (contentDescription = null) or described via chip labels.
 *
 * Medical safety:
 * - This composable never generates, modifies, or infers text — it only renders what
 *   [HistorySection] provides.
 *
 * @param section        The section data to render.
 * @param onViewSource   Called with a prescriptionId when the patient taps a source chip.
 */
@Composable
fun HistorySectionView(
    section: HistorySection,
    onViewSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Section title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = sectionIcon(section.type),
                    contentDescription = null, // decorative — title text conveys meaning
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // AI-generated chip — displayed before entries when section is AI-generated
            if (section.isAiGenerated) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(text = "AI-generated") },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                    modifier = Modifier.semantics {
                        contentDescription = "AI-generated section"
                    },
                )
            }

            // Entries
            if (section.entries.isEmpty()) {
                Text(
                    text = "No records available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                section.entries.forEach { entry ->
                    HistoryEntryRow(
                        entry = entry,
                        onViewSource = onViewSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: HistoryEntry,
    onViewSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = contentTypeIcon(entry.contentType),
                contentDescription = contentTypeDescription(entry.contentType),
                modifier = Modifier.size(16.dp),
                tint = contentTypeIconTint(entry.contentType),
            )
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }

        // Source chips
        if (entry.sourceReferences.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 24.dp),
            ) {
                entry.sourceReferences.forEach { ref ->
                    SourceChip(
                        reference = ref,
                        onViewSource = onViewSource,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceChip(
    reference: SourceReference,
    onViewSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = { onViewSource(reference.prescriptionId) },
        label = { Text(text = "Source ↗") },
        modifier = modifier.semantics {
            contentDescription = "View source prescription"
        },
    )
}

// -------------------------------------------------------------------------
// Icon helpers
// -------------------------------------------------------------------------

private fun sectionIcon(type: SectionType): ImageVector = when (type) {
    SectionType.MEDICINES  -> Icons.Outlined.MedicalServices
    SectionType.DOCTORS    -> Icons.Outlined.LocalHospital
    SectionType.TIMELINE   -> Icons.Outlined.Timeline
    SectionType.AI_SUMMARY -> Icons.Outlined.AutoAwesome
}

private fun contentTypeIcon(type: ContentType): ImageVector = when (type) {
    ContentType.DOCUMENTED_FACT   -> Icons.Outlined.CheckCircle
    ContentType.UNCERTAIN_EXTRACTED -> Icons.AutoMirrored.Outlined.HelpOutline
    ContentType.PATIENT_REPORTED  -> Icons.Outlined.Person
    ContentType.AI_GENERATED      -> Icons.Outlined.AutoAwesome
}

private fun contentTypeDescription(type: ContentType): String = when (type) {
    ContentType.DOCUMENTED_FACT     -> "Documented fact"
    ContentType.UNCERTAIN_EXTRACTED -> "Uncertain — low confidence extraction"
    ContentType.PATIENT_REPORTED    -> "Patient reported"
    ContentType.AI_GENERATED        -> "AI generated"
}

@Composable
private fun contentTypeIconTint(type: ContentType) = when (type) {
    ContentType.DOCUMENTED_FACT     -> MaterialTheme.colorScheme.primary
    ContentType.UNCERTAIN_EXTRACTED -> MaterialTheme.colorScheme.error
    ContentType.PATIENT_REPORTED    -> MaterialTheme.colorScheme.secondary
    ContentType.AI_GENERATED        -> MaterialTheme.colorScheme.tertiary
}
