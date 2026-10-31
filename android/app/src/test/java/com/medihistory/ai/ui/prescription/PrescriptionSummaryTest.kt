package com.medihistory.ai.ui.prescription

import com.medihistory.ai.ui.components.OcrStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [PrescriptionSummary] data class and [SyntheticPrescriptions] fixtures.
 *
 * Verifies construction behaviour and that fixture OcrStatus values match the specification.
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class PrescriptionSummaryTest {
    @Test
    fun `should construct without error when doctorName is Doctor removed`() {
        val summary =
            PrescriptionSummary(
                id = "test-id",
                doctorName = "Doctor removed",
                doctorSpecialty = "",
                prescriptionDate = "01 Jan 2024",
                medicineCount = 0,
                ocrStatus = OcrStatus.ERROR,
            )

        assertNotNull("PrescriptionSummary must not be null", summary)
        assertEquals("Doctor removed", summary.doctorName)
    }

    @Test
    fun `should have REVIEWED status for synth-rx-001`() {
        val entry = SyntheticPrescriptions.samples.first { it.id == "synth-rx-001" }
        assertEquals(
            "synth-rx-001 must have OcrStatus.REVIEWED",
            OcrStatus.REVIEWED,
            entry.ocrStatus,
        )
    }

    @Test
    fun `should have NEEDS_REVIEW status for synth-rx-002`() {
        val entry = SyntheticPrescriptions.samples.first { it.id == "synth-rx-002" }
        assertEquals(
            "synth-rx-002 must have OcrStatus.NEEDS_REVIEW",
            OcrStatus.NEEDS_REVIEW,
            entry.ocrStatus,
        )
    }

    @Test
    fun `should have PENDING status for synth-rx-003`() {
        val entry = SyntheticPrescriptions.samples.first { it.id == "synth-rx-003" }
        assertEquals(
            "synth-rx-003 must have OcrStatus.PENDING",
            OcrStatus.PENDING,
            entry.ocrStatus,
        )
    }

    @Test
    fun `should have ERROR status for the Doctor removed entry`() {
        val entry = SyntheticPrescriptions.samples.first { it.doctorName == "Doctor removed" }
        assertEquals(
            "Doctor removed entry must have OcrStatus.ERROR",
            OcrStatus.ERROR,
            entry.ocrStatus,
        )
    }

    @Test
    fun `should have four synthetic samples`() {
        assertEquals(
            "SyntheticPrescriptions.samples must contain exactly 4 entries",
            4,
            SyntheticPrescriptions.samples.size,
        )
    }

    @Test
    fun `should have all unique ids in synthetic samples`() {
        val ids = SyntheticPrescriptions.samples.map { it.id }
        assertEquals(
            "All synthetic sample ids must be unique",
            ids.size,
            ids.toSet().size,
        )
    }
}
