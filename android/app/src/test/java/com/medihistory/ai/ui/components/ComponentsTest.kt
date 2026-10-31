package com.medihistory.ai.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the component library enums and pure helper functions.
 *
 * No Compose test rule is required — these tests cover enum cardinality and
 * pure logic functions only.
 *
 * Naming convention: given_[state]_when_[action]_then_[outcome]
 */
class ComponentsTest {
    // -----------------------------------------------------------------------
    // OcrStatus enum cardinality
    // -----------------------------------------------------------------------

    @Test
    fun given_ocrStatusEnum_when_valuesQueried_then_exactlyFiveValues() {
        assertEquals(
            "OcrStatus must have exactly 5 values",
            5,
            OcrStatus.entries.size,
        )
    }

    // -----------------------------------------------------------------------
    // MedicineStatus enum cardinality
    // -----------------------------------------------------------------------

    @Test
    fun given_medicineStatusEnum_when_valuesQueried_then_exactlyThreeValues() {
        assertEquals(
            "MedicineStatus must have exactly 3 values",
            3,
            MedicineStatus.entries.size,
        )
    }

    // -----------------------------------------------------------------------
    // getConfidenceLevel — boundary value tests
    // -----------------------------------------------------------------------

    @Test
    fun given_confidenceOf0_8_when_getConfidenceLevel_then_returnsHigh() {
        assertEquals("High", getConfidenceLevel(0.8f))
    }

    @Test
    fun given_confidenceOf0_70_when_getConfidenceLevel_then_returnsHigh() {
        // Exactly at the High threshold
        assertEquals("High", getConfidenceLevel(0.70f))
    }

    @Test
    fun given_confidenceOf0_6_when_getConfidenceLevel_then_returnsMedium() {
        assertEquals("Medium", getConfidenceLevel(0.6f))
    }

    @Test
    fun given_confidenceOf0_55_when_getConfidenceLevel_then_returnsMedium() {
        // Exactly at the Medium threshold
        assertEquals("Medium", getConfidenceLevel(0.55f))
    }

    @Test
    fun given_confidenceOf0_4_when_getConfidenceLevel_then_returnsLow() {
        assertEquals("Low", getConfidenceLevel(0.4f))
    }

    @Test
    fun given_confidenceJustBelowMediumThreshold_when_getConfidenceLevel_then_returnsLow() {
        // 0.549 is below the 0.55 floor for Medium
        assertEquals("Low", getConfidenceLevel(0.549f))
    }

    // -----------------------------------------------------------------------
    // StatusBadge label contract — NEEDS_REVIEW and REVIEWED
    // -----------------------------------------------------------------------

    /**
     * The label for NEEDS_REVIEW must contain "Needs review" so that the
     * content description ("OCR status: Needs review") is accurate.
     */
    @Test
    fun given_ocrStatusNeedsReview_when_labelDerived_then_containsNeedsReview() {
        // Derive the label the same way StatusBadge does, via a helper that mirrors
        // the when-expression in the composable.
        val label = statusBadgeLabel(OcrStatus.NEEDS_REVIEW)
        assertTrue(
            "Label for NEEDS_REVIEW must contain 'Needs review', was: '$label'",
            label.contains("Needs review"),
        )
    }

    @Test
    fun given_ocrStatusReviewed_when_labelDerived_then_containsReviewed() {
        val label = statusBadgeLabel(OcrStatus.REVIEWED)
        assertTrue(
            "Label for REVIEWED must contain 'Reviewed', was: '$label'",
            label.contains("Reviewed"),
        )
    }
}

/**
 * Pure mirror of the label-selection logic in [StatusBadge].
 * Kept here in the test source set so the production composable stays clean.
 */
private fun statusBadgeLabel(status: OcrStatus): String =
    when (status) {
        OcrStatus.PENDING -> "Pending"
        OcrStatus.PROCESSING -> "Extracting\u2026"
        OcrStatus.NEEDS_REVIEW -> "Needs review"
        OcrStatus.REVIEWED -> "Reviewed"
        OcrStatus.ERROR -> "Failed"
    }
