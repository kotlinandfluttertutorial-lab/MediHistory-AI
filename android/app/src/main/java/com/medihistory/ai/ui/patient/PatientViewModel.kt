package com.medihistory.ai.ui.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Patient
import com.medihistory.ai.data.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the patient profile screen.
 *
 * Security invariants:
 * - Every repository call that loads a record passes [patientId] explicitly.
 * - No patient data is written to Logcat or any log sink.
 *
 * Validation rules:
 * - [PatientUiState.name] must not be blank.
 */
@HiltViewModel
class PatientViewModel
    @Inject
    constructor(
        private val patientRepository: PatientRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(PatientUiState())
        val uiState: StateFlow<PatientUiState> = _uiState.asStateFlow()

        // -------------------------------------------------------------------------
        // Load
        // -------------------------------------------------------------------------

        /**
         * Loads an existing patient by [patientId] and populates the form fields.
         * If no record is found the form stays blank (create mode).
         */
        fun loadPatient(patientId: String) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                runCatching {
                    patientRepository.getById(patientId)
                }.fold(
                    onSuccess = { patient ->
                        if (patient != null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    name = patient.name,
                                    dateOfBirth = patient.dateOfBirth,
                                    bloodGroup = patient.bloodGroup,
                                    allergies = patient.allergies ?: emptyList(),
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to load patient.",
                            )
                        }
                    },
                )
            }
        }

        // -------------------------------------------------------------------------
        // Form field mutations
        // -------------------------------------------------------------------------

        fun onNameChange(value: String) {
            _uiState.update { it.copy(name = value, nameError = null, isSaved = false) }
        }

        fun onDateOfBirthChange(value: LocalDate?) {
            _uiState.update { it.copy(dateOfBirth = value, isSaved = false) }
        }

        fun onBloodGroupChange(value: String?) {
            _uiState.update { it.copy(bloodGroup = value, isSaved = false) }
        }

        fun onAllergyAdded(allergy: String) {
            val trimmed = allergy.trim()
            if (trimmed.isBlank()) return
            _uiState.update { state ->
                if (trimmed in state.allergies) {
                    state
                } else {
                    state.copy(allergies = state.allergies + trimmed, isSaved = false)
                }
            }
        }

        fun onAllergyRemoved(allergy: String) {
            _uiState.update { it.copy(allergies = it.allergies - allergy, isSaved = false) }
        }

        // -------------------------------------------------------------------------
        // Save
        // -------------------------------------------------------------------------

        /**
         * Validates the current form state then inserts or replaces the [Patient] record.
         *
         * On success [PatientUiState.isSaved] is set to true.
         * On validation failure [PatientUiState.nameError] is set and no write occurs.
         *
         * @param patientId When non-null, updates the existing record with that id.
         *                  When null, a new UUID is generated (create mode).
         */
        fun onSaveClicked(patientId: String?) {
            val state = _uiState.value

            if (state.name.isBlank()) {
                _uiState.update { it.copy(nameError = "Name is required.") }
                return
            }

            _uiState.update { it.copy(isLoading = true, error = null, nameError = null) }

            viewModelScope.launch {
                runCatching {
                    val now = System.currentTimeMillis()
                    val existing = if (patientId != null) patientRepository.getById(patientId) else null
                    val patient =
                        Patient(
                            id = existing?.id ?: patientId ?: UUID.randomUUID().toString(),
                            name = state.name.trim(),
                            dateOfBirth = state.dateOfBirth,
                            bloodGroup = state.bloodGroup,
                            allergies = state.allergies,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                        )
                    if (existing != null) {
                        patientRepository.update(patient)
                    } else {
                        patientRepository.insert(patient)
                    }
                }.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(isLoading = false, isSaved = true, error = null)
                        }
                    },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to save patient.",
                            )
                        }
                    },
                )
            }
        }
    }
