package com.medihistory.ai.data.ocr

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OcrResult] computed properties and [StubOcrService] contract.
 *
 * All tests use synthetic data only — no real medicine brands, patient names, or documents.
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class OcrResultTest {
    // --- isSuccess ---

    @Test
    fun `should have isSuccess true when errorMessage is null`() {
        val result =
            OcrResult(
                prescriptionId = "synth-rx-001",
                fields = emptyList(),
                overallConfidence = 0.8f,
                errorMessage = null,
            )
        assertTrue("isSuccess must be true when errorMessage is null", result.isSuccess)
    }

    @Test
    fun `should have isSuccess false when errorMessage is set`() {
        val result =
            OcrResult(
                prescriptionId = "synth-rx-001",
                fields = emptyList(),
                overallConfidence = 0.0f,
                errorMessage = "OCR service unavailable",
            )
        assertFalse("isSuccess must be false when errorMessage is non-null", result.isSuccess)
    }

    // --- pendingReviewCount ---

    @Test
    fun `should count only fields with non-null rawValue and PENDING_REVIEW state`() {
        val fields =
            listOf(
                // Should count: rawValue non-null, state PENDING_REVIEW
                ExtractedField("doctor_name", "Dr. Synthetic", null, 0.85f, ReviewState.PENDING_REVIEW),
                // Should NOT count: rawValue null (absent field)
                ExtractedField("dose", null, null, 0.0f, ReviewState.PENDING_REVIEW),
                // Should NOT count: already reviewed
                ExtractedField("prescription_date", "2025-07-01", "2025-07-01", 0.9f, ReviewState.REVIEWED),
                // Should NOT count: rejected
                ExtractedField("medicine_name", "Synthamol 500mg", null, 0.6f, ReviewState.REJECTED),
                // Should count: rawValue non-null, state PENDING_REVIEW
                ExtractedField("frequency", "once daily", null, 0.75f, ReviewState.PENDING_REVIEW),
            )
        val result =
            OcrResult(
                prescriptionId = "synth-rx-002",
                fields = fields,
                overallConfidence = 0.72f,
            )
        assertEquals(
            "pendingReviewCount must count only non-null rawValue fields with PENDING_REVIEW",
            2,
            result.pendingReviewCount,
        )
    }

    @Test
    fun `should return zero pendingReviewCount when all fields are absent`() {
        val fields =
            listOf(
                ExtractedField("dose", null, null, 0.0f),
                ExtractedField("frequency", null, null, 0.0f),
            )
        val result =
            OcrResult(
                prescriptionId = "synth-rx-003",
                fields = fields,
                overallConfidence = 0.0f,
            )
        assertEquals(
            "pendingReviewCount must be 0 when all rawValues are null",
            0,
            result.pendingReviewCount,
        )
    }

    // --- StubOcrService contract ---

    @Test
    fun `should return only synthetic medicine names from StubOcrService`() =
        runTest {
            val service = StubOcrService()
            val result =
                service.extract(
                    documentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg",
                    mimeType = "image/jpeg",
                    prescriptionId = "synth-rx-001",
                )

            // Verify no real medicine brand names are present.
            // Synthetic names must be used in tests and demos per medical-safety steering.
            val realBrands =
                listOf(
                    "Crocin",
                    "Dolo",
                    "Paracetamol",
                    "Amoxicillin",
                    "Metformin",
                    "Aspirin",
                    "Ibuprofen",
                    "Combiflam",
                )
            val medicineField = result.fields.firstOrNull { it.fieldName == "medicine_name" }
            realBrands.forEach { brand ->
                assertFalse(
                    "StubOcrService must not return real medicine brand '$brand'",
                    medicineField?.rawValue?.contains(brand, ignoreCase = true) == true,
                )
            }
        }

    @Test
    fun `should return low confidence for medicine_name from StubOcrService`() =
        runTest {
            val service = StubOcrService()
            val result =
                service.extract(
                    documentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg",
                    mimeType = "image/jpeg",
                    prescriptionId = "synth-rx-002",
                )

            val medicineField = result.fields.firstOrNull { it.fieldName == "medicine_name" }
            assertTrue(
                "StubOcrService medicine_name confidence must be below 0.75 to force patient review",
                (medicineField?.confidence ?: 1.0f) < 0.75f,
            )
        }

    @Test
    fun `should return absent dose field from StubOcrService`() =
        runTest {
            val service = StubOcrService()
            val result =
                service.extract(
                    documentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg",
                    mimeType = "image/jpeg",
                    prescriptionId = "synth-rx-003",
                )

            val doseField = result.fields.firstOrNull { it.fieldName == "dose" }
            assertEquals(
                "StubOcrService dose field must have null rawValue (absent — never fabricated)",
                null,
                doseField?.rawValue,
            )
        }

    @Test
    fun `should return successful result from StubOcrService`() =
        runTest {
            val service = StubOcrService()
            val result =
                service.extract(
                    documentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg",
                    mimeType = "image/jpeg",
                    prescriptionId = "synth-rx-004",
                )
            assertTrue("StubOcrService must return a successful result", result.isSuccess)
        }

    @Test
    fun `should return correct prescriptionId from StubOcrService`() =
        runTest {
            val service = StubOcrService()
            val prescriptionId = "synth-rx-005"
            val result =
                service.extract(
                    documentPath = "/data/user/0/com.medihistory.ai/files/synth-doc.jpg",
                    mimeType = "image/jpeg",
                    prescriptionId = prescriptionId,
                )
            assertEquals(
                "OcrResult.prescriptionId must match the input prescriptionId",
                prescriptionId,
                result.prescriptionId,
            )
        }
}
