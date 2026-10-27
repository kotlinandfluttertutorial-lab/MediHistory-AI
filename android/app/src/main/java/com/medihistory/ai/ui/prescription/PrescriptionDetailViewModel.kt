package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Prescription
import com.medihistory.ai.data.repository.PrescriptionRepository
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
 * @param isLoading    True while the repository fetch is in progress.
 * @param error        Non-null error message when the fetch failed or the record was not found.
 */
data class PrescriptionDetailUiState(
    val prescription: Prescription? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for [PrescriptionDetailScreen].
 *
 * Loads a single [Prescription] by id, scoped to the owning patient.
 */
@HiltViewModel
class PrescriptionDetailViewModel @Inject constructor(
    private val repository: PrescriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionDetailUiState(isLoading = true))
    val uiState: StateFlow<PrescriptionDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads the prescription with [id] that belongs to [patientId].
     * Sets [PrescriptionDetailUiState.error] if the record does not exist.
     */
    fun load(id: String, patientId: String) {
        viewModelScope.launch {
            val prescription = repository.getById(id, patientId)
            _uiState.value = if (prescription != null) {
                PrescriptionDetailUiState(prescription = prescription)
            } else {
                PrescriptionDetailUiState(error = "Prescription not found")
            }
        }
    }
}
