package com.medihistory.ai.ui.prescription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.ui.components.EmptyStateView
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.components.StatusBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.medihistory.ai.ui.components.OcrStatus as UiOcrStatus

// ─────────────────────────────────────────────────────────────────────────────
// UI state
// ─────────────────────────────────────────────────────────────────────────────

/** UI state for [DoctorGroupScreen]. */
data class DoctorGroupUiState(
    val doctor: Doctor? = null,
    val prescriptions: List<PrescriptionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel for [DoctorGroupScreen].
 *
 * Loads the doctor record and all prescriptions for the given [doctorId] + [patientId] pair.
 * Prescriptions are filtered from the patient-scoped list — the data layer enforces no
 * cross-patient leakage. Filtering by doctorId happens here in the ViewModel.
 *
 * Privacy: never log [doctorId], [patientId], or any prescription data.
 */
@HiltViewModel
class DoctorGroupViewModel
    @Inject
    constructor(
        private val prescriptionRepository: PrescriptionRepository,
        private val doctorRepository: DoctorRepository,
        private val medicineRepository: MedicineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DoctorGroupUiState(isLoading = true))
        val uiState: StateFlow<DoctorGroupUiState> = _uiState.asStateFlow()

        fun load(
            doctorId: String,
            patientId: String,
        ) {
            viewModelScope.launch {
                combine(
                    prescriptionRepository.observeByPatient(patientId),
                    doctorRepository.observeByPatient(patientId),
                    medicineRepository.observeByPatient(patientId),
                ) { prescriptions, doctors, medicines ->
                    val doctor = doctors.firstOrNull { it.id == doctorId }
                    val medicineCountByRx = medicines.groupingBy { it.prescriptionId }.eachCount()
                    val filtered =
                        prescriptions
                            .filter { it.doctorId == doctorId }
                            .map { prescription ->
                                PrescriptionSummary(
                                    id = prescription.id,
                                    doctorName = doctor?.name ?: "Unknown doctor",
                                    doctorSpecialty =
                                        when {
                                            doctor == null -> ""
                                            doctor.specialty == com.medihistory.ai.data.model.DoctorSpecialty.OTHER ->
                                                doctor.specialtyOther ?: "Other"
                                            else -> doctor.specialty.name.lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                        },
                                    prescriptionDate = prescription.prescriptionDate?.toString() ?: "Date unknown",
                                    medicineCount = medicineCountByRx[prescription.id] ?: 0,
                                    ocrStatus = prescription.ocrStatus.toUiOcrStatus(),
                                )
                            }
                    DoctorGroupUiState(
                        doctor = doctor,
                        prescriptions = filtered,
                        isLoading = false,
                        error = null,
                    )
                }
                    .catch { _ ->
                        _uiState.update { it.copy(isLoading = false, error = "Failed to load prescriptions.") }
                    }
                    .collect { state -> _uiState.update { state } }
            }
        }

        private fun OcrStatus.toUiOcrStatus(): UiOcrStatus =
            when (this) {
                OcrStatus.PENDING -> UiOcrStatus.PENDING
                OcrStatus.PROCESSING -> UiOcrStatus.PROCESSING
                OcrStatus.NEEDS_REVIEW -> UiOcrStatus.NEEDS_REVIEW
                OcrStatus.REVIEWED -> UiOcrStatus.REVIEWED
                OcrStatus.ERROR -> UiOcrStatus.ERROR
            }
    }

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Doctor group screen — shows all prescriptions for a single doctor (SCR-02-DOCTOR-GROUP).
 *
 * Displays a doctor summary card (name, specialty, clinic, prescription count) above a
 * chronological prescription timeline (newest first). Each timeline entry shows the date,
 * visit label, medicine count, OCR status badge, and a "View" button.
 *
 * Security: all data is scoped to [patientId]. The [doctorId] acts as a client-side filter
 * on top of the patient-scoped repository.
 *
 * Accessibility:
 * - All interactive elements have ≥48×48 dp touch target via [TextButton] / [IconButton].
 * - Timeline entries carry a merged content description for TalkBack.
 *
 * @param doctorId           ID of the doctor whose prescriptions are shown.
 * @param patientId          The current patient's ID — used for data scoping.
 * @param onBack             Called when the user taps the back arrow.
 * @param onViewPrescription Called with the prescription ID when "View" is tapped.
 * @param viewModel          Injected by Hilt; override in tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorGroupScreen(
    doctorId: String,
    patientId: String,
    onBack: () -> Unit,
    onViewPrescription: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DoctorGroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(doctorId, patientId) {
        viewModel.load(doctorId, patientId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.doctor?.name ?: "Doctor Prescriptions",
                        maxLines = 1,
                    )
                },
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
        when {
            uiState.isLoading -> {
                LoadingIndicator(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentDescription = "Loading prescriptions",
                )
            }

            uiState.error != null -> {
                EmptyStateView(
                    message = uiState.error ?: "An error occurred.",
                    actionLabel = "Retry",
                    onAction = { viewModel.load(doctorId, patientId) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                ) {
                    // ── Doctor summary card ────────────────────────────────────────
                    item {
                        uiState.doctor?.let { doctor ->
                            DoctorSummaryCard(
                                doctor = doctor,
                                prescriptionCount = uiState.prescriptions.size,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                            )
                        }
                    }

                    // ── Prescription count header ──────────────────────────────────
                    item {
                        Text(
                            text = "${uiState.prescriptions.size} prescription(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // ── Empty state ────────────────────────────────────────────────
                    if (uiState.prescriptions.isEmpty()) {
                        item {
                            EmptyStateView(
                                message = "No prescriptions recorded for this doctor.",
                                actionLabel = "Go back",
                                onAction = onBack,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // ── Prescription timeline ──────────────────────────────────────
                    items(
                        items = uiState.prescriptions,
                        key = { rx -> rx.id },
                    ) { rx ->
                        PrescriptionTimelineEntry(
                            prescription = rx,
                            onView = { onViewPrescription(rx.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DoctorSummaryCard(
    doctor: Doctor,
    prescriptionCount: Int,
    modifier: Modifier = Modifier,
) {
    val specialtyLabel = when {
        doctor.specialty == com.medihistory.ai.data.model.DoctorSpecialty.OTHER ->
            doctor.specialtyOther ?: "Other"
        else -> doctor.specialty.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    ElevatedCard(
        modifier =
            modifier.semantics {
                contentDescription = "Doctor ${doctor.name}, $specialtyLabel, $prescriptionCount prescriptions"
            },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null, // decorative
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doctor.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = specialtyLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!doctor.clinicName.isNullOrBlank()) {
                    Text(
                        text = doctor.clinicName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$prescriptionCount prescription(s) recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PrescriptionTimelineEntry(
    prescription: PrescriptionSummary,
    onView: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val medicineLabel = if (prescription.medicineCount == 1) "1 medicine" else "${prescription.medicineCount} medicines"
    val entryContentDescription = "Prescription dated ${prescription.prescriptionDate}, $medicineLabel"

    ElevatedCard(
        modifier = modifier.semantics { contentDescription = entryContentDescription },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prescription.prescriptionDate,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = medicineLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = prescription.ocrStatus)
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onView,
                modifier =
                    Modifier.semantics {
                        contentDescription = "View prescription from ${prescription.prescriptionDate}"
                    },
            ) {
                Text(text = "View")
            }
        }
    }
}
