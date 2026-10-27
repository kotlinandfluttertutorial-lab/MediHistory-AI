package com.medihistory.ai.ui.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [InteractionResult] and [InteractionCheckStatus].
 *
 * These tests enforce the medical safety invariants:
 * - [InteractionResult.reviewWarning] is always non-empty.
 * - [InteractionResult.severity] never contains "safe" or "no interaction".
 * - [InteractionCheckStatus.NotConfigured] is distinct from [InteractionCheckStatus.Results]
 *   with an empty list — the two states have different semantics.
 *
 * Naming convention: `test_[outcome]_[condition]`
 */
class InteractionResultTest {

    // ─────────────────────────────────────────────────────────────────────────
    // InteractionResult construction
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test_valid_InteractionResult_when_severity_is_consult_doctor() {
        // Must not throw
        val result = InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Consult doctor",
            reviewWarning = "Please review this with your doctor or pharmacist.",
        )
        assertTrue(result.reviewWarning.isNotBlank())
        assertEquals("Consult doctor", result.severity)
    }

    @Test
    fun test_valid_InteractionResult_when_severity_is_review_with_pharmacist() {
        val result = InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Review with pharmacist",
            reviewWarning = "Please review this with your doctor or pharmacist.",
        )
        assertTrue(result.reviewWarning.isNotBlank())
        assertEquals("Review with pharmacist", result.severity)
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_reviewWarning_is_blank() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Consult doctor",
            reviewWarning = "",   // blank — must throw
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_reviewWarning_is_whitespace_only() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Consult doctor",
            reviewWarning = "   ",   // whitespace only — must throw
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_severity_is_safe() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "safe",   // forbidden — must throw
            reviewWarning = "Please review with your doctor.",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_severity_contains_safe_mixed_case() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Safe to combine",   // contains "safe" — must throw
            reviewWarning = "Please review with your doctor.",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_severity_is_no_interaction() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "no interaction",   // forbidden — must throw
            reviewWarning = "Please review with your doctor.",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_throws_when_severity_contains_no_interaction_mixed_case() {
        InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "No Interaction Found",   // contains "no interaction" — must throw
            reviewWarning = "Please review with your doctor.",
        )
    }

    @Test
    fun test_reviewWarning_is_non_empty_in_all_valid_results() {
        val results = listOf(
            InteractionResult(
                medicineAName = "Synthamol 500mg",
                medicineBName = "Testocillin 250mg",
                severity = "Consult doctor",
                reviewWarning = "Please review this with your doctor or pharmacist.",
            ),
            InteractionResult(
                medicineAName = "Synth-B 100mg",
                medicineBName = "Testomycin 500mg",
                severity = "Review with pharmacist",
                reviewWarning = "Discuss with your pharmacist before continuing.",
            ),
        )
        for (result in results) {
            assertTrue(
                "reviewWarning must be non-empty for result: $result",
                result.reviewWarning.isNotBlank(),
            )
        }
    }

    @Test
    fun test_severity_never_contains_safe_in_valid_results() {
        val validSeverities = listOf("Consult doctor", "Review with pharmacist")
        for (severity in validSeverities) {
            val result = InteractionResult(
                medicineAName = "Synthamol 500mg",
                medicineBName = "Testocillin 250mg",
                severity = severity,
                reviewWarning = "Please review with your doctor.",
            )
            assertFalse(
                "severity must not contain 'safe', was: ${result.severity}",
                result.severity.lowercase().contains("safe"),
            )
        }
    }

    @Test
    fun test_severity_never_contains_no_interaction_in_valid_results() {
        val result = InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Consult doctor",
            reviewWarning = "Please review with your doctor.",
        )
        assertFalse(
            "severity must not contain 'no interaction', was: ${result.severity}",
            result.severity.lowercase().contains("no interaction"),
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // InteractionCheckStatus — NotConfigured vs Results(emptyList())
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun test_NotConfigured_is_distinct_from_Results_with_empty_list() {
        val notConfigured: InteractionCheckStatus = InteractionCheckStatus.NotConfigured
        val emptyResults: InteractionCheckStatus = InteractionCheckStatus.Results(emptyList())

        // These must be different instances of different sealed class members.
        assertNotEquals(
            "NotConfigured must not equal Results(emptyList()). " +
                "An empty result list implies 'checking ran, found nothing'. " +
                "NotConfigured implies 'checking was not attempted'.",
            notConfigured,
            emptyResults,
        )
    }

    @Test
    fun test_NotConfigured_is_not_instance_of_Results() {
        val status: InteractionCheckStatus = InteractionCheckStatus.NotConfigured
        assertFalse(
            "NotConfigured must not be an instance of Results",
            status is InteractionCheckStatus.Results,
        )
    }

    @Test
    fun test_Results_with_empty_list_is_not_instance_of_NotConfigured() {
        val status: InteractionCheckStatus = InteractionCheckStatus.Results(emptyList())
        assertFalse(
            "Results(emptyList()) must not be an instance of NotConfigured",
            status is InteractionCheckStatus.NotConfigured,
        )
    }

    @Test
    fun test_Results_with_non_empty_list_contains_expected_interaction() {
        val result = InteractionResult(
            medicineAName = "Synthamol 500mg",
            medicineBName = "Testocillin 250mg",
            severity = "Consult doctor",
            reviewWarning = "Please review this with your doctor or pharmacist.",
        )
        val status: InteractionCheckStatus = InteractionCheckStatus.Results(listOf(result))

        assertTrue(status is InteractionCheckStatus.Results)
        assertEquals(1, (status as InteractionCheckStatus.Results).interactions.size)
    }

    @Test
    fun test_all_sealed_subclasses_are_present() {
        // Smoke test that the sealed class has the four expected subtypes.
        // If a subclass is accidentally removed, this test catches it.
        val notConfigured: InteractionCheckStatus = InteractionCheckStatus.NotConfigured
        val checking: InteractionCheckStatus = InteractionCheckStatus.Checking
        val results: InteractionCheckStatus = InteractionCheckStatus.Results(emptyList())
        val error: InteractionCheckStatus = InteractionCheckStatus.Error("Something went wrong")

        assertTrue(notConfigured is InteractionCheckStatus.NotConfigured)
        assertTrue(checking is InteractionCheckStatus.Checking)
        assertTrue(results is InteractionCheckStatus.Results)
        assertTrue(error is InteractionCheckStatus.Error)
    }
}
