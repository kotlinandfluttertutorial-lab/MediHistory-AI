package com.medihistory.ai.data.ai

import com.medihistory.ai.data.model.ExtractedFieldEntity
import com.medihistory.ai.data.model.Medicine
import com.medihistory.ai.data.ocr.ReviewState
import javax.inject.Inject

/**
 * AI agent that generates a plain-language explanation of a reviewed prescription (F11).
 *
 * Input:  REVIEWED [ExtractedFieldEntity] records + confirmed [Medicine] rows for that prescription.
 * Output: A list of [MedicineUnderstanding] entries — one per confirmed medicine name — each
 *         containing a plain-language note.
 *
 * Medical safety invariants:
 * - Only REVIEWED fields with non-null patientValue are used — never raw OCR output.
 * - Output NEVER diagnoses, recommends, or advises starting/stopping a medicine.
 * - Every output carries an "AI-generated — always verify with your doctor" disclaimer.
 * - If [GeminiService.generate] returns null (key not configured, no network, or blocked
 *   response), a safe fallback is returned so the screen still shows useful information.
 * - Medicine names and field values are passed to the Gemini API only as confirmed,
 *   patient-reviewed strings — never as raw document text.
 *
 * Privacy: nothing is logged here — not medicine names, not API responses.
 */
class PrescriptionUnderstandingAgent
    @Inject
    constructor(
        private val geminiService: GeminiService,
    ) {
        /**
         * Generates plain-language notes for all confirmed medicines on a single prescription.
         *
         * @param reviewedFields All REVIEWED extracted fields for the prescription.
         * @param medicines      Confirmed [Medicine] rows linked to this prescription.
         * @return List of [MedicineUnderstanding]; one entry per confirmed medicine.
         *                       Never empty when [medicines] is non-empty.
         */
        suspend fun explain(
            reviewedFields: List<ExtractedFieldEntity>,
            medicines: List<Medicine>,
        ): List<MedicineUnderstanding> {
            if (medicines.isEmpty()) return emptyList()

            // Build a concise, structured prompt from confirmed patient-reviewed data only.
            val medicineList =
                medicines.joinToString("\n") { med ->
                    val parts = mutableListOf(med.medicineName)
                    if (med.strength != null) parts += med.strength
                    if (med.dosageInstructions != null) parts += "(${med.dosageInstructions})"
                    "- ${parts.joinToString(" ")}"
                }

            val doctorField =
                reviewedFields
                    .firstOrNull { it.fieldName == "doctor_name" && it.reviewState == ReviewState.REVIEWED }
                    ?.patientValue

            val dateField =
                reviewedFields
                    .firstOrNull { it.fieldName == "prescription_date" && it.reviewState == ReviewState.REVIEWED }
                    ?.patientValue

            val contextLine =
                buildString {
                    if (doctorField != null) append("Prescribing doctor: $doctorField. ")
                    if (dateField != null) append("Prescription date: $dateField.")
                }.trim()

            val prompt = buildPrompt(medicineList, contextLine)
            val rawResponse = geminiService.generate(prompt)

            return if (rawResponse != null) {
                parseResponse(rawResponse, medicines)
            } else {
                // Graceful fallback — show the confirmed name with no AI note
                medicines.map { med ->
                    MedicineUnderstanding(
                        medicineName = med.medicineName,
                        note = null,
                        isAiFallback = true,
                    )
                }
            }
        }

        // -------------------------------------------------------------------------
        // Prompt construction
        // -------------------------------------------------------------------------

        private fun buildPrompt(
            medicineList: String,
            contextLine: String,
        ): String =
            """
            You are a patient health information assistant. A patient wants to understand
            what medicines are on their prescription.

            ${if (contextLine.isNotEmpty()) contextLine else ""}

            Medicines on the prescription:
            $medicineList

            For each medicine, write ONE short sentence (20–35 words) explaining its common
            general use in plain language a non-medical person can understand.
            Do NOT diagnose, recommend, or advise starting or stopping any medicine.
            Do NOT say any medicine is safe, unsafe, or appropriate for this patient.
            Do NOT include dosage advice or clinical recommendations.
            Start each line with the exact medicine name followed by a colon.
            If you are unsure about a medicine, write: "[medicine name]: Information not available."
            Example format:
            Metformin HCl 500mg: Commonly used to help manage blood sugar levels.
            Metoprolol Succinate 25mg: Often used to support heart rate and blood pressure management.
            """.trimIndent()

        // -------------------------------------------------------------------------
        // Response parsing
        // -------------------------------------------------------------------------

        /**
         * Parses the Gemini response line-by-line, matching each line to a confirmed medicine.
         * Falls back to a null note for any medicine not found in the response.
         */
        private fun parseResponse(
            response: String,
            medicines: List<Medicine>,
        ): List<MedicineUnderstanding> {
            // Build a map: first word(s) of the medicine name → note text
            val lineMap =
                response.lines()
                    .filter { it.contains(':') }
                    .associate { line ->
                        val colonIdx = line.indexOf(':')
                        val key = line.substring(0, colonIdx).trim()
                        val note = line.substring(colonIdx + 1).trim()
                        key to note
                    }

            return medicines.map { med ->
                // Try to find a matching line (case-insensitive prefix match)
                val note =
                    lineMap.entries.firstOrNull { (key, _) ->
                        med.medicineName.startsWith(key, ignoreCase = true) ||
                            key.startsWith(med.medicineName.split(" ").firstOrNull() ?: "", ignoreCase = true)
                    }?.value

                MedicineUnderstanding(
                    medicineName = med.medicineName,
                    note = note?.takeIf { it.isNotBlank() && it != "Information not available." },
                    isAiFallback = note == null,
                )
            }
        }
    }

/**
 * Plain-language explanation for a single medicine on a reviewed prescription.
 *
 * @param medicineName  The confirmed medicine name from the patient-reviewed OCR field.
 * @param note          AI-generated plain-language note, or null when unavailable.
 * @param isAiFallback  True when [note] is null because Gemini was unavailable or returned
 *                      no match — the UI shows "Information not available" in this case.
 */
data class MedicineUnderstanding(
    val medicineName: String,
    val note: String?,
    val isAiFallback: Boolean = false,
)
