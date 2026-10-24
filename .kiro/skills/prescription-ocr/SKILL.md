# Skill: Prescription OCR for MediHistory AI

## Purpose

Guide safe, provider-independent OCR integration for MediHistory AI. Covers the `OcrService` interface contract, confidence scoring, the review state machine, background processing via WorkManager, source document preservation, and the rules that can never be broken.

## When to Apply This Skill

- Implementing or modifying the OCR pipeline (capture → extract → review)
- Adding a new OCR provider behind the `OcrService` interface
- Handling extracted field confidence and review states
- Writing or updating `OcrWorker` or any WorkManager task that touches prescriptions
- Writing tests for OCR flows

## Steering Files to Read First

- `.kiro/steering/medical-safety.md`
- `.kiro/steering/tech.md`

---

## Step-by-Step Guidance

### 1. Work Only Through the OcrService Interface

The Android codebase has zero direct imports from any cloud OCR SDK. All OCR work goes through `OcrService`, which accepts a document path and MIME type and returns structured `OcrResult` output. Provider implementations live in the data layer and are injected by Hilt.

### 2. Store rawValue Separately From patientValue

Every extracted field has two value slots:
- `rawValue`: what the OCR engine returned — **never modified after initial write**.
- `patientValue`: what the patient confirms or corrects — set on the `REVIEWED` transition.

Editing a field writes to `patientValue`. `rawValue` is immutable once written.

### 3. Enforce the Review State Machine

Every extracted field starts as `PENDING_REVIEW`. The only valid transitions are:

```
PENDING_REVIEW → REVIEWED       (patient confirms or edits)
PENDING_REVIEW → REJECTED       (patient rejects)
REVIEWED       → PENDING_REVIEW (patient un-reviews)
REJECTED       → PENDING_REVIEW (patient un-rejects)
```

No other transitions are valid. Validate transitions in `ValidationUtils`; reject invalid ones with an error, never silently ignore them.

### 4. Flag Low-Confidence Fields — Never Hide Them

Fields with confidence below the threshold are flagged visually and require review before the prescription is considered fully reviewed. They are **shown** to the patient, not suppressed. A field is never auto-promoted to `REVIEWED` based on confidence alone.

### 5. Never Invent Missing Data

If a field (dose, duration, diagnosis, allergy, medicine status) is absent from the document, record it as absent. Do not infer, default, or fabricate a value. This rule has no exceptions.

### 6. Process OCR in a WorkManager CoroutineWorker

OCR is a long-running, deferrable operation. It runs in an `OcrWorker : CoroutineWorker`. The worker:
- Sets `OcrStatus.PROCESSING` at start.
- Sets `OcrStatus.NEEDS_REVIEW` on success.
- Sets `OcrStatus.ERROR` on failure and surfaces a plain-language error to the patient.

Never trigger OCR from a ViewModel, Activity, or foreground coroutine.

### 7. Preserve the Original Document Always

The source document file is preserved regardless of OCR status or outcome. The patient can always view the original. OCR extraction never replaces or deletes the source.

### 8. Log Only Non-Sensitive Metadata

Log the `prescriptionId` and `OcrStatus` transition. Never log raw OCR text, extracted field values, medicine names, or document content.

---

## Patterns to Follow

- `OcrService` is an interface; inject it — never import a provider SDK in Android code.
- Every extracted field stores: `rawValue`, `patientValue`, `confidence` (0.0–1.0), `reviewState`.
- `rawValue` is immutable after initial write; only `patientValue` is updated by the patient.
- Fields below the confidence threshold are flagged and shown — never hidden or auto-approved.
- `PENDING_REVIEW` → `REVIEWED` requires explicit patient action.
- Use a fake `OcrService` in all unit and integration tests — no real network calls.
- Log only `prescriptionId`, status, and timestamps — never field content.
- Original source document is always accessible to the patient.

## Patterns to Avoid

