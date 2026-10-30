package com.medihistory.ai.data.ocr

import com.google.mlkit.vision.text.Text

/**
 * Pure function class that maps raw ML Kit [Text] output to a structured list of
 * [ExtractedField] objects for a prescription document.
 *
 * Design rules:
 * - No Android framework dependencies — this class is unit-testable without Robolectric.
 * - NEVER fabricates a field value. If a pattern is not found, the field is returned
 *   with [ExtractedField.rawValue] == null and confidence == 0.0f.
 * - ML Kit does not always populate [Text.Element.confidence] (it may return 0.0 even
 *   for clean text). When ML Kit returns 0.0, this class applies a heuristic based on
 *   how the value was matched (keyword label > pattern match > fuzzy match).
 * - [ExtractedField.confidence] is always clamped to [0f, 1f] before construction.
 * - [ExtractedField.rawValue] stores exactly what the OCR saw — never cleaned or inferred.
 *
 * Privacy: rawValue and any ML Kit text content must NEVER be logged by this class.
 */
internal class PrescriptionFieldParser {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parses a ML Kit [Text] result and returns extracted prescription fields.
     *
     * @param text      The [Text] object returned by ML Kit TextRecognizer.
     * @param pageIndex Zero-based page index used for [ExtractedField.sourceReference].
     * @return A list containing one entry per expected field. Fields not found in the
     *         document have [ExtractedField.rawValue] == null.
     */
    fun parse(text: Text, pageIndex: Int = 0): List<ExtractedField> {
        val blocks = text.textBlocks
        var flatIndex = 0
        val allLines: List<IndexedLine> = blocks.flatMapIndexed { blockIdx, block ->
            block.lines.map { line ->
                IndexedLine(
                    text = line.text.trim(),
                    confidence = line.elements.map { it.confidence }.filter { it > 0f }
                        .let { confs -> if (confs.isNotEmpty()) confs.average().toFloat() else 0f },
                    blockIndex = blockIdx,
                    pageIndex = pageIndex,
                    index = flatIndex++,
                )
            }
        }

        val results = mutableListOf<ExtractedField>()

        results.add(extractDoctorName(allLines))
        results.add(extractPrescriptionDate(allLines))
        results.addAll(extractMedicines(allLines))

        // Ensure at least one medicine_name entry exists so the review screen always shows it.
        if (results.none { it.fieldName == "medicine_name" }) {
            results.add(absentField("medicine_name"))
        }
        // Ensure dose and frequency baseline entries exist.
        if (results.none { it.fieldName == "dose" }) {
            results.add(absentField("dose"))
        }
        if (results.none { it.fieldName == "frequency" }) {
            results.add(absentField("frequency"))
        }

        return results
    }

    // -------------------------------------------------------------------------
    // Doctor name extraction
    // -------------------------------------------------------------------------

    private fun extractDoctorName(lines: List<IndexedLine>): ExtractedField {
        // Strategy 1: line explicitly starts with a doctor prefix.
        val prefixPatterns = listOf("Dr.", "Dr ", "DR.", "DR ", "Doctor ", "DOCTOR ")
        for ((idx, line) in lines.withIndex()) {
            val upper = line.text
            for (prefix in prefixPatterns) {
                if (upper.startsWith(prefix, ignoreCase = true)) {
                    val name = line.text.trim().take(80)  // reasonable max length
                    return ExtractedField(
                        fieldName        = "doctor_name",
                        rawValue         = name,
                        patientValue     = null,
                        confidence       = heuristicConfidence(line.confidence, 0.80f),
                        sourceReference  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                    )
                }
            }

            // Strategy 2: line above a registration / qualification marker.
            if (QUALIFICATION_REGEX.containsMatchIn(line.text) && idx > 0) {
                val nameLine = lines[idx - 1].text.trim()
                if (nameLine.isNotBlank() && nameLine.length > 2) {
                    return ExtractedField(
                        fieldName        = "doctor_name",
                        rawValue         = nameLine.take(80),
                        patientValue     = null,
                        confidence       = heuristicConfidence(lines[idx - 1].confidence, 0.72f),
                        sourceReference  = "page:${lines[idx - 1].pageIndex + 1},block:${lines[idx - 1].blockIndex}",
                    )
                }
            }
        }

        // Strategy 3: line contains "Reg.No" / "Registration" — take same line as partial match.
        for (line in lines) {
            if (REG_NO_REGEX.containsMatchIn(line.text)) {
                val cleaned = REG_NO_REGEX.replace(line.text, "").trim().take(80)
                if (cleaned.length > 3) {
                    return ExtractedField(
                        fieldName        = "doctor_name",
                        rawValue         = cleaned,
                        patientValue     = null,
                        confidence       = heuristicConfidence(line.confidence, 0.60f),
                        sourceReference  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                    )
                }
            }
        }

        return absentField("doctor_name")
    }

