package com.medihistory.ai.ui.prescription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [PrescriptionListScreen].
 *
 * Currently backed by [SyntheticPrescriptions] with a simulated 300 ms load delay.
 * A real Room-backed repository will replace this stub in a future ticket once the
 * Prescription entity is defined.
 */
@HiltViewModel
class PrescriptionListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PrescriptionListUiState())
    val uiState: StateFlow<PrescriptionListUiState> = _uiState.asStateFlow()

    init {
        loadPrescriptions()
    }

    fun loadPrescriptions() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                delay(300L)
                val samples = SyntheticPrescriptions.samples
                val groups = samples
                    .groupBy { it.doctorName }
                    .map { (doctor, rxList) ->
                        DoctorGroup(
                            doctorName = doctor,
                            specialty = rxList.firstOrNull()?.doctorSpecialty ?: "",
                            prescriptions = rxList,
                        )
                    }
                _uiState.update {
                    it.copy(
                        prescriptions = samples,
                        doctorGroups = groups,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load prescriptions.",
                    )
                }
            }
        }
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
}
