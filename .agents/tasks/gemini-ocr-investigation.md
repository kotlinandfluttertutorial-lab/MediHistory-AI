# Gemini 1.5 Flash OCR Integration — Investigation Report

**Date:** 2025-01  
**Scope:** Read-only investigation. No code was modified.  
**Workspace root:** `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI`

---

## Summary Answers (one-liners)

| # | Question | Answer |
|---|----------|--------|
| 1 | Gemini Android SDK to use | **`com.google.genai:google-genai-kotlin:1.8.0`** (Google Gen AI Kotlin SDK, GA May 2025). The old `com.google.ai.client.generativeai:generativeai` is **deprecated** and archived. |
| 2 | Gemini already in libs.versions.toml? | **No.** No Gemini/GenAI entry exists. |
| 3 | local.properties pattern for secrets | 3-step pattern: read via `localProperties.getProperty("key") ?: System.getenv("KEY") ?: "default"`, then inject as `buildConfigField`. |
| 4 | MlKitOcrService wrapping strategy | Runs ML Kit → calls `PrescriptionFieldParser.parse()` → returns `OcrResult`. Gemini layer wraps at the service level, not the parser level. |
| 5 | OcrModule Hilt binding | `@Provides @Singleton` returning `MlKitOcrService(context)` — needs to change to provide `GeminiEnhancedOcrService`. |
| 6 | EncryptedSharedPreferences pattern | Centralised via `SecurePreferences` / `SecurePreferencesStore`. String key-value only; consent flag stored as `"true"`/`"false"` string. |
| 7 | ConsentRepository / SessionManager | `SessionManager` exists at `data/session/SessionManager.kt`. No `ConsentRepository`. Gemini consent flag should go into `SecurePreferences` with a new key constant. |
| 8 | BuildConfig feature flag pattern | Checked with `if (!BuildConfig.ENABLE_X)` guard. New `GEMINI_OCR_ENABLED` flag follows same pattern. |
| 9 | Fields PrescriptionFieldParser extracts | `doctor_name`, `prescription_date`, `medicine_name` (+ `_2`…`_5`), `dose`, `frequency`. |
| 10 | Existing HTTP client | **None.** No OkHttp, Retrofit, or Ktor dependency anywhere. The Gemini SDK brings its own. |
| 11 | UI consent dialog pattern | `AlertDialog` composable (Material 3) used throughout; existing `ConsentDialog.kt` for sharing. New Gemini consent follows the same `AlertDialog` pattern. |
| 12 | gradle.lockfile | `android/app/gradle.lockfile` **exists** and must be regenerated after adding new dependencies. |

---

## Evidence

### 1. Gemini SDK — exact Maven coordinate

