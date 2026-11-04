# MediHistory AI — Backend Migration Investigation Report
**Date:** 2026-10-29  
**Scope:** Move logic from Android app to FastAPI backend where beneficial  
**User intent:** "move python side code where ever possible"

---

## Executive Summary

The Android app currently runs six categories of logic on-device: OCR extraction (ML Kit + Gemini), duplicate detection, AI agent features (F11–F16), post-review processing, history compilation, and validation/normalization utilities. The FastAPI backend already has the data models, CRUD endpoints, and partial stubs for every domain, but **none of the compute logic is implemented on the backend yet** — every backend "service" is a thin CRUD layer.

The most impactful migrations (highest value, lowest disruption) in priority order are:

1. **OCR extraction → backend Gemini Vision endpoint** (Area 1) — eliminates the APK API key, centralises OCR quality, reduces APK size.
2. **PostReviewProcessor → `POST /api/v1/prescriptions/{id}/process-review`** (Area 4) — single authoritative flow; Android becomes a thin sync client.
3. **Duplicate detection → backend service** (Area 2) — server has the full medicine dataset; no need to sync all medicines to device.
4. **HistoryCompiler → backend history service** (Area 5) — eliminates duplication; backend already has the `generate` endpoint stub.
5. **AI agents F11/F12/F15 → backend endpoints** (Area 3) — removes Gemini API key from APK.
6. **IngredientNormalizer + validation rules → Python equivalents** (Area 7) — enables server-side duplicate detection.

Agents F14 (OcrCorrectionEngine) and F16 (ReminderAdvisorEngine) should **stay on Android** — they are offline-first, require no network, and add no value on the server.

The Android app currently has **no HTTP networking layer** (no Retrofit, no Ktor client). The BACKEND_BASE_URL BuildConfig field exists but no API client is wired up. Every migration requires building the Android-to-backend HTTP client first.

---

## Area 1 — OCR Processing

### Current state
- **Android:** `OcrWorker.kt` (WorkManager) dispatches to `OcrService`. Two implementations exist:
  - `MlKitOcrService.kt` — on-device ML Kit Text Recognition v2; uses `PrescriptionFieldParser.kt` (600+ lines of spatial regex logic) to map raw ML Kit output to structured `ExtractedField` list.
  - `GeminiEnhancedOcrService.kt` — ML Kit for pixel→text, then Gemini 1.5 Flash for structured JSON extraction. Gated by `BuildConfig.ENABLE_GEMINI_OCR` (feature flag), `BuildConfig.GEMINI_API_KEY` (in APK), and patient consent (`SecurePreferences.KEY_GEMINI_OCR_CONSENT`).
- **Backend:** `POST /api/v1/patients/me/prescriptions/{id}/ocr` exists in `prescriptions.py` but `trigger_ocr()` in `prescription_service.py` **only sets `ocr_status = "PROCESSING"` and returns a `job_id`** — no actual OCR is performed. No field extraction, no Gemini Vision call, no `ExtractedField` equivalents are stored server-side. The backend `PrescriptionMedicine` model (`medicine.py`) has `raw_value`, `confidence_score`, `review_state`, and `source_reference` columns that are designed to receive OCR output but are never populated.

### What can move to backend
The entire `GeminiEnhancedOcrService` Gemini call can move server-side. The flow becomes:
1. Android uploads document → `POST /api/v1/patients/me/prescriptions` (already works, stores file).
2. Android calls `POST /api/v1/patients/me/prescriptions/{id}/ocr` (already works, returns job_id).
3. Backend calls Gemini Vision API (server env var `GEMINI_API_KEY`), stores extracted fields as `PrescriptionMedicine` rows with `review_state = "PENDING_REVIEW"`.
4. Android polls `GET /api/v1/patients/me/prescriptions/{id}` or uses a webhook/status endpoint to know when fields are ready.
5. Android fetches the pending-review medicines and presents the OCR review screen.

**Required new backend endpoint:** `GET /api/v1/patients/me/prescriptions/{id}/fields` (or embed in existing detail) to return all `PrescriptionMedicine` rows in PENDING_REVIEW state with their `raw_value` and `confidence_score`.

