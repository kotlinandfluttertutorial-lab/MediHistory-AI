package com.medihistory.ai.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.ui.components.OcrStatus as UiOcrStatus
import com.medihistory.ai.ui.prescription.PrescriptionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [DashboardScreen].
 *
 * Aggregates counts from multiple repositories and exposes the result as [DashboardUiState].
 * All queries are scoped to the [patientId] passed to [load].
 *
 * Privacy: [patientId] is never logged or surfaced in UI strings.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val medicineRepository: MedicineRepository,
    private val doctorRepository: DoctorRepository,
    private val duplicateAlertDao: DuplicateAlertDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Starts observing all data sources for [patientId].
     *
     * Uses [combine] to keep all counts in sync; any update in any source
     * triggers a full recomposition of [DashboardUiState].
     */
    fun load(patientId: String) {
        viewModelScope.launch {
            combine(
                prescriptionRepository.observeByPatient(patientId),
                medicineRepository.observeByPatient(patientId),
                doctorRepository.observeByPatient(patientId),
                duplicateAlertDao.observeUnreviewedCount(patientId),
            ) { prescriptions, medicines, doctors, unreviewedAlerts ->
                val pendingOcrCount = prescriptions.count {
                    it.ocrStatus == OcrStatus.PENDING || it.ocrStatus == OcrStatus.NEEDS_REVIEW
                }
                // Build the 3-item recent prescriptions list from the repository data.
                // Prescriptions arrive newest-first per PrescriptionRepository contract.
                val recentSummaries = prescriptions.take(3).map { prescription ->
                    PrescriptionSummary(
                        id = prescription.id,
                        doctorName = prescription.doctorId ?: "Unknown doctor",
                        doctorSpecialty = "",
                        prescriptionDate = prescription.prescriptionDate?.toString() ?: "Date unknown",
                        medicineCount = 0, // resolved separately in a future ticket
                        ocrStatus = prescription.ocrStatus.toUiOcrStatus(),
                    )
                }
                DashboardUiState(
                    prescriptionCount = prescriptions.size,
                    doctorCount = doctors.size,
                    medicineCount = medicines.size,
                    pendingAlertCount = unreviewedAlerts,
                    pendingOcrCount = pendingOcrCount,
                    recentPrescriptions = recentSummaries,
                    isLoading = false,
                    error = null,
                )
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load dashboard data.",
                        )
                    }
                }
                .collect { state ->
                    _uiState.update { state }
                }
        }
    }

    private fun OcrStatus.toUiOcrStatus(): UiOcrStatus = when (this) {
        OcrStatus.PENDING      -> UiOcrStatus.PENDING
        OcrStatus.PROCESSING   -> UiOcrStatus.PROCESSING
        OcrStatus.NEEDS_REVIEW -> UiOcrStatus.NEEDS_REVIEW
        OcrStatus.REVIEWED     -> UiOcrStatus.REVIEWED
        OcrStatus.ERROR        -> UiOcrStatus.ERROR
    }
}
