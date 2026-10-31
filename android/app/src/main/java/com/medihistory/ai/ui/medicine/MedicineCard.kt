package com.medihistory.ai.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Medication
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
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.ui.components.MediCard
import com.medihistory.ai.ui.components.MedicineStatusChip
import com.medihistory.ai.data.model.MedicineStatus as DataMedicineStatus
import com.medihistory.ai.ui.components.MedicineStatus as UiMedicineStatus

/**
 * Card displaying a summary of a single [Medicine] record.
 *
 * Accessibility: [contentDescription] is set on the card surface so TalkBack can
 * announce the medicine name, active ingredient, and status in one phrase.
 * The medicine icon is decorative (contentDescription = null).
 *
 * Touch target: the entire card is clickable and typically exceeds 48 dp in height.
 *
 * Privacy: do not log any parameter value — they contain patient medicine data.
 *
 * @param medicine  The medicine to display.
 * @param onClick   Called with the medicine ID when the card is tapped.
 * @param modifier  Modifier applied to the card.
 */
@Composable
fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ingredientLabel = medicine.activeIngredient ?: "Active ingredient unknown"
    val strengthFormulation =
        listOfNotNull(medicine.strength, medicine.formulation)
            .joinToString(" · ")

    val cardContentDescription =
        buildString {
            append("Medicine ${medicine.medicineName}")
            if (medicine.activeIngredient != null) {
                append(", ${medicine.activeIngredient}")
            }
            append(", prescribed by doctor")
            append(", status ${medicine.status.name.lowercase()}")
        }

    MediCard(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = cardContentDescription },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Medicine icon — decorative
            Icon(
                imageVector = Icons.Outlined.Medication,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Medicine details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = medicine.medicineName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = ingredientLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color =
                        if (medicine.activeIngredient == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (strengthFormulation.isNotEmpty()) {
                    Text(
                        text = strengthFormulation,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (medicine.prescriptionDate != null) {
                    Text(
                        text = medicine.prescriptionDate.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                MedicineStatusChip(
                    status = medicine.status.toUiStatus(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Trailing chevron — decorative
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Maps the data-layer [DataMedicineStatus] to the UI-layer [UiMedicineStatus].
 *
 * Both enums have the same three values (CURRENT, PAST, UNKNOWN); this mapping
 * keeps the data and UI layers decoupled.
 */
private fun DataMedicineStatus.toUiStatus(): UiMedicineStatus =
    when (this) {
        DataMedicineStatus.CURRENT -> UiMedicineStatus.CURRENT
        DataMedicineStatus.PAST -> UiMedicineStatus.PAST
        DataMedicineStatus.UNKNOWN -> UiMedicineStatus.UNKNOWN
    }