**Required new backend service:** `OcrService` in `backend/app/services/ocr_service.py` that calls Gemini Vision API (`google-generativeai` Python SDK, key from `GEMINI_API_KEY` env var), returns structured fields, and writes `PrescriptionMedicine` rows.

### What must stay on Android
- `MlKitOcrService` as offline fallback — if the user has no network, on-device ML Kit + `PrescriptionFieldParser` still runs.
- `OcrWorker` becomes a dispatcher: try backend first, fall back to on-device on network failure.
- Patient consent gate remains on device (`SecurePreferences.KEY_GEMINI_OCR_CONSENT`).

### Estimated effort: **High**
New `ocr_service.py`, update `trigger_ocr` to actually run extraction, new Android HTTP client, offline fallback logic, schema update for structured field storage, Alembic migration.

### Specific changes needed
- `backend/app/services/ocr_service.py` — Gemini Vision extraction logic.
- `backend/app/services/prescription_service.py` — `trigger_ocr()` calls `OcrService` async.
- `backend/app/routers/prescriptions.py` — `GET /{id}/fields` endpoint.
- `backend/app/schemas/prescription.py` — `OcrFieldsResponse` schema.
- Android: HTTP client (Retrofit/Ktor), `BackendOcrService` implementing `OcrService`, `OcrWorker` selects backend vs on-device based on connectivity and consent.
- Alembic migration: no schema change needed (`PrescriptionMedicine` already has all required columns).

---

## Area 2 — Duplicate Detection

### Current state
- **Android:** `DuplicateDetector.kt` — a pure Kotlin `object` (singleton). Deterministic algorithm: filters `MedicineStatus.CURRENT | UNKNOWN`, requires non-null `activeIngredient`, sorts by `Medicine.id`, then O(n²) pair comparison using `IngredientNormalizer.isSynonym()` (exact synonym map) and `splitIngredients()` (combination product partial match). Produces `DuplicateCandidate` list with `EXACT` (0.95 confidence) or `PARTIAL` (0.70 confidence) match types. Called from `PostReviewProcessor.kt` after medicines are inserted.
- **Backend:** `duplicate_service.py` + `duplicates.py` — **CRUD only**. `DuplicateService` has `list_alerts()`, `get_alert()`, and `mark_reviewed()`. There is **no detection logic** — no equivalent of `DuplicateDetector.detect()` or `IngredientNormalizer`. `DuplicateAlert` model has `matched_active_ingredient`, `match_type`, `match_confidence`, `formulation_notes` columns ready to receive detection output. The `DuplicateAlert` model has a 3rd match type `SYNONYM` not present in the Android enum.
- **Key gap:** Backend's `PrescriptionMedicine` has `active_ingredient` and `active_ingredient_slug` fields, but the Android `Medicine.kt` entity has `activeIngredient` as well. These are structurally equivalent but never synced.

### What can move to backend
All detection logic should move server-side. When `PostReviewProcessor` is migrated (Area 4), the backend naturally has access to the full medicine list for the patient and can run detection without any data sync to Android.

**Required new backend service:** `backend/app/services/ingredient_normalizer.py` — Python port of `IngredientNormalizer.kt` (the synonym table is small and explicit; direct translation). `backend/app/services/duplicate_detector.py` — Python port of `DuplicateDetector.detect()`.

**Required new backend endpoint:** Called internally by the process-review endpoint (Area 4), not a standalone REST call. The Android app reads detected alerts via the existing `GET /api/v1/patients/me/duplicates`.

### What must stay on Android
The Android `DuplicateDetector` and `IngredientNormalizer` must remain as offline fallback for the `PostReviewProcessor` on-device path (for users without connectivity during review). They can be deprecated once sync is established.

### Estimated effort: **Medium**
Python port of two Kotlin files, integration into the process-review flow. The algorithm is simple and well-tested.

### Specific changes needed
- `backend/app/services/ingredient_normalizer.py` — port of `IngredientNormalizer.kt`.
- `backend/app/services/duplicate_detector.py` — port of `DuplicateDetector.detect()`.
- `backend/app/services/prescription_service.py` — call duplicate detector after medicines are upserted.
- No new router endpoints needed (alerts already readable via existing duplicates router).

