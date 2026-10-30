package com.medihistory.ai.data.duplicate

import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.Medicine
import javax.inject.Inject

/**
 * Generates a plain-language explanation for a duplicate ingredient alert pair.
 *
 * Medical safety invariants:
 * - Output ALWAYS uses "potential duplicate" — never "duplicate medication".
 * - Output NEVER uses the words: safe, unsafe, dangerous, harmful, interchangeable.
 * - Output NEVER gives clinical advice or implies a drug combination is clinically
 *   problematic or should be changed.
 * - The suggested doctor question is informational only.
 * - [explain] is a pure function — no I/O, no side effects.
 *
 * This engine reads only confirmed [Medicine] data (REVIEWED fields via PostReviewProcessor).
 * It never reads raw OCR output.
 */
class DuplicateExplainerEngine @Inject constructor() {

    /**
     * Generates a plain-language explanation for [alert].
     *
     * @param alert     The duplicate alert entity.
     * @param medicineA The Medicine entity for [DuplicateAlert.medicineAId].
     * @param medicineB The Medicine entity for [DuplicateAlert.medicineBId].
     * @return          A non-blank explanation string. Never null.
     */
    fun explain(
        alert: DuplicateAlert,
        medicineA: Medicine,
        medicineB: Medicine,
    ): String {
        val nameA = medicineA.medicineName
        val nameB = medicineB.medicineName
        val ingredient = alert.matchingIngredient

        val body = when (alert.matchType) {
            MatchType.EXACT -> buildExactExplanation(nameA, nameB, ingredient)
            MatchType.PARTIAL -> buildPartialExplanation(alert, nameA, nameB, ingredient)
        }

        val suggestedQuestion =
            "\n\nSuggested question for your doctor: " +
                "\u2018Are both $nameA and $nameB still needed, or can one be reviewed?\u2019"

        return body + suggestedQuestion
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun buildExactExplanation(
        nameA: String,
        nameB: String,
        ingredient: String,
    ): String =
        "This is a potential duplicate alert. " +
            "Both $nameA and $nameB contain $ingredient as the active ingredient. " +
            "Taking both may increase the total amount of $ingredient you receive. " +
            "Please review this with your doctor or pharmacist before making any changes."

    private fun buildPartialExplanation(
        alert: DuplicateAlert,
        nameA: String,
        nameB: String,
        ingredient: String,
    ): String {
        val combinationContext = if (!alert.combinationNote.isNullOrBlank()) {
            " ${alert.combinationNote}"
        } else {
            " $nameA may be a combination product containing $ingredient."
        }
        return "This is a potential duplicate alert.$combinationContext " +
            "$nameB also contains $ingredient. " +
            "This may result in overlapping amounts of $ingredient. " +
            "Please review this with your doctor or pharmacist before making any changes."
    }
}
