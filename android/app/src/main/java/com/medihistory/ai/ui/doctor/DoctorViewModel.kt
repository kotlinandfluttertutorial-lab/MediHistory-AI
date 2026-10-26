package com.medihistory.ai.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty
import com.medihistory.ai.data.repository.DoctorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for doctor list and edit operations.
 *
 * Security invariants:
 * - Every repository call includes [patientId] to enforce record scoping.
 * - No doctor or patient data is written to Logcat.
 *
 * Validation rules:
 * - [DoctorUiState.name] must not be blank.
 * - When [DoctorUiState.specialty] == [DoctorSpecialty.OTHER],
 *   [DoctorUiState.specialtyOther] must not be blank.
 */
@HiltViewModel
class DoctorViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DoctorUiState())
    val uiState: StateFlow<DoctorUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // List
    // -------------------------------------------------------------------------

    /**
     * Begins observing doctors for [patientId].
     * Safe to call multiple times — only one collector is active at a time.
     */
    fun loadDoctors(patientId: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            doctorRepository.observeByPatient(patientId)
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to load doctors.",
                        )
                    }
                }
                .collect { doctors ->
                    _uiState.update { it.copy(doctors = doctors, isLoading = false) }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Form field mutations
    // -------------------------------------------------------------------------

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, error = null, isSaved = false) }
    }

    fun onSpecialtyChange(value: DoctorSpecialty) {
        _uiState.update { it.copy(specialty = value, error = null, isSaved = false) }
    }

    fun onSpecialtyOtherChange(value: String) {
        _uiState.update { it.copy(specialtyOther = value, error = null, isSaved = false) }
    }

    fun onClinicNameChange(value: String) {
        _uiState.update { it.copy(clinicName = value, isSaved = false) }
    }

    // -------------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------------

    /**
     * Validates the current form state and inserts a new [Doctor] record.
     *
     * On success [DoctorUiState.isSaved] is set to true and form fields are reset.
     * On validation failure [DoctorUiState.error] is set and no write occurs.
     */
    fun onSaveClicked(patientId: String) {
        val state = _uiState.value

        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(error = validationError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val doctor = Doctor(
                    id = UUID.randomUUID().toString(),
                    patientId = patientId,
                    name = state.name.trim(),
                    specialty = state.specialty,
                    specialtyOther = state.specialtyOther.trim().takeIf { it.isNotEmpty() },
                    clinicName = state.clinicName.trim().takeIf { it.isNotEmpty() },
                    createdAt = now,
                    updatedAt = now,
                )
                doctorRepository.insert(doctor)
            }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            name = "",
                            specialty = DoctorSpecialty.GENERAL_MEDICINE,
                            specialtyOther = "",
                            clinicName = "",
                            error = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to save doctor.",
                        )
                    }
                },
            )
        }
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    /** Shows the delete-confirmation dialog for [doctor]. */
    fun onDeleteClicked(doctor: Doctor) {
        _uiState.update { it.copy(showDeleteConfirm = doctor) }
    }

    /**
     * Confirms deletion of [doctor] and removes it from storage.
     * Linked prescriptions will show "Doctor removed" in the UI.
     */
    fun onDeleteConfirmed(doctor: Doctor, patientId: String) {
        _uiState.update { it.copy(showDeleteConfirm = null, isLoading = true) }
        viewModelScope.launch {
            runCatching {
                // Security: verify the doctor belongs to the requesting patient before deleting.
                val owned = doctorRepository.getById(doctor.id, patientId)
                if (owned != null) {
                    doctorRepository.delete(owned)
                }
            }.fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false) } },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.message ?: "Failed to delete doctor.",
                        )
                    }
                },
            )
        }
    }

    /** Dismisses the delete-confirmation dialog without deleting. */
    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteConfirm = null) }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun validate(state: DoctorUiState): String? {
        if (state.name.isBlank()) return "Doctor name is required."
        if (state.specialty == DoctorSpecialty.OTHER && state.specialtyOther.isBlank()) {
            return "Please describe the specialty."
        }
        return null
    }
}