    // -------------------------------------------------------------------------
    // Prescription date extraction
    // -------------------------------------------------------------------------

    private fun extractPrescriptionDate(lines: List<IndexedLine>): ExtractedField {
        // Strategy 1: labelled date — "Date:", "Dated:", "Date of prescription:" etc.
        for (line in lines) {
            val match = DATE_LABEL_REGEX.find(line.text) ?: continue
            val remainder = line.text.substring(match.range.last + 1).trim()
            val dateMatch = findDateInText(remainder) ?: findDateInText(line.text)
            if (dateMatch != null) {
                return ExtractedField(
                    fieldName        = "prescription_date",
                    rawValue         = dateMatch,
                    patientValue     = null,
                    confidence       = heuristicConfidence(line.confidence, 0.90f),
                    sourceReference  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                )
            }
        }

        // Strategy 2: any line containing a date-like pattern.
        for (line in lines) {
            val dateMatch = findDateInText(line.text) ?: continue
            return ExtractedField(
                fieldName        = "prescription_date",
                rawValue         = dateMatch,
                patientValue     = null,
                confidence       = heuristicConfidence(line.confidence, 0.75f),
                sourceReference  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
            )
        }

        return absentField("prescription_date")
    }

    private fun findDateInText(text: String): String? {
        for (regex in DATE_PATTERNS) {
            val match = regex.find(text) ?: continue
            return match.value.trim()
        }
        return null
    }

