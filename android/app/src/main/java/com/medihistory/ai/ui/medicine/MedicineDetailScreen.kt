package com.medihistory.ai.ui.medicine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.components.MedicineStatusChip
import com.medihistory.ai.ui.components.MedicineStatus as UiMedicineStatus

/**
 * Medicine detail screen — feature F04 (Medicine Explorer).
 *
 * Displays a single medicine's extracted fields, source prescription reference, and
 * a disclaimer reminding the patient to verify information with a doctor or pharmacist.
 *
 * Layout:
 * 1. [TopAppBar] with back navigation.
 * 2. Header card: icon, medicine name, active ingredient (or fallback), status chip.
 * 3. "Details" section: strength, formulation, dosage instructions.
 * 4. "Source prescription" section: date, doctor ID, view-prescription button.
 * 5. Disclaimer text.
 *
 * Accessibility:
 * - All sections carry meaningful [contentDescription] via the [semantics] modifier.
 * - The disclaimer is included in the semantic tree so TalkBack reads it.
 * - All interactive elements meet the 48×48 dp minimum touch target.
 *
 * Medical safety:
 * - Null fields are shown as "Not available" — values are NEVER fabricated or inferred.
 * - The disclaimer is always visible and cannot be dismissed.
 *
 * Privacy: no medicine data is logged. [patientId] is never exposed in UI strings.
 *
 * @param medicineId         ID of the medicine to display.
 * @param patientId          ID of the current patient; used to scope the repository call.
 * @param onBack             Called when the user taps the back arrow.
 * @param onViewPrescription Called with the prescription ID when the button is tapped.
 * @param viewModel          Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineDetailScreen(
    medicineId: String,
    patientId: String,
    onBack: () -> Unit,
    onViewPrescription: (String) -> Unit,
    onViewDuplicates: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MedicineDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(medicineId, patientId) {
        viewModel.load(medicineId = medicineId, patientId = patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.medicine?.medicineName ?: "Medicine",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Navigate back"
                            },
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
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentDescription = "Loading medicine details",
                )
            }

            uiState.error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = uiState.error ?: "An error occurred.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Error: ${uiState.error}"
                            },
                    )
                }
            }

            uiState.medicine != null -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    // Duplicate alert banner — safety critical, shown first
                    if (uiState.hasDuplicateAlert) {
                        androidx.compose.material3.Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors =
                                androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    contentDescription = "Duplicate alert",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Potential duplicate ingredient",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "${uiState.duplicateAlertCount} alert(s) — review with your doctor",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                TextButton(onClick = onViewDuplicates) {
                                    Text("Review →", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    MedicineDetailContent(
                        medicine = requireNotNull(uiState.medicine),
                        onViewPrescription = onViewPrescription,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Content composable — separated so it can be tested with a known medicine
// ---------------------------------------------------------------------------

private const val NOT_AVAILABLE = "Not available"
internal const val DISCLAIMER_TEXT =
    "This information was extracted from a prescription document. " +
        "Please verify with your doctor or pharmacist."

/**
 * Maps the data-layer [MedicineStatus] to the UI-layer [UiMedicineStatus].
 *
 * Both enums mirror each other intentionally; keeping them in separate packages
 * prevents the UI from depending on Room annotations.
 */
private fun MedicineStatus.toUiStatus(): UiMedicineStatus =
    when (this) {
        MedicineStatus.CURRENT -> UiMedicineStatus.CURRENT
        MedicineStatus.PAST -> UiMedicineStatus.PAST
        MedicineStatus.UNKNOWN -> UiMedicineStatus.UNKNOWN
    }

@Composable
private fun MedicineDetailContent(
    medicine: Medicine,
    onViewPrescription: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── 1. Header card ────────────────────────────────────────────────
        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Medicine header: ${medicine.medicineName}" },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Medication,
                    contentDescription = null, // decorative — name is in the text below
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = medicine.medicineName,
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (medicine.activeIngredient != null) {
                    Text(
                        text = medicine.activeIngredient,
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                } else {
                    Text(
                        text = "Active ingredient unknown",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MedicineStatusChip(status = medicine.status.toUiStatus())
            }
        }

        // ── 2. Details section ────────────────────────────────────────────
        SectionCard(
            title = "Details",
            contentDescription = "Medicine details",
        ) {
            LabeledRow(label = "Strength", value = medicine.strength)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LabeledRow(label = "Formulation", value = medicine.formulation)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LabeledRow(label = "Dosage instructions", value = medicine.dosageInstructions)
        }

        // ── 3. Source prescription section ────────────────────────────────
        SectionCard(
            title = "Source prescription",
            contentDescription = "Source prescription details",
        ) {
            LabeledRow(
                label = "Prescription date",
                value = medicine.prescriptionDate?.toString(),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LabeledRow(
                label = "Prescribing doctor",
                value = medicine.prescribingDoctorId?.let { "Doctor ID: $it" },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ElevatedButton(
                onClick = { onViewPrescription(medicine.prescriptionId) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "View source prescription" },
            ) {
                Text(text = "View prescription")
            }
        }

        // ── 4. Disclaimer ─────────────────────────────────────────────────
        Text(
            text = DISCLAIMER_TEXT,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .semantics { contentDescription = DISCLAIMER_TEXT },
        )
    }
}

/** A card with a title header and slot content below a divider. */
@Composable
private fun SectionCard(
    title: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

/** A two-column row showing a [label] and its [value], or "Not available" when null. */
@Composable
private fun LabeledRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$label: ${value ?: NOT_AVAILABLE}"
                },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value ?: NOT_AVAILABLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f),
        )
    }
}
