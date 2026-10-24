# Skill: Medicine Normalisation and Duplicate Detection for MediHistory AI

## Purpose

Guide correct, deterministic medicine normalisation and duplicate detection in MediHistory AI. Covers active ingredient extraction, brand-to-generic mapping, synonym resolution, combination product decomposition, strength normalisation, uncertainty handling, and the duplicate-matching algorithm.

## When to Apply This Skill

- Implementing or modifying `DuplicateDetector`
- Adding entries to the synonym table or brand-to-generic map
- Writing normalisation logic for medicine names or strengths
- Adding or updating `AlertGenerationWorker`
- Writing tests for duplicate detection edge cases

## Steering Files to Read First

- `.kiro/steering/medical-safety.md`
- `.kiro/steering/product.md`

---

## Step-by-Step Guidance

### 1. Normalise Before Any Comparison

Always normalise both sides of a comparison using the same pipeline:
1. Lowercase
2. Remove punctuation (hyphens, commas, parentheses)
3. Trim whitespace
4. Resolve synonyms from the local synonym table (e.g. paracetamol → acetaminophen)
5. Normalise strength units (e.g. "500 mg" → "500mg")

Store both the original `rawName` and the `normalisedName` — never discard the original.

### 2. Decompose Combination Products

A medicine name matching the pattern "A + B" (or "A/B") is a combination product. Decompose it into its constituent ingredients and match each ingredient independently. Store the constituent list alongside the original name.

### 3. Resolve Synonyms from a Local Table Only

The synonym table is a static local resource (a bundled file or compiled constant map). Synonym resolution never calls an external API. If a synonym is missing, it is added to the local table in a code change — not fetched at runtime.

### 4. Keep DuplicateDetector Pure

`DuplicateDetector` is a pure function (or a stateless class with no dependencies):
- Input: two lists of `NormalisedMedicine` records.
- Output: a list of `DuplicatePair` candidates.
- No database reads or writes.
- No side effects.
- Same inputs → same output, always (deterministic).

### 5. Only Include REVIEWED Records

Duplicate detection runs only over `PrescriptionMedicine` records whose `reviewState == REVIEWED`. Unreviewed (`PENDING_REVIEW`) and rejected (`REJECTED`) records are excluded from detection input.

### 6. Exclude Self-Matches

A medicine is never matched against itself (same prescription, same record). Self-matches are filtered before returning results.

### 7. Assign Match Confidence Correctly

| Match type | Condition | Confidence |
|---|---|---|
| Full match | All active ingredients match | Standard (e.g. 1.0) |
| Partial match | Combination vs single: one ingredient matches | Reduced (e.g. 0.6), `combinationNote` populated |

### 8. Label Every Alert as "Potential Duplicate"

Every `DuplicatePair` result is labelled "Potential duplicate" in the UI. Never use "Duplicate medication", "Safe", "Confirmed", or any clinical language. Alerts are informational — they do not block recording medicines.

### 9. Trigger Detection via AlertGenerationWorker

Duplicate detection is triggered by `AlertGenerationWorker` (WorkManager) after a `PrescriptionMedicine` record transitions to `REVIEWED`. It is not run inline in a ViewModel or on the main thread.

---

## Patterns to Follow

- Normalise to lowercase, remove punctuation, trim whitespace before any comparison.
- Resolve synonyms from a local static table — no external API calls.
- Decompose combination products into constituent ingredients.
- Normalise strength units consistently (e.g. "500 mg" → "500mg").
- Store both `rawName` and `normalisedName` — never discard the original.
- `DuplicateDetector` is a pure function with no database writes or side effects.
- Only `REVIEWED` records participate in detection.
- Exclude self-matches (same prescription).
- Label all alerts as "Potential duplicate" — never clinical language.
- Wrap detection in `AlertGenerationWorker`; never block the main thread.

## Patterns to Avoid

