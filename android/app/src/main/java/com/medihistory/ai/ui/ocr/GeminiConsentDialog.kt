package com.medihistory.ai.ui.ocr

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * One-time consent dialog shown before the first Gemini-enhanced OCR extraction.
 *
 * Medical-safety: explains clearly that OCR text (not the image) will be sent to
 * Google's Gemini AI servers. The patient can decline; the app falls back to
 * on-device ML Kit only.
 *
 * Privacy: the dialog body is static copy — no patient data is rendered inside it.
 *
 * @param onAccept  Called when the patient taps "Allow" — consent is granted.
 * @param onDecline Called when the patient taps "Not now" or dismisses the dialog.
 */
@Composable
fun GeminiConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Text(
                text = "Improve prescription reading?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text =
                    "To better read medicine names (especially handwritten prescriptions), " +
                        "text from your prescription is sent to Google Gemini AI.\n\n" +
                        "No images or personal details are sent. Google does not store this data " +
                        "after processing.\n\n" +
                        "You can change this in More \u2192 Privacy.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Allow Gemini AI to improve prescription reading"
                    },
            ) {
                Text("Allow")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                modifier =
                    Modifier.semantics {
                        contentDescription = "Decline Gemini AI, use on-device reading only"
                    },
            ) {
                Text("Not now")
            }
        },
    )
}
