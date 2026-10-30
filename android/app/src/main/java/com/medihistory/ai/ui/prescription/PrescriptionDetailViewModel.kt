package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.medihistory.ai.data.model.OcrStatus
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.DoctorRepository
import com.medihistory.ai.data.repository.PrescriptionRepository
import com.medihistory.ai.worker.OcrWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for [PrescriptionDetailScreen].
 *
 * @param prescription The loaded prescription; null while loading or on error.
 * @param doctorName   Resolved doctor name; null when not yet loaded or doctor unset.
 * @param isLoading    True while the repository fetch is in progress.
 * @param error        Non-null error message when the fetch failed or the record was not found.
 */
data class PrescriptionDetailUiState(
    val prescription: Prescription? = null,
    val doctorName: String? = null,
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
class PrescriptionDetailViewModel @Inject constructor(
    private val repository: PrescriptionRepository,
    private val doctorRepository: DoctorRepository,
    private val workManager: WorkManager,
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
            val prescription = repository.getById(id, patientId)
            if (prescription != null) {
                val doctorName = prescription.doctorId?.let { doctorId ->
                    doctorRepository.getById(doctorId, patientId)?.name
                }
                _uiState.value = PrescriptionDetailUiState(
                    prescription = prescription,
                    doctorName = doctorName,
                )
            } else {
                _uiState.value = PrescriptionDetailUiState(error = "Prescription not found")
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
    fun triggerOcrExtraction(prescriptionId: String, patientId: String) {
        val prescription = _uiState.value.prescription ?: return
        val documentPath = prescription.documentPath ?: return
        val mimeType = prescription.documentMimeType ?: return

        workManager.enqueue(
            OcrWorker.buildRequest(
                prescriptionId = prescriptionId,
                patientId = patientId,
                documentPath = documentPath,
                mimeType = mimeType,
            )
        )
        _uiState.value = _uiState.value.copy(
            prescription = prescription.copy(ocrStatus = OcrStatus.PROCESSING),
        )
    }
}