---

## Area 3 — AI Agent Features (F11–F16)

### Current state
- **Android:** All 6 agents run on-device. `GeminiService.kt` is a thin wrapper around the `google-generativeai` Kotlin SDK (`gemini-1.5-flash`, temperature 0.3, maxOutputTokens 512). The API key is `BuildConfig.GEMINI_API_KEY`.
  - `PrescriptionUnderstandingAgent.kt` (F11) — takes REVIEWED `ExtractedFieldEntity` + confirmed `Medicine` rows, generates one-sentence notes per medicine.
  - `PrepareForVisitAgent.kt` (F12) — takes confirmed medicines + open `DuplicateAlert` rows, generates 3–5 suggested questions.
  - `MedicineHistoryNarratorAgent.kt` (F15) — takes REVIEWED prescriptions + confirmed medicines/doctors, generates 3–5 sentence narrative.
  - `DuplicateExplainerEngine.kt` (F13) — **pure Kotlin, no Gemini** — deterministic template-based text generation. Takes `DuplicateAlert` + two `Medicine` entities.
  - `OcrCorrectionEngine.kt` (F14) — **pure Kotlin, no Gemini** — Levenshtein-based doctor name matching + date normalisation + medicine name cleaning.
  - `ReminderAdvisorEngine.kt` (F16) — **pure Kotlin, no Gemini** — pattern-matching on dosage instruction strings (OD/BD/TDS/QID, meal modifiers) → suggested `LocalTime` values.
- **Backend:** `history.py` has `POST /patients/me/history/generate` which creates a `MedicalHistorySummary` row in `GENERATING` state with a `job_id`. `history_service.py` creates the row but **never calls any AI model** — the background worker that would call Gemini is entirely absent ("out of scope" per the service comment).

### What can move to backend

**F11 (PrescriptionUnderstandingAgent) → High priority for migration.**  
New endpoint: `POST /api/v1/patients/me/prescriptions/{id}/understand`  
Reads REVIEWED medicines from DB, calls `google-generativeai` Python SDK, returns `{medicines: [{name, note}], disclaimer, is_ai_generated: true}`.

**F12 (PrepareForVisitAgent) → High priority for migration.**  
New endpoint: `POST /api/v1/patients/me/visit-summary`  
Reads current medicines + open duplicate alerts from DB, calls Gemini, returns `{current_medicines, open_alerts, suggested_questions, disclaimer}`.

**F15 (MedicineHistoryNarratorAgent) → Already stubbed on backend, complete it.**  
The `POST /patients/me/history/generate` + `HistoryService.generate()` path already exists. The background worker that calls Gemini just needs to be implemented. This replaces `MedicineHistoryNarratorAgent` on Android for the connected flow.

**F13 (DuplicateExplainerEngine) → Medium priority.**  
Pure template logic — easy Python port. Called internally by duplicate detector (Area 2). Stores explanation in `DuplicateAlert.formulation_notes` (or add an `explanation` column). Android reads it from the existing `GET /duplicates/{id}` endpoint.

### What must stay on Android

**F14 (OcrCorrectionEngine) — stays on device.** OCR review is the one flow that is explicitly offline-first. The correction engine operates on the local `ExtractedFieldEntity` rows at review time, before any sync. Its Levenshtein doctor-name fuzzy match uses the local `knownDoctors` list. No network value added.

**F16 (ReminderAdvisorEngine) — stays on device.** Reminders are local (no backend reminder execution), fully offline, and the pattern-matching is trivial. Moving it server-side adds latency with no benefit.

### Estimated effort
- F15 (complete history narrator): **Medium** (add background worker/task to call Gemini, update summary_json).
- F11 + F12 (new endpoints): **Medium** each — new router + service + Pydantic schema + Gemini call.
- F13 (DuplicateExplainerEngine Python port): **Low** — pure template logic, no Gemini.
- F14, F16: **Not applicable** (stay on Android).