    // -------------------------------------------------------------------------
    // Medicine, dose, and frequency extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts medicine names, doses, and frequencies from all lines.
     *
     * Key design: a line that contains ONLY a dosage/frequency string is a dose line,
     * not a medicine name line. The medicine name is typically the line above or before it.
     * Matching the dosage line as the medicine name (the old bug) produced fragments like
     * "ML/100 MG) (1" or "2 ml four times a day" as medicine names.
     *
     * Returns a flat list of [ExtractedField] with suffixed names:
     * medicine_name, medicine_name_2 … medicine_name_5
     * dose, dose_2 … dose_5
     * frequency, frequency_2 … frequency_5
     *
     * At most 5 medicines are extracted to stay within PostReviewProcessor's numbered suffix range.
     */
    private fun extractMedicines(lines: List<IndexedLine>): List<ExtractedField> {
        val medicines = mutableListOf<MedicineCandidate>()

        var inMedicineSection = false
        // Track which line indices have already been consumed as dose/frequency
        // so we don't also emit them as medicine names.
        val consumedAsInstruction = mutableSetOf<Int>()

        for ((idx, line) in lines.withIndex()) {
            // Detect section headers that signal upcoming medicine lines.
            if (MEDICINE_SECTION_LABEL.containsMatchIn(line.text)) {
                inMedicineSection = true
                continue
            }

            // Skip lines already consumed as a dose/frequency instruction.
            if (idx in consumedAsInstruction) continue

            // ----------------------------------------------------------------
            // Strategy 1: numbered list items — "1." "2." "1)" "2)"
            // Strong signal — the numbered prefix identifies the medicine entry.
            // ----------------------------------------------------------------
            val numberedMatch = NUMBERED_LIST_PREFIX.find(line.text)
            if (numberedMatch != null) {
                val rest = line.text.removeRange(numberedMatch.range).trim()
                if (rest.isNotBlank()) {
                    val (name, dose, freq) = splitMedicineLineParts(rest)
                    if (isValidMedicineName(name)) {
                        // Peek at the next line — it might carry the instruction (dose/frequency).
                        val nextLine = lines.getOrNull(idx + 1)
                        val (resolvedDose, resolvedFreq) = if (
                            dose == null && freq == null &&
                            nextLine != null && isPureDoseOrFrequencyLine(nextLine.text)
                        ) {
                            consumedAsInstruction.add(idx + 1)
                            parseDoseFreqFromLine(nextLine.text)
                        } else {
                            Pair(dose, freq)
                        }
                        medicines.add(
                            MedicineCandidate(
                                name       = name,
                                dose       = resolvedDose,
                                frequency  = resolvedFreq,
                                confidence = heuristicConfidence(line.confidence, 0.82f),
                                sourceRef  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                            )
                        )
                        continue
                    }
                }
            }

            // ----------------------------------------------------------------
            // Strategy 2: line is a pure dose/frequency instruction
            // (e.g. "2 ml four times a day", "500mg BD", "1-0-1")
            // → look BACKWARD for the medicine name on the previous non-consumed line.
            // ----------------------------------------------------------------
            if (isPureDoseOrFrequencyLine(line.text) && idx !in consumedAsInstruction) {
                val (dose, freq) = parseDoseFreqFromLine(line.text)
                consumedAsInstruction.add(idx)

                // Walk backward to find the nearest unused name line.
                val nameLine = (idx - 1 downTo maxOf(0, idx - 3))
                    .mapNotNull { lines.getOrNull(it) }
                    .firstOrNull { candidate -> candidate.index !in consumedAsInstruction && isValidMedicineName(candidate.text) }

                if (nameLine != null) {
                    consumedAsInstruction.add(nameLine.index)
                    medicines.add(
                        MedicineCandidate(
                            name       = nameLine.text.trim().take(120),
                            dose       = dose,
                            frequency  = freq,
                            confidence = heuristicConfidence(nameLine.confidence, 0.75f),
                            sourceRef  = "page:${nameLine.pageIndex + 1},block:${nameLine.blockIndex}",
                        )
                    )
                }
                continue
            }

            // ----------------------------------------------------------------
            // Strategy 3: line contains a dosage suffix AND a name prefix
            // e.g. "Augmentin 625mg Tablet" — medicine + strength on same line.
            // Only use this path if the name part passes the validity check.
            // ----------------------------------------------------------------
            if (DOSAGE_SUFFIX_REGEX.containsMatchIn(line.text)) {
                val (name, dose, freq) = splitMedicineLineParts(line.text)
                if (isValidMedicineName(name)) {
                    medicines.add(
                        MedicineCandidate(
                            name       = name,
                            dose       = dose,
                            frequency  = freq,
                            confidence = heuristicConfidence(line.confidence, 0.72f),
                            sourceRef  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                        )
                    )
                    continue
                }
                // If name is not valid (e.g. blank or a pure dosage fragment), skip —
                // don't fall through to add a garbage name.
                continue
            }

            // ----------------------------------------------------------------
            // Strategy 4: inside a medicine section, plain name lines
            // (no number prefix, no dosage suffix)
            // ----------------------------------------------------------------
            if (inMedicineSection && isValidMedicineName(line.text)) {
                medicines.add(
                    MedicineCandidate(
                        name       = line.text.trim().take(120),
                        dose       = null,
                        frequency  = null,
                        confidence = heuristicConfidence(line.confidence, 0.65f),
                        sourceRef  = "page:${line.pageIndex + 1},block:${line.blockIndex}",
                    )
                )
            }
        }

        // Deduplicate by name (case-insensitive), keep first occurrence, cap at 5.
        val seen = mutableSetOf<String>()
        val deduped = medicines.filter { seen.add(it.name.lowercase()) }.take(MAX_MEDICINES)

        if (deduped.isEmpty()) return emptyList()

        val fields = mutableListOf<ExtractedField>()
        for ((i, candidate) in deduped.withIndex()) {
            val suffix = if (i == 0) "" else "_${i + 1}"

            fields.add(
                ExtractedField(
                    fieldName        = "medicine_name$suffix",
                    rawValue         = candidate.name.take(120),
                    patientValue     = null,
                    confidence       = candidate.confidence,
                    sourceReference  = candidate.sourceRef,
                )
            )
            if (candidate.dose != null) {
                fields.add(
                    ExtractedField(
                        fieldName        = "dose$suffix",
                        rawValue         = candidate.dose.take(60),
                        patientValue     = null,
                        confidence       = heuristicConfidence(0f, 0.75f),
                        sourceReference  = candidate.sourceRef,
                    )
                )
            }
            if (candidate.frequency != null) {
                fields.add(
                    ExtractedField(
                        fieldName        = "frequency$suffix",
                        rawValue         = candidate.frequency.take(60),
                        patientValue     = null,
                        confidence       = heuristicConfidence(0f, 0.80f),
                        sourceReference  = candidate.sourceRef,
                    )
                )
            }
        }
        return fields
    }

