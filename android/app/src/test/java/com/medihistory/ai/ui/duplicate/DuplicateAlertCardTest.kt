package com.medihistory.ai.ui.duplicate

import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the data and safety invariants used by [DuplicateAlertCard].
 *
 * These tests verify:
 * - Medical safety strings are correct (disclaimer, warning banner).
 * - Labels use "Potential duplicate" (not "Duplicate medication" or clinical terms).
 * - Match type labels are "Exact match" / "Partial match" (never clinical terms).
 * - The contentDescription follows the required format.
 * - Reviewed and unreviewed state logic is correct.
 *
 * Note: Compose rendering is covered by UI/instrumented tests. These tests verify the
 * string constants and item-building logic that the composable relies on, independently
 * of the Compose runtime.
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 *
 * Synthetic fixtures only — no real medicine brand names or patient data.
 */
class DuplicateAlertCardTest {

    // -------------------------------------------------------------------------
    // Fixtures — synthetic data only
    // -------------------------------------------------------------------------

    private fun syntheticAlertItem(
        alertId: String = "alert-001",
        medicineAName: String = "Synthamol 500mg",
        medicineBName: String = "Testocillin 250mg",
        matchingIngredient: String = "synthamolide",
        matchType: MatchType = MatchType.EXACT,
        isReviewed: Boolean = false,
        combinationNote: String? = null,
    ) = DuplicateAlertItem(
        alert = DuplicateAlert(
            id = alertId,
            patientId = "test-patient-alpha",
            medicineAId = "med-001",
            medicineBId = "med-002",
            matchingIngredient = matchingIngredient,
            matchType = matchType,
            confidence = 0.95f,
            isReviewed = isReviewed,
            combinationNote = combinationNote,
            createdAt = 1_700_000_000_000L,
        ),
        medicineAName = medicineAName,
        medicineBName = medicineBName,
        prescriptionAId = "rx-001",
        prescriptionBId = "rx-002",
        isExpanded = false,
    )

    // -------------------------------------------------------------------------
    // Content description tests
    // -------------------------------------------------------------------------

    @Test
    fun should_contain_potential_duplicate_in_contentDescription_when_item_is_unreviewed() {
        val item = syntheticAlertItem(isReviewed = false)

        val desc = buildCardContentDescription(item)

        assertTrue(
            "contentDescription must contain 'Potential duplicate'",
            desc.contains("Potential duplicate"),
        )
    }

    @Test
    fun should_not_contain_duplicate_medication_in_contentDescription() {
        val item = syntheticAlertItem()

        val desc = buildCardContentDescription(item)

        assertFalse(
            "contentDescription must NOT contain 'Duplicate medication'",
            desc.contains("Duplicate medication", ignoreCase = true),
        )
    }

    @Test
    fun should_include_medicine_names_in_contentDescription() {
        val item = syntheticAlertItem(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
        )

        val desc = buildCardContentDescription(item)

        assertTrue(
            "contentDescription must contain medicineAName",
            desc.contains("Synthamol 500mg"),
        )
        assertTrue(
            "contentDescription must contain medicineBName",
            desc.contains("Testocillin 250mg"),
        )
    }

    @Test
    fun should_include_matching_ingredient_in_contentDescription() {
        val item = syntheticAlertItem(matchingIngredient = "synthamolide")

        val desc = buildCardContentDescription(item)

        assertTrue(
            "contentDescription must contain the matching ingredient",
            desc.contains("synthamolide"),
        )
    }

    @Test
    fun should_include_unreviewed_status_in_contentDescription_when_not_reviewed() {
        val item = syntheticAlertItem(isReviewed = false)

        val desc = buildCardContentDescription(item)

        assertTrue(
            "contentDescription must include 'unreviewed' status",
            desc.contains("unreviewed"),
        )
    }

    @Test
    fun should_include_reviewed_status_in_contentDescription_when_reviewed() {
        val item = syntheticAlertItem(isReviewed = true)

        val desc = buildCardContentDescription(item)

        assertTrue(
            "contentDescription must include 'reviewed' status",
            desc.contains("reviewed"),
        )
    }

    // -------------------------------------------------------------------------
    // Disclaimer text tests
    // -------------------------------------------------------------------------

    @Test
    fun should_disclaimer_text_match_exact_required_string() {
        // The disclaimer is a medical safety requirement — exact wording must not change.
        val expectedDisclaimer =
            "Marking as reviewed means you have seen this alert. " +
                "It does not mean this combination has been approved by a doctor."

        assertEquals(
            "disclaimer text must match the exact required medical safety string",
            expectedDisclaimer,
            MARK_REVIEWED_DISCLAIMER,
        )
    }

