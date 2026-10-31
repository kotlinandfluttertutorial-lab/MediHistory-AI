package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.medihistory.ai.data.ai.PrescriptionUnderstandingAgent
import com.medihistory.ai.data.dao.ExtractedFieldDao
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.MedicineRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.worker.OcrWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for [PrescriptionDetailScreen].
 *
 * @param prescription       The loaded prescription; null while loading or on error.
 * @param doctorName         Resolved doctor name; null when not yet loaded or doctor unset.
 * @param understanding      AI-generated medicine explanations (F11); null until generated.
 * @param isUnderstandingLoading True while the understanding agent is running.
 * @param understandingError Non-null when the understanding call failed.
 * @param isLoading          True while the repository fetch is in progress.
 * @param error              Non-null error message when the fetch failed or the record was not found.
 */
data class PrescriptionDetailUiState(
    val prescription: Prescription? = null,
    val doctorName: String? = null,
    val medicines: List<com.medihistory.ai.data.model.Medicine> = emptyList(),
    val understanding: List<com.medihistory.ai.data.ai.MedicineUnderstanding> = emptyList(),
    val isUnderstandingLoading: Boolean = false,
    val understandingError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for [PrescriptionDetailScreen].
 *
 * Loads a single [Prescription] by id, scoped to the owning patient.
 * Resolves the doctor name from [DoctorRepository] after the prescription is loaded.
 */
@HiltViewModel
class PrescriptionDetailViewModel
    @Inject
    constructor(
        private val repository: PrescriptionRepository,
        private val doctorRepository: DoctorRepository,
        private val medicineRepository: MedicineRepository,
        private val extractedFieldDao: ExtractedFieldDao,
        private val workManager: WorkManager,
        private val prescriptionUnderstandingAgent: PrescriptionUnderstandingAgent,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PrescriptionDetailUiState(isLoading = true))
        val uiState: StateFlow<PrescriptionDetailUiState> = _uiState.asStateFlow()

        /**
         * Loads the prescription with [id] that belongs to [patientId].
         * Sets [PrescriptionDetailUiState.error] if the record does not exist.
         * When a doctorId is present, also resolves the doctor name.
         */
        fun load(id: String, patientId: String) {
            viewModelScope.launch {
                combine(
                    repository.observeByPatient(patientId),
                    medicineRepository.observeByPatient(patientId),
                ) { prescriptions, allMedicines ->
                    val prescription = prescriptions.firstOrNull { it.id == id }
                    if (prescription != null) {
                        val doctorName = prescription.doctorId?.let { did ->
                            doctorRepository.getById(did, patientId)?.name
                        }
                        val medicines = allMedicines.filter { it.prescriptionId == id }
                        PrescriptionDetailUiState(
                            prescription = prescription,
                            doctorName = doctorName,
                            medicines = medicines,
                        )
                    } else {
                        PrescriptionDetailUiState(error = "Prescription not found")
                    }
                }
                    .catch { _ ->
                        _uiState.value = PrescriptionDetailUiState(error = "Failed to load prescription.")
                    }
                    .collect { state -> _uiState.value = state }
            }
        }

        /**
         * Calls [PrescriptionUnderstandingAgent] to generate plain-language explanations
         * for the confirmed medicines on this prescription (F11).
         *
         * Only runs when [OcrStatus.REVIEWED] — there must be confirmed fields to explain.
         * Results are shown in the "Understand this prescription" bottom sheet.
         *
         * Medical safety: the agent only reads REVIEWED fields and confirmed medicine rows.
         * Its output always carries a non-dismissible disclaimer in the UI.
         */
        fun generateUnderstanding(
            prescriptionId: String,
            patientId: String,
        ) {
            val prescription = _uiState.value.prescription ?: return
            if (prescription.ocrStatus != OcrStatus.REVIEWED) return

            _uiState.value =
                _uiState.value.copy(
                    isUnderstandingLoading = true,
                    understandingError = null,
                )
            viewModelScope.launch {
                try {
                    val fields =
                        extractedFieldDao
                            .observeByPrescription(prescriptionId)
                            .first()
                    val medicines =
                        medicineRepository
                            .observeByPatient(patientId)
                            .first()
                            .filter { it.prescriptionId == prescriptionId }

                    val result = prescriptionUnderstandingAgent.explain(fields, medicines)
                    _uiState.value =
                        _uiState.value.copy(
                            understanding = result,
                            isUnderstandingLoading = false,
                            understandingError = null,
                        )
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    _uiState.value =
                        _uiState.value.copy(
                            isUnderstandingLoading = false,
                            understandingError = "Could not generate explanation. Please try again.",
                        )
                }
            }
        }

        /**
         * Enqueues an OCR extraction worker for the current prescription.
         *
         * Returns immediately if the prescription has no document path or MIME type,
         * as there is nothing to extract from.
         *
         * Optimistically updates the OCR status to [OcrStatus.PROCESSING] so the UI
         * reflects the queued state before the worker starts.
         */
        fun triggerOcrExtraction(
            prescriptionId: String,
            patientId: String,
        ) {
            val prescription = _uiState.value.prescription ?: return
            val documentPath = prescription.documentPath ?: return
            val mimeType = prescription.documentMimeType ?: return

            workManager.enqueue(
                OcrWorker.buildRequest(
                    prescriptionId = prescriptionId,
                    patientId = patientId,
                    documentPath = documentPath,
                    mimeType = mimeType,
                ),
            )
            _uiState.value =
                _uiState.value.copy(
                    prescription = prescription.copy(ocrStatus = OcrStatus.PROCESSING),
                )
        }
    }
