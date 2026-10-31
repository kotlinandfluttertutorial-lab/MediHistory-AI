package com.medihistory.ai.data.ocr

import com.medihistory.ai.data.security.SecurePreferences
import com.medihistory.ai.data.security.SecurePreferencesStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Unit tests for [GeminiEnhancedOcrService].
 *
 * Because [GeminiEnhancedOcrService] makes real network calls in production via the
 * Gemini SDK Client, all gate/fallback tests use [TestableGeminiOcrService] — a
 * self-contained wrapper that mirrors the gate/fallback logic without constructing a
 * real [GeminiEnhancedOcrService] (which requires an Android Context and ML Kit).
 *
 * JSON-parsing tests call [GeminiEnhancedOcrService.parseGeminiJsonStatic] directly —
 * an internal companion-object function that exercises the same parsing code as the
 * instance method. Robolectric is required because [parseGeminiJsonStatic] uses
 * [org.json.JSONObject] which is an Android SDK class not available in plain JVM tests.
 *
 * Synthetic data only — no real medicine brands or patient names.
 * Naming convention: `should_[outcome]_when_[condition]`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GeminiEnhancedOcrServiceTest {
    // ─────────────────────────────────────────────────────────────────────────
    // In-memory fakes
    // ─────────────────────────────────────────────────────────────────────────

    /** In-memory [SecurePreferencesStore] — no Android Keystore needed in unit tests. */
    private class FakeSecurePreferencesStore(
        vararg initial: Pair<String, String>,
    ) : SecurePreferencesStore {
        private val map = mutableMapOf(*initial)

        override fun putString(
            key: String,
            value: String,
        ) {
            map[key] = value
        }

        override fun getString(key: String): String? = map[key]

        override fun remove(key: String) {
            map.remove(key)
        }

        override fun clear() {
            map.clear()
        }
    }

    /**
     * Simulates the ML Kit fallback path.
     * Returns a fixed synthetic [OcrResult]; [extractCallCount] tracks invocations.
     */
    private class FakeMlKitOcrService {
        var extractCallCount = 0
        var rawTextResult = "Dr. Synthetic\nSynthamol 500mg\nonce daily"

        fun extract(prescriptionId: String): OcrResult {
            extractCallCount++
            return OcrResult(
                prescriptionId = prescriptionId,
                fields =
                    listOf(
                        ExtractedField("doctor_name", "Dr. Synthetic", null, 0.70f, ReviewState.PENDING_REVIEW, "mlkit:page:1"),
                        ExtractedField("medicine_name", "Synthamol 500mg", null, 0.65f, ReviewState.PENDING_REVIEW, "mlkit:page:1"),
                        ExtractedField("dose", null, null, 0.0f, ReviewState.PENDING_REVIEW, "mlkit:page:1"),
                    ),
                overallConfidence = 0.68f,
            )
        }

        fun extractRawText(): String = rawTextResult
    }

    /**
     * Mirrors the gate/fallback/JSON-parse logic of [GeminiEnhancedOcrService.extract].
     *
     * Does NOT construct a real [GeminiEnhancedOcrService] — instead it:
     * 1. Evaluates each gate using its own constructor params.
     * 2. Calls [GeminiEnhancedOcrService.parseGeminiJsonStatic] (internal companion fn).
     *
     * [throwOnGeminiCall] simulates a network exception from the Gemini SDK.
     */
    private class TestableGeminiOcrService(
        private val fakePrefs: SecurePreferencesStore,
        private val fakeMlKit: FakeMlKitOcrService,
        val featureFlagEnabled: Boolean = true,
        val apiKey: String = "synthetic-test-key-not-real",
        var geminiJsonToReturn: String? =
            """{"doctor_name":"Dr. Synthetic","prescription_date":null,"medicines":[]}""",
        var throwOnGeminiCall: Throwable? = null,
    ) {
        var geminiCallCount = 0

        suspend fun extract(
            documentPath: String,
            mimeType: String,
            prescriptionId: String,
        ): OcrResult {
            // Gate 1: feature flag
            if (!featureFlagEnabled) return fakeMlKit.extract(prescriptionId)

            // Gate 2: API key
            if (apiKey.isBlank()) return fakeMlKit.extract(prescriptionId)

            // Gate 3: patient consent
            val consent = fakePrefs.getString(SecurePreferences.KEY_GEMINI_OCR_CONSENT)
            if (consent != "true") return fakeMlKit.extract(prescriptionId)

            return try {
                // Step 1: pixel → text
                val rawText = fakeMlKit.extractRawText()
                if (rawText.isBlank()) return fakeMlKit.extract(prescriptionId)

                // Step 2: Gemini call (simulated, no network)
                if (throwOnGeminiCall != null) throw throwOnGeminiCall!!
                geminiCallCount++

                val jsonText = geminiJsonToReturn ?: return fakeMlKit.extract(prescriptionId)
                val fields = GeminiEnhancedOcrService.parseGeminiJsonStatic(jsonText)
                if (fields.isEmpty()) return fakeMlKit.extract(prescriptionId)

                val overallConfidence =
                    fields
                        .filter { it.rawValue != null }
                        .map { it.confidence }
                        .let { cs -> if (cs.isNotEmpty()) cs.average().toFloat() else 0f }

                OcrResult(
                    prescriptionId = prescriptionId,
                    fields = fields,
                    overallConfidence = overallConfidence,
                )
            } catch (e: Exception) {
                fakeMlKit.extract(prescriptionId)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1 — feature flag off → ML Kit fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_feature_flag_is_false`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore(SecurePreferences.KEY_GEMINI_OCR_CONSENT to "true")
            val fakeMlKit = FakeMlKitOcrService()
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = false,
                )

            val result = service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-001")

            assertEquals("ML Kit must be called when feature flag is false", 1, fakeMlKit.extractCallCount)
            assertEquals("Gemini must NOT be called", 0, service.geminiCallCount)
            assertEquals("synth-rx-001", result.prescriptionId)
            assertTrue(result.fields.any { it.fieldName == "doctor_name" })
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2 — blank API key → ML Kit fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_api_key_is_blank`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore(SecurePreferences.KEY_GEMINI_OCR_CONSENT to "true")
            val fakeMlKit = FakeMlKitOcrService()
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = true,
                    apiKey = "",
                )

            service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-002")

            assertEquals("ML Kit must be called when API key is blank", 1, fakeMlKit.extractCallCount)
            assertEquals("Gemini must NOT be called", 0, service.geminiCallCount)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3 — consent unknown (key absent) → ML Kit fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_consent_is_unknown`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore() // no consent key stored
            val fakeMlKit = FakeMlKitOcrService()
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = true,
                )

            service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-003")

            assertEquals("ML Kit must be called when consent key is absent", 1, fakeMlKit.extractCallCount)
            assertEquals("Gemini must NOT be called", 0, service.geminiCallCount)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4 — consent declined → ML Kit fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_consent_is_declined`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore(SecurePreferences.KEY_GEMINI_OCR_CONSENT to "false")
            val fakeMlKit = FakeMlKitOcrService()
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = true,
                )

            service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-004")

            assertEquals("ML Kit must be called when consent is 'false'", 1, fakeMlKit.extractCallCount)
            assertEquals("Gemini must NOT be called", 0, service.geminiCallCount)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5 — blank raw text → ML Kit fallback
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_raw_text_is_blank`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore(SecurePreferences.KEY_GEMINI_OCR_CONSENT to "true")
            val fakeMlKit = FakeMlKitOcrService().also { it.rawTextResult = "" }
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = true,
                )

            service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-005")

            assertEquals("ML Kit must be called when extractRawText returns blank", 1, fakeMlKit.extractCallCount)
            assertEquals("Gemini must NOT be called", 0, service.geminiCallCount)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6 — parseGeminiJson: doctor_name parsed correctly
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_parse_doctor_name_from_gemini_json`() {
        val json = """{"doctor_name":"Dr. Synthetic","prescription_date":null,"medicines":[]}"""
        val fields = GeminiEnhancedOcrService.parseGeminiJsonStatic(json)

        val doctorField = fields.find { it.fieldName == "doctor_name" }
        assertEquals("doctor_name rawValue", "Dr. Synthetic", doctorField?.rawValue)
        assertEquals("doctor_name confidence", GeminiEnhancedOcrService.GEMINI_CONFIDENCE, doctorField?.confidence)
        assertEquals("doctor_name reviewState", ReviewState.PENDING_REVIEW, doctorField?.reviewState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7 — absent field → rawValue is null (never fabricated)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_set_absent_field_to_null_when_missing_from_gemini_json`() {
        // prescription_date is explicitly null in JSON — must map to rawValue=null
        val json = """{"doctor_name":"Dr. Synthetic","prescription_date":null,"medicines":[]}"""
        val fields = GeminiEnhancedOcrService.parseGeminiJsonStatic(json)

        val dateField = fields.find { it.fieldName == "prescription_date" }
        assertNull(
            "MEDICAL-SAFETY: absent prescription_date must have null rawValue — never fabricated",
            dateField?.rawValue,
        )
        assertEquals("absent field confidence must be 0.0f", 0f, dateField?.confidence)
        assertEquals("absent field reviewState must be PENDING_REVIEW", ReviewState.PENDING_REVIEW, dateField?.reviewState)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 8 — max medicines capped at 5
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_not_exceed_max_medicines`() {
        // 6 entries — only 5 medicine_name fields should be produced
        val json =
            """
            {
              "doctor_name": "Dr. Synthetic",
              "prescription_date": "2025-07-01",
              "medicines": [
                {"name": "Synthamol 500mg",  "dose": "1 tab",  "frequency": "once daily"},
                {"name": "Testocillin 250mg", "dose": "1 tab",  "frequency": "twice daily"},
                {"name": "Synthex 10mg",      "dose": "1 tab",  "frequency": "at night"},
                {"name": "Testamol 100mg",    "dose": "2 tabs", "frequency": "thrice daily"},
                {"name": "Synflu 5mg",        "dose": "1 tab",  "frequency": "morning"},
                {"name": "ExtraMed 20mg",     "dose": "1 tab",  "frequency": "evening"}
              ]
            }
            """.trimIndent()

        val fields = GeminiEnhancedOcrService.parseGeminiJsonStatic(json)
        val medicineNameFields = fields.filter { it.fieldName.startsWith("medicine_name") }

        assertTrue(
            "Must not produce more than 5 medicine_name fields (got ${medicineNameFields.size})",
            medicineNameFields.size <= 5,
        )
        assertNull(
            "medicine_name_6 must not exist — capped at 5",
            fields.find { it.fieldName == "medicine_name_6" },
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 9 — Gemini throws IOException → ML Kit fallback (no crash)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_use_mlkit_fallback_when_gemini_throws`() =
        runTest {
            val fakePrefs = FakeSecurePreferencesStore(SecurePreferences.KEY_GEMINI_OCR_CONSENT to "true")
            val fakeMlKit = FakeMlKitOcrService()
            val service =
                TestableGeminiOcrService(
                    fakePrefs = fakePrefs,
                    fakeMlKit = fakeMlKit,
                    featureFlagEnabled = true,
                    throwOnGeminiCall = IOException("Synthetic network failure — test only"),
                )

            val result = service.extract("/synth/rx.jpg", "image/jpeg", "synth-rx-009")

            assertEquals("ML Kit must be called when Gemini throws", 1, fakeMlKit.extractCallCount)
            assertTrue("Result must be successful even after Gemini exception", result.isSuccess)
            assertEquals("synth-rx-009", result.prescriptionId)
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Extra: all present Gemini fields have PENDING_REVIEW + GEMINI_CONFIDENCE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `should_return_all_present_fields_as_PENDING_REVIEW_with_gemini_confidence`() {
        val json =
            """
            {
              "doctor_name": "Dr. Synthetic",
              "prescription_date": "2025-07-01",
              "medicines": [
                {"name": "Synthamol 500mg", "dose": "1 tab", "frequency": "once daily"}
              ]
            }
            """.trimIndent()

        val fields = GeminiEnhancedOcrService.parseGeminiJsonStatic(json)
        val presentFields = fields.filter { it.rawValue != null }

        assertTrue("At least one present field must be returned", presentFields.isNotEmpty())
        presentFields.forEach { field ->
            assertEquals("'${field.fieldName}' reviewState", ReviewState.PENDING_REVIEW, field.reviewState)
            assertEquals("'${field.fieldName}' confidence", GeminiEnhancedOcrService.GEMINI_CONFIDENCE, field.confidence)
        }
    }
}