- Directly calling a cloud OCR API from a ViewModel, Activity, or composable.
- Storing raw OCR text in plaintext `SharedPreferences`.
- Treating OCR output as verified without explicit patient review.
- Auto-populating dose, duration, or diagnosis fields from OCR output.
- Modifying `rawValue` after initial write.
- Marking a field as `REVIEWED` without an explicit patient action.
- Suppressing or hiding low-confidence fields.
- Making real OCR API calls in any test.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```kotlin
// ── OcrService interface (domain layer — no provider SDK import here) ─────────
interface OcrService {
    suspend fun extract(documentPath: String, mimeType: String): OcrResult
}

data class OcrResult(
    val fields: List<ExtractedField>,
    val rawText: String,          // stored securely, never logged
    val processingNotes: String?
)

data class ExtractedField(
    val fieldType: FieldType,
    val rawValue: String,         // immutable after creation
    val confidence: Float,        // 0.0–1.0
    val reviewState: ReviewState = ReviewState.PENDING_REVIEW
)

enum class ReviewState { PENDING_REVIEW, REVIEWED, REJECTED }
enum class FieldType  { MEDICINE_NAME, DOSE, DURATION, DOCTOR_NAME, ISSUED_DATE }


// ── Review state transition validation ───────────────────────────────────────
object ValidationUtils {
    fun validateReviewTransition(from: ReviewState, to: ReviewState): Boolean =
        when (from to to) {
            ReviewState.PENDING_REVIEW to ReviewState.REVIEWED       -> true
            ReviewState.PENDING_REVIEW to ReviewState.REJECTED       -> true
            ReviewState.REVIEWED       to ReviewState.PENDING_REVIEW -> true
            ReviewState.REJECTED       to ReviewState.PENDING_REVIEW -> true
            else -> false
        }
}


// ── OcrWorker skeleton ────────────────────────────────────────────────────────
class OcrWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ocrService: OcrService,           // interface, not provider SDK
    private val repository: PrescriptionRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prescriptionId = inputData.getString(KEY_PRESCRIPTION_ID)
            ?: return Result.failure()

        repository.updateOcrStatus(prescriptionId, OcrStatus.PROCESSING)

        return try {
            val prescription = repository.getById(prescriptionId)
                ?: return Result.failure()
            val ocrResult = ocrService.extract(prescription.documentPath, prescription.mimeType)
            repository.saveOcrResult(prescriptionId, ocrResult)
            repository.updateOcrStatus(prescriptionId, OcrStatus.NEEDS_REVIEW)
            // Log only non-sensitive metadata
            Log.i(TAG, "OCR complete: id=$prescriptionId status=NEEDS_REVIEW")
            Result.success()
        } catch (e: Exception) {
            repository.updateOcrStatus(prescriptionId, OcrStatus.ERROR)
            Log.e(TAG, "OCR failed: id=$prescriptionId")  // no content in log
            Result.failure()
        }
    }

    companion object {
        const val KEY_PRESCRIPTION_ID = "prescription_id"
        private const val TAG = "OcrWorker"
    }
}


// ── Fake OcrService for tests (no network calls) ──────────────────────────────
class FakeOcrService(
    private val result: OcrResult = syntheticOcrResult()
) : OcrService {
    override suspend fun extract(documentPath: String, mimeType: String): OcrResult = result
}

// All test fixtures use synthetic data — never real medicine names or patient names
fun syntheticOcrResult() = OcrResult(
    fields = listOf(
        ExtractedField(FieldType.MEDICINE_NAME, rawValue = "Synthamol 500mg", confidence = 0.92f),
        ExtractedField(FieldType.DOSE,          rawValue = "1 tablet",        confidence = 0.45f), // below threshold → flagged
        ExtractedField(FieldType.DURATION,      rawValue = "",                confidence = 0.0f),  // absent → recorded as absent
    ),
    rawText = "[synthetic-raw-text]",
    processingNotes = null
)
```

---

## Key Constraints (from Steering)

- OCR is fallible: every field shows confidence and review state to the patient.
- `rawValue` is immutable: editing writes to `patientValue` only.
- Never infer missing dose, duration, diagnosis, allergy, or medicine status.
- No field is `REVIEWED` without explicit patient action.
- Original document is always preserved and accessible.
- Never log prescription text or extracted field content.
- Use synthetic data in all tests; never use real patient data.