### Specific changes needed
- `backend/app/services/gemini_service.py` — shared Gemini client (key from `GEMINI_API_KEY` env var).
- `backend/app/services/history_service.py` — implement the actual Gemini call in `generate()`.
- `backend/app/routers/prescriptions.py` — add `POST /{id}/understand` (F11).
- `backend/app/routers/history.py` — new `POST /patients/me/visit-summary` (F12), or a separate `visit_summary` router.
- `backend/app/services/duplicate_explainer_service.py` — Python port of `DuplicateExplainerEngine.kt` (F13).
- All AI responses must include disclaimer and `is_ai_generated: true` per product principles.

---

## Area 4 — PostReviewProcessor

### Current state
- **Android:** `PostReviewProcessor.kt` — the orchestrator that runs after the patient finishes OCR review. It:
  1. Reads all `ExtractedFieldEntity` rows where `reviewState == REVIEWED` and `patientValue != null`.
  2. Looks up or creates a `Doctor` row from `doctor_name` field.
  3. Parses `prescription_date` (7 date formats supported).
  4. Updates `Prescription.doctorId` and `prescriptionDate`.
  5. Creates `Medicine` rows (one per `medicine_name*` field, with dose/frequency from matched suffixed fields).
  6. Runs `DuplicateDetector.detect()` on all patient medicines.
  7. Inserts new `DuplicateAlert` rows for previously unseen pairs.
  8. Calls `DuplicateExplainerEngine.explain()` and stores explanation.
- **Backend:** No equivalent endpoint exists. The backend has all the data models and CRUD services needed but no single "process review" orchestration endpoint.

### What should move to backend

This is the **highest-value single migration**. It converts Android from a data-owner to a sync client.

**New endpoint:** `POST /api/v1/patients/me/prescriptions/{id}/process-review`

**Request body:**
```json
{
  "reviewed_fields": [
    {
      "field_name": "doctor_name",
      "patient_value": "Dr. Synthetic",
      "raw_value": "Dr Synthetic",
      "confidence": 0.85,
      "review_state": "REVIEWED",
      "source_reference": "page:1,block:0"
    }
  ]
}
```

**Response:** Updated `PrescriptionDetail` including newly created medicines and any new duplicate alert IDs.

**New backend service:** `backend/app/services/review_processor_service.py` — Python port of `PostReviewProcessor` logic:
- Find-or-create doctor by name (case-insensitive, patient-scoped).
- Parse date with the same 7 formats.
- Update prescription row.
- Insert `PrescriptionMedicine` rows.
- Run duplicate detector (Area 2).
- Run duplicate explainer (Area 3 / F13).
- Return full updated prescription.

### What must stay on Android
The `OcrReviewViewModel` and `OcrReviewScreen` remain entirely on Android — this is the patient interaction layer. The Android side calls `POST /process-review` when the patient taps "Submit Review", rather than writing directly to Room. Room is then updated from the API response so the offline cache reflects the confirmed state.

For offline fallback: the existing `PostReviewProcessor.kt` path runs when the backend is unreachable. Confirmed data is queued and synced when connectivity returns (requires a sync queue — see Area 8).

### Estimated effort: **High**
New service, new router, new schemas, Android HTTP call, offline fallback + sync queue design.

---

## Area 5 — HistoryCompiler

### Current state
- **Android:** `HistoryCompiler.kt` — pure Kotlin `object`, deterministic, no AI. Compiles 4 sections from REVIEWED prescriptions: `MEDICINES` (active medicines list), `DOCTORS` (doctors list), `TIMELINE` (chronological prescription list), `AI_SUMMARY` (stub). Data is read from Room; same input always produces same output.
- **Backend:** `history_service.py` + `history.py`:
  - `POST /patients/me/history/generate` — creates a `MedicalHistorySummary` row in `GENERATING` state. Returns `summary_id`, `status`, `job_id`.
  - `GET /patients/me/history/{summary_id}` — returns the stored `summary_json` parsed into `SummaryContent`.
  - `MedicalHistorySummary` model has `summary_json` (TEXT) but it is **never populated** — the generation worker is absent.
  - `SummaryContent` Pydantic schema exists in `backend/app/schemas/history.py` but is not verified.

### What can move to backend

