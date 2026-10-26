package com.medihistory.ai.ui.doctor

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.DoctorSpecialty

/**
 * UI state for doctor list and edit screens.
 *
 * [doctors]         — live list of doctors scoped to the current patient.
 * [name]            — current value in the name input field.
 * [specialty]       — selected specialty; defaults to GENERAL_MEDICINE.
 * [specialtyOther]  — free-text specialty; required when specialty == OTHER.
 * [clinicName]      — optional clinic or hospital name.
 * [isLoading]       — true while an async operation (load / save / delete) is running.
 * [isSaved]         — true after a successful insert or update; reset on next edit.
 * [showDeleteConfirm] — non-null when the delete-confirmation dialog should be shown.
 * [error]           — validation or persistence error message; null when clear.
 */
data class DoctorUiState(
    val doctors: List<Doctor> = emptyList(),
    val name: String = "",
    val specialty: DoctorSpecialty = DoctorSpecialty.GENERAL_MEDICINE,
    val specialtyOther: String = "",
    val clinicName: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val showDeleteConfirm: Doctor? = null,
    val error: String? = null,
)
