package com.medihistory.ai.data.ocr

import android.content.Context
import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.Schema
import com.google.genai.kotlin.types.Type
import com.medihistory.ai.BuildConfig
import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject

/**
 * [OcrService] that uses ML Kit for pixel→text OCR and Gemini 1.5 Flash for
 * structured field extraction.
 *
 * Architecture:
 * 1. [MlKitOcrService.extractRawText] converts the document to a raw text string.
 * 2. The raw text is sent to Gemini 1.5 Flash with a structured JSON schema prompt.
 * 3. The JSON response is parsed into [ExtractedField] list with high confidence.
 * 4. If any step fails, or if the patient has not consented, falls back silently to
 *    [MlKitOcrService.extract] (the full PrescriptionFieldParser path).
 *
 * Consent gate: [SecurePreferences.KEY_GEMINI_OCR_CONSENT] must equal "true".
 * Feature gate: [BuildConfig.ENABLE_GEMINI_OCR] must be true.
 * API key gate: [BuildConfig.GEMINI_API_KEY] must be non-blank.
 *
 * Privacy invariants:
 * - Raw OCR text is NEVER logged.
 * - Gemini response JSON is NEVER logged.
 * - The API key is read from BuildConfig only; never hardcoded.
 * - On any exception, falls back silently — error class only is logged at DEBUG level.
 *
 * Medical-safety:
 * - Absent fields from Gemini (null/missing in JSON) stay null — never fabricated.
 * - All returned fields start as [ReviewState.PENDING_REVIEW].
 * - Confidence is set to [GEMINI_CONFIDENCE] for Gemini-extracted fields (always < 1.0
 *   to ensure patient review is prompted).
 */
