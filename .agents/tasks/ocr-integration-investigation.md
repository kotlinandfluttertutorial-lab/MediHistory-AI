# OCR Integration Investigation Report
**Task:** Replace StubOcrService with Google ML Kit Text Recognition v2  
**Date:** Investigation complete  
**Scope:** Read-only analysis of existing OCR infrastructure

---

## Summary Answers (quick reference)

| Question | Answer |
|---|---|
| OcrService.extract() signature | `suspend fun extract(documentPath: String, mimeType: String, prescriptionId: String): OcrResult` |
| Input type | Absolute file path in app-private storage (not a URI) |
| ML Kit in libs.versions.toml | **None.** Zero ML Kit dependencies exist. |
| compileSdk / minSdk / targetSdk | 34 / 26 / 34 |
| OcrWorker input to extract() | String file path (KEY_DOCUMENT_PATH), never a URI |
| Image processing utilities | **None** — no Bitmap, InputImage, or image-prep utilities exist |
| Parsing utilities for structured fields | **None** — only `parseDate()` in PostReviewProcessor |
| ADR-006 exists? | Yes — records ML Kit as a "candidate adapter" but deferred |
| ReviewState enum | PENDING_REVIEW, REVIEWED, REJECTED |
| Confidence range | Float in [0.0, 1.0] |

---

## 1. OcrService Interface

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrService.kt`

```kotlin
interface OcrService {
    suspend fun extract(
        documentPath: String,   // Absolute path, app-private storage only
        mimeType: String,       // "image/jpeg", "image/png", "application/pdf"
        prescriptionId: String, // Traceability ID for the returned OcrResult
    ): OcrResult
}
```

Key constraints from the KDoc:
- `documentPath` is always an **absolute file path** (not a content URI or Uri object)
- mimeType drives extraction strategy — the implementation must branch on it
- Returns `OcrResult` — never throws for low-confidence results; only fails on total service failure
- No cloud-provider imports are permitted in this interface or its callers

---

## 2. OcrResult Structure

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrResult.kt`

```kotlin
data class OcrResult(
    val prescriptionId: String,
    val fields: List<ExtractedField>,
    val overallConfidence: Float,           // [0.0, 1.0] aggregate
    val errorMessage: String? = null,       // null = success, non-null = total failure
) {
    val isSuccess: Boolean get() = errorMessage == null
    val pendingReviewCount: Int             // count of non-null rawValue fields in PENDING_REVIEW
}
```

The `isSuccess = false` path only fires on a **total failure** (e.g., cannot read file, ML Kit init error). A completely blank/unreadable page still returns `isSuccess = true` with all fields having `rawValue = null`.

---

## 3. ExtractedField Structure

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/ExtractedField.kt`

```kotlin
data class ExtractedField(
    val fieldName: String,          // e.g. "doctor_name", "medicine_name", "dose"
    val rawValue: String?,          // null = field absent in document — NEVER fabricate
    val patientValue: String?,      // null until patient reviews
    val confidence: Float,          // [0.0, 1.0]; validated in init block
    val reviewState: ReviewState = ReviewState.PENDING_REVIEW,
    val sourceReference: String? = null,  // e.g. "page:1" or bounding box ref
)
```

**init block enforces:** `require(confidence in 0f..1f)` — ML Kit confidence must be normalised to this range before constructing ExtractedField objects.

---

## 4. ReviewState Enum

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/ReviewState.kt`

```kotlin
enum class ReviewState { PENDING_REVIEW, REVIEWED, REJECTED }
```

Valid transitions (enforced in ValidationUtils, referenced but not yet implemented as a separate class):
- `PENDING_REVIEW → REVIEWED`
- `PENDING_REVIEW → REJECTED`
- `REVIEWED → PENDING_REVIEW`
- `REJECTED → PENDING_REVIEW`

---

