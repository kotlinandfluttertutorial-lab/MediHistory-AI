package com.medihistory.ai.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.history.HistoryCompiler
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [MedicalHistoryScreen].
 *
 * Fetches reviewed prescriptions, medicines, and doctors for [patientId],
 * then delegates deterministic compilation to [HistoryCompiler].
 *
 * Medical safety invariants:
 * - Only [OcrStatus.REVIEWED] prescriptions contribute to the compiled history.
 * - [HistoryCompiler] never fabricates data — all entries link to source records.
 * - The AI_SUMMARY section is a stub; no AI model is called here.
 *
 * Privacy: never log patient ID, medicine names, or doctor names in Logcat.
 */
@HiltViewModel
class MedicalHistoryViewModel
    @Inject
    constructor(
        private val prescriptionRepository: PrescriptionRepository,
        private val medicineRepository: MedicineRepository,
        private val doctorRepository: DoctorRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MedicalHistoryUiState())
        val uiState: StateFlow<MedicalHistoryUiState> = _uiState.asStateFlow()

        /**
         * Compiles the medical history for [patientId].
         *
         * Progress steps reported via [MedicalHistoryUiState.generationProgress]:
         * 0.0 — started
         * 0.33 — prescriptions fetched
         * 0.66 — medicines fetched
         * 1.0 — doctors fetched; compilation complete
         */
        fun generateHistory(patientId: String) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isGenerating = true,
                        generationProgress = 0f,
                        error = null,
                    )
                }

                try {
                    val prescriptions = prescriptionRepository.observeByPatient(patientId).first()
                    _uiState.update { it.copy(generationProgress = 0.33f) }

                    val medicines = medicineRepository.observeByPatient(patientId).first()
                    _uiState.update { it.copy(generationProgress = 0.66f) }

                    val doctors = doctorRepository.observeByPatient(patientId).first()
                    _uiState.update { it.copy(generationProgress = 1f) }

                    val sections = HistoryCompiler.compile(prescriptions, medicines, doctors)

                    _uiState.update {
                        it.copy(
                            sections = sections,
                            isGenerating = false,
                            generationProgress = 0f,
                            lastUpdated = System.currentTimeMillis(),
                            error = null,
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to compile medical history")
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationProgress = 0f,
                            error = "Failed to compile history. Please try again.",
                        )
                    }
                }
            }
        }

        /**
         * Refreshes the history for [patientId] by re-running [generateHistory].
         * Convenience alias so the UI can call `refresh` without needing to know
         * that it delegates to [generateHistory].
         */
        fun refresh(patientId: String) {
            generateHistory(patientId)
        }
    }
