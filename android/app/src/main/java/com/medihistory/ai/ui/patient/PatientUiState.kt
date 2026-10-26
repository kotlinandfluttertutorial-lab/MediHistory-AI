package com.medihistory.ai.ui.patient

import java.time.LocalDate

/**
 * UI state for the patient profile screen.
 *
 * [name]        — current value in the name input field.
 * [dateOfBirth] — selected date of birth; null when not set.
 * [bloodGroup]  — selected ABO/Rh blood group string; null when not set.
 * [allergies]   — current list of allergy entries.
 * [isLoading]   — true while a load or save operation is in progress.
 * [isSaved]     — true after a successful save; consumed by the screen to navigate back.
 * [nameError]   — non-null when the name field fails validation.
 * [error]       — generic persistence error message; null when clear.
 */
data class PatientUiState(
    val name: String = "",
    val dateOfBirth: LocalDate? = null,
    val bloodGroup: String? = null,
    val allergies: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val error: String? = null,
)
