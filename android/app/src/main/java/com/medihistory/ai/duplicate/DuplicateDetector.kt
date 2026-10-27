package com.medihistory.ai.duplicate

import com.medihistory.ai.data.model.MatchType
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus

/**
 * Deterministic algorithm for detecting potential duplicate active ingredients across a
 * patient's medicine list.
 *
 * ## What this does
 * Compares every pair of active medicines and flags pairs that share one or more active
 * ingredients (exact or via synonym) as "potential duplicates".
 *
 * ## What this does NOT do
 * - It does NOT indicate that any combination is safe or approved.
 * - It does NOT indicate that medicines are clinically interchangeable.
 * - It does NOT diagnose any condition or recommend any action.
 * - It does NOT produce AI-generated or probabilistic output — every result is a
 *   deterministic consequence of the input data.
 *
 * ## Medical safety
 * All results are labelled "Potential duplicate". The caller is responsible for surfacing
 * the disclaimer "Potential duplicate — please review with your doctor or pharmacist."
 * Duplicate alerts are informational only; they do not block the patient from recording
 * medicines.
 *
 * ## Determinism guarantee
 * Given the same list of medicines, this function always returns the same ordered list
 * of candidates. Input is sorted by [Medicine.id] before processing; output is sorted by
 * (medicineAId, medicineBId).
 */
object DuplicateDetector {

    /**
     * Detects potential duplicate active ingredients across [medicines].
     *
     * Only medicines with [MedicineStatus.CURRENT] or [MedicineStatus.UNKNOWN] status are
     * considered. [MedicineStatus.PAST] medicines are excluded because they are no longer
     * part of the patient's active regimen.
     *
     * Medicines with a null or blank [Medicine.activeIngredient] are also excluded — the
     * algorithm never fabricates or infers missing ingredient data.
     *
     * @param medicines Full list of medicines to evaluate. May be empty.
     * @return Sorted list of [DuplicateCandidate] pairs. Empty if no potential duplicates
     *         are found. Never null.
     */
    fun detect(medicines: List<Medicine>): List<DuplicateCandidate> {
        // Filter to active medicines with a known ingredient, sorted deterministically
        val active = medicines
            .filter { it.status != MedicineStatus.PAST }
            .filter { !it.activeIngredient.isNullOrBlank() }
            .sortedBy { it.id }

        val candidates = mutableListOf<DuplicateCandidate>()

        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                checkPair(active[i], active[j])?.let { candidates.add(it) }
            }
        }

        // Sort output deterministically so same input always produces same output order
        return candidates.sortedWith(compareBy({ it.medicineAId }, { it.medicineBId }))
    }

    /**
     * Checks one pair of medicines for a potential duplicate ingredient.
     *
     * Returns a [DuplicateCandidate] if a match is found, or null if there is no overlap.
     *
     * Match priority:
     * 1. EXACT: the full normalised active ingredient strings are synonyms.
     * 2. PARTIAL: one or more ingredient tokens in a split ("+", "/", ",") overlap after
     *    normalisation. Produces a lower-confidence candidate with a combination note.
     */
    private fun checkPair(a: Medicine, b: Medicine): DuplicateCandidate? {
        val ingA = a.activeIngredient ?: return null
        val ingB = b.activeIngredient ?: return null

        // --- EXACT match (including synonym equivalence) ---
        if (IngredientNormalizer.isSynonym(ingA, ingB)) {
            return DuplicateCandidate(
                medicineAId = a.id,
                medicineBId = b.id,
                matchingIngredient = IngredientNormalizer.normalize(ingA),
                matchType = MatchType.EXACT,
                confidence = 0.95f,
                combinationNote = null,
            )
        }

        // --- PARTIAL match: combination product overlap ---
        // Split each ingredient string on common combination delimiters
        val aNorm = IngredientNormalizer.normalize(ingA)
        val bNorm = IngredientNormalizer.normalize(ingB)
        val aParts = splitIngredients(aNorm)
        val bParts = splitIngredients(bNorm)

        // Only check for partial matches when at least one side is a combination
        if (aParts.size <= 1 && bParts.size <= 1) return null

        val common = aParts.intersect(bParts.toSet())
        if (common.isEmpty()) return null

        val matchingIng = common.first()
        return DuplicateCandidate(
            medicineAId = a.id,
            medicineBId = b.id,
            matchingIngredient = matchingIng,
            matchType = MatchType.PARTIAL,
            confidence = 0.70f,
            combinationNote = buildCombinationNote(
                medicineAName = a.medicineName,
                medicineBName = b.medicineName,
                matchingIngredient = matchingIng,
            ),
        )
    }

    /**
     * Splits a (possibly already-normalised) ingredient string on "+", "/" and ","
     * delimiters and trims whitespace from each token.
     *
     * Single-ingredient strings return a one-element list.
     */
    private fun splitIngredients(ingredient: String): List<String> =
        ingredient.split("+", "/", ",").map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Builds the combination note that accompanies a PARTIAL match.
     *
     * The note is informational only. It names both medicines and the matching ingredient
     * but makes NO clinical claim about safety or interchangeability.
     */
    private fun buildCombinationNote(
        medicineAName: String,
        medicineBName: String,
        matchingIngredient: String,
    ): String =
        "Both $medicineAName and $medicineBName contain $matchingIngredient. " +
            "One or both may be a combination product. " +
            "Please review with your doctor or pharmacist."
}

/**
 * An intermediate result produced by [DuplicateDetector.detect] before it is persisted
 * as a [com.medihistory.ai.data.model.DuplicateAlert].
 *
 * Medical safety: this value type never claims that a combination is safe, approved, or
 * clinically interchangeable. It is always labelled "Potential duplicate" in the UI.
 */
data class DuplicateCandidate(
    val medicineAId: String,
    val medicineBId: String,
    val matchingIngredient: String,
    val matchType: MatchType,
    /** Confidence score between 0.0 and 1.0. Lower values indicate greater uncertainty. */
    val confidence: Float,
    /**
     * Informational note for PARTIAL (combination product) matches.
     * Null for EXACT matches.
     * Never implies clinical safety or approval.
     */
    val combinationNote: String?,
)
