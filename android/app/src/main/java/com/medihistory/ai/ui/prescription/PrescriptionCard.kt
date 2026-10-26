package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.components.MediCard
import com.medihistory.ai.ui.components.StatusBadge

/**
 * Card that represents a single [PrescriptionSummary] in the prescription list.
 *
 * Layout: [72×72 dp placeholder icon] | [doctor name / specialty + date / medicine count / status badge]
 * followed by a trailing chevron.
 *
 * Accessibility: the outer [MediCard] provides a merged content description that announces
 * all key fields to TalkBack. Individual text nodes are not separately focusable.
 *
 * If [PrescriptionSummary.doctorName] equals "Doctor removed", the name is rendered in italic
 * with [MaterialTheme.colorScheme.onSurfaceVariant] to signal that the doctor record no longer
 * exists.
 *
 * @param prescription The data to display.
 * @param onClick      Called when the card is tapped.
 * @param modifier     Optional modifier applied to the card.
 */
@Composable
fun PrescriptionCard(
    prescription: PrescriptionSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDoctorRemoved = prescription.doctorName == "Doctor removed"
    val ocrStatusLabel = prescription.ocrStatus.name
        .lowercase()
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }

    val cardContentDescription =
        "Prescription from ${prescription.doctorName}, " +
            "dated ${prescription.prescriptionDate}, " +
            "${prescription.medicineCount} medicines, " +
            ocrStatusLabel

    MediCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = cardContentDescription
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            // Prescription document icon placeholder — decorative, description on the card root.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null, // decorative; card root has the full description
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content column
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = prescription.doctorName,
                    style = MaterialTheme.typography.titleMedium.let { style ->
                        if (isDoctorRemoved) style.copy(fontStyle = FontStyle.Italic) else style
                    },
                    color = if (isDoctorRemoved) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                val subtitleParts = buildList {
                    if (prescription.doctorSpecialty.isNotEmpty()) add(prescription.doctorSpecialty)
                    add(prescription.prescriptionDate)
                }
                Text(
                    text = subtitleParts.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                val medicineLabel = if (prescription.medicineCount == 1) {
                    "1 medicine"
                } else {
                    "${prescription.medicineCount} medicines"
                }
                Text(
                    text = medicineLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                StatusBadge(status = prescription.ocrStatus)
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing chevron — decorative navigation affordance
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null, // decorative; overall action is on the card
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