- Using AI, ML models, or fuzzy matching for duplicate detection (deterministic only).
- Calling an external API for synonym resolution.
- Running detection on `PENDING_REVIEW` or `REJECTED` records.
- Producing a `DuplicatePair` for a medicine matched against itself.
- Labelling any alert as "Duplicate medication", "Confirmed", "Safe", or "Cleared".
- Modifying the database inside `DuplicateDetector`.
- Blocking the main thread with detection logic.
- Discarding `rawName` after normalisation.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```kotlin
// ── Synonym table (local static resource, no external API) ───────────────────
object SynonymTable {
    // Map each synonym to its canonical active ingredient name
    private val table: Map<String, String> = mapOf(
        "paracetamol"  to "acetaminophen",
        "metformin hcl" to "metformin",
        // ... add entries via code change, never fetched at runtime
    )

    fun resolve(name: String): String = table[name.lowercase().trim()] ?: name.lowercase().trim()
}


// ── Normalisation pipeline ────────────────────────────────────────────────────
object MedicineNormaliser {
    private val punctuationRegex = Regex("[^a-z0-9 ]")
    private val strengthRegex    = Regex("""(\d+)\s*(mg|ml|mcg|g)""")

    fun normalise(rawName: String): NormalisedMedicine {
        val step1 = rawName.lowercase()
        val step2 = step1.replace(punctuationRegex, " ").trim()
        val step3 = SynonymTable.resolve(step2)
        val step4 = normaliseStrength(step3)
        val ingredients = decomposeCombination(step4)
        return NormalisedMedicine(rawName = rawName, normalisedName = step4, ingredients = ingredients)
    }

    private fun normaliseStrength(name: String): String =
        name.replace(strengthRegex) { "${it.groupValues[1]}${it.groupValues[2]}" }

    private fun decomposeCombination(name: String): List<String> =
        if (name.contains('+') || name.contains('/'))
            name.split('+', '/').map { it.trim() }
        else
            listOf(name)
}

data class NormalisedMedicine(
    val rawName: String,          // original — never discarded
    val normalisedName: String,
    val ingredients: List<String>
)


// ── DuplicateDetector (pure function — no DB writes, no side effects) ─────────
object DuplicateDetector {

    fun detect(
        reviewedMedicines: List<ReviewedMedicine>   // REVIEWED records only
    ): List<DuplicatePair> {
        val pairs = mutableListOf<DuplicatePair>()
        for (i in reviewedMedicines.indices) {
            for (j in i + 1 until reviewedMedicines.size) {
                val a = reviewedMedicines[i]
                val b = reviewedMedicines[j]
                if (a.prescriptionId == b.prescriptionId) continue  // exclude self-matches
                match(a, b)?.let { pairs.add(it) }
            }
        }
        return pairs  // deterministic: same input → same output
    }

    private fun match(a: ReviewedMedicine, b: ReviewedMedicine): DuplicatePair? {
        val sharedIngredients = a.normalisedMedicine.ingredients
            .intersect(b.normalisedMedicine.ingredients.toSet())
        return when {
            sharedIngredients.isEmpty() -> null
            sharedIngredients.size == a.normalisedMedicine.ingredients.size &&
            sharedIngredients.size == b.normalisedMedicine.ingredients.size ->
                // Full match — all ingredients match
                DuplicatePair(a, b, confidence = 1.0f, combinationNote = null)
            else ->
                // Partial match — combination vs single or partial overlap
                DuplicatePair(
                    a, b,
                    confidence = 0.6f,
                    combinationNote = "Shared ingredient(s): ${sharedIngredients.joinToString()}"
                )
        }
    }
}

data class DuplicatePair(
    val medicineA: ReviewedMedicine,
    val medicineB: ReviewedMedicine,
    val confidence: Float,
    val combinationNote: String?    // populated for partial combination matches
    // Label in UI: always "Potential duplicate" — never "confirmed" or "safe"
)


// ── AlertGenerationWorker (triggers detection after review state change) ──────
class AlertGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: PrescriptionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val patientId = inputData.getString(KEY_PATIENT_ID) ?: return Result.failure()
        val reviewedMedicines = repository.getReviewedMedicinesForPatient(patientId)
        val duplicates = DuplicateDetector.detect(reviewedMedicines)
        repository.saveDuplicateAlerts(patientId, duplicates)
        return Result.success()
    }

    companion object { const val KEY_PATIENT_ID = "patient_id" }
}
```

---

## Key Constraints (from Steering)

- Detection is deterministic: same inputs must always produce the same output.
- Only `REVIEWED` records participate in detection.
- Every alert is "Potential duplicate" — never clinical language.
- `DuplicateDetector` has no database writes or side effects.
- Synonym resolution uses a local static table only — no external API calls at runtime.
- Never discard `rawName` after normalisation.
- Use synthetic medicine names in all tests (e.g. "Synthamol 500mg", "Testocillin 250mg").
