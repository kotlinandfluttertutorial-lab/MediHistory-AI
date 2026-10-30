package com.medihistory.ai.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.dao.DuplicateAlertDao
import com.medihistory.ai.data.repository.MedicineRepository
import kotlinx.coroutines.flow.first
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for [MedicineDetailScreen].
 *
 * Loads a single [com.medihistory.ai.data.model.Medicine] by ID, scoped to [patientId].
 * Returns an error state when the ID is unknown or belongs to a different patient —
 * intentionally indistinguishable to prevent cross-patient data leakage.
 *
 * Privacy: never log [patientId], medicine ID, or any field of the loaded medicine.
 */
@HiltViewModel
class MedicineDetailViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val duplicateAlertDao: DuplicateAlertDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicineDetailUiState())
    val uiState: StateFlow<MedicineDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads the medicine with [medicineId] for [patientId].
     *
     * Sets [MedicineDetailUiState.isLoading] to true immediately, then either
     * populates [MedicineDetailUiState.medicine] on success or sets
     * [MedicineDetailUiState.error] when the record is not found or an exception occurs.
     *
     * Safe to call multiple times; each call replaces the previous state.
     */
    fun load(medicineId: String, patientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val medicine = repository.getById(id = medicineId, patientId = patientId)
                if (medicine != null) {
                    // Check for unreviewed duplicate alerts involving this medicine
                    val alerts = duplicateAlertDao.observeByPatient(patientId).first()
                    val medicineAlerts = alerts.filter { alert ->
                        !alert.isReviewed &&
                            (alert.medicineAId == medicineId || alert.medicineBId == medicineId)
                    }
                    _uiState.update {
                        it.copy(
                            medicine = medicine,
                            hasDuplicateAlert = medicineAlerts.isNotEmpty(),
                            duplicateAlertCount = medicineAlerts.size,
                            isLoading = false,
                        )
                    }
                } else {
                    Timber.w("Medicine record not found or inaccessible")
                    _uiState.update {
                        it.copy(
                            medicine = null,
                            isLoading = false,
                            error = "Medicine not found.",
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load medicine record")
                _uiState.update {
                    it.copy(
                        medicine = null,
                        isLoading = false,
                        error = e.message ?: "Failed to load medicine.",
                    )
                }
            }
        }
    }
}
