package com.medihistory.ai.data.duplicate

import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DuplicateExplainerEngine].
 *
 * All fixtures use synthetic data only — no real medicine brand names or patient data.
 * Naming convention: `test_[outcome]_[condition]`
 */
class DuplicateExplainerEngineTest {

    private lateinit var engine: DuplicateExplainerEngine

    // -------------------------------------------------------------------------
    // Synthetic fixtures
    // -------------------------------------------------------------------------

    private fun syntheticMedicineA() = Medicine(
        id                  = "med-synth-a",
        prescriptionId      = "presc-synth-001",
        patientId           = "test-patient-alpha",
        medicineName        = "Synthamol 500mg",
        activeIngredient    = "paracetamol",
        status              = MedicineStatus.CURRENT,
        createdAt           = 1_700_000_000_000L,
        updatedAt           = 1_700_000_000_000L,
    )

    private fun syntheticMedicineB() = Medicine(
        id                  = "med-synth-b",
        prescriptionId      = "presc-synth-002",
        patientId           = "test-patient-alpha",
        medicineName        = "Testamol XR",
        activeIngredient    = "paracetamol",
        status              = MedicineStatus.CURRENT,
        createdAt           = 1_700_000_000_000L,
        updatedAt           = 1_700_000_000_000L,
    )

    private fun exactAlert(combinationNote: String? = null) = DuplicateAlert(
        id                 = "alert-exact-001",
        patientId          = "test-patient-alpha",
        medicineAId        = "med-synth-a",
        medicineBId        = "med-synth-b",
        matchingIngredient = "paracetamol",
        matchType          = MatchType.EXACT,
        confidence         = 0.95f,
        combinationNote    = combinationNote,
        createdAt          = 1_700_000_000_000L,
    )

    private fun partialAlert(combinationNote: String? = null) = DuplicateAlert(
        id                 = "alert-partial-001",
        patientId          = "test-patient-alpha",
        medicineAId        = "med-synth-a",
        medicineBId        = "med-synth-b",
        matchingIngredient = "paracetamol",
        matchType          = MatchType.PARTIAL,
        confidence         = 0.70f,
        combinationNote    = combinationNote,
        createdAt          = 1_700_000_000_000L,
    )

    @Before
    fun setUp() {
        engine = DuplicateExplainerEngine()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun test_exact_match_explanation_contains_ingredient_names() {
        val result = engine.explain(exactAlert(), syntheticMedicineA(), syntheticMedicineB())

        assertTrue(
            "Output must contain medicine A name",
            result.contains("Synthamol 500mg"),
        )
        assertTrue(
            "Output must contain medicine B name",
            result.contains("Testamol XR"),
        )
        assertTrue(
            "Output must contain the matching ingredient",
            result.contains("paracetamol"),
        )
    }

    @Test
    fun test_partial_match_with_combination_note_uses_note() {
        val combinationNote = "Synthamol 500mg is a combination product."
        val result = engine.explain(
            partialAlert(combinationNote = combinationNote),
            syntheticMedicineA(),
            syntheticMedicineB(),
        )

        assertTrue(
            "Output must include the combination note",
            result.contains(combinationNote),
        )
    }

    @Test
    fun test_partial_match_without_combination_note_uses_fallback() {
        val result = engine.explain(
            partialAlert(combinationNote = null),
            syntheticMedicineA(),
            syntheticMedicineB(),
        )

        assertTrue(
            "Output must include medicine A name in fallback",
            result.contains("Synthamol 500mg"),
        )
        assertTrue(
            "Output must mention combination product in fallback",
            result.contains("combination product"),
        )
    }

    @Test
    fun test_output_never_contains_forbidden_words() {
        val exactResult = engine.explain(exactAlert(), syntheticMedicineA(), syntheticMedicineB())
        val partialResult = engine.explain(partialAlert(), syntheticMedicineA(), syntheticMedicineB())

        // Forbidden words per medical safety brief
        val forbiddenWords = listOf("safe", "unsafe", "dangerous", "harmful", "interchangeable")

        forbiddenWords.forEach { word ->
            assertFalse(
                "EXACT explanation must NOT contain forbidden word: '$word'",
                exactResult.lowercase().contains(word),
            )
            assertFalse(
                "PARTIAL explanation must NOT contain forbidden word: '$word'",
                partialResult.lowercase().contains(word),
            )
        }
    }

    @Test
    fun test_explanation_contains_potential_duplicate_label() {
        val exactResult = engine.explain(exactAlert(), syntheticMedicineA(), syntheticMedicineB())
        val partialResult = engine.explain(partialAlert(), syntheticMedicineA(), syntheticMedicineB())

        assertTrue(
            "EXACT explanation must use 'potential duplicate' language",
            exactResult.lowercase().contains("potential duplicate"),
        )
        assertTrue(
            "PARTIAL explanation must use 'potential duplicate' language",
            partialResult.lowercase().contains("potential duplicate"),
        )
    }
}
