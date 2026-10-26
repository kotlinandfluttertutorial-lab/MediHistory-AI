package com.medihistory.ai.ui.prescription

import com.medihistory.ai.ui.components.OcrStatus

/**
 * UI model for a single prescription entry shown in the list screen.
 *
 * This is a pure UI model — no Room entity exists yet. It carries only the
 * data needed to render [PrescriptionCard].
 *
 * @param id               Unique identifier for the prescription (used as the navigation key).
 * @param doctorName       Display name of the prescribing doctor. May be "Doctor removed" if the
 *                         doctor record has been deleted.
 * @param doctorSpecialty  Specialty string (e.g. "Cardiologist"). Empty string if unknown.
 * @param prescriptionDate Human-readable date string (e.g. "15 Jan 2024").
 * @param medicineCount    Number of medicines listed on this prescription.
 * @param ocrStatus        Current OCR review state for this prescription.
 */
data class PrescriptionSummary(
    val id: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val prescriptionDate: String,
    val medicineCount: Int,
    val ocrStatus: OcrStatus,
)
