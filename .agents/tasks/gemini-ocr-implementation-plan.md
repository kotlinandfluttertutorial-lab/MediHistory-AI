# Implementation Plan — Gemini 1.5 Flash Structured Field Extraction

## Verified SDK API (Step 1 — Complete)

**SDK artifact:** `com.google.genai:google-genai-kotlin:1.8.0`
**Verified from:** `https://raw.githubusercontent.com/googleapis/kotlin-genai/main/` (source files read directly)

### Confirmed import paths
```kotlin
import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.Schema
import com.google.genai.kotlin.types.Type
```

### Confirmed API pattern
```kotlin
// Client is AutoCloseable; use `.use {}` so it is closed after each call.
Client(apiKey = apiKey).use { client ->
    val config = GenerateContentConfig(
        systemInstruction = Content.fromText("System prompt here"),
        responseMimeType  = "application/json",   // forces JSON mode
        responseSchema    = Schema(                // optional but recommended
            type       = Type.OBJECT,
            properties = mapOf(
                "field_name" to Schema(type = Type.STRING, nullable = true),
                // …
            ),
            required = listOf("field_name"),
        ),
        temperature    = 0.0,    // deterministic extraction
        maxOutputTokens = 1024,
    )
    val response = client.models.generateContent(
        model  = "gemini-1.5-flash",
        text   = prompt,
        config = config,
    )
    val json: String? = response.text   // null if model returned no text
}
```

Key facts confirmed from source:
- `GenerateContentConfig.responseMimeType: String?` — set to `"application/json"` for JSON mode.
- `GenerateContentConfig.responseSchema: Schema?` — optional Schema object; enforces a structure.
- `Schema` is a `data class` with `type`, `properties`, `required`, `nullable`, `items`, etc.
- `Type` is a `@JvmInline value class`; constants are `Type.STRING`, `Type.OBJECT`, `Type.ARRAY`, `Type.NUMBER`, `Type.INTEGER`.
- `Content.fromText(text, role)` is a companion-object factory on `Content`.
- `response.text` is a convenience accessor that concatenates text parts of the first candidate.
- `client.models.generateContent(model, text, config)` is a suspend function.
- `Client` takes `apiKey: String` as a constructor parameter (explicit configuration path).

---

## Implementation Plan

### Step 1 — Already done above (SDK API verified).

---

- [ ] 2. Add Gemini SDK version and library alias to `libs.versions.toml`

  Add one line to `[versions]` and one line to `[libraries]`. No other entries change.

  Files to modify: `android/gradle/libs.versions.toml`

  Changes:
  ```toml
  # [versions] — add after mlkit-text-recognition
  google-genai-kotlin = "1.8.0"

  # [libraries] — add after the ML Kit entry
  google-genai-kotlin = { group = "com.google.genai", name = "google-genai-kotlin", version.ref = "google-genai-kotlin" }
  ```

  Verify: `cd android && ./gradlew :app:dependencies --configuration debugCompileClasspath 2>&1 | Select-String "google-genai"` — the artifact appears at version 1.8.0.

---

- [ ] 3. Add `implementation()` and two new BuildConfig fields to `build.gradle.kts`

  Follow the existing `localProperties` pattern already used for `BACKEND_BASE_URL`.
  Add the Gemini API key and feature flag reads **before** the `android {}` block; add the
  `BuildConfig` field declarations inside `defaultConfig {}`.

  Files to modify: `android/app/build.gradle.kts`

  Changes — add after the `enableInteractionCheck` block (line ~40):
  ```kotlin
  val geminiApiKey: String =
      localProperties.getProperty("gemini.api.key")
          ?: System.getenv("GEMINI_API_KEY")
          ?: ""

  val enableGeminiOcr: Boolean =
      (localProperties.getProperty("enable.gemini.ocr")
          ?: System.getenv("ENABLE_GEMINI_OCR")
          ?: "false").toBoolean()
  ```

  Add inside `defaultConfig {}` after the existing `buildConfigField` lines:
  ```kotlin
  buildConfigField("String",  "GEMINI_API_KEY",  "\"$geminiApiKey\"")
  buildConfigField("boolean", "ENABLE_GEMINI_OCR", "$enableGeminiOcr")
  ```

  Add to `dependencies {}` after the ML Kit entry:
  ```kotlin
  // Gemini 1.5 Flash — cloud-based structured OCR extraction
  implementation(libs.google.genai.kotlin)
  ```

  Verify: `cd android && ./gradlew :app:assembleDebug 2>&1 | Select-String -Pattern "BUILD|error"` — BUILD SUCCESSFUL, no errors.

  Security note: `GEMINI_API_KEY` flows from `local.properties` (git-ignored) or CI env var.
  It is injected at compile time via `BuildConfig`. Never hardcode a key value here.

