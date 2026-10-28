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
                // Simulated async load — replace with repository call when entity exists.
                delay(300L)
                _uiState.update {
                    it.copy(
                        prescriptions = SyntheticPrescriptions.samples,
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
}
