package com.medihistory.ai.ui.newdoctor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Consent dialog shown before the patient shares their medical summary.
 *
 * Medical safety and privacy requirements:
 * - MUST be shown before any sharing occurs. Sharing is impossible without this confirmation.
 * - Shows a clear disclaimer that this is NOT a clinical record.
 * - Shows the number of prescriptions the patient is about to share.
 * - The patient can cancel at any time — cancellation does not share anything.
 *
 * The "Share now" button calls [onConfirm] only when the patient explicitly taps it.
 * There is no auto-confirm or default confirm path.
 *
 * @param selectedCount Number of prescription records selected for sharing. Shown in the dialog
 *                      body so the patient knows exactly what they are consenting to share.
 * @param onConfirm     Called only when the patient explicitly taps "Share now".
 *                      The caller must then initiate the Android share sheet.
 * @param onDismiss     Called when the patient taps "Cancel" or dismisses the dialog.
 *                      No sharing occurs.
 */
@Composable
fun ConsentDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val prescriptionWord = if (selectedCount == 1) "prescription" else "prescriptions"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Share Medical Summary")
        },
        text = {
            Column {
                Text(
                    text = "You are about to share a patient-compiled summary with " +
                        "$selectedCount $prescriptionWord.",
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This does NOT constitute a clinical record.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Disclaimer: Patient-compiled record. Not a clinical report. " +
                        "Please verify all information with your doctor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Share now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
