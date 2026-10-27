package com.medihistory.ai.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [ExtractedField] validation and [ReviewState] invariants.
 *
 * All tests use synthetic data only — no real patient names, medicine names, or documents.
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class ExtractedFieldTest {

    // --- confidence range validation ---

    @Test
    fun `should construct without error when confidence is 0_5f`() {
        val field = ExtractedField(
            fieldName = "medicine_name",
            rawValue = "Synthamol 500mg",
            patientValue = null,
            confidence = 0.5f,
        )
        assertEquals(0.5f, field.confidence)
    }

    @Test
    fun `should construct without error when confidence is 1_0f`() {
        val field = ExtractedField(
            fieldName = "doctor_name",
            rawValue = "Dr. Synthetic",
            patientValue = null,
            confidence = 1.0f,
        )
        assertEquals(1.0f, field.confidence)
    }

    @Test
    fun `should construct without error when confidence is 0_0f`() {
        val field = ExtractedField(
            fieldName = "dose",
            rawValue = null,
            patientValue = null,
            confidence = 0.0f,
        )
        assertEquals(0.0f, field.confidence)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw IllegalArgumentException when confidence is negative`() {
        ExtractedField(
            fieldName = "dose",
            rawValue = null,
            patientValue = null,
            confidence = -0.1f,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `should throw IllegalArgumentException when confidence exceeds 1_0f`() {
        ExtractedField(
            fieldName = "dose",
            rawValue = null,
            patientValue = null,
            confidence = 1.1f,
        )
    }

    // --- absent field invariant ---

    @Test
    fun `should have null rawValue when field was not found in document`() {
        val field = ExtractedField(
            fieldName = "frequency",
            rawValue = null,
            patientValue = null,
            confidence = 0.0f,
        )
        // null rawValue means the field was absent — never fabricate a value.
        assertNull("rawValue must be null for an absent field", field.rawValue)
    }

    // --- default review state ---

    @Test
    fun `should default to PENDING_REVIEW when no reviewState is provided`() {
        val field = ExtractedField(
            fieldName = "prescription_date",
            rawValue = "2025-07-01",
            patientValue = null,
            confidence = 0.9f,
        )
        assertEquals(ReviewState.PENDING_REVIEW, field.reviewState)
    }

    // --- ReviewState enum completeness ---

    @Test
    fun `should have exactly three ReviewState values`() {
        val values = ReviewState.values()
        assertEquals(
            "ReviewState must have exactly 3 values: PENDING_REVIEW, REVIEWED, REJECTED",
            3,
            values.size,
        )
    }

    @Test
    fun `should contain PENDING_REVIEW in ReviewState values`() {
        val values = ReviewState.values().map { it.name }
        assert(values.contains("PENDING_REVIEW")) {
            "ReviewState must contain PENDING_REVIEW"
        }
    }

    @Test
    fun `should contain REVIEWED in ReviewState values`() {
        val values = ReviewState.values().map { it.name }
        assert(values.contains("REVIEWED")) {
            "ReviewState must contain REVIEWED"
        }
    }

    @Test
    fun `should contain REJECTED in ReviewState values`() {
        val values = ReviewState.values().map { it.name }
        assert(values.contains("REJECTED")) {
            "ReviewState must contain REJECTED"
        }
    }
}
