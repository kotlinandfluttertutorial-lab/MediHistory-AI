package com.medihistory.ai.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Consent management screen — lets the patient review and control which features
 * send data to external AI providers (Gemini API).
 *
 * Privacy note: the app currently sends only patient-confirmed medicine names and
 * doctor names to the Gemini API. No raw document content, OCR text, or personal
 * identifiers are included in any API call.
 *
 * This screen is informational for the MVP — it explains what data is sent and
 * by which features. Full per-feature opt-in/opt-out toggles require a backend
 * consent store and are deferred to a post-MVP release.
 *
 * @param onBack Called when the patient taps the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentManagementScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Consent & Data Sharing") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Navigate back" },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Overview card ─────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "How MediHistory AI uses AI features",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Some features use the Google Gemini API to generate plain-language " +
                            "summaries. Only patient-confirmed medicine names and doctor names " +
                            "are sent — never raw prescription images, OCR text, or personal identifiers. " +
                            "All AI-generated content is clearly labelled and must be verified with " +
                            "your doctor or pharmacist before acting on it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            // ── Feature list ──────────────────────────────────────────────────
            Text(
                text = "AI features that use external services",
                style = MaterialTheme.typography.titleMedium,
            )

            ConsentFeatureRow(
                featureName    = "Prescription Understanding (F11)",
                description    = "Sends confirmed medicine names to Gemini to generate plain-language explanations.",
                dataShared     = "Medicine names and strengths (confirmed by you)",
                isEnabled      = true, // always-on for MVP; per-feature toggles in post-MVP
            )

            HorizontalDivider()

            ConsentFeatureRow(
                featureName    = "Medical History Narrator (F15)",
                description    = "Sends your reviewed prescription timeline to Gemini to write a plain-language narrative.",
                dataShared     = "Prescription dates, medicine names, doctor names (all confirmed by you)",
                isEnabled      = true,
            )

            HorizontalDivider()

            ConsentFeatureRow(
                featureName    = "Prepare for Visit (F12)",
                description    = "Sends your current medicines and open alerts to Gemini to suggest questions for your doctor.",
                dataShared     = "Medicine names, alert ingredient names (confirmed by you)",
                isEnabled      = true,
            )

            // ── On-device features ────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(
                text = "On-device features (no data sent externally)",
                style = MaterialTheme.typography.titleMedium,
            )

            ConsentFeatureRow(
                featureName    = "Duplicate Alert Explainer (F13)",
                description    = "Generates explanations from alert data stored on your device only.",
                dataShared     = "Nothing — runs fully on-device",
                isEnabled      = true,
            )

            HorizontalDivider()

            ConsentFeatureRow(
                featureName    = "OCR Correction Assistant (F14)",
                description    = "Suggests corrections by matching your confirmed doctor names against extracted text.",
                dataShared     = "Nothing — runs fully on-device",
                isEnabled      = true,
            )

            HorizontalDivider()

            ConsentFeatureRow(
                featureName    = "Reminder Advisor (F16)",
                description    = "Parses your confirmed dosage instructions to suggest reminder times.",
                dataShared     = "Nothing — runs fully on-device",
                isEnabled      = true,
            )

            Spacer(Modifier.height(8.dp))

            // ── Notice ────────────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Per-feature opt-out controls will be available in a future update. " +
                        "To opt out of all AI features now, you can disable them by removing " +
                        "your Gemini API key from the app settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ConsentFeatureRow(
    featureName: String,
    description: String,
    dataShared: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = featureName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Data shared: $dataShared",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = null, // read-only in MVP; full toggle in post-MVP
            modifier = Modifier.semantics { contentDescription = "$featureName enabled" },
        )
    }
}
