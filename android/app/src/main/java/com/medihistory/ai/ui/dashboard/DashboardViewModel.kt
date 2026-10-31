package com.medihistory.ai.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.ai.PrepareForVisitAgent
import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.DuplicateAlertRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PatientRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.ui.prescription.PrescriptionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import com.medihistory.ai.ui.components.OcrStatus as UiOcrStatus

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
    private val duplicateAlertRepository: DuplicateAlertRepository,
    private val patientRepository: PatientRepository,
    private val prepareForVisitAgent: PrepareForVisitAgent,
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun load(patientId: String) {
        val today = LocalDate.now().format(dateFormatter)
        viewModelScope.launch {
            // Resolve patient name separately — it's a one-shot read, not a reactive stream.
            val patientName = patientRepository.getById(patientId)?.name?.ifBlank { null }

            combine(
                prescriptionRepository.observeByPatient(patientId),
                medicineRepository.observeByPatient(patientId),
                doctorRepository.observeByPatient(patientId),
                duplicateAlertDao.observeUnreviewedCount(patientId),
            ) { prescriptions, medicines, doctors, unreviewedAlerts ->
                val pendingOcrCount = prescriptions.count {
                    it.ocrStatus == OcrStatus.PENDING || it.ocrStatus == OcrStatus.NEEDS_REVIEW
                }
                // Count medicines per prescription — mirrors the logic in PrescriptionListViewModel.
                val medicineCountByRx = medicines.groupingBy { it.prescriptionId }.eachCount()
                val recentSummaries = prescriptions.take(3).map { prescription ->
                    PrescriptionSummary(
                        id = prescription.id,
                        doctorName = prescription.doctorId ?: "Unknown doctor",
                        doctorSpecialty = "",
                        prescriptionDate = prescription.prescriptionDate?.toString() ?: "Date unknown",
                        medicineCount = medicineCountByRx[prescription.id] ?: 0,
                        ocrStatus = prescription.ocrStatus.toUiOcrStatus(),
                    )
                }
                DashboardUiState(
                    patientName = patientName ?: "",
                    currentDate = today,
                    syncStatus = "MediHistory AI care records sync is active",
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
                .catch { _ ->
                    _uiState.update {
                        it.copy(isLoading = false, error = "Failed to load dashboard data.")
                    }
                }
                .collect { state -> _uiState.update { state } }
        }
    }

    /**
     * Generates a pre-visit summary using [PrepareForVisitAgent] (F12).
     *
     * Called when the patient taps "Prepare for visit" on the Dashboard.
     * Result is stored in [DashboardUiState.visitSummary].
     *
     * Medical safety: input is confirmed medicine data and open duplicate alerts only.
     * Output always carries an "AI-generated" disclaimer shown in the UI.
     *
     * Privacy: [patientId] is scoped by every repository call — never logged.
     */
    fun prepareForVisit(patientId: String) {
        _uiState.update { it.copy(isVisitSummaryLoading = true) }
        viewModelScope.launch {
            try {
                val medicines = medicineRepository.observeByPatient(patientId).first()
                val openAlerts = duplicateAlertRepository.observeByPatient(patientId)
                    .first()
                    .filter { !it.isReviewed }

                val summary = prepareForVisitAgent.prepare(
                    medicines  = medicines,
                    openAlerts = openAlerts,
                )
                _uiState.update { it.copy(visitSummary = summary, isVisitSummaryLoading = false) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _uiState.update { it.copy(isVisitSummaryLoading = false) }
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
