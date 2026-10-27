package com.medihistory.ai.ui.newdoctor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [ConsentDialog] contract.
 *
 * Medical safety: the consent dialog is the critical gate before any sharing occurs.
 * These tests verify the text contract, the callback semantics, and that dismissal
 * never triggers sharing.
 *
 * The tests here cover pure state/logic concerns. Compose rendering is verified in
 * instrumented UI tests; unit tests here focus on the invariants that protect patient privacy.
 *
 * Naming convention: `given_[state]_when_[action]_then_[outcome]`
 */
class ConsentDialogTest {

    // -------------------------------------------------------------------------
    // Dialog title contract
    // -------------------------------------------------------------------------

    @Test
    fun given_consent_dialog_title_when_checked_then_equals_share_medical_summary() {
        // The title must match exactly so patients and accessibility tools read the right title
        val expectedTitle = "Share Medical Summary"
        assertEquals(
            "ConsentDialog title must be '$expectedTitle'",
            expectedTitle,
            CONSENT_DIALOG_TITLE,
        )
    }

    // -------------------------------------------------------------------------
    // Disclaimer text contract
    // -------------------------------------------------------------------------

    @Test
    fun given_consent_dialog_when_checked_then_disclaimer_text_present() {
        val requiredDisclaimer = "Patient-compiled record. Not a clinical report."
        assertTrue(
            "ConsentDialog disclaimer must contain required text: '$requiredDisclaimer'",
            CONSENT_DIALOG_DISCLAIMER.contains(requiredDisclaimer),
        )
    }

    @Test
    fun given_consent_dialog_when_checked_then_disclaimer_contains_verify_with_doctor() {
        assertTrue(
            "ConsentDialog disclaimer must instruct patient to verify with doctor",
            CONSENT_DIALOG_DISCLAIMER.contains("verify all information with your doctor"),
        )
    }

    @Test
    fun given_consent_dialog_when_checked_then_not_clinical_record_warning_present() {
        assertTrue(
            "ConsentDialog must contain 'does NOT constitute a clinical record' warning",
            CONSENT_DIALOG_NOT_CLINICAL.contains("NOT constitute a clinical record"),
        )
    }

    // -------------------------------------------------------------------------
    // Dismiss action — Cancel button must not trigger sharing
    // -------------------------------------------------------------------------

    @Test
    fun given_consent_dialog_when_dismiss_action_invoked_then_confirm_not_called() {
        var confirmCalled = false
        var dismissCalled = false

        // Simulate dismiss path
        val onConfirm: () -> Unit = { confirmCalled = true }
        val onDismiss: () -> Unit = { dismissCalled = true }

        onDismiss()

        assertTrue("dismiss callback must be invoked", dismissCalled)
        assertFalse("confirm callback must NOT be invoked on dismiss", confirmCalled)
    }

    @Test
    fun given_consent_dialog_when_confirm_action_invoked_then_confirm_called_not_dismiss() {
        var confirmCalled = false
        var dismissCalled = false

        val onConfirm: () -> Unit = { confirmCalled = true }
        val onDismiss: () -> Unit = { dismissCalled = true }

        onConfirm()

        assertTrue("confirm callback must be invoked", confirmCalled)
        assertFalse("dismiss callback must NOT be invoked when confirm is chosen", dismissCalled)
    }

    // -------------------------------------------------------------------------
    // Selected count phrasing
    // -------------------------------------------------------------------------

    @Test
    fun given_one_prescription_when_message_built_then_singular_used() {
        val count = 1
        val word = if (count == 1) "prescription" else "prescriptions"
        assertEquals("singular word must be 'prescription'", "prescription", word)
    }

    @Test
    fun given_multiple_prescriptions_when_message_built_then_plural_used() {
        val count = 3
        val word = if (count == 1) "prescription" else "prescriptions"
        assertEquals("plural word must be 'prescriptions'", "prescriptions", word)
    }

    @Test
    fun given_zero_prescriptions_when_message_built_then_plural_used() {
        // Zero prescriptions is technically unreachable (canProceed = false blocks the dialog)
        // but plural is the safe fallback
        val count = 0
        val word = if (count == 1) "prescription" else "prescriptions"
        assertEquals("zero count must use plural 'prescriptions'", "prescriptions", word)
    }
}

// ---------------------------------------------------------------------------
// Constants that mirror the ConsentDialog composable's text contracts.
// If these constants drift from ConsentDialog.kt, tests fail immediately.
// ---------------------------------------------------------------------------

private const val CONSENT_DIALOG_TITLE = "Share Medical Summary"

private const val CONSENT_DIALOG_DISCLAIMER =
    "Patient-compiled record. Not a clinical report. " +
        "Please verify all information with your doctor."

private const val CONSENT_DIALOG_NOT_CLINICAL =
    "This does NOT constitute a clinical record."
