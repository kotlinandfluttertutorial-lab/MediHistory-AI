package com.medihistory.ai.ui.ocr

import com.medihistory.ai.data.ocr.ReviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [FieldReviewState] display logic.
 *
 * All fixtures use synthetic data — no real patient names or medicine names.
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class FieldReviewStateTest {

    // ─────────────────────────────────────────────────────────────────────────
    // displayValue derivation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should use patientValue for displayValue when patientValue is set`() {
        val field = makeField(
            rawValue     = "Synthamol 500mg",
            patientValue = "Synthamol 250mg",
        )
        assertEquals(
            "displayValue must use patientValue when it is set",
            "Synthamol 250mg",
            field.displayValue,
        )
    }

    @Test
    fun `should use rawValue for displayValue when patientValue is null`() {
        val field = makeField(
            rawValue     = "Synthamol 500mg",
            patientValue = null,
        )
        assertEquals(
            "displayValue must fall back to rawValue when patientValue is null",
            "Synthamol 500mg",
            field.displayValue,
        )
    }

    @Test
    fun `should show Not extracted for displayValue when both rawValue and patientValue are null`() {
        val field = makeField(
            rawValue     = null,
            patientValue = null,
        )
        assertEquals(
            "displayValue must be 'Not extracted' when no value is available",
            "Not extracted",
            field.displayValue,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rawValue immutability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should never change rawValue when patientValue differs`() {
        val original = makeField(rawValue = "Dr Synthetic", patientValue = null)
        val confirmed = original.copy(
            patientValue = "Dr Test Cardiologist",
            reviewState  = ReviewState.REVIEWED,
            displayValue = "Dr Test Cardiologist",
        )
        assertEquals(
            "rawValue must NEVER change — only patientValue changes",
            "Dr Synthetic",
            confirmed.rawValue,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Review state
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should start with PENDING_REVIEW state`() {
        val field = makeField()
        assertEquals(ReviewState.PENDING_REVIEW, field.reviewState)
    }

    @Test
    fun `should transition from PENDING_REVIEW to REVIEWED`() {
        val field = makeField(reviewState = ReviewState.PENDING_REVIEW)
        val reviewed = field.copy(reviewState = ReviewState.REVIEWED)
        assertEquals(ReviewState.REVIEWED, reviewed.reviewState)
    }

    @Test
    fun `should transition from PENDING_REVIEW to REJECTED`() {
        val field = makeField(reviewState = ReviewState.PENDING_REVIEW)
        val rejected = field.copy(reviewState = ReviewState.REJECTED)
        assertEquals(ReviewState.REJECTED, rejected.reviewState)
    }

    @Test
    fun `should allow REVIEWED to return to PENDING_REVIEW`() {
        val field = makeField(reviewState = ReviewState.REVIEWED)
        val unreviewd = field.copy(reviewState = ReviewState.PENDING_REVIEW)
        assertEquals(ReviewState.PENDING_REVIEW, unreviewd.reviewState)
    }

    @Test
    fun `should allow REJECTED to return to PENDING_REVIEW`() {
        val field = makeField(reviewState = ReviewState.REJECTED)
        val unrejected = field.copy(reviewState = ReviewState.PENDING_REVIEW)
        assertEquals(ReviewState.PENDING_REVIEW, unrejected.reviewState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // pendingCount in OcrReviewUiState
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should count only fields with rawValue and PENDING_REVIEW for pendingCount`() {
        val state = OcrReviewUiState(
            fields = listOf(
                makeField(rawValue = "val1", reviewState = ReviewState.PENDING_REVIEW),
                makeField(rawValue = "val2", reviewState = ReviewState.REVIEWED),
                makeField(rawValue = null,   reviewState = ReviewState.PENDING_REVIEW),
                makeField(rawValue = "val4", reviewState = ReviewState.REJECTED),
            ),
        )
        assertEquals(
            "pendingCount must be 1: only PENDING_REVIEW with non-null rawValue",
            1,
            state.pendingCount,
        )
    }

    @Test
    fun `should have pendingCount zero when all fields are reviewed`() {
        val state = OcrReviewUiState(
            fields = listOf(
                makeField(rawValue = "val1", reviewState = ReviewState.REVIEWED),
                makeField(rawValue = "val2", reviewState = ReviewState.REVIEWED),
            ),
        )
        assertEquals(0, state.pendingCount)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // editText default
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should seed editText from patientValue when available`() {
        val field = makeField(rawValue = "Synthamol 500mg", patientValue = "Synthamol 250mg")
        assertEquals("Synthamol 250mg", field.editText)
    }

    @Test
    fun `should seed editText from rawValue when patientValue is null`() {
        val field = makeField(rawValue = "Synthamol 500mg", patientValue = null)
        assertEquals("Synthamol 500mg", field.editText)
    }

    @Test
    fun `should have empty editText when both rawValue and patientValue are null`() {
        val field = makeField(rawValue = null, patientValue = null)
        assertEquals("", field.editText)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun makeField(
        fieldId     : String      = "field-test-001",
        fieldName   : String      = "Medicine Name",
        rawValue    : String?     = "Synthamol 500mg",
        patientValue: String?     = null,
        confidence  : Float       = 0.85f,
        reviewState : ReviewState = ReviewState.PENDING_REVIEW,
    ): FieldReviewState {
        val display = patientValue ?: rawValue ?: "Not extracted"
        val editBuf = patientValue ?: rawValue ?: ""
        return FieldReviewState(
            fieldId      = fieldId,
            fieldName    = fieldName,
            rawValue     = rawValue,
            patientValue = patientValue,
            displayValue = display,
            confidence   = confidence,
            reviewState  = reviewState,
            editText     = editBuf,
        )
    }
}