**Full HistoryCompiler logic** should move to a `history_compiler_service.py`:
- Reads REVIEWED prescriptions, linked medicines, and doctors from PostgreSQL.
- Produces the same 4-section structure as the Kotlin version.
- Serialises to `summary_json` and updates the `MedicalHistorySummary` row from `GENERATING` to `COMPLETED`.
- Calls `GeminiService` to produce the AI_SUMMARY section (F15, from Area 3).

This would make `POST /patients/me/history/generate` fully functional end-to-end.

Android reads the compiled history via `GET /patients/me/history/{id}` and displays it. The Android `HistoryCompiler.kt` becomes the offline fallback only.

### What must stay on Android
`HistoryCompiler.kt` as offline fallback. `HistorySection`, `HistoryEntry`, `SourceReference` data classes remain since the UI uses them directly.

### Estimated effort: **Medium**
Python implementation of the deterministic compilation + wiring into the existing `generate` endpoint.

### Specific changes needed
- `backend/app/services/history_compiler_service.py` — port `HistoryCompiler.kt` logic.
- `backend/app/services/history_service.py` — `generate()` calls the compiler then writes `summary_json`.
- `backend/app/schemas/history.py` — verify `SummaryContent` matches the Kotlin `HistorySection` structure.
- No new router endpoints needed.

---

## Area 6 — DuplicateExplainerEngine, OcrCorrectionEngine, ReminderAdvisorEngine

### Current state
All three are pure Kotlin engines with no Android UI dependency.

| Engine | Location | Uses Gemini? | Uses Room? |
|---|---|---|---|
| `DuplicateExplainerEngine.kt` | `data/duplicate/` | No | No |
| `OcrCorrectionEngine.kt` | `data/ocr/` | No | No (doctors passed in) |
| `ReminderAdvisorEngine.kt` | `data/reminders/` | No | No |

### Migration recommendation

**DuplicateExplainerEngine → move to backend.** It generates the plain-language explanation that is stored as `DuplicateAlert.formulation_notes` (or a new `explanation` column). Called internally by `duplicate_detector.py`. The template strings are trivial to port to Python.

**OcrCorrectionEngine → stays on Android.** It is only used during the OCR review flow (`OcrReviewViewModel` suggests corrections for low-confidence fields). The review flow is always on-device (offline-first, real-time user interaction). The Levenshtein doctor name matching uses the local `knownDoctors` list; adding a round-trip to the backend for each keystroke adds latency with no benefit.

**ReminderAdvisorEngine → stays on Android.** Reminder scheduling is entirely local. The engine is a pure pattern-matcher operating on a single `dosageInstructions` string. No server context is needed or useful.

### Estimated effort
- DuplicateExplainerEngine Python port: **Low** (pure template strings).
- OcrCorrectionEngine, ReminderAdvisorEngine: not applicable.

---

## Area 7 — Business Rules and Validation

### Current state
- **Android:** `ValidationUtils` is referenced throughout specs and steering docs and enforced in the `build.gradle.kts` JaCoCo gate, but **`ValidationUtils.kt` does not exist as a file** — it is specified but not yet implemented. The review state machine (`ReviewState.kt`) is defined; the transitions enforced in `ValidationUtils` are described in `.kiro/steering/medical-safety.md` and the OCR skill. `IngredientNormalizer.kt` exists and is fully implemented.
- **Backend:** No equivalent validation utility exists. `PrescriptionMedicine.review_state` is stored as a raw String with no state machine enforcement. The `UpdateMedicineRequest` schema in `medicine_service.py` accepts any `review_state` string.

### What should move to backend
Both ValidationUtils (review state machine) and IngredientNormalizer (synonym table) must be implemented in Python since they are needed server-side:

- **`backend/app/services/validation_utils.py`** — `validate_review_transition(from_state, to_state) -> bool`. Same 4 valid transitions as the Android spec. Applied in `medicine_service.py` `update()` before writing `review_state`.
- **`backend/app/services/ingredient_normalizer.py`** — Python port of `IngredientNormalizer.kt` (already planned in Area 2).