internal class GeminiEnhancedOcrService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mlKitService: MlKitOcrService,
    private val securePrefs: SecurePreferencesStore,
) : OcrService {

    override suspend fun extract(
        documentPath: String,
        mimeType: String,
        prescriptionId: String,
    ): OcrResult {
        // Gate 1: feature flag
        if (!BuildConfig.ENABLE_GEMINI_OCR) {
            return mlKitService.extract(documentPath, mimeType, prescriptionId)
        }

        // Gate 2: API key present
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return mlKitService.extract(documentPath, mimeType, prescriptionId)
        }

        // Gate 3: patient consent
        val consent = securePrefs.getString(SecurePreferences.KEY_GEMINI_OCR_CONSENT)
        if (consent != "true") {
            return mlKitService.extract(documentPath, mimeType, prescriptionId)
        }

        return try {
            // Step 1: pixel → text (on-device, ML Kit)
            val rawText = mlKitService.extractRawText(documentPath, mimeType)
            if (rawText.isBlank()) {
                return mlKitService.extract(documentPath, mimeType, prescriptionId)
            }

            // Step 2: text → structured fields (Gemini 1.5 Flash)
            val geminiFields = callGemini(apiKey, rawText)
            if (geminiFields.isEmpty()) {
                return mlKitService.extract(documentPath, mimeType, prescriptionId)
            }

            val overallConfidence = geminiFields
                .filter { it.rawValue != null }
                .map { it.confidence }
                .let { cs -> if (cs.isNotEmpty()) cs.average().toFloat() else 0f }

            OcrResult(
                prescriptionId    = prescriptionId,
                fields            = geminiFields,
                overallConfidence = overallConfidence,
            )
        } catch (e: Exception) {
            // Fallback: never surface Gemini errors to the patient as a failure.
            // Log error class only — never message content (may contain prescription data).
            android.util.Log.d("GeminiOcr", "Gemini extraction failed (${e.javaClass.simpleName}), using ML Kit fallback")
            mlKitService.extract(documentPath, mimeType, prescriptionId)
        }
    }

    // -------------------------------------------------------------------------
    // Gemini call
    // -------------------------------------------------------------------------

    internal suspend fun callGemini(apiKey: String, rawText: String): List<ExtractedField> {
        val prompt = buildPrompt(rawText)

        val config = GenerateContentConfig(
            systemInstruction = Content.fromText(SYSTEM_INSTRUCTION),
            responseMimeType  = "application/json",
            responseSchema    = PRESCRIPTION_SCHEMA,
            temperature       = 0.0,
            maxOutputTokens   = 1024,
        )

        val jsonText: String = Client(apiKey = apiKey).use { client ->
            client.models.generateContent(
                model  = MODEL_ID,
                text   = prompt,
                config = config,
            ).text ?: return emptyList()
        }

        return parseGeminiJson(jsonText)
    }

    // -------------------------------------------------------------------------
    // Prompt construction
    // -------------------------------------------------------------------------

    private fun buildPrompt(rawText: String): String =
        """
        Extract structured fields from the following Indian prescription OCR text.
        Return only the JSON object — no explanation, no markdown.
        If a field is not present in the text, set its value to null.
        Never infer or fabricate a value that is not explicitly present.

        OCR text:
        $rawText
        """.trimIndent()

    // -------------------------------------------------------------------------
    // JSON → ExtractedField list
    // -------------------------------------------------------------------------

    /**
     * Parses Gemini's JSON response into [ExtractedField] list.
     *
     * Medical-safety: any field with a null or blank JSON value is recorded with
     * rawValue == null — never replaced with an inferred value.
     * Privacy: the JSON string is never logged.
     */
    internal fun parseGeminiJson(json: String): List<ExtractedField> {
        val obj = try { JSONObject(json) } catch (e: Exception) { return emptyList() }
        val fields = mutableListOf<ExtractedField>()

        fun addScalarField(fieldName: String, jsonKey: String, conf: Float = GEMINI_CONFIDENCE) {
            val value = obj.optString(jsonKey, "").takeIf { it.isNotBlank() }
            fields.add(
                ExtractedField(
                    fieldName       = fieldName,
                    rawValue        = value,
                    patientValue    = null,
                    confidence      = if (value != null) conf else 0f,
                    reviewState     = ReviewState.PENDING_REVIEW,
                    sourceReference = GEMINI_SOURCE_REF,
                )
            )
        }

        addScalarField("doctor_name",       "doctor_name")
        addScalarField("prescription_date", "prescription_date")

        // Medicines: up to MAX_MEDICINES entries
        val medicinesArray = obj.optJSONArray("medicines")
        if (medicinesArray != null) {
            val count = minOf(medicinesArray.length(), MAX_MEDICINES)
            for (i in 0 until count) {
                val medicine = medicinesArray.optJSONObject(i) ?: continue
                val sfx = if (i == 0) "" else "_${i + 1}"

                val name = medicine.optString("name", "").takeIf { it.isNotBlank() }
                fields.add(
                    ExtractedField(
                        fieldName       = "medicine_name$sfx",
                        rawValue        = name,
                        patientValue    = null,
                        confidence      = if (name != null) GEMINI_CONFIDENCE else 0f,
                        reviewState     = ReviewState.PENDING_REVIEW,
                        sourceReference = GEMINI_SOURCE_REF,
                    )
                )

                val dose = medicine.optString("dose", "").takeIf { it.isNotBlank() }
                if (dose != null) {
                    fields.add(
                        ExtractedField(
                            fieldName       = "dose$sfx",
                            rawValue        = dose,
                            patientValue    = null,
                            confidence      = GEMINI_CONFIDENCE,
                            reviewState     = ReviewState.PENDING_REVIEW,
                            sourceReference = GEMINI_SOURCE_REF,
                        )
                    )
                }

                val freq = medicine.optString("frequency", "").takeIf { it.isNotBlank() }
                if (freq != null) {
                    fields.add(
                        ExtractedField(
                            fieldName       = "frequency$sfx",
                            rawValue        = freq,
                            patientValue    = null,
                            confidence      = GEMINI_CONFIDENCE,
                            reviewState     = ReviewState.PENDING_REVIEW,
                            sourceReference = GEMINI_SOURCE_REF,
                        )
                    )
                }
            }
        }

        // Ensure sentinel absent fields for medicine_name, dose, frequency if none extracted.
        if (fields.none { it.fieldName == "medicine_name" }) {
            fields.add(ExtractedField("medicine_name", null, null, 0f, sourceReference = GEMINI_SOURCE_REF))
        }
        if (fields.none { it.fieldName == "dose" }) {
            fields.add(ExtractedField("dose", null, null, 0f, sourceReference = GEMINI_SOURCE_REF))
        }
        if (fields.none { it.fieldName == "frequency" }) {
            fields.add(ExtractedField("frequency", null, null, 0f, sourceReference = GEMINI_SOURCE_REF))
        }

        return fields
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        private const val MODEL_ID           = "gemini-1.5-flash"
        internal const val GEMINI_CONFIDENCE = 0.85f
        private const val MAX_MEDICINES      = 5
        internal const val GEMINI_SOURCE_REF = "gemini:page:1"

        private const val SYSTEM_INSTRUCTION =
            "You are a medical prescription parser. Extract structured fields from OCR text " +
            "of Indian prescriptions. Never guess or infer values that are not explicitly present. " +
            "Return null for any missing field."

        /**
         * JSON schema for Gemini structured output.
         * Matches the fields expected by [PrescriptionFieldParser] / [OcrWorker].
         */
        private val PRESCRIPTION_SCHEMA = Schema(
            type = Type.OBJECT,
            properties = mapOf(
                "doctor_name"       to Schema(type = Type.STRING, nullable = true),
                "prescription_date" to Schema(type = Type.STRING, nullable = true),
                "medicines"         to Schema(
                    type  = Type.ARRAY,
                    items = Schema(
                        type = Type.OBJECT,
                        properties = mapOf(
                            "name"      to Schema(type = Type.STRING, nullable = true),
                            "dose"      to Schema(type = Type.STRING, nullable = true),
                            "frequency" to Schema(type = Type.STRING, nullable = true),
                        ),
                        required = listOf("name"),
                    ),
                ),
            ),
            required = listOf("doctor_name", "prescription_date", "medicines"),
        )
    }
}
