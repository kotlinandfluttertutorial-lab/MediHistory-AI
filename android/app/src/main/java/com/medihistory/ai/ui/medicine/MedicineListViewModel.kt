package com.medihistory.ai.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for [MedicineListScreen].
 *
 * [medicines] is the currently displayed list, filtered by [searchQuery] when non-blank.
 * [error] is set on repository failures and cleared on the next successful load.
 *
 * Privacy: do not log any field of this state — it contains patient medicine data.
 */
data class MedicineListUiState(
    val medicines: List<Medicine> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for [MedicineListScreen].
 *
 * Switches between [MedicineRepository.observeByPatient] (empty query) and
 * [MedicineRepository.searchByPatient] (non-empty query) using [flatMapLatest].
 * Both patientId changes and searchQuery changes cancel the previous subscription.
 *
 * Privacy: never log [patientId] or medicine data in Logcat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MedicineListViewModel
    @Inject
    constructor(
        private val repository: MedicineRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MedicineListUiState())
        val uiState: StateFlow<MedicineListUiState> = _uiState.asStateFlow()

        /** Holds the active patientId; null means no load has been initiated yet. */
        private val _patientId = MutableStateFlow<String?>(null)

        /** Holds the current search query, kept in sync with [_uiState]. */
        private val _searchQuery = MutableStateFlow("")

        init {
            observeMedicines()
        }

        /**
         * Starts observing medicines for [patientId].
         *
         * Safe to call multiple times. Switching [patientId] triggers [flatMapLatest]
         * to cancel the previous subscription and open a new one.
         */
        fun loadMedicines(patientId: String) {
            _patientId.value = patientId
        }

        /**
         * Updates the search query. An empty string returns the full unfiltered list.
         * Never passes query text to Logcat.
         */
        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
            _uiState.update { it.copy(searchQuery = query) }
        }

        private fun observeMedicines() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }

                // Combine patientId and searchQuery so any change triggers flatMapLatest.
                combine(_patientId, _searchQuery) { patientId, query ->
                    Pair(patientId, query)
                }
                    .flatMapLatest { (patientId, query) ->
                        when {
                            patientId == null -> flowOf(emptyList())
                            query.isBlank() -> repository.observeByPatient(patientId)
                            else -> repository.searchByPatient(patientId, query)
                        }
                    }
                    .catch { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load medicines.",
                            )
                        }
                    }
                    .collect { medicines ->
                        _uiState.update {
                            it.copy(
                                medicines = medicines,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
            }
        }
    }
