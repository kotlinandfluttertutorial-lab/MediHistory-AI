package com.medihistory.ai.data.ai

import com.medihistory.ai.data.model.DuplicateAlert
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.MedicineStatus
import javax.inject.Inject

/**
 * AI agent that prepares a visit summary for the patient before a doctor appointment (F12).
 *
 * Input:  Confirmed [Medicine] rows, unreviewed [DuplicateAlert] records.
 * Output: [VisitSummary] containing current medicines, alerts to discuss, and
 *         AI-generated suggested questions.
 *
 * Medical safety invariants:
 * - Input is confirmed, patient-reviewed data only — never raw OCR output.
 * - Output NEVER diagnoses, recommends, or advises starting/stopping a medicine.
 * - Suggested questions are framed as "consider asking your doctor" — never directives.
 * - Every output carries an "AI-generated" disclaimer shown in the UI.
 * - If [GeminiService] is unavailable, a safe rule-based fallback is used so the screen
 *   always has useful content.
 *
 * Privacy: medicine names are passed to Gemini only as confirmed patient-reviewed strings.
 * Nothing is logged.
 */
class PrepareForVisitAgent @Inject constructor(
    private val geminiService: GeminiService,
) {

    /**
     * Generates a [VisitSummary] for the upcoming appointment.
     *
     * @param medicines      All confirmed medicines for the patient.
     * @param openAlerts     Unreviewed duplicate alerts.
     * @param doctorName     Optional name of the doctor being visited, for personalisation.
     */
    suspend fun prepare(
        medicines: List<Medicine>,
        openAlerts: List<DuplicateAlert>,
        doctorName: String? = null,
    ): VisitSummary {
        val currentMedicines = medicines.filter { it.status != MedicineStatus.PAST }

        // Generate AI suggested questions (falls back to rule-based if Gemini unavailable).
        val suggestedQuestions = generateSuggestedQuestions(
            medicines     = currentMedicines,
            openAlerts    = openAlerts,
            doctorName    = doctorName,
        )

        return VisitSummary(
            currentMedicines   = currentMedicines,
            openAlerts         = openAlerts,
            suggestedQuestions = suggestedQuestions,
            isAiFallback       = suggestedQuestions.all { it.isFallback },
        )
    }

    // -------------------------------------------------------------------------
    // Question generation
    // -------------------------------------------------------------------------

    private suspend fun generateSuggestedQuestions(
        medicines: List<Medicine>,
        openAlerts: List<DuplicateAlert>,
        doctorName: String?,
    ): List<SuggestedQuestion> {
        if (medicines.isEmpty() && openAlerts.isEmpty()) return emptyList()

        val medicineList = medicines
            .take(8)   // cap to avoid oversized prompts
            .joinToString("\n") { med ->
                val parts = mutableListOf(med.medicineName)
                if (med.strength != null) parts += med.strength
                "- ${parts.joinToString(" ")}"
            }

        val alertList = openAlerts
            .take(3)
            .joinToString("\n") { alert ->
                "- Potential duplicate: ${alert.matchingIngredient} found in two medicines."
            }

        val doctorPart = if (doctorName != null) "You are visiting $doctorName." else ""

        val prompt = """
            You are a patient health assistant helping a patient prepare for a doctor appointment.
            $doctorPart

            Current medicines:
            $medicineList

            ${if (openAlerts.isNotEmpty()) "Potential duplicate ingredient alerts:\n$alertList" else ""}

            Write 3–5 plain-language questions the patient should consider asking their doctor.
            Rules:
            - Frame every question as something the patient "might want to ask" — never a directive.
            - Do NOT say any medicine is safe, unsafe, or should be stopped.
            - Do NOT diagnose or recommend treatment.
            - Keep each question to one sentence.
            - Start each question with a dash (-).
        """.trimIndent()

        val response = geminiService.generate(prompt)

        return if (response != null) {
            parseQuestions(response)
        } else {
            ruleBasedFallbackQuestions(medicines, openAlerts)
        }
    }

    private fun parseQuestions(response: String): List<SuggestedQuestion> =
        response.lines()
            .map { it.trimStart('-', ' ').trim() }
            .filter { it.isNotBlank() && it.length > 10 }
            .take(5)
            .map { SuggestedQuestion(text = it, isFallback = false) }

    /** Rule-based fallback used when Gemini is unavailable. */
    private fun ruleBasedFallbackQuestions(
        medicines: List<Medicine>,
        openAlerts: List<DuplicateAlert>,
    ): List<SuggestedQuestion> {
        val questions = mutableListOf<SuggestedQuestion>()

        if (medicines.isNotEmpty()) {
            questions += SuggestedQuestion(
                text = "Are all ${medicines.size} of my current medicines still needed?",
                isFallback = true,
            )
        }
        if (openAlerts.isNotEmpty()) {
            questions += SuggestedQuestion(
                text = "I have ${openAlerts.size} potential duplicate ingredient alert(s) — could you review them with me?",
                isFallback = true,
            )
        }
        questions += SuggestedQuestion(
            text = "Should I make any changes to my current medicines before the next appointment?",
            isFallback = true,
        )

        return questions
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data classes
// ─────────────────────────────────────────────────────────────────────────────

/**
 * The full pre-visit summary for a patient (F12 output).
 *
 * @param currentMedicines   Medicines with [MedicineStatus] != PAST.
 * @param openAlerts         Unreviewed duplicate alerts to discuss with the doctor.
 * @param suggestedQuestions AI-generated or rule-based suggested questions.
 * @param isAiFallback       True when all questions came from the rule-based fallback.
 */
data class VisitSummary(
    val currentMedicines: List<Medicine>,
    val openAlerts: List<DuplicateAlert>,
    val suggestedQuestions: List<SuggestedQuestion>,
    val isAiFallback: Boolean = false,
)

/**
 * A single suggested question for the upcoming doctor visit.
 *
 * @param text       The question text. Always framed as "consider asking" — never a directive.
 * @param isFallback True when generated by the rule-based fallback (Gemini unavailable).
 */
data class SuggestedQuestion(
    val text: String,
    val isFallback: Boolean = false,
)