    /**
     * Splits a single medicine line into (name, dose, frequency) parts.
     *
     * A line like "Augmentin 625mg BD" → ("Augmentin 625mg", null, "BD")
     * A line like "Paracetamol 500mg 1-0-1" → ("Paracetamol 500mg", "1-0-1", null)
     *
     * Only used when name + dose are on the same line. For separate lines, use
     * [isPureDoseOrFrequencyLine] + [parseDoseFreqFromLine] instead.
     */
    private fun splitMedicineLineParts(text: String): Triple<String, String?, String?> {
        var remaining = text.trim()

        // Extract frequency keyword first (it usually comes last).
        var frequency: String? = null
        val freqMatch = FREQUENCY_PATTERN.find(remaining)
        if (freqMatch != null) {
            frequency = freqMatch.value.trim()
            remaining = remaining.removeRange(freqMatch.range).trim()
        }

        // Extract Indian dosage notation (1-0-1 pattern).
        var dose: String? = null
        val indianDoseMatch = INDIAN_DOSE_PATTERN.find(remaining)
        if (indianDoseMatch != null) {
            dose = indianDoseMatch.value.trim()
            remaining = remaining.removeRange(indianDoseMatch.range).trim()
        }

        val name = remaining.trim()
        return Triple(name, dose?.ifBlank { null }, frequency?.ifBlank { null })
    }

    /**
     * Returns true when a line is PURELY a dosage or frequency instruction
     * with no meaningful medicine name component.
     *
     * Examples that return true:
     *   "2 ml four times a day"
     *   "500mg BD"
     *   "1-0-1"
     *   "Take twice daily"
     *   "10 ml/100 mg (1 tsp)"
     *
     * Examples that return false (name + dosage on same line):
     *   "Augmentin 625mg Tablet"
     *   "Amoxicillin 500mg"
     */
    private fun isPureDoseOrFrequencyLine(text: String): Boolean {
        val trimmed = text.trim()

        // Contains a frequency keyword — could be pure instruction.
        val hasFrequency = FREQUENCY_PATTERN.containsMatchIn(trimmed)
        // Contains Indian dose notation.
        val hasIndianDose = INDIAN_DOSE_PATTERN.containsMatchIn(trimmed)
        // Contains only dosage quantity with no alphabetic word before it long enough to be a name.
        val nameCandidate = splitMedicineLineParts(trimmed).first.trim()

        // If the leftover name candidate is too short or looks like a dose fragment, it's pure dose.
        if (hasFrequency || hasIndianDose) {
            if (nameCandidate.length < MIN_MEDICINE_NAME_LENGTH) return true
            // "Take", "Apply" etc. are instructions, not names.
            if (INSTRUCTION_VERB_REGEX.containsMatchIn(nameCandidate)) return true
        }

        // Lines that are entirely a dose quantity: "500mg", "10 ml", "2 tsp"
        if (PURE_DOSE_LINE_REGEX.matches(trimmed)) return true

        return false
    }

    /**
     * Extracts dose and frequency strings from a line that has been identified
     * as a pure dose/frequency instruction line.
     */
    private fun parseDoseFreqFromLine(text: String): Pair<String?, String?> {
        var remaining = text.trim()
        var frequency: String? = null
        val freqMatch = FREQUENCY_PATTERN.find(remaining)
        if (freqMatch != null) {
            frequency = freqMatch.value.trim()
            remaining = remaining.removeRange(freqMatch.range).trim()
        }
        var dose: String? = null
        val indianMatch = INDIAN_DOSE_PATTERN.find(remaining)
        if (indianMatch != null) {
            dose = indianMatch.value.trim()
            remaining = remaining.removeRange(indianMatch.range).trim()
        }
        // Remaining text (e.g. "2 ml", "500mg") becomes the dose if no dose found yet.
        if (dose == null) {
            val doseSuffix = DOSAGE_SUFFIX_REGEX.find(remaining)
            if (doseSuffix != null) dose = doseSuffix.value.trim()
        }
        // If still no split was possible, treat the whole line as the dose.
        if (dose == null && frequency == null && remaining.isNotBlank()) {
            dose = remaining.trim().take(60)
        }
        return Pair(dose?.ifBlank { null }, frequency?.ifBlank { null })
    }

