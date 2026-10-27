package com.medihistory.ai.duplicate

/**
 * Deterministic normaliser for active ingredient names.
 *
 * Maps known synonyms to a single canonical form so that duplicate detection works
 * across equivalent ingredient names regardless of how they appear on prescriptions.
 *
 * This is a static, deterministic lookup table — NOT AI-generated.
 * Same input always produces the same output.
 *
 * Medical safety: normalisation establishes naming equivalence only.
 * It does NOT imply that medicines sharing an active ingredient are clinically
 * interchangeable, that any combination is safe, or that any medicine can be substituted
 * for another.
 *
 * To add a new synonym: add an entry mapping the alternative name (lowercase) to its
 * canonical form (lowercase). The canonical form is the most widely used INN name.
 */
object IngredientNormalizer {

    /**
     * Synonym map: alternative name (lowercase) → canonical INN name (lowercase).
     *
     * Both directions must be explicit when the canonical form differs from the alternative.
     * e.g. "acetaminophen" → "paracetamol" (canonical is paracetamol per INN),
     *      "paracetamol"   → "paracetamol" (identity mapping for direct lookups).
     */
    private val synonyms: Map<String, String> = mapOf(
        // Paracetamol / acetaminophen
        "paracetamol" to "paracetamol",
        "acetaminophen" to "paracetamol",

        // Metoprolol salt forms — distinct salts, each maps to itself
        "metoprolol succinate" to "metoprolol succinate",
        "metoprolol tartrate" to "metoprolol tartrate",

        // Ibuprofen
        "ibuprofen" to "ibuprofen",

        // Metformin
        "metformin" to "metformin",
        "metformin hydrochloride" to "metformin",

        // Amlodipine
        "amlodipine" to "amlodipine",
        "amlodipine besylate" to "amlodipine",
        "amlodipine maleate" to "amlodipine",

        // Atorvastatin
        "atorvastatin" to "atorvastatin",
        "atorvastatin calcium" to "atorvastatin",

        // Omeprazole
        "omeprazole" to "omeprazole",
        "omeprazole magnesium" to "omeprazole",

        // Pantoprazole
        "pantoprazole" to "pantoprazole",
        "pantoprazole sodium" to "pantoprazole",

        // Aspirin
        "aspirin" to "aspirin",
        "acetylsalicylic acid" to "aspirin",

        // Cetirizine
        "cetirizine" to "cetirizine",
        "cetirizine hydrochloride" to "cetirizine",

        // Levothyroxine
        "levothyroxine" to "levothyroxine",
        "levothyroxine sodium" to "levothyroxine",

        // Lisinopril
        "lisinopril" to "lisinopril",
    )

    /**
     * Returns the canonical form of [ingredient].
     *
     * If [ingredient] is in the synonym map its canonical name is returned.
     * Otherwise the trimmed lowercase form is returned as-is.
     *
     * This function is pure and deterministic: same input always produces same output.
     */
    fun normalize(ingredient: String): String {
        val key = ingredient.lowercase().trim()
        return synonyms[key] ?: key
    }

    /**
     * Returns true if [a] and [b] normalise to the same canonical ingredient name.
     *
     * This establishes naming equivalence only. It does NOT imply clinical
     * interchangeability or that a combination of these ingredients is safe.
     */
    fun isSynonym(a: String, b: String): Boolean = normalize(a) == normalize(b)
}
