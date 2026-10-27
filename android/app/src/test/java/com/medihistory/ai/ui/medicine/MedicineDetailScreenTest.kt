package com.medihistory.ai.ui.medicine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure unit tests for [MedicineDetailUiState] and [DISCLAIMER_TEXT] constant.
 *
 * These tests verify state shape and invariants without a Compose test rule or Hilt.
 * UI interaction tests (compose semantics, TalkBack) live in the androidTest source set.
 *
 * Naming convention: `should_[outcome]_when_[condition]`
 */
class MedicineDetailScreenTest {

    @Test
    fun should_have_isLoading_false_in_default_state() {
        val state = MedicineDetailUiState()
        assertFalse(
            "default MedicineDetailUiState must not be in loading state",
            state.isLoading,
        )
    }

    @Test
    fun should_have_null_medicine_in_default_state() {
        val state = MedicineDetailUiState()
        assertNull(
            "default MedicineDetailUiState must have null medicine",
            state.medicine,
        )
    }

    @Test
    fun should_have_null_error_in_default_state() {
        val state = MedicineDetailUiState()
        assertNull(
            "default MedicineDetailUiState must have null error",
            state.error,
        )
    }

    @Test
    fun should_reflect_isLoading_true_when_set() {
        val state = MedicineDetailUiState(isLoading = true)
        assertEquals(
            "isLoading must be true when explicitly set",
            true,
            state.isLoading,
        )
    }

    @Test
    fun should_contain_doctor_verification_text_in_disclaimer() {
        // The disclaimer must always reference doctor or pharmacist so the patient
        // knows where to verify the extracted data.
        val lowerDisclaimer = DISCLAIMER_TEXT.lowercase()
        val containsDoctor = lowerDisclaimer.contains("doctor")
        val containsPharmacist = lowerDisclaimer.contains("pharmacist")
        assert(containsDoctor && containsPharmacist) {
            "DISCLAIMER_TEXT must mention both 'doctor' and 'pharmacist' but was: $DISCLAIMER_TEXT"
        }
    }

    @Test
    fun should_contain_prescription_reference_in_disclaimer() {
        // The disclaimer must explain that the data came from a prescription document.
        assert(DISCLAIMER_TEXT.lowercase().contains("prescription")) {
            "DISCLAIMER_TEXT must mention 'prescription' but was: $DISCLAIMER_TEXT"
        }
    }
}
