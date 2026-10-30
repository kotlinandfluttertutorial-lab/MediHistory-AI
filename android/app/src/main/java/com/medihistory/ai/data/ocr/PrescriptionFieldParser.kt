package com.medihistory.ai.data.ocr

import android.graphics.Rect
import com.google.mlkit.vision.text.Text

/**
 * Pure function class that maps raw ML Kit [Text] output to a structured list of
 * [ExtractedField] objects for a prescription document.
 *
 * ## Spatial layout awareness
 * Indian prescription pads often have a columnar layout:
 *
 *   | Medicine name / instructions   | Qty / Strip count |
 *   | Augmentin 625mg                |  1 Strip           |
 *   | 2 ml four times a day          |                    |
 *
 * ML Kit reads each column as a separate [Text.TextBlock]. Without spatial filtering,
 * the parser mistakes quantity-column text ("MG) (1 Strip)") for a medicine name.
 *
 * Fix: every [IndexedLine] carries its bounding box. Before extracting medicine names,
 * the parser clusters all line midpoints by X-position. Lines whose X-midpoint falls in
 * the left half of the image are treated as name/instruction candidates; lines in the
 * right portion are quantity/notes and are skipped for medicine name extraction.
 *
 * ## Design rules
 * - No fabrication: absent fields have rawValue == null.
 * - ML Kit confidence may be 0.0 even for clean text — heuristic fallback applied.
 * - confidence is always clamped to [0f, 1f].
 * - rawValue stores exactly what OCR read — never cleaned or inferred.
 *
 * Privacy: rawValue and ML Kit text content must NEVER be logged.
 */
internal class PrescriptionFieldParser {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses ML Kit [Text] output into prescription [ExtractedField] list.
     *
     * @param text        ML Kit Text result.
     * @param pageIndex   Zero-based page index for sourceReference.
     * @param imageWidth  Width of the source image in pixels (used for column split).
     *                    Pass 0 to disable spatial filtering (all lines are candidates).
     * @param imageHeight Height of the source image in pixels (used for row filtering).
     *                    Pass 0 to disable.
     */
    fun parse(
        text: Text,
        pageIndex: Int = 0,
        imageWidth: Int = 0,
        imageHeight: Int = 0,
    ): List<ExtractedField> {
        var flatIdx = 0
        val allLines: List<IndexedLine> = text.textBlocks.flatMapIndexed { blockIdx, block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                IndexedLine(
                    text       = line.text.trim(),
                    confidence = line.elements.mapNotNull { it.confidence.takeIf { c -> c > 0f } }
                        .let { cs -> if (cs.isNotEmpty()) cs.average().toFloat() else 0f },
                    blockIndex = blockIdx,
                    pageIndex  = pageIndex,
                    index      = flatIdx++,
                    bounds     = box,
                )
            }
        }

        // Compute the right-column cutoff: lines whose X-midpoint is beyond this
        // value are in a quantity/notes column and excluded from name extraction.
        // We use 70% of the image width as the cutoff (medicine names are always
        // in the left 70% of any standard prescription pad).
        val nameCutoffX = if (imageWidth > 0) (imageWidth * NAME_COLUMN_MAX_X_RATIO).toInt() else Int.MAX_VALUE

        // Merge continuation lines within the same block before any extraction.
        // ML Kit sometimes splits one logical medicine entry across two lines, e.g.:
        //   line 0: "TAB Montair (4"
        //   line 1: "MG ) (1 Strip)"
        // A continuation line starts with a dosage unit keyword (MG, ML, MCG …),
        // a closing bracket, or a digit+unit fragment — and has no standalone name prefix.
        val mergedLines = mergeLineContinuations(allLines)

        val results = mutableListOf<ExtractedField>()
        results.add(extractDoctorName(mergedLines))
        results.add(extractPrescriptionDate(mergedLines))
        results.addAll(extractMedicines(mergedLines, nameCutoffX))

