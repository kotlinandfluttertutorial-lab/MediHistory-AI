package com.medihistory.ai.ui.prescription

import com.medihistory.ai.ui.components.OcrStatus

/**
 * Synthetic prescription fixtures for use in previews, stub ViewModels, and unit tests.
 *
 * All names are fake. No real patient names, doctor names, or medicine brand names are used.
 * Fixtures live here so tests and the stub ViewModel share a single source of truth.
 */
object SyntheticPrescriptions {
    val samples: List<PrescriptionSummary> =
        listOf(
            PrescriptionSummary(
                id = "synth-rx-001",
                doctorName = "Dr. Synthetic Mehta",
                doctorSpecialty = "Cardiologist",
                prescriptionDate = "12 Jan 2024",
                medicineCount = 3,
                ocrStatus = OcrStatus.REVIEWED,
            ),
            PrescriptionSummary(
                id = "synth-rx-002",
                doctorName = "Dr. Synthetic Krishnamurthy",
                doctorSpecialty = "General Physician",
                prescriptionDate = "28 Mar 2024",
                medicineCount = 2,
                ocrStatus = OcrStatus.NEEDS_REVIEW,
            ),
            PrescriptionSummary(
                id = "synth-rx-003",
                doctorName = "Dr. Synthetic Mehta",
                doctorSpecialty = "Cardiologist",
                prescriptionDate = "05 Jun 2024",
                medicineCount = 4,
                ocrStatus = OcrStatus.PENDING,
            ),
            PrescriptionSummary(
                id = "synth-rx-004",
                doctorName = "Doctor removed",
                doctorSpecialty = "",
                prescriptionDate = "19 Aug 2024",
                medicineCount = 1,
                ocrStatus = OcrStatus.ERROR,
            ),
        )
}
