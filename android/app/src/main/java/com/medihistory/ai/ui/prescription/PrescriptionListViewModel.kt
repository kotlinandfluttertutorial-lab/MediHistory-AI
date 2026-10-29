package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.data.session.SessionManager
import com.medihistory.ai.ui.components.OcrStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for [PrescriptionListScreen].
 *
 * Combines prescriptions and doctors from Room for the current patient, then maps
 * to [PrescriptionSummary] UI models grouped by doctor name.
 *
 * [SyntheticPrescriptions] is intentionally NOT used here — it is for Compose
 * previews and unit tests only.
 */
@HiltViewModel
class PrescriptionListViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val doctorRepository: DoctorRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionListUiState())
    val uiState: StateFlow<PrescriptionListUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    init {
        observePrescriptions()
    }

    private fun observePrescriptions() {
        val patientId = sessionManager.getPatientId()
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            combine(
                prescriptionRepository.observeByPatient(patientId),
                doctorRepository.observeByPatient(patientId),
            ) { prescriptions, doctors ->
                val doctorMap = doctors.associateBy { it.id }
                prescriptions to doctorMap
            }
                .catch { _ ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load prescriptions.")
                    }
                }
                .collect { (prescriptions, doctorMap) ->
                    val summaries = prescriptions.map { rx -> rx.toSummary(doctorMap[rx.doctorId]) }
                    val groups = summaries
                        .groupBy { it.doctorName }
                        .map { (doctorName, rxList) ->
                            // Preserve existing expand state if the group already exists
                            val existingExpanded = _uiState.value.doctorGroups
                                .firstOrNull { it.doctorName == doctorName }
                                ?.isExpanded ?: true
                            DoctorGroup(
                                doctorName = doctorName,
                                specialty = rxList.firstOrNull()?.doctorSpecialty ?: "",
                                prescriptions = rxList,
                                isExpanded = existingExpanded,
                            )
                        }
                    _uiState.update {
                        it.copy(
                            prescriptions = summaries,
                            doctorGroups = groups,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    /** Kept for the error-state retry button in [PrescriptionListScreen]. */
    fun loadPrescriptions() {
        observePrescriptions()
    }

    /** Toggles the expand/collapse state of the doctor group with [doctorName]. */
    fun onToggleGroup(doctorName: String) {
        _uiState.update { state ->
            state.copy(
                doctorGroups = state.doctorGroups.map { group ->
                    if (group.doctorName == doctorName) group.copy(isExpanded = !group.isExpanded)
                    else group
                }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private fun Prescription.toSummary(doctor: Doctor?): PrescriptionSummary {
        val displayName = doctor?.name ?: "Doctor removed"
        val displaySpecialty = when {
            doctor == null -> ""
            doctor.specialty == DoctorSpecialty.OTHER -> doctor.specialtyOther ?: "Other"
            else -> doctor.specialty.name
                .replace('_', ' ')
                .lowercase()
                .replaceFirstChar { it.titlecase() }
        }
        val displayDate = prescriptionDate?.format(dateFormatter) ?: "Date unknown"
        return PrescriptionSummary(
            id = id,
            doctorName = displayName,
            doctorSpecialty = displaySpecialty,
            prescriptionDate = displayDate,
            medicineCount = 0, // populated by a future medicines query
            ocrStatus = ocrStatus.toUiOcrStatus(),
        )
    }

    private fun com.medihistory.ai.data.model.OcrStatus.toUiOcrStatus(): OcrStatus = when (this) {
        com.medihistory.ai.data.model.OcrStatus.PENDING -> OcrStatus.PENDING
        com.medihistory.ai.data.model.OcrStatus.PROCESSING -> OcrStatus.PENDING
        com.medihistory.ai.data.model.OcrStatus.NEEDS_REVIEW -> OcrStatus.NEEDS_REVIEW
        com.medihistory.ai.data.model.OcrStatus.REVIEWED -> OcrStatus.REVIEWED
        com.medihistory.ai.data.model.OcrStatus.ERROR -> OcrStatus.ERROR
    }
}