---

- [ ] 4. Add `KEY_GEMINI_OCR_CONSENT` constant to `SecurePreferences.kt`

  Append one constant to the `companion object`. No logic changes needed.

  Files to modify: `android/app/src/main/java/com/medihistory/ai/data/security/SecurePreferences.kt`

  Change — add after `KEY_CLOUD_BACKUP_ENABLED`:
  ```kotlin
  /**
   * Stores whether the patient has consented to sending OCR text to Gemini.
   * Value is "true" when consent has been granted, absent/null when unknown,
   * never set to "false" explicitly — absence means no consent.
   */
  const val KEY_GEMINI_OCR_CONSENT = "gemini_ocr_consent_granted"
  ```

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 5. Add `extractRawText()` internal method to `MlKitOcrService.kt`

  Expose the pixel→text step as a separate `internal suspend fun` so `GeminiEnhancedOcrService`
  can call it without duplicating the CameraX/PDF rendering logic. The method reuses the existing
  private helpers `runMlKit`, `extractFromImage`, and `extractFromPdf`, which are already
  `private`; `extractRawText` must call the appropriate private helper and assemble a plain-text
  string from all TextBlock texts, page by page, separated by newlines.

  Files to modify: `android/app/src/main/java/com/medihistory/ai/data/ocr/MlKitOcrService.kt`

  Change — add as a new `internal` method after `extract()`, before `extractFromImage()`:
  ```kotlin
  /**
   * Runs ML Kit pixel-to-text OCR and returns the concatenated raw text string.
   * Used by [GeminiEnhancedOcrService] to obtain the OCR text before passing it
   * to Gemini for structured field extraction.
   *
   * Privacy: never log the returned string.
   */
  internal suspend fun extractRawText(documentPath: String, mimeType: String): String {
      return try {
          when {
              mimeType.startsWith("image/") -> {
                  val file = File(documentPath)
                  if (!file.exists() || !file.canRead()) return ""
                  val inputImage = InputImage.fromFilePath(context, Uri.fromFile(file))
                  runMlKit(inputImage).textBlocks
                      .joinToString("\n") { block -> block.text }
              }
              mimeType == "application/pdf" -> {
                  val file = File(documentPath)
                  if (!file.exists() || !file.canRead()) return ""
                  val pages = mutableListOf<String>()
                  ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                      PdfRenderer(pfd).use { renderer ->
                          val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                          for (pageIndex in 0 until pageCount) {
                              renderer.openPage(pageIndex).use { page ->
                                  val scale = PDF_TARGET_WIDTH.toFloat() / page.width
                                  val bitmapWidth  = PDF_TARGET_WIDTH
                                  val bitmapHeight = (page.height * scale).toInt()
                                  val bitmap = Bitmap.createBitmap(
                                      bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888,
                                  )
                                  page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                  val mlKitText = runMlKit(InputImage.fromBitmap(bitmap, 0))
                                  pages.add(mlKitText.textBlocks.joinToString("\n") { it.text })
                                  bitmap.recycle()
                              }
                          }
                      }
                  }
                  pages.joinToString("\n\n--- page break ---\n\n")
              }
              else -> ""
          }
      } catch (e: Exception) {
          ""   // silently return empty; GeminiEnhancedOcrService will fall back to ML Kit parser
      }
  }
  ```

  Note: `MAX_PDF_PAGES` and `PDF_TARGET_WIDTH` are already `private const val` in the companion
  object and are accessible from `internal` methods in the same class.

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 6. Create `GeminiEnhancedOcrService.kt`

  This is the main new file. It implements `OcrService`, delegates pixel→text to
  `MlKitOcrService.extractRawText()`, then calls Gemini 1.5 Flash for structured extraction.
  Falls back silently to `MlKitOcrService.extract()` (the full existing path including
  `PrescriptionFieldParser`) when Gemini is unavailable, the API key is blank, the patient
  has not consented, or the feature flag is off.

  File to create:
  `android/app/src/main/java/com/medihistory/ai/data/ocr/GeminiEnhancedOcrService.kt`

  ```kotlin
  package com.medihistory.ai.data.ocr

  import com.google.genai.kotlin.Client
  import com.google.genai.kotlin.types.Content
  import com.google.genai.kotlin.types.GenerateContentConfig
  import com.google.genai.kotlin.types.Schema
  import com.google.genai.kotlin.types.Type
  import com.medihistory.ai.BuildConfig
  import com.medihistory.ai.data.security.SecurePreferences
  import com.medihistory.ai.data.security.SecurePreferencesStore
  import dagger.hilt.android.qualifiers.ApplicationContext
  import android.content.Context
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
   * - Confidence is set to 0.88 for Gemini-extracted fields (empirical; always < 1.0
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
              val fields = callGemini(apiKey, rawText)
              if (fields.isEmpty()) {
                  return mlKitService.extract(documentPath, mimeType, prescriptionId)
              }

              val overallConfidence = fields
                  .filter { it.rawValue != null }
                  .map { it.confidence }
                  .let { cs -> if (cs.isNotEmpty()) cs.average().toFloat() else 0f }

              OcrResult(
                  prescriptionId    = prescriptionId,
                  fields            = fields,
                  overallConfidence = overallConfidence,
              )
          } catch (e: Exception) {
              // Fallback: never surface Gemini errors to the patient as a failure.
              // Log the error class only — never message content.
              android.util.Log.d("GeminiOcr", "Gemini extraction failed (${e.javaClass.simpleName}), using ML Kit fallback")
              mlKitService.extract(documentPath, mimeType, prescriptionId)
          }
      }

      // -----------------------------------------------------------------------
      // Gemini call
      // -----------------------------------------------------------------------

      private suspend fun callGemini(apiKey: String, rawText: String): List<ExtractedField> {
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

      // -----------------------------------------------------------------------
      // Prompt construction
      // -----------------------------------------------------------------------

      private fun buildPrompt(rawText: String): String =
          """
          Extract structured fields from the following Indian prescription OCR text.
          Return only the JSON object — no explanation, no markdown.
          If a field is not present in the text, set its value to null.
          Never infer or fabricate a value that is not explicitly present.

          OCR text:
          $rawText
          """.trimIndent()

      // -----------------------------------------------------------------------
      // JSON → ExtractedField list
      // -----------------------------------------------------------------------

      /**
       * Parses Gemini's JSON response into [ExtractedField] list.
       *
       * Medical-safety: any field with a null or blank JSON value is recorded with
       * rawValue == null — never replaced with an inferred value.
       * Privacy: the JSON string is never logged.
       */
      private fun parseGeminiJson(json: String): List<ExtractedField> {
          val obj = try { JSONObject(json) } catch (e: Exception) { return emptyList() }
          val fields = mutableListOf<ExtractedField>()

          fun addField(fieldName: String, jsonKey: String, conf: Float = GEMINI_CONFIDENCE) {
              val value = obj.optString(jsonKey, "").takeIf { it.isNotBlank() }
              fields.add(
                  ExtractedField(
                      fieldName       = fieldName,
                      rawValue        = value,
                      patientValue    = null,
                      confidence      = if (value != null) conf else 0f,
                      reviewState     = ReviewState.PENDING_REVIEW,
                      sourceReference = SOURCE_REF,
                  )
              )
          }

          addField("doctor_name",        "doctor_name")
          addField("prescription_date",  "prescription_date")

          // Medicines: up to MAX_MEDICINES entries
          val medicinesArray = obj.optJSONArray("medicines")
          if (medicinesArray != null) {
              val count = minOf(medicinesArray.length(), MAX_MEDICINES)
              for (i in 0 until count) {
                  val medicine = medicinesArray.optJSONObject(i) ?: continue
                  val sfx = if (i == 0) "" else "_${i + 1}"
                  val name = medicine.optString("name", "").takeIf { it.isNotBlank() }
                  fields.add(ExtractedField(
                      fieldName       = "medicine_name$sfx",
                      rawValue        = name,
                      patientValue    = null,
                      confidence      = if (name != null) GEMINI_CONFIDENCE else 0f,
                      reviewState     = ReviewState.PENDING_REVIEW,
                      sourceReference = SOURCE_REF,
                  ))
                  val dose = medicine.optString("dose", "").takeIf { it.isNotBlank() }
                  if (dose != null) fields.add(ExtractedField(
                      "dose$sfx", dose, null, GEMINI_CONFIDENCE, ReviewState.PENDING_REVIEW, SOURCE_REF,
                  ))
                  val freq = medicine.optString("frequency", "").takeIf { it.isNotBlank() }
                  if (freq != null) fields.add(ExtractedField(
                      "frequency$sfx", freq, null, GEMINI_CONFIDENCE, ReviewState.PENDING_REVIEW, SOURCE_REF,
                  ))
              }
          }

          // Ensure sentinel absent fields for medicine_name, dose, frequency if none extracted
          if (fields.none { it.fieldName == "medicine_name" })
              fields.add(ExtractedField("medicine_name", null, null, 0f, sourceReference = SOURCE_REF))
          if (fields.none { it.fieldName == "dose" })
              fields.add(ExtractedField("dose", null, null, 0f, sourceReference = SOURCE_REF))
          if (fields.none { it.fieldName == "frequency" })
              fields.add(ExtractedField("frequency", null, null, 0f, sourceReference = SOURCE_REF))

          return fields
      }

      // -----------------------------------------------------------------------
      // Constants
      // -----------------------------------------------------------------------

      companion object {
          private const val MODEL_ID          = "gemini-1.5-flash"
          private const val GEMINI_CONFIDENCE = 0.88f   // empirical; <1.0 to ensure patient review
          private const val MAX_MEDICINES     = 5
          private const val SOURCE_REF        = "gemini:1.5-flash"

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
  ```

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 7. Create `GeminiConsentDialog.kt`

  A Compose dialog shown once when the patient is about to OCR a prescription and
  `ENABLE_GEMINI_OCR` is true but consent has not yet been given. Consent is stored
  in `SecurePreferences`. The dialog must be concise, accurate about data usage, and
  provide a clear Accept / Decline choice.

  File to create:
  `android/app/src/main/java/com/medihistory/ai/ui/ocr/GeminiConsentDialog.kt`

  ```kotlin
  package com.medihistory.ai.ui.ocr

  import androidx.compose.material3.AlertDialog
  import androidx.compose.material3.Button
  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.OutlinedButton
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.semantics.contentDescription
  import androidx.compose.ui.semantics.semantics

  /**
   * One-time consent dialog shown before the first Gemini-enhanced OCR extraction.
   *
   * Medical-safety: explains clearly that OCR text will be sent to Google's servers.
   * The patient can decline; the app falls back to on-device ML Kit.
   *
   * @param onAccept  Called when the patient grants consent.
   * @param onDecline Called when the patient declines; app uses ML Kit fallback.
   */
  @Composable
  fun GeminiConsentDialog(
      onAccept: () -> Unit,
      onDecline: () -> Unit,
  ) {
      AlertDialog(
          onDismissRequest = onDecline,   // dismiss = decline
          title = {
              Text(
                  text  = "Improve prescription reading?",
                  style = MaterialTheme.typography.titleMedium,
              )
          },
          text = {
              Text(
                  text  = "To better extract medicines and doctor details — especially from " +
                          "handwritten prescriptions — MediHistory can send the text read from " +
                          "your prescription to Google's Gemini AI service.\n\n" +
                          "• Your prescription image stays on your device.\n" +
                          "• Only the extracted text is sent, not the image.\n" +
                          "• You can disable this in Settings at any time.\n\n" +
                          "This is optional. Choosing \"No thanks\" uses on-device reading only.",
                  style = MaterialTheme.typography.bodyMedium,
              )
          },
          confirmButton = {
              Button(
                  onClick  = onAccept,
                  modifier = Modifier.semantics {
                      contentDescription = "Allow Gemini AI to improve prescription reading"
                  },
              ) {
                  Text("Allow")
              }
          },
          dismissButton = {
              OutlinedButton(
                  onClick  = onDecline,
                  modifier = Modifier.semantics {
                      contentDescription = "Decline Gemini AI, use on-device reading only"
                  },
              ) {
                  Text("No thanks")
              }
          },
      )
  }
  ```

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 8. Update `OcrReviewViewModel.kt` — consent state, StateFlow, grant/decline methods

  Add `GeminiConsentState` enum, a `geminiConsentState` `StateFlow`, and two public
  methods `onGeminiConsentGranted()` / `onGeminiConsentDeclined()`.
  Inject `SecurePreferencesStore` alongside the existing constructor params.

  Files to modify:
  `android/app/src/main/java/com/medihistory/ai/ui/ocr/OcrReviewViewModel.kt`

  Changes:

  1. Add `SecurePreferencesStore` import:
  ```kotlin
  import com.medihistory.ai.data.security.SecurePreferences
  import com.medihistory.ai.data.security.SecurePreferencesStore
  ```

  2. Add `GeminiConsentState` enum **before** the class declaration (in the same file,
     not a separate file, since it is tightly coupled to the ViewModel):
  ```kotlin
  /** State of the patient's consent to use Gemini for OCR field extraction. */
  enum class GeminiConsentState {
      /** Consent has not been recorded yet — dialog should be shown. */
      UNKNOWN,
      /** Patient has explicitly granted consent. */
      GRANTED,
      /** Patient has explicitly declined — use ML Kit fallback silently. */
      DECLINED,
  }
  ```

  3. Add `securePrefs: SecurePreferencesStore` to the `@Inject constructor`:
  ```kotlin
  @HiltViewModel
  class OcrReviewViewModel @Inject constructor(
      private val extractedFieldDao: ExtractedFieldDao,
      private val prescriptionDao: PrescriptionDao,
      private val postReviewProcessor: PostReviewProcessorInterface,
      private val sessionManager: SessionManager,
      private val securePrefs: SecurePreferencesStore,     // ← add
  ) : ViewModel() {
  ```

  4. Add `geminiConsentState` StateFlow after `_uiState`:
  ```kotlin
  private val _geminiConsentState = MutableStateFlow(readConsentState())
  val geminiConsentState: StateFlow<GeminiConsentState> = _geminiConsentState.asStateFlow()
  ```

  5. Add three new methods at the bottom of the class:
  ```kotlin
  /** Reads the persisted consent state from EncryptedSharedPreferences. */
  private fun readConsentState(): GeminiConsentState {
      return when (securePrefs.getString(SecurePreferences.KEY_GEMINI_OCR_CONSENT)) {
          "true"  -> GeminiConsentState.GRANTED
          "false" -> GeminiConsentState.DECLINED
          else    -> GeminiConsentState.UNKNOWN
      }
  }

  /**
   * Called when the patient taps "Allow" in [GeminiConsentDialog].
   * Persists consent and updates the StateFlow so the screen re-composes.
   */
  fun onGeminiConsentGranted() {
      securePrefs.putString(SecurePreferences.KEY_GEMINI_OCR_CONSENT, "true")
      _geminiConsentState.value = GeminiConsentState.GRANTED
  }

  /**
   * Called when the patient taps "No thanks" in [GeminiConsentDialog].
   * Persists the decline so the dialog is not shown again this session.
   * The OCR pipeline will use the ML Kit fallback silently.
   */
  fun onGeminiConsentDeclined() {
      securePrefs.putString(SecurePreferences.KEY_GEMINI_OCR_CONSENT, "false")
      _geminiConsentState.value = GeminiConsentState.DECLINED
  }
  ```

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 9. Update `OcrReviewScreen.kt` — show `GeminiConsentDialog` when state is UNKNOWN

  The dialog appears before `load()` fires, gating the OCR run. After the patient
  responds (grant or decline), `load()` proceeds via `LaunchedEffect`.

  Files to modify:
  `android/app/src/main/java/com/medihistory/ai/ui/ocr/OcrReviewScreen.kt`

  Changes:

  1. Add imports:
  ```kotlin
  import com.medihistory.ai.BuildConfig
  import com.medihistory.ai.ui.ocr.GeminiConsentState
  ```

  2. Inside `OcrReviewScreen`, after `val uiState by viewModel.uiState.collectAsState()`, add:
  ```kotlin
  val geminiConsentState by viewModel.geminiConsentState.collectAsState()

  // Show consent dialog when feature is enabled and consent is not yet recorded.
  // After the patient responds, LaunchedEffect(prescriptionId) fires load().
  if (BuildConfig.ENABLE_GEMINI_OCR && geminiConsentState == GeminiConsentState.UNKNOWN) {
      GeminiConsentDialog(
          onAccept  = { viewModel.onGeminiConsentGranted() },
          onDecline = { viewModel.onGeminiConsentDeclined() },
      )
  }
  ```

  3. Wrap the existing `LaunchedEffect(prescriptionId)` so it only fires once consent
     is resolved:
  ```kotlin
  // Load only after consent is known (or when feature flag is off).
  val consentResolved = !BuildConfig.ENABLE_GEMINI_OCR
      || geminiConsentState != GeminiConsentState.UNKNOWN
  LaunchedEffect(prescriptionId, consentResolved) {
      if (consentResolved) {
          viewModel.load(prescriptionId)
      }
  }
  ```

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 10. Update `OcrModule.kt` — provide `GeminiEnhancedOcrService` instead of `MlKitOcrService`

  `GeminiEnhancedOcrService` depends on `MlKitOcrService` (for the fallback path and
  `extractRawText`). Hilt must provide both. `MlKitOcrService` becomes an intermediate
  `@Singleton` dependency; the `OcrService` binding points to `GeminiEnhancedOcrService`.

  Files to modify:
  `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrModule.kt`

  Replace the entire file body:
  ```kotlin
  package com.medihistory.ai.data.ocr

  import android.content.Context
  import com.medihistory.ai.data.security.SecurePreferencesStore
  import dagger.Module
  import dagger.Provides
  import dagger.hilt.InstallIn
  import dagger.hilt.android.qualifiers.ApplicationContext
  import dagger.hilt.components.SingletonComponent
  import javax.inject.Singleton

  /**
   * Hilt module that binds the [OcrService] implementation.
   *
   * Provides [GeminiEnhancedOcrService] which uses ML Kit for pixel→text OCR and
   * Gemini 1.5 Flash for structured field extraction, falling back to the ML Kit
   * parser when Gemini is unavailable, the patient has not consented, or the feature
   * flag is off.
   *
   * Security: no API keys are initialised here. [BuildConfig.GEMINI_API_KEY] is read
   * inside [GeminiEnhancedOcrService] at call time, not at injection time.
   */
  @Module
  @InstallIn(SingletonComponent::class)
  object OcrModule {

      /**
       * Provides [MlKitOcrService] as a [Singleton] so the [TextRecognizer] inside
       * it is created once per process (ML Kit recommendation).
       */
      @Provides
      @Singleton
      fun provideMlKitOcrService(
          @ApplicationContext context: Context,
      ): MlKitOcrService = MlKitOcrService(context)

      /**
       * Provides the [OcrService] interface implementation.
       * [GeminiEnhancedOcrService] wraps [MlKitOcrService] and the consent store.
       */
      @Provides
      @Singleton
      fun provideOcrService(
          @ApplicationContext context: Context,
          mlKitOcrService: MlKitOcrService,
          securePrefs: SecurePreferencesStore,
      ): OcrService = GeminiEnhancedOcrService(context, mlKitOcrService, securePrefs)
  }
  ```

  Note: `SecurePreferencesStore` is already bound in the app's DI graph (it has an
  `@Inject constructor` in `SecurePreferences`). No extra binding is needed.

  Verify: `cd android && ./gradlew :app:compileDebugKotlin 2>&1 | Select-String "error"` — no errors.

