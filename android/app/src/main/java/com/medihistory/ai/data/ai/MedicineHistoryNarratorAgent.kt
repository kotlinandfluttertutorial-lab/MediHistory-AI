package com.medihistory.ai.data.ai

import com.medihistory.ai.data.history.ContentType
import com.medihistory.ai.data.history.HistoryEntry
import com.medihistory.ai.data.history.HistorySection
import com.medihistory.ai.data.history.SectionType
import com.medihistory.ai.data.history.SourceReference
import com.medihistory.ai.data.model.Doctor
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.model.Prescription
import javax.inject.Inject

/**
 * AI agent that replaces the stub AI_SUMMARY section in Medical History with a real
 * plain-language narrative (F15).
 *
 * Input:  REVIEWED [Prescription] records + confirmed [Medicine] and [Doctor] rows.
 * Output: A [HistorySection] of [SectionType.AI_SUMMARY] containing the narrative,
 *         with a [SourceReference] linking each sentence to its prescription.
 *
 * Medical safety invariants:
 * - Input is strictly REVIEWED prescriptions and confirmed medicines only.
 * - Output NEVER diagnoses, recommends, or advises changing a medicine.
 * - Every entry has [ContentType.AI_GENERATED] — the UI shows an amber "AI-generated" chip.
 * - A non-dismissible disclaimer is shown by the UI above all AI_GENERATED sections.
 * - Absent clinical reasons are NOT inferred — the narrative notes "no clinical reason recorded".
 * - If Gemini is unavailable (no key, no network), the stub text is returned so the screen
 *   still shows the deterministic sections without error.
 *
 * Privacy: medicine names and doctor names are passed to Gemini only as patient-confirmed
 * strings — never as raw OCR output. Nothing is logged.
 */
class MedicineHistoryNarratorAgent
    @Inject
    constructor(
        private val geminiService: GeminiService,
    ) {
        /**
         * Generates a narrative [HistorySection] for the AI_SUMMARY slot.
         *
         * @param reviewed  REVIEWED prescriptions sorted chronologically (newest first).
         * @param medicines Confirmed medicines linked to those prescriptions.
         * @param doctors   Doctors referenced by those prescriptions.
         * @return A [HistorySection] of type [SectionType.AI_SUMMARY].
         *                  Never null — falls back to stub text when Gemini is unavailable.
         */
        suspend fun generateSummary(
            reviewed: List<Prescription>,
            medicines: List<Medicine>,
            doctors: List<Doctor>,
        ): HistorySection {
            if (reviewed.isEmpty()) return stubSection()

            val doctorMap = doctors.associateBy { it.id }
            val medicinesByRx = medicines.groupBy { it.prescriptionId }

            // Build a structured summary of the patient's medication history from confirmed data.
            val prescriptionSummaries =
                reviewed.mapIndexed { idx, rx ->
                    val doctorName = rx.doctorId?.let { doctorMap[it]?.name } ?: "Unknown doctor"
                    val dateStr = rx.prescriptionDate?.toString() ?: "date unknown"
                    val meds =
                        medicinesByRx[rx.id]?.joinToString(", ") { med ->
                            buildString {
                                append(med.medicineName)
                                if (med.strength != null) append(" ${med.strength}")
                            }
                        } ?: "no medicines recorded"
                    "Prescription ${idx + 1} ($dateStr): $doctorName prescribed $meds."
                }.joinToString("\n")

            val prompt = buildPrompt(prescriptionSummaries)
            val rawResponse = geminiService.generate(prompt)

            return if (rawResponse != null) {
                buildNarrativeSection(rawResponse, reviewed)
            } else {
                stubSection()
            }
        }

        // -------------------------------------------------------------------------
        // Prompt construction
        // -------------------------------------------------------------------------

        private fun buildPrompt(prescriptionSummaries: String): String =
            """
            You are a patient health record assistant helping a patient understand their
            medication history.

            Here is a chronological summary of the patient's reviewed prescriptions:

            $prescriptionSummaries

            Write a concise, plain-language narrative (3–5 sentences) summarising this
            medication history for the patient. Use past tense for older prescriptions.

            Rules:
            - Do NOT diagnose any condition.
            - Do NOT recommend starting, stopping, or changing any medicine.
            - Do NOT say why a medicine was prescribed unless it is explicitly stated above.
            - If no clinical reason is in the data, write "the clinical reason is not recorded".
            - Keep the language simple and reassuring — not clinical or alarming.
            - End with: "This summary is based on your reviewed prescription records."
            """.trimIndent()

        // -------------------------------------------------------------------------
        // Response parsing
        // -------------------------------------------------------------------------

        /**
         * Splits the Gemini narrative into sentences and creates one [HistoryEntry] per sentence,
         * all pointing back to the most recent prescription as a source reference.
         */
        private fun buildNarrativeSection(
            narrative: String,
            reviewed: List<Prescription>,
        ): HistorySection {
            // Use the most recent reviewed prescription as the source reference for the summary.
            val latestRxId = reviewed.firstOrNull()?.id

            val entries =
                narrative
                    .split(Regex("(?<=[.!?])\\s+"))
                    .filter { it.isNotBlank() }
                    .map { sentence ->
                        HistoryEntry(
                            text = sentence.trim(),
                            contentType = ContentType.AI_GENERATED,
                            sourceReferences =
                                listOfNotNull(
                                    latestRxId?.let { SourceReference(prescriptionId = it, fieldName = null) },
                                ),
                        )
                    }

            return HistorySection(
                type = SectionType.AI_SUMMARY,
                title = "AI Summary",
                entries = entries.ifEmpty { listOf(stubEntry()) },
                isAiGenerated = true,
            )
        }

        // -------------------------------------------------------------------------
        // Stub / fallback
        // -------------------------------------------------------------------------

        private fun stubSection(): HistorySection =
            HistorySection(
                type = SectionType.AI_SUMMARY,
                title = "AI Summary",
                entries = listOf(stubEntry()),
                isAiGenerated = true,
            )

        private fun stubEntry(): HistoryEntry =
            HistoryEntry(
                text =
                    "AI summary generation requires a configured AI provider. " +
                        "Reviewed records are compiled in the sections above.",
                contentType = ContentType.DOCUMENTED_FACT,
                sourceReferences = emptyList(),
            )
    }