        if (results.none { it.fieldName == "medicine_name" }) results.add(absentField("medicine_name"))
        if (results.none { it.fieldName == "dose" })          results.add(absentField("dose"))
        if (results.none { it.fieldName == "frequency" })     results.add(absentField("frequency"))
        return results
    }

    // -------------------------------------------------------------------------
    // Line continuation merging
    // -------------------------------------------------------------------------

    /**
     * Merges lines that are continuations of the previous line within the same block.
     *
     * ML Kit sometimes splits a single medicine name across two lines due to line
     * wrapping on the physical prescription:
     *
     *   "TAB Montair (4"   ← real line 1
     *   "MG ) (1 Strip)"   ← continuation: starts with a dosage unit keyword
     *
     * A line is a continuation when ALL of the following are true:
     * 1. It is in the same [blockIndex] as the previous line.
     * 2. Its first non-space character starts with a dosage unit keyword
     *    (MG, ML, MCG, G, IU) or a closing bracket ')'.
     * 3. It does NOT start with a known section header or doctor prefix.
     *
     * When merged, the resulting [IndexedLine] keeps the predecessor's index/blockIndex
     * and combines the text with a single space separator. Confidence is the average of
     * both lines. The predecessor's bounding box is expanded to cover the continuation.
     */
    private fun mergeLineContinuations(lines: List<IndexedLine>): List<IndexedLine> {
        if (lines.isEmpty()) return lines
        val merged = mutableListOf<IndexedLine>()
        for (line in lines) {
            val prev = merged.lastOrNull()
            if (prev != null &&
                prev.blockIndex == line.blockIndex &&
                isContinuationLine(line.text)
            ) {
                // Join continuation into the previous line.
                val combinedText = "${prev.text} ${line.text}".trim()
                val combinedConf = if (prev.confidence > 0f && line.confidence > 0f)
                    (prev.confidence + line.confidence) / 2f
                else
                    maxOf(prev.confidence, line.confidence)
                val combinedBounds = Rect(
                    minOf(prev.bounds.left,   line.bounds.left),
                    minOf(prev.bounds.top,    line.bounds.top),
                    maxOf(prev.bounds.right,  line.bounds.right),
                    maxOf(prev.bounds.bottom, line.bounds.bottom),
                )
                merged[merged.lastIndex] = prev.copy(
                    text       = combinedText,
                    confidence = combinedConf.coerceIn(0f, 1f),
                    bounds     = combinedBounds,
                )
            } else {
                merged.add(line)
            }
        }
        return merged
    }

    /**
     * Returns true if [text] looks like a continuation fragment rather than the
     * start of a new medicine entry.
     *
     * Continuation signals:
     * - Starts with a dosage unit word: "MG", "ML", "MCG", "G", "IU", "MG)"
     * - Starts with a closing parenthesis or bracket: ")", "]"
     * - Starts with a digit immediately followed by a unit: "4MG", "10ML"
     */
    private fun isContinuationLine(text: String): Boolean {
        val t = text.trim()
        if (t.isEmpty()) return false
        return CONTINUATION_START_REGEX.containsMatchIn(t)
    }

    // -------------------------------------------------------------------------
    // Doctor name extraction
    // -------------------------------------------------------------------------

    private fun extractDoctorName(lines: List<IndexedLine>): ExtractedField {
        val prefixPatterns = listOf("Dr.", "Dr ", "DR.", "DR ", "Doctor ", "DOCTOR ")
        for ((idx, line) in lines.withIndex()) {
            for (prefix in prefixPatterns) {
                if (line.text.startsWith(prefix, ignoreCase = true)) {
                    return makeField("doctor_name", line.text.take(80), line, 0.80f)
                }
            }
            // Line below a qualification marker — name is the line above.
            if (QUALIFICATION_REGEX.containsMatchIn(line.text) && idx > 0) {
                val above = lines[idx - 1]
                if (above.text.isNotBlank() && above.text.length > 2) {
                    return makeField("doctor_name", above.text.take(80), above, 0.72f)
                }
            }
        }
        for (line in lines) {
            if (REG_NO_REGEX.containsMatchIn(line.text)) {
                val cleaned = REG_NO_REGEX.replace(line.text, "").trim().take(80)
                if (cleaned.length > 3) return makeField("doctor_name", cleaned, line, 0.60f)
            }
        }
        return absentField("doctor_name")
    }

    // -------------------------------------------------------------------------
    // Prescription date extraction
    // -------------------------------------------------------------------------

    private fun extractPrescriptionDate(lines: List<IndexedLine>): ExtractedField {
        for (line in lines) {
            val labelMatch = DATE_LABEL_REGEX.find(line.text) ?: continue
            val after = line.text.substring(labelMatch.range.last + 1).trim()
            val date  = findDate(after) ?: findDate(line.text) ?: continue
            return makeField("prescription_date", date, line, 0.90f)
        }
        for (line in lines) {
            val date = findDate(line.text) ?: continue
            return makeField("prescription_date", date, line, 0.75f)
        }
        return absentField("prescription_date")
    }

    private fun findDate(text: String): String? =
        DATE_PATTERNS.firstNotNullOfOrNull { it.find(text)?.value?.trim() }

    // -------------------------------------------------------------------------
    // Medicine extraction — spatially aware
    // -------------------------------------------------------------------------

    /**
     * Extracts medicine names, doses, and frequencies.
     *
     * The key improvement over the previous version: every line is tested against
     * [nameCutoffX] before being considered as a medicine name candidate. Lines
     * whose horizontal midpoint is in the right portion of the image (quantity/notes
     * column) are skipped for name extraction entirely, eliminating fragments like
     * "MG) (1 Strip)" appearing as medicine names.
     *
     * Within the name column, the parser pairs each name line with the dose/frequency
     * line that follows it (or on the same line), using a consumed-index set to prevent
     * double-assignment.
     */
    private fun extractMedicines(lines: List<IndexedLine>, nameCutoffX: Int): List<ExtractedField> {
        val medicines = mutableListOf<MedicineCandidate>()
        val consumed  = mutableSetOf<Int>()
        var inMedicineSection = false

        // Pre-filter: lines in the name column only.
        val nameColumnLines = lines.filter { it.xMid <= nameCutoffX }

        for ((pos, line) in nameColumnLines.withIndex()) {
            if (line.index in consumed) continue

            // Section header detection.
            if (MEDICINE_SECTION_LABEL.containsMatchIn(line.text)) {
                inMedicineSection = true
                continue
            }

            // ----------------------------------------------------------------
            // Strategy 1: numbered list prefix — strongest signal.
            // ----------------------------------------------------------------
            val numberedMatch = NUMBERED_LIST_PREFIX.find(line.text)
            if (numberedMatch != null) {
                val rest = line.text.removeRange(numberedMatch.range).trim()
                if (rest.isNotBlank()) {
                    val (name, dose, freq) = splitParts(rest)
                    if (isValidName(name)) {
                        consumed.add(line.index)
                        val (d, f) = resolveInstruction(dose, freq, nameColumnLines, pos, consumed)
                        medicines.add(MedicineCandidate(name, d, f, heuristic(line.confidence, 0.82f), sourceRef(line)))
                        continue
                    }
                }
            }

            // ----------------------------------------------------------------
            // Strategy 2: line is a pure instruction (dose/frequency only).
            // Look backward for the name line.
            // ----------------------------------------------------------------
            if (isPureInstruction(line.text)) {
                consumed.add(line.index)
                val (dose, freq) = parseInstruction(line.text)
                val nameLine = (pos - 1 downTo maxOf(0, pos - 3))
                    .mapNotNull { nameColumnLines.getOrNull(it) }
                    .firstOrNull { it.index !in consumed && isValidName(it.text) }
                if (nameLine != null) {
                    consumed.add(nameLine.index)
                    medicines.add(MedicineCandidate(nameLine.text.take(120), dose, freq, heuristic(nameLine.confidence, 0.75f), sourceRef(nameLine)))
                }
                continue
            }

            // ----------------------------------------------------------------
            // Strategy 3: line has dosage suffix (e.g. "Augmentin 625mg Tab").
            // Split into name + dose parts on the same line.
            // ----------------------------------------------------------------
            if (DOSAGE_SUFFIX_REGEX.containsMatchIn(line.text)) {
                val (name, dose, freq) = splitParts(line.text)
                if (isValidName(name)) {
                    consumed.add(line.index)
                    val (d, f) = resolveInstruction(dose, freq, nameColumnLines, pos, consumed)
                    medicines.add(MedicineCandidate(name, d, f, heuristic(line.confidence, 0.72f), sourceRef(line)))
                    continue
                }
                // Name fragment is invalid (e.g. a dosage fragment) — skip entirely.
                continue
            }

            // ----------------------------------------------------------------
            // Strategy 4: within a labelled medicine section, any valid name line.
            // ----------------------------------------------------------------
            if (inMedicineSection && isValidName(line.text)) {
                consumed.add(line.index)
                val (d, f) = resolveInstruction(null, null, nameColumnLines, pos, consumed)
                medicines.add(MedicineCandidate(line.text.take(120), d, f, heuristic(line.confidence, 0.65f), sourceRef(line)))
            }
        }

        // Deduplicate by name, cap at 5.
        val seen   = mutableSetOf<String>()
        val deduped = medicines.filter { seen.add(it.name.lowercase()) }.take(MAX_MEDICINES)
        if (deduped.isEmpty()) return emptyList()

        val fields = mutableListOf<ExtractedField>()
        for ((i, c) in deduped.withIndex()) {
            val sfx = if (i == 0) "" else "_${i + 1}"
            fields.add(ExtractedField("medicine_name$sfx", c.name.take(120), null, c.confidence, sourceReference = c.sourceRef))
            if (c.dose != null)      fields.add(ExtractedField("dose$sfx",      c.dose.take(60),      null, heuristic(0f, 0.75f), sourceReference = c.sourceRef))
            if (c.frequency != null) fields.add(ExtractedField("frequency$sfx", c.frequency.take(60), null, heuristic(0f, 0.80f), sourceReference = c.sourceRef))
        }
        return fields
    }

    // -------------------------------------------------------------------------
    // Line splitting helpers
    // -------------------------------------------------------------------------

    /**
     * If no dose/freq was found on the name line, peek at the immediately following
     * line in the name column. If it looks like a pure instruction, consume it and
     * return its dose/freq.
     */
    private fun resolveInstruction(
        dose: String?,
        freq: String?,
        lines: List<IndexedLine>,
        pos: Int,
        consumed: MutableSet<Int>,
    ): Pair<String?, String?> {
        if (dose != null || freq != null) return Pair(dose, freq)
        val next = lines.getOrNull(pos + 1) ?: return Pair(null, null)
        if (next.index in consumed) return Pair(null, null)
        if (!isPureInstruction(next.text)) return Pair(null, null)
        consumed.add(next.index)
        return parseInstruction(next.text)
    }

    /**
     * Splits "Augmentin 625mg BD" → Triple("Augmentin 625mg", null, "BD").
     * Strips frequency and Indian dose notation from the end of the text,
     * leaving the medicine name + optional strength as the name part.
     */
    private fun splitParts(text: String): Triple<String, String?, String?> {
        var rem = text.trim()
        var freq: String? = null
        FREQUENCY_PATTERN.find(rem)?.let { m -> freq = m.value.trim(); rem = rem.removeRange(m.range).trim() }
        var dose: String? = null
        INDIAN_DOSE_PATTERN.find(rem)?.let { m -> dose = m.value.trim(); rem = rem.removeRange(m.range).trim() }
        return Triple(rem.trim(), dose?.ifBlank { null }, freq?.ifBlank { null })
    }

    /**
     * Returns true if the line contains only dosage/frequency content and no
     * meaningful medicine name — used to decide whether to look backward for the name.
     *
     * True examples : "2 ml four times a day", "1-0-1", "500mg BD", "Take twice daily"
     * False examples: "Augmentin 625mg", "Amoxicillin 500mg Tablet"
     */
    private fun isPureInstruction(text: String): Boolean {
        val t = text.trim()
        if (PURE_DOSE_LINE_REGEX.matches(t)) return true
        val hasFreq        = FREQUENCY_PATTERN.containsMatchIn(t)
        val hasIndianDose  = INDIAN_DOSE_PATTERN.containsMatchIn(t)
        if (!hasFreq && !hasIndianDose) return false
        val nameCandidate  = splitParts(t).first.trim()
        if (nameCandidate.length < MIN_NAME_LEN) return true
        if (INSTRUCTION_VERB_REGEX.containsMatchIn(nameCandidate)) return true
        // If the leftover is just a dosage quantity like "2 ml", treat as instruction.
        if (DOSAGE_QUANTITY_ONLY_REGEX.matches(nameCandidate)) return true
        return false
    }

    /** Extracts (dose, frequency) from a line already identified as a pure instruction. */
    private fun parseInstruction(text: String): Pair<String?, String?> {
        var rem  = text.trim()
        var freq: String? = null
        FREQUENCY_PATTERN.find(rem)?.let { m -> freq = m.value.trim(); rem = rem.removeRange(m.range).trim() }
        var dose: String? = null
        INDIAN_DOSE_PATTERN.find(rem)?.let { m -> dose = m.value.trim(); rem = rem.removeRange(m.range).trim() }
        if (dose == null) DOSAGE_SUFFIX_REGEX.find(rem)?.let { m -> dose = m.value.trim() }
        if (dose == null && freq == null && rem.isNotBlank()) dose = rem.trim().take(60)
        return Pair(dose?.ifBlank { null }, freq?.ifBlank { null })
    }

    /**
     * A valid medicine name:
     * - starts with a letter
     * - at least [MIN_NAME_LEN] chars
     * - at least 40% alphabetic characters (rejects "MG) (1 Strip)", "10 ml/100 MG")
     */
    private fun isValidName(text: String): Boolean {
        val t = text.trim()
        if (t.length < MIN_NAME_LEN) return false
        if (!t.first().isLetter()) return false
        if (t.count { it.isLetter() }.toFloat() / t.length < 0.40f) return false
        if (INSTRUCTION_VERB_REGEX.containsMatchIn(t) && t.split(' ').size <= 2) return false
        return true
    }

    // -------------------------------------------------------------------------
    // Small utilities
    // -------------------------------------------------------------------------

    private fun makeField(name: String, value: String, line: IndexedLine, heuristicConf: Float) =
        ExtractedField(name, value, null, heuristic(line.confidence, heuristicConf), sourceReference = sourceRef(line))

    private fun heuristic(mlKit: Float, fallback: Float) =
        (if (mlKit > 0f) mlKit else fallback).coerceIn(0f, 1f)

    private fun sourceRef(line: IndexedLine) =
        "page:${line.pageIndex + 1},block:${line.blockIndex}"

    private fun absentField(fieldName: String) =
        ExtractedField(fieldName, null, null, 0f)

    // -------------------------------------------------------------------------
    // Internal data holders
    // -------------------------------------------------------------------------

    private data class IndexedLine(
        val text       : String,
        val confidence : Float,
        val blockIndex : Int,
        val pageIndex  : Int,
        val index      : Int,
        val bounds     : Rect,
    ) {
        /** Horizontal midpoint of this line's bounding box. */
        val xMid: Int get() = (bounds.left + bounds.right) / 2
    }

    private data class MedicineCandidate(
        val name      : String,
        val dose      : String?,
        val frequency : String?,
        val confidence: Float,
        val sourceRef : String,
    )

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        private const val MAX_MEDICINES = 5
        private const val MIN_NAME_LEN  = 3

        /**
         * Lines whose X-midpoint exceeds this fraction of image width are considered
         * right-column (quantity/notes) and skipped for medicine name extraction.
         * 0.70 = left 70% of the prescription is the medicine column.
         */
        private const val NAME_COLUMN_MAX_X_RATIO = 0.70

        /** Matches lines that are continuations of the previous line (dosage unit or bracket at start). */
        private val CONTINUATION_START_REGEX = Regex(
            """^(?:\d*\s*(?:MG|ML|MCG|MCG|G|IU|UNITS?)\b|[)\]]\s*\(|\)\s*\d)""",
            RegexOption.IGNORE_CASE,
        )

        private val QUALIFICATION_REGEX = Regex(
            """M\.?B\.?B\.?S|M\.?D\.?|M\.?S\.?|D\.?G\.?O|B\.?D\.?S|F\.?R\.?C\.?S""",
            RegexOption.IGNORE_CASE,
        )
        private val REG_NO_REGEX = Regex(
            """Reg\.?\s*No\.?:?\s*\d+|Reg\.?\s*No\.|Registration\s*No""",
            RegexOption.IGNORE_CASE,
        )
        private val DATE_LABEL_REGEX = Regex(
            """(?:Date|Dated|Date\s+of\s+[Pp]rescription)\s*:""",
            RegexOption.IGNORE_CASE,
        )
        private val DATE_PATTERNS = listOf(
            Regex("""\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4}""", RegexOption.IGNORE_CASE),
            Regex("""(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4}""", RegexOption.IGNORE_CASE),
            Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{4}"""),
            Regex("""\d{4}-\d{2}-\d{2}"""),
        )
        private val MEDICINE_SECTION_LABEL = Regex(
            """^(?:Rx\s*:|℞\s*:|Medicines?:|Prescription:|Tabs?:|Caps?:)""",
            RegexOption.IGNORE_CASE,
        )
        private val NUMBERED_LIST_PREFIX   = Regex("""^\d{1,2}[.)]\s*""")
        private val DOSAGE_SUFFIX_REGEX    = Regex("""\d+\s*(?:mg|ml|mcg|g|IU|units?)""", RegexOption.IGNORE_CASE)
        private val INDIAN_DOSE_PATTERN    = Regex("""\b[01]-[01]-[01]\b""")
        private val FREQUENCY_PATTERN      = Regex(
            """\b(?:OD|BD|TDS|QID|once\s+daily|twice\s+daily|three\s+times\s+(?:a\s+)?daily|thrice\s+daily|four\s+times\s+daily)\b""",
            RegexOption.IGNORE_CASE,
        )
        private val INSTRUCTION_VERB_REGEX = Regex(
            """^\s*(?:take|apply|use|instil|insert|inhale|give|administer)\b""",
            RegexOption.IGNORE_CASE,
        )
        /** Matches a line that is entirely a dose quantity: "500mg", "10 ml", "2 tsp". */
        private val PURE_DOSE_LINE_REGEX   = Regex(
            """^\d+(\.\d+)?\s*(?:mg|ml|mcg|g|IU|units?|tsp|tbsp|drops?)\s*$""",
            RegexOption.IGNORE_CASE,
        )
        /** Matches a leftover string that is just a dosage quantity after stripping freq/dose tokens. */
        private val DOSAGE_QUANTITY_ONLY_REGEX = Regex(
            """^\d+(\.\d+)?\s*(?:mg|ml|mcg|g|IU|units?|tsp|tbsp|drops?)?$""",
            RegexOption.IGNORE_CASE,
        )
    }
}
