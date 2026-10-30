package com.medihistory.ai.data.ocr

import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.ExtractedFieldEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Suggests corrections for low-confidence OCR fields.
 *
 * Medical safety invariants:
 * - NEVER suggests a value for dose, frequency, diagnosis, allergy, duration, or any
 *   other clinical field. Returns null unconditionally for those fields.
 * - Returns null for any field not explicitly handled — no fabrication.
 * - Suggestions are derived directly from [rawValue] — nothing is invented.
 * - The patient MUST confirm every suggestion before it is applied.
 *
 * Privacy: field values must never be logged inside this class.
 *
 * This is a pure, synchronous class. Doctor lookup is passed in as a parameter
 * so callers (ViewModels) can resolve the list asynchronously before calling.
 */
class OcrCorrectionEngine
    @Inject
    constructor() {
        /**
         * Returns a suggested corrected value for [fieldName] with [rawValue], or null if
         * no confident suggestion can be derived.
         *
         * @param fieldName      The OCR field key (e.g. "doctor_name", "prescription_date").
         * @param rawValue       The raw OCR string for this field. May be blank.
         * @param allFields      All extracted fields for the same prescription (for context).
         * @param knownDoctors   The patient's known doctors — used for fuzzy name matching.
         */
        fun suggest(
            fieldName: String,
            rawValue: String,
            allFields: List<ExtractedFieldEntity> = emptyList(),
            knownDoctors: List<Doctor> = emptyList(),
        ): String? {
            if (rawValue.isBlank()) return null

            return when {
                fieldName == "doctor_name" ->
                    suggestDoctorName(rawValue, knownDoctors)

                fieldName == "prescription_date" ->
                    suggestDate(rawValue)

                fieldName == "medicine_name" ||
                    fieldName.startsWith("medicine_name_") ->
                    suggestMedicineName(rawValue)

                // NEVER suggest for clinical fields — return null unconditionally.
                fieldName.startsWith("dose") ||
                    fieldName.startsWith("frequency") ||
                    fieldName == "diagnosis" ||
                    fieldName == "allergy" ||
                    fieldName == "duration" ||
                    fieldName == "instructions" ->
                    null

                else -> null
            }
        }

        // -----------------------------------------------------------------------
        // Field-specific suggestion logic
        // -----------------------------------------------------------------------

        /**
         * Cleans OCR artefacts from a doctor name and fuzzy-matches against known doctors.
         * Returns the best Levenshtein ≤ 2 match from [knownDoctors], or the cleaned raw
         * value if no match exists but cleaning produced a meaningful change.
         */
        private fun suggestDoctorName(
            rawValue: String,
            knownDoctors: List<Doctor>,
        ): String? {
            // Strip common OCR symbol artefacts, keep letters, spaces, dot, apostrophe, hyphen
            val cleaned =
                rawValue
                    .replace(Regex("[0-9]"), "")
                    .replace(Regex("[^a-zA-Z .'\"-]"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()

            if (cleaned.isBlank()) return null

            // Check Levenshtein ≤ 2 match among patient's doctors
            val match = knownDoctors.minByOrNull { levenshtein(cleaned.lowercase(), it.name.lowercase()) }
            if (match != null && levenshtein(cleaned.lowercase(), match.name.lowercase()) <= 2) {
                return match.name
            }

            // Return cleaned version if it differs from raw input and looks like a name
            return if (cleaned != rawValue.trim() && cleaned.length >= 3) cleaned else null
        }

        /**
         * Tries the same 7 date formats used in PostReviewProcessor and returns
         * an ISO-formatted date if any format succeeds and the result differs from input.
         */
        private fun suggestDate(rawValue: String): String? {
            val formats =
                listOf(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                    DateTimeFormatter.ofPattern("d MMM yyyy"),
                    DateTimeFormatter.ofPattern("dd MMM yyyy"),
                    DateTimeFormatter.ofPattern("MMMM d, yyyy"),
                    DateTimeFormatter.ofPattern("d/M/yyyy"),
                )
            val parsed =
                formats.firstNotNullOfOrNull { fmt ->
                    try {
                        LocalDate.parse(rawValue.trim(), fmt)
                    } catch (_: DateTimeParseException) {
                        null
                    }
                } ?: return null

            val iso = parsed.format(DateTimeFormatter.ISO_LOCAL_DATE)
            return if (iso != rawValue.trim()) iso else null
        }

        /**
         * Strips trailing punctuation and normalises spacing in a medicine name.
         * Returns null if the cleaned value equals the raw input (no suggestion needed).
         */
        private fun suggestMedicineName(rawValue: String): String? {
            val cleaned =
                rawValue
                    .trimEnd('.', ',', ';', ':', '-')
                    .replace(Regex("\\s+"), " ")
                    .trim()
            return if (cleaned != rawValue.trim()) cleaned else null
        }

        // -----------------------------------------------------------------------
        // Levenshtein distance — simple iterative implementation
        // -----------------------------------------------------------------------

        internal fun levenshtein(
            a: String,
            b: String,
        ): Int {
            if (a == b) return 0
            if (a.isEmpty()) return b.length
            if (b.isEmpty()) return a.length
            val dp = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) dp[i][0] = i
            for (j in 0..b.length) dp[0][j] = j
            for (i in 1..a.length) {
                for (j in 1..b.length) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                }
            }
            return dp[a.length][b.length]
        }
    }