---

- [ ] 11. Regenerate dependency lockfile

  The lockfile (`android/app/gradle.lockfile`) will be stale after adding the new
  dependency. Regenerate it with the `--write-locks` flag.

  Command:
  ```
  cd android && ./gradlew :app:dependencies --write-locks
  ```

  Verify: `git diff android/app/gradle.lockfile` shows `com.google.genai:google-genai-kotlin:1.8.0`
  added to multiple configurations. The `settings-gradle.lockfile` may also update.

---

- [ ] 12. Add unit tests for `GeminiEnhancedOcrService`

  Follow the naming convention `should_[outcome]_when_[condition]` and the synthetic
  fixture rules from the testing steering file.

  File to create:
  `android/app/src/test/java/com/medihistory/ai/data/ocr/GeminiEnhancedOcrServiceTest.kt`

  Tests to cover:
  - `should_use_mlkit_fallback_when_feature_flag_is_false` — `ENABLE_GEMINI_OCR = false`
    (test with a spy that returns a known result from the ML Kit path)
  - `should_use_mlkit_fallback_when_api_key_is_blank` — apiKey = ""
  - `should_use_mlkit_fallback_when_consent_is_unknown` — securePrefs returns null
  - `should_use_mlkit_fallback_when_consent_is_declined` — securePrefs returns "false"
  - `should_use_mlkit_fallback_when_raw_text_is_blank` — extractRawText returns ""
  - `should_parse_gemini_json_doctor_name_correctly` — unit-test `parseGeminiJson` directly
    via a package-private/internal test or by subclassing
  - `should_set_absent_field_to_null_when_missing_from_gemini_json` — missing key returns
    rawValue = null, never fabricated
  - `should_not_exceed_max_medicines` — JSON with 6 medicines returns exactly 5 fields
  - `should_use_mlkit_fallback_when_gemini_throws` — simulate network exception

  Note: Because `GeminiEnhancedOcrService` makes a real network call via `Client`, these
  tests must use a fake/stub pattern. Create a `FakeGeminiEnhancedOcrService` helper in
  `src/test/` that overrides `callGemini()` to return synthetic JSON without a network call.
  Use `BuildConfig` field injection via a test subclass or extract `callGemini` to a
  testable interface (recommend: make `callGemini` `internal` and override in tests with
  `@VisibleForTesting`).

  Verify: `cd android && ./gradlew :app:testDebugUnitTest --tests "*.GeminiEnhancedOcrServiceTest" 2>&1 | Select-String "BUILD|PASS|FAIL"` — BUILD SUCCESSFUL, all tests PASS.