The Android side still needs `ValidationUtils.kt` implemented (it is a blocker for the CI coverage gate); the backend needs its own Python equivalent because the state machine must be enforced server-side to prevent clients from making invalid transitions.

### Estimated effort: **Low**
Both are small, table-driven. `IngredientNormalizer` is a 60-line synonym map. `ValidationUtils` state machine is 4 valid pairs.

---

## Area 8 — Data Sync

### Current state
- **Android:** The app writes **exclusively to Room** (local SQLite). There is no HTTP networking layer — `BACKEND_BASE_URL` BuildConfig field exists and is set from `local.properties` / CI env var, but no Retrofit/Ktor client is wired up anywhere in the Kotlin source. `AuthRepositoryImpl` uses Firebase Authentication; no backend JWT is obtained or stored.
- **Backend:** The FastAPI backend uses its own PostgreSQL database (`DATABASE_URL` env var, SQLAlchemy async). It has a complete JWT auth system (`auth.py`, `security.py`), patient-scoped CRUD for all entities, and a separate `refresh_token` table. There is **no concept of a sync protocol** — no `last_modified`, no device ID, no conflict resolution strategy.

### Key observation: two disjoint systems
Currently the Android Room DB and the FastAPI PostgreSQL DB are completely separate. The Android app uses Firebase Auth; the backend has its own JWT auth. There is no bridge. A patient logging into the app never calls the FastAPI backend.

### What is needed

1. **Android HTTP client** — Retrofit (or Ktor-client) module with JWT interceptor. This is a prerequisite for every migration area above.

2. **Auth bridge** — After Firebase sign-in, the Android app should obtain a FastAPI JWT. Options:
   - Firebase Custom Token → backend verifies Firebase JWT and issues its own JWT (lowest disruption to the backend auth system).
   - Or migrate Android auth to use the backend's own email/password auth (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`). The backend already has this.

3. **Sync strategy** — Given the offline-first product requirement:
   - **Write-through:** Android writes to Room first (offline), then replicates to backend when online. A `pendingSyncAt` timestamp column per entity marks dirty records.
   - **Backend-authoritative** (simpler, preferred for migrated flows like OCR and PostReviewProcessor): Android calls backend endpoints, stores the response in Room as the cache. On network failure, queues the call for retry via `WorkManager`.

4. **Conflict resolution** — The simplest strategy for MVP: last-write-wins with server timestamp. Room entities should gain a `serverUpdatedAt: Long?` nullable column; the sync worker skips records where `serverUpdatedAt >= localUpdatedAt`.

### Estimated effort: **High**
This is the foundational prerequisite for all other migrations. It needs careful design before implementation.

### Specific changes needed
- Android: `data/network/` — Retrofit interface, OkHttp client with JWT interceptor and refresh logic.
- Android: `data/network/AuthInterceptor.kt` — reads JWT from `EncryptedSharedPreferences`, injects as `Authorization: Bearer`.
- Android: `data/network/SyncWorker.kt` — WorkManager periodic/connectivity-triggered sync.
- Android: `data/di/NetworkModule.kt` — Hilt bindings for Retrofit and all API service interfaces.
- Backend: No changes required for the sync protocol — the existing REST endpoints are sufficient.
- Backend: Consider adding `GET /api/v1/patients/me/sync?since=<iso_timestamp>` for efficient delta sync.

---

## Prioritised Migration Plan

Ordered by: value delivered ÷ implementation risk, respecting offline-first constraint.

| # | Migration | Value | Risk | Effort | Prerequisite |
|---|---|---|---|---|---|
| 1 | **Android HTTP client + auth bridge** | Foundational | Medium | High | None |
| 2 | **IngredientNormalizer + ValidationUtils Python ports** | Enables #3 | Low | Low | None |
| 3 | **DuplicateExplainerEngine Python port** | Enables #4 | Low | Low | #2 |
| 4 | **Duplicate detection server-side** (`duplicate_detector.py`) | High — centralised, no device sync needed | Low | Medium | #2 |
| 5 | **PostReviewProcessor → `POST /process-review`** | Very High — single authoritative flow | High | High | #1, #4 |
| 6 | **HistoryCompiler → `history_compiler_service.py`** | High — completes the existing generate stub | Medium | Medium | #1, #4 |
| 7 | **F15 MedicineHistoryNarratorAgent → backend** | High — removes Gemini key from APK | Low | Medium | #6 |
| 8 | **F11 PrescriptionUnderstandingAgent → backend** | Medium | Low | Medium | #1 |
| 9 | **F12 PrepareForVisitAgent → backend** | Medium | Low | Medium | #1 |
| 10 | **OCR → backend Gemini Vision** | High — removes OCR key from APK, improves quality | High | High | #1, #5 |