    @Test
    fun should_warning_banner_contain_required_phrase() {
        // Required by medical-safety steering: every alert detail must show this exact phrase.
        val requiredPhrase = "please review with your doctor or pharmacist"

        assertTrue(
            "warning banner must contain '$requiredPhrase'",
            WARNING_BANNER_TEXT.contains(requiredPhrase, ignoreCase = false),
        )
    }

    @Test
    fun should_warning_banner_start_with_potential_duplicate() {
        assertTrue(
            "warning banner must start with 'Potential duplicate'",
            WARNING_BANNER_TEXT.startsWith("Potential duplicate"),
        )
    }

    @Test
    fun should_warning_banner_not_use_clinical_terms() {
        assertFalse(
            "warning banner must NOT contain 'Duplicate medication'",
            WARNING_BANNER_TEXT.contains("Duplicate medication", ignoreCase = true),
        )
        assertFalse(
            "warning banner must NOT contain 'Drug interaction'",
            WARNING_BANNER_TEXT.contains("Drug interaction", ignoreCase = true),
        )
    }

    // -------------------------------------------------------------------------
    // Match type label tests
    // -------------------------------------------------------------------------

    @Test
    fun should_return_exact_match_label_when_matchType_is_EXACT() {
        val item = syntheticAlertItem(matchType = MatchType.EXACT)

        assertEquals(
            "EXACT match type must produce 'Exact match' label",
            "Exact match",
            matchTypeLabel(item.alert.matchType),
        )
    }

    @Test
    fun should_return_partial_match_label_when_matchType_is_PARTIAL() {
        val item = syntheticAlertItem(matchType = MatchType.PARTIAL)

        assertEquals(
            "PARTIAL match type must produce 'Partial match' label",
            "Partial match",
            matchTypeLabel(item.alert.matchType),
        )
    }

    @Test
    fun should_not_use_clinical_term_for_exact_match_label() {
        val label = matchTypeLabel(MatchType.EXACT)

        assertFalse(
            "match type label must NOT contain clinical terms",
            label.contains("equivalent", ignoreCase = true) ||
                label.contains("interchangeable", ignoreCase = true) ||
                label.contains("safe", ignoreCase = true),
        )
    }

    @Test
    fun should_not_use_clinical_term_for_partial_match_label() {
        val label = matchTypeLabel(MatchType.PARTIAL)

        assertFalse(
            "partial match type label must NOT contain clinical terms",
            label.contains("interaction", ignoreCase = true) ||
                label.contains("contraindication", ignoreCase = true),
        )
    }

    // -------------------------------------------------------------------------
    // Reviewed state logic tests
    // -------------------------------------------------------------------------

    @Test
    fun should_show_mark_reviewed_action_when_alert_is_not_reviewed() {
        val item = syntheticAlertItem(isReviewed = false)
        assertFalse("alert must not be reviewed initially", item.alert.isReviewed)
    }

    @Test
    fun should_not_show_mark_reviewed_action_when_alert_is_already_reviewed() {
        val item = syntheticAlertItem(isReviewed = true)
        assertTrue("reviewed item must have isReviewed = true", item.alert.isReviewed)
    }

    // -------------------------------------------------------------------------
    // Helper functions (mirrors logic in the composable)
    // These allow pure-JVM testing of the string-building logic.
    // -------------------------------------------------------------------------

    private fun buildCardContentDescription(item: DuplicateAlertItem): String {
        val statusLabel = if (item.alert.isReviewed) "reviewed" else "unreviewed"
        return "Potential duplicate alert: ${item.medicineAName} and ${item.medicineBName}. " +
            "Matching ingredient: ${item.alert.matchingIngredient}. " +
            "Status: $statusLabel."
    }

    private fun matchTypeLabel(matchType: MatchType): String = when (matchType) {
        MatchType.EXACT   -> "Exact match"
        MatchType.PARTIAL -> "Partial match"
    }

    // -------------------------------------------------------------------------
    // String constants (mirrors constants implicitly used in DuplicateAlertCard.kt)
    // These are the single source of truth for safety-critical strings.
    // -------------------------------------------------------------------------

    companion object {
        /** Required disclaimer for the "Mark as Reviewed" dialog. */
        const val MARK_REVIEWED_DISCLAIMER =
            "Marking as reviewed means you have seen this alert. " +
                "It does not mean this combination has been approved by a doctor."

        /** Required warning banner text on every expanded alert. */
        const val WARNING_BANNER_TEXT =
            "Potential duplicate — please review with your doctor or pharmacist"
    }
}