## 5. Field Names Expected by PostReviewProcessor

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/PostReviewProcessor.kt`

PostReviewProcessor explicitly handles:

| fieldName | Action taken |
|---|---|
| `doctor_name` | Look up or create Doctor row; link to Prescription.doctorId |
| `prescription_date` | Parse with 7 date formats (ISO, dd/MM/yyyy, dd-MM-yyyy, d MMM yyyy, dd MMM yyyy, MMMM d, yyyy, d/M/yyyy) |
| `medicine_name` | Create Medicine row (status=UNKNOWN) |
| `medicine_name_2`, `medicine_name_N` | Additional medicines (numbered suffix pattern) |
| `dose` | Combined into dosageInstructions with frequency (same suffix logic) |
| `dose_2`, `dose_N` | For medicine_name_N |
| `frequency` | Combined with dose into dosageInstructions |
| `frequency_2`, `frequency_N` | For medicine_name_N |

The StubOcrService currently only emits: `doctor_name`, `prescription_date`, `medicine_name`, `dose` (null), `frequency` (null). The real implementation should also attempt to extract `dose` and `frequency` from the document text.

---

## 6. How OcrWorker Calls OcrService

**File:** `android/app/src/main/java/com/medihistory/ai/worker/OcrWorker.kt`

```kotlin
// OcrWorker passes a plain String file path — never a Uri
val documentPath = inputData.getString(KEY_DOCUMENT_PATH) ?: return Result.failure()
val mimeType = inputData.getString(KEY_MIME_TYPE) ?: return Result.failure()
val result = ocrService.extract(documentPath, mimeType, prescriptionId)
```

WorkManager input data keys:
- `KEY_DOCUMENT_PATH = "document_path"` — absolute path in app-private storage
- `KEY_MIME_TYPE = "mime_type"` — one of `image/jpeg`, `image/png`, `application/pdf`

The ML Kit implementation will receive a file path, not a Uri or Bitmap. It must create a `File(documentPath)` and pass it to `InputImage.fromFilePath(context, Uri.fromFile(file))` or `BitmapFactory.decodeFile(documentPath)`.

**OcrWorker persists results as `ExtractedFieldEntity` with:**
- `id = "$prescriptionId-${field.fieldName}"` — composite key, one entity per field name
- All fields start at `ReviewState.PENDING_REVIEW`
- Sets prescription `OcrStatus` to `NEEDS_REVIEW` if pendingReviewCount > 0, else `REVIEWED`

---

## 7. Current ML Kit / Dependency Situation

**File:** `android/gradle/libs.versions.toml`

**No ML Kit dependency exists.** The full dependency list contains:
- `agp = "8.4.2"`, `kotlin = "1.9.24"`, `compose-bom = "2024.06.00"`
- `room = "2.6.1"`, `hilt = "2.51.1"`, `camerax = "1.3.3"`, `work = "2.9.0"`
- Firebase BOM `33.1.2` (firebase-auth, firebase-analytics)
- **Zero ML Kit or text recognition libraries**

SDK versions from `android/app/build.gradle.kts`:
- `compileSdk = 34`
- `minSdk = 26` (Android 8.0 — ML Kit Text Recognition v2 requires minSdk 21, so this is fine)
- `targetSdk = 34`
- `jvmTarget = "17"`, `sourceCompatibility = JavaVersion.VERSION_17`

---

## 8. Existing Image Processing Utilities

**None exist.** A search across all `.kt` files found:
- No `BitmapFactory`, `InputImage`, `ImageProxy`, or any image processing code in production source
- `CameraViewModel.onImageCaptured()` only stores a file path and copies the file — no image decoding
- `CaptureConfirmScreen.kt` has a comment: "Image preview placeholder (real image rendering requires coil, not yet added)"
- No image preprocessing (rotation correction, contrast enhancement, etc.)

The ML Kit implementation will need to handle `InputImage` creation from a file path entirely from scratch.

---

## 9. ADR-006 — OCR Provider Decision

**File:** `docs/architecture/adr/ADR-006.md`  
**Status:** Accepted  
**Date:** 2026-10-25

ADR-006 establishes the **provider-independent interface pattern** (already implemented). On the provider selection question:

> **"On-device ML Kit (Google): Viable for some extraction tasks; lacks structured field extraction for prescription formats out of the box; accuracy on handwritten prescriptions is lower; retained as a candidate adapter for offline/privacy-sensitive deployments but not selected as the sole option."**

The ADR defers final provider selection pending evaluation against cost, accuracy, latency, and data-residency criteria. However, the user's explicit choice ("Option A — Integrate a real OCR provider (e.g. Google ML Kit)") overrides the deferral. ML Kit is explicitly listed as a candidate adapter in the ADR, so implementing it is architecturally consistent.

---

## 10. Existing Parsing Utilities

**No prescription field parsing utilities exist.** The only parsing logic in the codebase:
- `PostReviewProcessor.parseDate()` — parses 7 date formats for `prescription_date` fields (this is in the *post-OCR* layer, not in the OCR service itself)
- Date formatters in various ViewModels — display formatting only

The ML Kit implementation will need to write all regex/keyword-based field extraction logic from scratch, mapping raw OCR text blocks to the expected `fieldName` keys.

---

## 11. OcrModule — Hilt Binding

**File:** `android/app/src/main/java/com/medihistory/ai/data/ocr/OcrModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {
    @Provides
    @Singleton
    fun provideOcrService(): OcrService = StubOcrService()
    // TODO(ADR-006): replace StubOcrService with the selected provider adapter.
}
```

To wire in ML Kit: replace `StubOcrService()` with `MlKitOcrService(context)`. The `@Singleton` scope is appropriate since ML Kit's `TextRecognizer` is reusable and should not be recreated per call.

---

## Conclusions and Recommendations

### What needs to be built

1. **New dependency entries in `libs.versions.toml`:**
   - Add `mlkit-text-recognition = "16.0.1"` (bundled, works offline)
   - Alternatively `play-services-mlkit-text-recognition = "19.0.1"` (unbundled, smaller APK, requires Play Services)
   - Recommendation: use **bundled** (`com.google.mlkit:text-recognition`) since offline access is a core product principle (product steering: "Offline access is core")

2. **New dependency in `build.gradle.kts`:**
   ```kotlin
   implementation(libs.mlkit.text.recognition)
   ```

3. **New class: `MlKitOcrService`** (in `data/ocr/` per structure steering):
   - Implements `OcrService`
   - Creates `InputImage` from `documentPath` via `InputImage.fromFilePath(context, Uri.fromFile(File(documentPath)))` for images, or via `BitmapFactory.decodeFile()` + `InputImage.fromBitmap()`
   - PDF support: ML Kit Text Recognition does NOT support PDFs natively. For `mimeType = "application/pdf"`, the implementation must either:
     - Return an error `OcrResult` with a descriptive `errorMessage` (safest for MVP)
     - Use `PdfRenderer` (API 21+) to render the first page to a Bitmap, then pass to ML Kit (more complete)
     - Recommendation: implement PdfRenderer path since `minSdk = 26` (> 21) guarantees availability
   - Runs recognition via `suspendCoroutine` wrapping the ML Kit Task API
   - Maps `Text.TextBlock` / `Text.Line` results to `ExtractedField` objects using keyword/regex heuristics

4. **New class: `PrescriptionFieldParser`** (in `data/ocr/`):
   - Pure function class: takes raw ML Kit `Text` output, returns `List<ExtractedField>`
   - Extracts `doctor_name` (look for "Dr.", "Dr ", "Doctor", registration number patterns)
   - Extracts `prescription_date` (look for date patterns matching the formats in `PostReviewProcessor.parseDate()`)
   - Extracts `medicine_name` (look for medicine-like tokens: capitalized words, dosage suffixes like mg/ml/mcg)
   - Extracts `dose` and `frequency` (look for dosage patterns: "1-0-1", "twice daily", "BD", "TDS", "OD")
   - Assigns confidence based on ML Kit `Text.Line.confidence` (where available) or block-level heuristics
   - Sets `sourceReference` to `"block:N"` or `"line:N"` for traceability
   - **Never fabricates absent fields** — if a pattern is not found, returns `ExtractedField` with `rawValue = null` and `confidence = 0.0f`

5. **Update `OcrModule`:**
   - Replace `StubOcrService()` with `MlKitOcrService(context)`
   - `MlKitOcrService` needs `@ApplicationContext context: Context` via Hilt injection

6. **New test: `MlKitOcrServiceTest`** (in `test/java/…/data/ocr/`):
   - Unit-testable only via Robolectric or via mocking the ML Kit Task — recommend testing `PrescriptionFieldParser` in isolation with synthetic raw text strings
   - `MlKitOcrService` itself should be tested with an integration/instrumented test using a synthetic JPEG with known text

### Architecture fits perfectly

The provider-independent `OcrService` interface is already designed for exactly this swap. The change is:
- **Zero modifications** to `OcrWorker`, `OcrReviewViewModel`, `OcrReviewScreen`, `PostReviewProcessor`, `OcrResult`, `ExtractedField`, or `ReviewState`
- **One class added** (`MlKitOcrService`)
- **One helper class added** (`PrescriptionFieldParser`)
- **One module line changed** (`OcrModule.provideOcrService()`)
- **Two TOML entries added** (mlkit version + library alias)
- **One build.gradle.kts line added** (implementation dependency)

### Key risk: ML Kit confidence scores

ML Kit's `Text.Element.confidence` is available but not always populated (it may return `0.0` even for high-quality text). The `PrescriptionFieldParser` should use a heuristic: if ML Kit returns confidence > 0 on an element, use it; if 0, apply a text-quality heuristic (e.g., 0.85 for clearly legible structured fields found by keyword match, 0.60 for fields found by fuzzy pattern match). This satisfies `ExtractedField`'s `require(confidence in 0f..1f)` constraint.

### Key risk: PDF support

ML Kit does not process PDFs. Since `OcrService` declares `mimeType = "application/pdf"` as a supported input, the implementation must handle it. `PdfRenderer` (built-in since API 21) renders pages to Bitmap and is the recommended path. The implementation should render page 1 (and optionally page 2) to Bitmap, then run ML Kit on each page, and merge the `ExtractedField` results (using `"page:1"` / `"page:2"` as `sourceReference`).

### Context injection note

`MlKitOcrService` needs `Context` to create `InputImage.fromFilePath()`. It should use `@ApplicationContext context: Context` via `@Inject constructor`. OcrModule must be updated to use `@Provides` with a `@ApplicationContext Context` parameter instead of the current zero-parameter factory method.
