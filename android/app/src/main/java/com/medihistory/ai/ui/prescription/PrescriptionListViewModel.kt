package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
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
 * Combines prescriptions, doctors, and medicines from Room for the current patient,
 * then maps to [PrescriptionSummary] UI models grouped by doctor name.
 */
@HiltViewModel
class PrescriptionListViewModel
    @Inject
    constructor(
        private val prescriptionRepository: PrescriptionRepository,
        private val doctorRepository: DoctorRepository,
        private val medicineRepository: MedicineRepository,
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
                    medicineRepository.observeByPatient(patientId),
                ) { prescriptions, doctors, medicines ->
                    val doctorMap = doctors.associateBy { it.id }
                    // Count medicines per prescription for the card subtitle.
                    val medicineCountByRx = medicines.groupingBy { it.prescriptionId }.eachCount()
                    Triple(prescriptions, doctorMap, medicineCountByRx)
                }
                    .catch { _ ->
                        _uiState.update {
                            it.copy(isLoading = false, error = "Failed to load prescriptions.")
                        }
                    }
                    .collect { (prescriptions, doctorMap, medicineCountByRx) ->
                        val summaries =
                            prescriptions.map { rx ->
                                rx.toSummary(
                                    doctor = doctorMap[rx.doctorId],
                                    medicineCount = medicineCountByRx[rx.id] ?: 0,
                                )
                            }
                        val groups =
                            summaries
                                .groupBy { it.doctorName }
                                .map { (doctorName, rxList) ->
                                    val existingExpanded =
                                        _uiState.value.doctorGroups
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

        fun loadPrescriptions() {
            observePrescriptions()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.update { state ->
                val groups =
                    if (query.isBlank()) {
                        groupFromSummaries(state.prescriptions, state.doctorGroups)
                    } else {
                        val filtered =
                            state.prescriptions.filter { rx ->
                                rx.doctorName.contains(query, ignoreCase = true) ||
                                    rx.doctorSpecialty.contains(query, ignoreCase = true) ||
                                    rx.prescriptionDate.contains(query, ignoreCase = true)
                            }
                        groupFromSummaries(filtered, state.doctorGroups)
                    }
                state.copy(searchQuery = query, doctorGroups = groups)
            }
        }

        fun onToggleSearch() {
            _uiState.update { state ->
                if (state.isSearchActive) {
                    // Closing — restore all groups unfiltered
                    val allGroups = groupFromSummaries(state.prescriptions, state.doctorGroups)
                    state.copy(isSearchActive = false, searchQuery = "", doctorGroups = allGroups)
                } else {
                    state.copy(isSearchActive = true)
                }
            }
        }

        fun onSortNewestFirst() {
            _uiState.update { state ->
                val sorted = state.prescriptions.sortedByDescending { it.prescriptionDate }
                state.copy(prescriptions = sorted, doctorGroups = groupFromSummaries(sorted, state.doctorGroups))
            }
        }

        fun onSortOldestFirst() {
            _uiState.update { state ->
                val sorted = state.prescriptions.sortedBy { it.prescriptionDate }
                state.copy(prescriptions = sorted, doctorGroups = groupFromSummaries(sorted, state.doctorGroups))
            }
        }

        private fun groupFromSummaries(
            summaries: List<PrescriptionSummary>,
            existing: List<DoctorGroup>,
        ): List<DoctorGroup> =
            summaries
                .groupBy { it.doctorName }
                .map { (doctorName, rxList) ->
                    val existingExpanded = existing.firstOrNull { it.doctorName == doctorName }?.isExpanded ?: true
                    DoctorGroup(
                        doctorName = doctorName,
                        specialty = rxList.firstOrNull()?.doctorSpecialty ?: "",
                        prescriptions = rxList,
                        isExpanded = existingExpanded,
                    )
                }

        fun onToggleGroup(doctorName: String) {
            _uiState.update { state ->
                state.copy(
                    doctorGroups =
                        state.doctorGroups.map { group ->
                            if (group.doctorName == doctorName) {
                                group.copy(isExpanded = !group.isExpanded)
                            } else {
                                group
                            }
                        },
                )
            }
        }

        // -------------------------------------------------------------------------
        // Mapping helpers
        // -------------------------------------------------------------------------

        private fun Prescription.toSummary(
            doctor: Doctor?,
            medicineCount: Int,
        ): PrescriptionSummary {
            val displayName = doctor?.name ?: "Doctor removed"
            val displaySpecialty =
                when {
                    doctor == null -> ""
                    doctor.specialty == DoctorSpecialty.OTHER -> doctor.specialtyOther ?: "Other"
                    else ->
                        doctor.specialty.name
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
                medicineCount = medicineCount,
                ocrStatus = ocrStatus.toUiOcrStatus(),
            )
        }

        private fun com.medihistory.ai.data.model.OcrStatus.toUiOcrStatus(): OcrStatus =
            when (this) {
                com.medihistory.ai.data.model.OcrStatus.PENDING -> OcrStatus.PENDING
                com.medihistory.ai.data.model.OcrStatus.PROCESSING -> OcrStatus.PENDING
                com.medihistory.ai.data.model.OcrStatus.NEEDS_REVIEW -> OcrStatus.NEEDS_REVIEW
                com.medihistory.ai.data.model.OcrStatus.REVIEWED -> OcrStatus.REVIEWED
                com.medihistory.ai.data.model.OcrStatus.ERROR -> OcrStatus.ERROR
            }
    }