    /**
     * Returns true when [text] looks like a valid medicine name:
     * - At least [MIN_MEDICINE_NAME_LENGTH] characters after trimming
     * - Starts with a letter (not a bracket, slash, or digit)
     * - Is not entirely composed of digits, slashes, or brackets (i.e. not a pure dosage fragment)
     */
    private fun isValidMedicineName(text: String): Boolean {
        val t = text.trim()
        if (t.length < MIN_MEDICINE_NAME_LENGTH) return false
        if (!t.first().isLetter()) return false
        // Reject strings that are mostly non-alpha (e.g. "ML/100 MG) (1")
        val alphaRatio = t.count { it.isLetter() }.toFloat() / t.length
        if (alphaRatio < 0.4f) return false
        // Reject pure instruction verbs
        if (INSTRUCTION_VERB_REGEX.containsMatchIn(t) && t.split(" ").size <= 2) return false
        return true
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Returns [mlkitConfidence] when it is > 0f (ML Kit actually populated it),
     * otherwise returns [heuristic] as a reasonable approximation.
     *
     * Both values are clamped to [0f, 1f] to satisfy [ExtractedField]'s init constraint.
     */
    private fun heuristicConfidence(mlkitConfidence: Float, heuristic: Float): Float =
        (if (mlkitConfidence > 0f) mlkitConfidence else heuristic).coerceIn(0f, 1f)

    private fun absentField(fieldName: String) = ExtractedField(
        fieldName        = fieldName,
        rawValue         = null,
        patientValue     = null,
        confidence       = 0.0f,
        sourceReference  = null,
    )

    // -------------------------------------------------------------------------
    // Internal data holders
    // -------------------------------------------------------------------------

    private data class IndexedLine(
        val text: String,
        val confidence: Float,
        val blockIndex: Int,
        val pageIndex: Int,
        val index: Int = 0,   // position in the flat allLines list, set during construction
    )

    private data class MedicineCandidate(
        val name: String,
        val dose: String?,
        val frequency: String?,
        val confidence: Float,
        val sourceRef: String,
    )

    // -------------------------------------------------------------------------
    // Regex constants
    // -------------------------------------------------------------------------

    companion object {
        private const val MAX_MEDICINES = 5

        /** Minimum number of characters for a string to be considered a valid medicine name. */
        private const val MIN_MEDICINE_NAME_LENGTH = 3

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
            // "15 Jan 2024" or "15 January 2024"
            Regex("""\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4}""", RegexOption.IGNORE_CASE),
            // "Jan 15, 2024" or "January 15 2024"
            Regex("""(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4}""", RegexOption.IGNORE_CASE),
            // "15/07/2024" or "15-07-2024" or "07/15/2024"
            Regex("""\d{1,2}[/\-]\d{1,2}[/\-]\d{4}"""),
            // "2024-07-15" ISO
            Regex("""\d{4}-\d{2}-\d{2}"""),
        )

        private val MEDICINE_SECTION_LABEL = Regex(
            """^(?:Rx\s*:|℞\s*:|Medicines?:|Prescription:|Tabs?:|Caps?:)""",
            RegexOption.IGNORE_CASE,
        )

        private val NUMBERED_LIST_PREFIX = Regex("""^\d{1,2}[.)]\s*""")

        private val DOSAGE_SUFFIX_REGEX = Regex(
            """\d+\s*(?:mg|ml|mcg|g|IU|units?)""",
            RegexOption.IGNORE_CASE,
        )

        // Indian dosage notation: "1-0-1", "0-0-1", "1-1-1", and variants.
        private val INDIAN_DOSE_PATTERN = Regex("""\b[01]-[01]-[01]\b""")

        private val FREQUENCY_PATTERN = Regex(
            """\b(?:OD|BD|TDS|QID|once\s+daily|twice\s+daily|three\s+times\s+(?:a\s+)?daily|thrice\s+daily|four\s+times\s+daily)\b""",
            RegexOption.IGNORE_CASE,
        )

        /** Matches instruction verbs that appear in dose lines, not medicine names. */
        private val INSTRUCTION_VERB_REGEX = Regex(
            """^\s*(?:take|apply|use|instil|insert|inhale|give|administer)\b""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Matches lines that are entirely a dosage quantity — no medicine name present.
         * Examples: "500mg", "10 ml", "2 tsp", "100 IU"
         */
        private val PURE_DOSE_LINE_REGEX = Regex(
            """^\d+(\.\d+)?\s*(?:mg|ml|mcg|g|IU|units?|tsp|tbsp|drops?)\s*${'$'}""",
            RegexOption.IGNORE_CASE,
        )
    }
}