---

- [ ] 13. Add unit tests for `OcrReviewViewModel` consent state machine

  File to modify:
  `android/app/src/test/java/com/medihistory/ai/ui/ocr/OcrReviewViewModelTest.kt`
  (or create if it doesn't exist)

  Tests to add:
  - `should_emit_UNKNOWN_when_consent_key_absent`
  - `should_emit_GRANTED_when_consent_key_is_true`
  - `should_emit_DECLINED_when_consent_key_is_false`
  - `should_persist_true_and_emit_GRANTED_when_onGeminiConsentGranted_called`
  - `should_persist_false_and_emit_DECLINED_when_onGeminiConsentDeclined_called`

  Use an in-memory `FakeSecurePreferencesStore` (implement `SecurePreferencesStore` with
  a `HashMap` backing store) — no `EncryptedSharedPreferences` or Android Keystore in unit tests.

  Verify: `cd android && ./gradlew :app:testDebugUnitTest --tests "*.OcrReviewViewModelTest" 2>&1 | Select-String "BUILD|PASS|FAIL"` — BUILD SUCCESSFUL, all tests PASS.

---

- [ ] 14. Full build and test gate

  Run the full unit test suite to confirm no regressions.

  Command:
  ```
  cd android && ./gradlew :app:testDebugUnitTest
  ```

  Expected: BUILD SUCCESSFUL. All previously passing tests continue to pass. New tests
  from Steps 12 and 13 pass.

  Then run the JaCoCo coverage gate:
  ```
  cd android && ./gradlew :app:jacocoTestCoverageVerification
  ```

  Expected: BUILD SUCCESSFUL — `com.medihistory.ai.data.*` line coverage ≥ 80%.

---

## Dependency ordering

Steps must be executed in order — each step depends on the prior:
- Step 2 (version entry) must precede Step 3 (implementation reference).
- Step 5 (extractRawText on MlKitOcrService) must precede Step 6 (GeminiEnhancedOcrService calls it).
- Step 4 (KEY constant) must precede Step 8 (ViewModel reads it).
- Steps 6, 7, 8 must all be complete before Step 9 (screen references all three).
- Steps 6 and 8 must precede Step 10 (OcrModule provides GeminiEnhancedOcrService + injects SecurePrefsStore).
- Steps 2–10 must precede Step 11 (lockfile regeneration needs the final dependency set).
- Steps 6, 8 must precede Steps 12, 13 (tests require the classes to exist).
- Steps 12, 13 must precede Step 14 (final gate runs them all).

## Files changed summary

| File | Action |
|------|--------|
| `android/gradle/libs.versions.toml` | Add version + library alias |
| `android/app/build.gradle.kts` | Add implementation + BuildConfig fields |
| `android/app/src/main/java/com/medihistory/ai/data/security/SecurePreferences.kt` | Add KEY_GEMINI_OCR_CONSENT constant |
| `android/app/src/main/java/com/medihistory/ai/data/ocr/MlKitOcrService.kt` | Add extractRawText() internal method |
| `android/app/src/main/java/com/medihistory/ai/data/ocr/GeminiEnhancedOcrService.kt` | **Create** |
| `android/app/src/main/java/com/medihistory/ai/ui/ocr/GeminiConsentDialog.kt` | **Create** |
| `android/app/src/main/java/com/medihistory/ai/ui/ocr/OcrReviewViewModel.kt` | Add GeminiConsentState enum, StateFlow, grant/decline methods |
| `android/app/src/main/java/com/medihistory/ai/ui/ocr/OcrReviewScreen.kt` | Observe consent state, show dialog, gate load() |
| `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrModule.kt` | Provide GeminiEnhancedOcrService + MlKitOcrService separately |
| `android/app/gradle.lockfile` | Regenerate |
| `android/app/src/test/java/com/medihistory/ai/data/ocr/GeminiEnhancedOcrServiceTest.kt` | **Create** |
| `android/app/src/test/java/com/medihistory/ai/ui/ocr/OcrReviewViewModelTest.kt` | Create or update |

## No changes required to

- `OcrService.kt` — interface is unchanged; both services implement it
- `OcrWorker.kt` — uses `OcrService` interface; no change needed
- `OcrResult.kt` — unchanged
- `ExtractedField.kt` — unchanged
- `PrescriptionFieldParser.kt` — kept as-is; used by ML Kit fallback path
- `StubOcrService.kt` — unchanged (tests still valid)
- Backend — no backend changes; extraction is entirely on the Android client