### Migrations that should NOT move to backend (stay on Android)
- `MlKitOcrService` + `PrescriptionFieldParser` — offline OCR fallback, required for offline-first.
- `OcrCorrectionEngine` (F14) — real-time review flow, offline.
- `ReminderAdvisorEngine` (F16) — local reminder scheduling, no server value.
- `OcrReviewScreen`, `OcrReviewViewModel` — patient interaction, always on-device.
- `HistoryCompiler` — kept as offline fallback after server migration.
- `DuplicateDetector`, `IngredientNormalizer` — kept as offline fallback for PostReviewProcessor.

---

## Evidence Index

| File | Key finding |
|---|---|
| `android/.../OcrWorker.kt` | WorkManager job; ML Kit + optional Gemini; GEMINI_API_KEY in APK |
| `android/.../GeminiEnhancedOcrService.kt` | Gemini call with API key from BuildConfig; patient consent gated |
| `android/.../MlKitOcrService.kt` | On-device ML Kit; `extractRawText()` feeds Gemini pipeline |
| `android/.../PrescriptionFieldParser.kt` | 600-line spatial OCR parser; runs on-device only |
| `android/.../PostReviewProcessor.kt` | Doctor/medicine/duplicate orchestrator; writes to Room only |
| `android/.../DuplicateDetector.kt` | Pure Kotlin detection algorithm; `IngredientNormalizer.isSynonym()` |
| `android/.../IngredientNormalizer.kt` | 60-line synonym map; pure function; easy to port |
| `android/.../HistoryCompiler.kt` | Pure Kotlin deterministic compiler; 4 sections |
| `android/.../DuplicateExplainerEngine.kt` | Template-based; no Gemini; trivial Python port |
| `android/.../OcrCorrectionEngine.kt` | Levenshtein + date normalisation; offline-only use |
| `android/.../ReminderAdvisorEngine.kt` | Pattern matching; local only; no server value |
| `android/.../GeminiService.kt` | Thin wrapper; `GEMINI_API_KEY` from BuildConfig |
| `android/.../PrescriptionUnderstandingAgent.kt` | F11; Gemini prompt; confirmed data only |
| `android/.../PrepareForVisitAgent.kt` | F12; Gemini prompt; confirmed data only |
| `android/.../MedicineHistoryNarratorAgent.kt` | F15; Gemini prompt; confirmed data only |
| `android/.../AuthRepositoryImpl.kt` | Firebase Auth; no FastAPI JWT obtained |
| `android/app/build.gradle.kts` | `BACKEND_BASE_URL` BuildConfig exists; `GEMINI_API_KEY` in APK |
| `backend/app/services/prescription_service.py` | `trigger_ocr()` sets status only; no actual OCR |
| `backend/app/services/duplicate_service.py` | CRUD only; no detection logic |
| `backend/app/services/history_service.py` | Creates row in GENERATING state; no Gemini call |
| `backend/app/routers/prescriptions.py` | Upload + OCR trigger endpoints exist; no fields endpoint |
| `backend/app/routers/duplicates.py` | List/get/mark-reviewed; no detection endpoint |
| `backend/app/routers/history.py` | generate/list/get; worker absent |
| `backend/app/models/medicine.py` | Has `raw_value`, `confidence_score`, `review_state`, `source_reference` ready |
| `backend/app/models/duplicate_alert.py` | Has `matched_active_ingredient`, `match_type`, `match_confidence`; no `explanation` column yet |
| `backend/app/main.py` | All 10 routers registered; no OCR service registered |
| `backend/app/core/config.py` | `GEMINI_API_KEY` not present — needs adding as env var |