**Recommended (GA, current):**
```
com.google.genai:google-genai-kotlin:1.8.0
```
Source: [googleapis/kotlin-genai README](https://github.com/googleapis/kotlin-genai/blob/main/README.md) — Google Gen AI Kotlin SDK reached General Availability in May 2025. Supports Android API 21+, Kotlin 1.9+, JDK 17 — all matching the project's constraints (minSdk=26, Kotlin 1.9.24, JVM 17).

**Deprecated alternative (do NOT use):**
```
com.google.ai.client.generativeai:generativeai:0.9.0
```
The `google-gemini/generative-ai-android` repository was **archived on Dec 16, 2025** and is now read-only. Do not use this coordinate.

**Firebase alternative:**
`firebase-vertexai` is available via the Firebase BOM (already in the project) but routes through Vertex AI and requires more cloud configuration. For direct Gemini Developer API access (just an API key), the `google-genai-kotlin` SDK is simpler and more appropriate.

**Security note from the SDK README:**  
> "For public mobile apps connecting directly to generative models from client devices, we strongly recommend using Firebase AI Logic with Firebase App Check enabled."  
> "Use this SDK on Android only when connecting through your own secure backend service, or for internal/prototype applications."

This warning means the API key must be kept in `local.properties` / CI env var and injected via `BuildConfig`, exactly as the project already does for `BACKEND_BASE_URL`. Never hardcode in source.

---

### 2. libs.versions.toml — format and gap

**File:** `android/gradle/libs.versions.toml`

No Gemini/GenAI entry of any kind. The style for a non-BOM library with a pinned version:

```toml
# [versions] section — add:
google-genai-kotlin = "1.8.0"

# [libraries] section — add (matching column-aligned format):
google-genai-kotlin = { group = "com.google.genai", name = "google-genai-kotlin", version.ref = "google-genai-kotlin" }
```

Existing style reference (from the file):
```toml
mlkit-text-recognition = "16.0.1"
mlkit-text-recognition = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkit-text-recognition" }
```

New entry must follow the same pattern: version in `[versions]`, library alias in `[libraries]` using `version.ref`.

---

### 3. build.gradle.kts — local.properties reading pattern and BuildConfig fields

**File:** `android/app/build.gradle.kts`

The existing 3-step pattern (lines ~28–41):
```kotlin
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

val backendBaseUrl: String =
    localProperties.getProperty("backend.base.url")
        ?: System.getenv("BACKEND_BASE_URL")
        ?: ""
```

Then injected as:
```kotlin
buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
```

**New additions needed:**
```kotlin
// In local.properties: gemini.api.key=AIza...
val geminiApiKey: String =
    localProperties.getProperty("gemini.api.key")
        ?: System.getenv("GEMINI_API_KEY")
        ?: ""

val enableGeminiOcr: Boolean =
    (localProperties.getProperty("enable.gemini.ocr")
        ?: System.getenv("ENABLE_GEMINI_OCR")
        ?: "false").toBoolean()
```
```kotlin
buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
buildConfigField("boolean", "ENABLE_GEMINI_OCR", "$enableGeminiOcr")
```

`local.properties` is already git-ignored. The `GEMINI_API_KEY` BuildConfig field must be read at runtime and passed to the SDK — never logged.

**Where to add the dependency** in the `dependencies {}` block (after ML Kit entry):
```kotlin
// Gemini 1.5 Flash — structured field extraction on top of ML Kit OCR
implementation(libs.google.genai.kotlin)
```

---

### 4. MlKitOcrService — wrapping analysis

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/MlKitOcrService.kt`

Key call chain:
1. `extract(documentPath, mimeType, prescriptionId)` dispatches to `extractFromImage` or `extractFromPdf`.
2. Both call `runMlKit(inputImage): Text` (coroutine wrapper around ML Kit Task).
3. Both then call `parser.parse(text, pageIndex, imageWidth, imageHeight): List<ExtractedField>`.
4. Returns `OcrResult(prescriptionId, allFields, overallConfidence)`.

**Wrap strategy:** Create `GeminiEnhancedOcrService` that:
1. Calls `MlKitOcrService.extract()` to get the baseline `OcrResult` (raw text + regex-parsed fields).
2. Extracts the full raw text from ML Kit (needs small adapter — current API returns `List<ExtractedField>`, not raw text; see §4a below).
3. Sends raw text to Gemini with a structured JSON prompt.
4. Parses Gemini JSON response into `List<ExtractedField>`, merging/overriding the ML Kit fields.
5. Falls back to returning the ML Kit `OcrResult` as-is if: `ENABLE_GEMINI_OCR` is false, Gemini consent not granted, network unavailable, or Gemini throws.

**§4a — Raw text gap:**  
`MlKitOcrService` does not currently expose the raw ML Kit `Text` object or concatenated raw text. The `PrescriptionFieldParser.parse()` consumes it internally. Options:
- Option A: Add a `extractRawText(documentPath, mimeType): String` method to `MlKitOcrService` (or a new internal helper) that returns the flat text for Gemini. This is the cleanest approach.
- Option B: `GeminiEnhancedOcrService` independently calls ML Kit for the raw text string before calling `MlKitOcrService.extract()`. This duplicates the ML Kit call but avoids changing `MlKitOcrService`.

**Recommendation: Option A.** Add `internal suspend fun extractRawText(...)` to `MlKitOcrService` that runs ML Kit and returns `text.textBlocks.joinToString("\n")`. `GeminiEnhancedOcrService` can then call this before or alongside `extract()`.

---

### 5. OcrModule.kt — current Hilt binding

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrModule.kt`

```kotlin
@Provides
@Singleton
fun provideOcrService(@ApplicationContext context: Context): OcrService =
    MlKitOcrService(context)
```

This must change to provide `GeminiEnhancedOcrService`. The cleanest approach:
```kotlin
@Provides
@Singleton
fun provideOcrService(
    @ApplicationContext context: Context,
    securePreferences: SecurePreferencesStore,
): OcrService =
    GeminiEnhancedOcrService(
        mlKitService = MlKitOcrService(context),
        securePreferences = securePreferences,
    )
```

Alternatively, `GeminiEnhancedOcrService` can use `@Inject constructor` directly (since `MlKitOcrService` also uses `@Inject`), and `OcrModule` only needs to change the `OcrService` binding to `GeminiEnhancedOcrService`.

---

### 6. EncryptedSharedPreferences — usage pattern

**File:** `android/app/src/main/java/com/medihistory/ai/data/security/SecurePreferences.kt`

The project uses a centralised `SecurePreferences` singleton (extends `SecurePreferencesStore` interface). The interface is string-only:
```kotlin
fun putString(key: String, value: String)
fun getString(key: String): String?
fun remove(key: String)
fun clear()
```

**Pattern for consent flag storage:**  
Booleans are stored as strings. See `KEY_CLOUD_BACKUP_ENABLED = "cloud_backup_enabled"` in `SecurePreferences.kt`. New key:

```kotlin
// Add to SecurePreferences.kt companion object:
const val KEY_GEMINI_OCR_CONSENT = "gemini_ocr_consent_granted"
```

Reading: `securePreferences.getString(KEY_GEMINI_OCR_CONSENT) == "true"`  
Writing: `securePreferences.putString(KEY_GEMINI_OCR_CONSENT, "true")`

**FakeSecurePreferences pattern** for tests: use the existing `FakeSecurePreferencesStore` defined in `PrivacyViewModelTest.kt` — it implements the interface with a `HashMap<String, String>`.

---

### 7. SessionManager and ConsentRepository

**File:** `android/app/src/main/java/com/medihistory/ai/data/session/SessionManager.kt`

`SessionManager` exists and manages patient ID + session token via `SecurePreferencesStore`. It does **not** manage feature consent.

**No `ConsentRepository` exists.** For the Gemini consent flag, two options:
- Option A (simplest): Store directly in `SecurePreferences` with a new key (`KEY_GEMINI_OCR_CONSENT`) — no new class needed. `GeminiEnhancedOcrService` reads it via `SecurePreferencesStore`.
- Option B: Create a `GeminiConsentRepository` interface + `SecureGeminiConsentRepository` impl. More testable but more boilerplate.

**Recommendation: Option A** for MVP — match the existing `KEY_CLOUD_BACKUP_ENABLED` pattern already established in `SecurePreferences`. A repository can be added later when there are multiple consent flags.

---

### 8. BuildConfig feature flag pattern

**File:** `android/app/src/main/java/com/medihistory/ai/ui/safety/SafetyCenterViewModel.kt`

```kotlin
if (!BuildConfig.ENABLE_INTERACTION_CHECK) {
    _uiState.update { it.copy(interactionStatus = InteractionCheckStatus.NotConfigured) }
    return
}
```

`GeminiEnhancedOcrService` should use the same guard:
```kotlin
if (!BuildConfig.ENABLE_GEMINI_OCR) {
    return mlKitService.extract(documentPath, mimeType, prescriptionId)
}
```

Test (`EnvironmentConfigTest.kt`) verifies feature flags default to `false` in CI — `ENABLE_GEMINI_OCR` should also default `false`.

---

### 9. Fields PrescriptionFieldParser extracts

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/PrescriptionFieldParser.kt`

Fields returned per prescription:
| Field key | Description |
|-----------|-------------|
| `doctor_name` | Doctor name from prefix ("Dr."), qualification line, or Reg. No. |
| `prescription_date` | Date matching Indian date patterns |
| `medicine_name` | First medicine (also `medicine_name_2`…`medicine_name_5`) |
| `dose` | Dose for first medicine (also `dose_2`…`dose_5`) |
| `frequency` | OD/BD/TDS/QID etc. (also `frequency_2`…`frequency_5`) |

**The Gemini prompt must request exactly these field names** plus their indexed variants, to ensure clean merging with the ML Kit baseline. Raw text cleaning issues (medicine name split across lines like `"TAB Montair (4"` / `"MG ) (1 Strip)"`) are precisely what Gemini fixes — it understands natural language and context.

Suggested Gemini JSON output schema:
```json
{
  "doctor_name": "string | null",
  "prescription_date": "string | null",
  "medicines": [
    {
      "medicine_name": "string",
      "dose": "string | null",
      "frequency": "string | null"
    }
  ]
}
```
The `medicines` array maps back to `medicine_name`, `medicine_name_2`, etc. (max 5).

---

### 10. Existing HTTP client dependencies

**Grep result:** No OkHttp, Retrofit, Ktor, or `HttpClient` anywhere in the codebase.

The `google-genai-kotlin` SDK bundles its own HTTP transport (OkHttp under the hood). No separate HTTP client dependency is needed. However, the Gemini SDK does bring in OkHttp as a transitive dependency — if the lockfile is strictly managed, it will appear there after `./gradlew dependencies` is re-run.

---

### 11. UI consent dialog pattern

**Files:** `android/app/src/main/java/com/medihistory/ai/ui/newdoctor/ConsentDialog.kt`, `ReminderScreen.kt`, `DuplicateAlertCard.kt`, `PrivacyScreen.kt`

All consent and confirmation dialogs use Material 3 `AlertDialog` composable with:
- `title = { Text(...) }` 
- `text = { ... }` (disclaimer body)
- `confirmButton = { Button { ... } }`
- `dismissButton = { TextButton { ... } }`

The existing `ConsentDialog.kt` for sharing is the closest model for the Gemini consent dialog. The Gemini consent dialog needs:
- Title: "Use AI for better medicine reading?"
- Body: explain cloud processing, that prescription text is sent to Google Gemini, and that the patient can decline.
- Disclaimer: "Text from your prescriptions will be sent to Google's Gemini API. No data is stored by Google after processing. You can change this in Privacy settings."
- Confirm: "Allow" | Dismiss: "Not now"

No `BottomSheet` is currently used anywhere — `AlertDialog` is the correct choice.

---

### 12. gradle.lockfile

**File:** `android/app/gradle.lockfile`  
**File:** `android/settings-gradle.lockfile`

Both exist. After adding the `google-genai-kotlin` dependency, the lockfile **must** be regenerated:
```bash
./gradlew :app:dependencies --write-locks
```
Or the full resolution:
```bash
cd android && ./gradlew dependencies --write-locks
```

Failure to regenerate will cause a build error: `Resolved 'com.google.genai:google-genai-kotlin:1.8.0' which is not part of the dependency lock state`.

---

## Architecture Design for Implementation

Based on the investigation, here is the recommended implementation architecture:

```
OcrService (interface)
  └─ GeminiEnhancedOcrService          ← NEW (wraps ML Kit + Gemini)
       ├─ MlKitOcrService              ← UNCHANGED (existing, injected)
       └─ GenerativeModel (Gemini SDK) ← NEW (from google-genai-kotlin)
```

### Data flow

```
extract(documentPath, mimeType, prescriptionId)
    │
    ├─ [Guard] ENABLE_GEMINI_OCR == false → return mlKitService.extract(...)
    ├─ [Guard] geminiConsentGranted == false → return mlKitService.extract(...)
    │
    ├─ Step 1: mlKitService.extract(...) → OcrResult (baseline fields)
    ├─ Step 2: mlKitService.extractRawText(...) → String (flat OCR text)
    │
    ├─ Step 3: sendToGemini(rawText) → GeminiJsonResponse
    │   [on network error / timeout / parse failure] → return Step 1 result
    │
    └─ Step 4: mergeFields(mlKitResult, geminiResult) → OcrResult (enhanced)
```

### Merge strategy
- Gemini-extracted `medicine_name` overrides ML Kit's if non-null (Gemini is better at multi-line names).
- ML Kit fields are kept as fallback for any field Gemini returns null for.
- `sourceReference` for Gemini fields: `"gemini:page:N"` to distinguish from `"page:N,block:M"`.
- `confidence` for Gemini fields: use `0.85f` as a fixed heuristic (Gemini does not return per-field confidence). This is above the typical review threshold but still requires patient review.

### Consent flag lifecycle
1. First OCR attempt: show `GeminiConsentDialog`.
2. User allows: store `KEY_GEMINI_OCR_CONSENT = "true"` in `SecurePreferences`.
3. User declines: store `"false"`, fall back to ML Kit only, never ask again this session.
4. Privacy screen: expose toggle to revoke consent (sets key back to `"false"`).

---

## Conclusions and Recommendations

1. **Use `com.google.genai:google-genai-kotlin:1.8.0`** — the GA-released Google Gen AI Kotlin SDK. Do not use the deprecated `com.google.ai.client.generativeai`.

2. **No new HTTP client needed** — the SDK bundles its own transport. The lockfile must be regenerated after adding the dependency.

3. **Add `GEMINI_API_KEY` to BuildConfig** using the existing `local.properties` / env-var pattern. Store the key in `local.properties` locally and in CI secrets — never in source.

4. **Add `ENABLE_GEMINI_OCR` boolean BuildConfig flag** defaulting to `false`, matching the `ENABLE_INTERACTION_CHECK` pattern. Flip to `true` in `local.properties` / CI to enable.

5. **Store consent flag in `SecurePreferences`** with a new constant `KEY_GEMINI_OCR_CONSENT`. No new repository class needed for MVP.

6. **Add `extractRawText()` to `MlKitOcrService`** (or make it a package-private helper) so `GeminiEnhancedOcrService` can pass the flat OCR string to Gemini without re-running ML Kit twice.

7. **Prompt Gemini for the same field names** as `PrescriptionFieldParser` uses (`doctor_name`, `prescription_date`, `medicine_name`…`medicine_name_5`, `dose`…`dose_5`, `frequency`…`frequency_5`) as a JSON schema. This ensures clean field-level merging.

8. **Gemini confidence heuristic: `0.85f`** for all Gemini-returned non-null fields. Gemini does not return per-field confidence scores. This value is high enough to avoid unnecessary low-confidence warnings while still requiring patient review per the review state machine.

9. **`OcrModule.kt` needs updating** — replace the direct `MlKitOcrService` provision with `GeminiEnhancedOcrService`. `MlKitOcrService` remains as an internal collaborator.

10. **Lockfile must be regenerated** after adding the Gradle dependency — run `./gradlew :app:dependencies --write-locks` from the `android/` directory.

11. **Test coverage requirements** (from testing steering):
    - Unit test `GeminiEnhancedOcrService` with a fake Gemini response (no real API calls in tests).
    - Test: Gemini disabled flag → returns ML Kit result.
    - Test: Gemini consent not granted → returns ML Kit result.
    - Test: Gemini network failure → falls back to ML Kit result (no crash).
    - Test: Gemini returns partial fields → merged correctly with ML Kit baseline.
    - Test: Gemini returns all null → ML Kit result returned as-is.
    - All test fixtures: synthetic medicine names only (`Synthamol 500mg`, `Testocillin BD`).

---

*Content was rephrased for compliance with licensing restrictions where web sources were referenced.*
