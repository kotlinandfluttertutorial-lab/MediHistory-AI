package com.medihistory.ai.ui.newdoctor

/**
 * Represents a single prescription that can be selected for inclusion in the New Doctor summary.
 *
 * Privacy: fields in this class must never be logged or transmitted without explicit patient consent.
 *
 * @param prescriptionId  Unique identifier of the source prescription.
 * @param doctorName      Display name of the prescribing doctor.
 * @param prescriptionDate Human-readable date string (e.g. "12 Jan 2024").
 * @param medicineCount   Number of medicines on this prescription.
 * @param isSelected      Whether the patient has selected this record for sharing. Defaults to true
 *                        so that all records are included until the patient explicitly excludes one.
 */
data class RecordSelection(
    val prescriptionId: String,
    val doctorName: String,
    val prescriptionDate: String,
    val medicineCount: Int,
    val isSelected: Boolean = true,
)

/**
 * Immutable UI state for [NewDoctorModeScreen].
 *
 * Medical safety: this state contains patient prescription data. It must never be logged.
 *
 * @param records       All prescriptions available for selection.
 * @param isLoading     True while the prescription list is being loaded.
 * @param selectedCount Number of records currently marked as selected.
 * @param error         Non-null when an error occurred during load; null on success.
 */
data class NewDoctorSelectionUiState(
    val records: List<RecordSelection> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCount: Int = 0,
    val error: String? = null,
) {
    /**
     * True when at least one record is selected and the patient can proceed to generate a summary.
     * Sharing is only possible when the patient has made an active selection.
     */
    val canProceed: Boolean get() = selectedCount > 0
}
