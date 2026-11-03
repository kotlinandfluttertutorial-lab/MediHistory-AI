# Final Gap Scan — MediHistory AI (Post-Migration)
**Date:** 2026-10-29
**Scope:** Android app + FastAPI backend, after F01–F16 implementation and the Python-side migration
**Investigator:** Kiro automated investigation step (read-only)
**Report path:** `.agents/tasks/final-gap-scan-2026-10-29.md`

---

## Summary Answer

The MVP features and AI agents are functionally complete on-device, and the Python-side migration is real and tested — `review_processor_service.py`, `duplicate_engine.py`, `history_compiler.py`, `ingredient_normalizer.py`, and the `/ai/*` agent endpoints are genuine, working implementations, not stubs. **The single dominant gap is that the Android app and the FastAPI backend are two disjoint systems with no connection between them.** There is no HTTP client in the Android app, no shared authentication, no data sync, and no production deployment path. The backend is a fully-built, well-tested island that no real Android user can reach. Every other gap (cloud backup, server-side OCR, production deploy) is downstream of this one missing bridge.

Concretely:
- Android authenticates with **Firebase Auth** and stores the Firebase UID as `patient_id`; the backend has its **own JWT + bcrypt + PostgreSQL** auth. There is no token exchange, and nothing in production code ever writes the backend `access_token` that `SessionManager.isSessionActive()` checks for.
- The Android app has **no Retrofit, OkHttp, or Ktor dependency** anywhere. `BuildConfig.BACKEND_BASE_URL` exists but is read only by a non-null test.
- The backend OCR endpoint `trigger_ocr` only flips `ocr_status` to `PROCESSING`; **no actual OCR runs server-side**. OCR is fully on-device (`OcrWorker` → `MlKitOcrService`/`GeminiEnhancedOcrService`), with the Gemini API key embedded in the APK.
- The migrated backend endpoints (`/process-review`, `/ai/understand-prescription`, `/ai/narrate-history`, `/ai/prepare-visit`) have **no Android caller** — the Android `PostReviewProcessor`, `ImportViewModel`, and `OcrReviewViewModel` all operate locally on Room.
- There is **no Dockerfile, no docker-compose, no deploy workflow**, no `.env.example`, and `requirements.txt` ships the SQLite driver (`aiosqlite`) — not the PostgreSQL driver (`asyncpg`) that `architecture.md` specifies.

---

## 1. Android ↔ Backend Sync Gap — CRITICAL (disconnected)

**Finding:** The Android app and the backend use two unrelated authentication systems and share no data path.

**Evidence:**
- `android/.../data/auth/AuthRepositoryImpl.kt` — uses `firebaseAuth.signInWithEmailAndPassword()` / `createUserWithEmailAndPassword()` (Firebase). The `AuthRepository` interface comment (line ~30) explicitly states the email is "Used only for display — never passed to backend as an auth credential." There is no backend credential exchange.
- `android/.../MainActivity.kt` lines 70–71 and `ui/auth/LoginViewModel.kt` lines 134–135:
  ```kotlin
  val uid = authRepository.currentUserId()   // Firebase UID
  sessionManager.setPatientId(uid)            // stored as the Room patient_id
  ```
  The Firebase UID becomes the local `patient_id`. It is never sent to the backend.
- `android/.../data/session/SessionManager.kt` lines 39–50 — `isSessionActive()` checks `KEY_ACCESS_TOKEN` in `SecurePreferences`, but a grep shows `KEY_ACCESS_TOKEN` is **only ever written by test code** (`SecurePreferencesTest.kt`, `SessionManagerTest.kt`). No production code path obtains or stores a backend JWT. So `isSessionActive()` is effectively always false in production — the session model is half-built, expecting a backend JWT that is never fetched.
- `backend/app/routers/auth.py` — `POST /auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout` implement a self-contained JWT (HS256) + bcrypt + refresh-token-table system. `backend/app/services/auth_service.py` confirms bcrypt hashing and a `RefreshToken` model. There is **no Firebase-token-verification endpoint** anywhere in `backend/app/routers/`.
- **No HTTP client in Android:** grep across all `*.kt` for `Retrofit|OkHttp|HttpURLConnection|api/v1` returns only a comment in `ConsentManagementScreen.kt` and the `BACKEND_BASE_URL` test. The Android `data/` layer has directories `ai, auth, dao, database, di, duplicate, history, model, ocr, privacy, reminders, repository, security, session` — **no `remote/` or `network/` directory**, despite `architecture.md` line 81 documenting a `remote/` package for "Retrofit/Ktor services, DTOs".
- `android/gradle/libs.versions.toml` — has `firebase-bom`, `firebase-auth`, `firebase-analytics`, but **no `retrofit`, `okhttp`, or `ktor`** entries. `BACKEND_BASE_URL` is declared in `app/build.gradle.kts` line 95 but consumed only by `EnvironmentConfigTest.kt`.

**Conclusion:** A patient logging into the app authenticates against Firebase and works entirely against the local Room database. They never reach the FastAPI backend. The backend's 97 passing tests validate logic that no client can invoke.

---

## 2. Backend OCR Endpoint — stub only, no real OCR

**Finding:** `trigger_ocr` performs no OCR; OCR runs fully on-device with the API key in the APK.

**Evidence:**
- `backend/app/services/prescription_service.py`, method `trigger_ocr` (final method): executes `select(Prescription)`, sets `prescription.ocr_status = "PROCESSING"`, commits, and returns `{"prescription_id", "ocr_status": "PROCESSING", "job_id"}`. **There is no Gemini Vision call, no field extraction, and no `PrescriptionMedicine`/extracted-field rows written.** The `job_id` is a freshly generated UUID that nothing ever consumes.
- `backend/app/routers/prescriptions.py` — `POST /{prescription_id}/ocr` (HTTP 202) just delegates to `service.trigger_ocr()`. There is no `GET /{id}/fields` endpoint to retrieve extracted fields (the migration plan noted this need; it was not built).
- Android OCR is fully on-device: `android/.../worker/OcrWorker.kt` calls `ocrService.extract(documentPath, mimeType, prescriptionId)` and writes `ExtractedFieldEntity` rows to Room via `extractedFieldDao.insertAll()`. The `ocrService` is `MlKitOcrService` (on-device ML Kit) or `GeminiEnhancedOcrService` (Gemini 1.5 Flash), gated by `BuildConfig.ENABLE_GEMINI_OCR`.
- `android/.../data/ai/GeminiService.kt` and `GeminiEnhancedOcrService` read the key from `BuildConfig.GEMINI_API_KEY` (`app/build.gradle.kts` line 98). This embeds the Gemini key in the APK — which `security-threat-model.md` NFR-019 / AC-059 flags as a violation ("No API keys in APK or git"). The backend migration was intended to remove this; the migration built the server-side endpoints but the Android app still calls Gemini directly, so the key remains in the APK.

**What would be needed:** A backend `OcrService` calling Gemini Vision (server env var `GEMINI_API_KEY`), a `GET /{id}/fields` endpoint, and the Android `OcrWorker` becoming a dispatcher (backend-first, on-device fallback) — per the migration plan, all gated on the missing HTTP client.

---

## 3. Backend Missing Features — two intentional stubs + one dependency gap

**Finding:** Each router was read. Two stubs are intentional MVP scope; the AI endpoints have an undeclared dependency; everything else is implemented.

**Evidence (per router):**
- `export.py` — all 3 endpoints (`POST /pdf`, `GET /{export_id}`, `GET /{export_id}/download`) return `raise_http_error(501, "NOT_IMPLEMENTED", ...)`. Intentional: "PDF export is not available in MVP." PDF generation is done client-side by Android's `PdfGenerator.kt` (New Doctor Mode, F07).
- `interactions.py` — `POST /check` returns a safe `{"checked": false, "reason": "unavailable", ...}` when `ENABLE_INTERACTION_CHECK` is false (default), or a stub "not yet integrated" response when true. Intentional F09 scope deferral.
- `ai_agents.py` — `/understand-prescription` (F11), `/narrate-history` (F15), `/prepare-visit` (F12) are **fully implemented** with prompts, parsing, and fallbacks. **However** `_generate()` does `import google.generativeai as genai` inside the function. `requirements.txt` does **not** list `google-generativeai` (grep for `google|generativeai|gemini|genai` returns nothing). On a fresh deploy the import fails, the `except` swallows it, and endpoints silently degrade to fallback text. The package must be added to `requirements.txt`.
- `prescriptions.py` — list/upload/get/document/update/delete/ocr/process-review/update-medicine all implemented. `process-review` delegates to the real `ReviewProcessorService`.
- `auth.py`, `patients.py`, `doctors.py`, `medicines.py`, `duplicates.py`, `history.py`, `reminders.py` — all implemented with patient-scoped CRUD; `history.generate` now compiles deterministic sections and persists `summary_json` (see §4).

---

## 4. Android Missing Backend Calls — migrated endpoints have no client

**Finding:** The Android import/OCR-review/post-review flows operate purely locally on Room. The migrated backend equivalents are unreached.

**Evidence:**
- `android/.../data/ocr/PostReviewProcessor.kt` — the on-device orchestrator. It reads `ExtractedFieldEntity` (REVIEWED), find-or-creates a `Doctor` in Room, parses the date, inserts `Medicine` rows, runs `DuplicateDetector.detect()`, inserts `DuplicateAlert` rows, and calls `DuplicateExplainerEngine.explain()`. **All writes are to the local Room DB** (`prescriptionDao`, `doctorRepository`, `medicineRepository`, `duplicateAlertRepository`). There is no backend call.
- `backend/app/services/review_processor_service.py` — a complete Python port of the same logic (`_find_or_create_doctor`, date parsing, medicine creation, `_run_duplicate_detection` via `duplicate_engine.detect`, status → COMPLETED). It is reachable via `POST /api/v1/patients/me/prescriptions/{id}/process-review` — but no Android code calls it.
- `ImportViewModel`, `OcrReviewViewModel` — operate against Room and the on-device `OcrService`/`PostReviewProcessor`. Grep for `process-review|understand-prescription|narrate-history` in `*.kt` returns nothing.
- **Result:** There are now two parallel, correct implementations of post-review processing and duplicate detection — one in Kotlin (used) and one in Python (unused). The Gemini-based agents similarly run twice: `GeminiService.kt` (Android, used) and `ai_agents.py` (backend, unused).

**Implication:** The migration delivered backend parity but did not rewire the app to use it. Until the Android HTTP client exists, the migration's value (centralised logic, no API key in APK, cross-device sync) is unrealised.

---

## 5. Database Sync — designed in docs, absent in code

**Finding:** Room (device) and the backend DB are entirely separate. A sync strategy is specified in `architecture.md` but none of it is implemented.

**Evidence:**
- `docs/architecture/architecture.md` §6 "Offline-first strategy" (lines 261–274) specifies: write-to-Room-first → dirty flag → `SyncWorker` pushes dirty entities to backend → on success clears flag and stores server-assigned ID → on conflict server returns 409 → conflict stored for patient resolution. It also documents `OcrWorker`, `SyncWorker`, `ExportWorker` (line 82/118).
- **Implementation:** only `OcrWorker.kt` exists under `android/.../worker/`. There is **no `SyncWorker.kt`**, no `ExportWorker.kt`. The Android `data/` layer has no `remote/` package. Room entities have no `dirty`/`pendingSyncAt`/`serverUpdatedAt` columns (grep for these in `*.kt` returns nothing). The backend models (`backend/app/models/*.py`) have no `last_modified`/`device_id`/sync columns either. No `GET /sync?since=` delta endpoint exists.
- `README.md` line 43 describes the backend DB as "PostgreSQL (optional sync; local-only mode supported)" — but no sync code connects the two.
- No dedicated sync spec exists beyond `architecture.md` §6 prose; `docs/architecture/` has no `sync.md` or `data-sync.md` file.

**Conclusion:** The architecture is "local-only mode" in practice. Sync is a documented future state, not a present one.

---

## 6. Cloud Backup (F10) — not implemented

**Finding:** F10 ships local JSON export + two-step deletion. There is no cloud backup upload, provider, or opt-in flow beyond a feature flag and explanatory text.

**Evidence:**
- `android/.../ui/privacy/PrivacyViewModel.kt` — `onExportClicked` calls `DataExporter.export(...)` writing to app-private cache (shared via FileProvider). `onDeleteStep2Confirmed` calls `DataDeletion.deleteAllData(...)` (local Room + secure prefs). Both are device-local.
- `PrivacyViewModel.CLOUD_BACKUP_RETENTION_EXPLANATION` (companion const) is static text explaining that cloud copies "may remain on the cloud server." It is shown in the deletion dialog — but no cloud backup actually runs.
- `app/build.gradle.kts` line 96 declares `ENABLE_CLOUD_BACKUP` (defaults `false`), read from `local.properties`/env. Grep shows `ENABLE_CLOUD_BACKUP` is referenced **only by `EnvironmentConfigTest.kt`** (asserts it defaults false). No production logic reads it.
- `AndroidManifest.xml` line 10: `android:allowBackup="false"` — Android auto-back is disabled (correct for a medical app, but confirms no backup agent exists). No `<service>` or backup agent is declared.
- `backend` — grep for `backup` across routers/services returns nothing. There is no backup/upload/restore endpoint.
- `docs/architecture/security-threat-model.md` §6 lists "Backup cloud provider not selected — encryption approach to be validated" as **High severity, Open — required before enabling cloud backup.**

**Conclusion:** Cloud backup is opt-in *by design* and deferred. F10's implemented portion is local export/deletion + consent/retention messaging; the actual cloud backup is unbuilt and explicitly gated by an open threat-model item.

---

## 7. Push Notifications — none (acceptable for MVP)

**Finding:** No FCM/push infrastructure. Reminders are purely local `AlarmManager`/`WorkManager`.

**Evidence:**
- `libs.versions.toml` grep for `messaging|firebase-messaging` returns nothing — no FCM dependency.
- `AndroidManifest.xml` grep for `service|receiver|FCM|messaging` returns only `FileProvider` and `InitializationProvider`; no `FirebaseMessagingService`.
- `reminders/` exists under `android/.../data/` (local `ReminderAdvisorEngine`, F16, on-device).
- This is consistent with the offline-first MVP; push notifications are not in scope. Noted for completeness.

---

## 8. Production Readiness — no deployment path

**Finding:** There is no containerisation, deploy workflow, or production DB driver. CI runs tests only.

**Evidence:**
- `.github/workflows/` contains `android-ci.yml`, `backend-ci.yml`, `pr-checks.yml` — all lint/test/coverage gates. **No deploy/release workflow.** `backend-ci.yml` runs `pytest --cov-fail-under=90` with `DATABASE_URL: sqlite+aiosqlite:///:memory:` and a CI-only JWT secret.
- **No Dockerfile / docker-compose / Procfile** anywhere in the repo (recursive search returned nothing).
- `backend/requirements.txt` — pinned deps include `aiosqlite==0.20.0` but **no `asyncpg`/`psycopg`**. `backend/app/core/config.py` defaults `DATABASE_URL` to `sqlite+aiosqlite:///./medihistory.db`. `architecture.md` §3.6 specifies "PostgreSQL with async SQLAlchemy and asyncpg driver" — so the documented production DB driver is not installed.
- No `.env.example` in `backend/`; only `alembic.ini` and `pyproject.toml`. `UPLOAD_DIR` defaults to `./uploads` (local filesystem).

**Conclusion:** The backend runs locally (`uvicorn`) against SQLite. Reaching production needs a container, the Postgres driver, an env template, and a deploy pipeline — none of which exist.

---

## 9. Security Gaps

**Findings (mix of confirmed-good and open):**

- **No hardcoded secrets (GOOD):** `backend/app/core/config.py` reads `JWT_SECRET_KEY` via `os.environ["JWT_SECRET_KEY"]` — raises `KeyError` at import if absent, so the server refuses to start misconfigured. `backend/app/core/security.py` and `auth_service.py` read the same env var. `ai_agents.py` reads `GEMINI_API_KEY` from `os.environ`. No secret literals found in these files.
- **JWT is HS256 (OPEN):** `security.py` line `_ALGORITHM = "HS256"`; `auth_service.py` same. `security-threat-model.md` §4.1/§6 flags "JWT algorithm is HS256 — RS256 preferred for production" as Medium/Open.
- **No HTTPS enforcement (OPEN):** `main.py` has no `TrustedHostMiddleware`, no HSTS, no HTTPS redirect. Relies on a reverse proxy that is not configured (no deployment exists).
- **Rate limiter is in-process (OPEN):** `backend/app/middleware/rate_limit.py` uses a per-instance `dict`; the file's own docstring says "Replace this in-process dict with a Redis-backed limiter before deploying at scale. The in-process store does not share state across multiple worker processes." Multiple uvicorn workers would each track separately.
- **Gemini API key in APK (OPEN — HIGH):** `app/build.gradle.kts` line 98 embeds `GEMINI_API_KEY` into the APK; `GeminiService.kt`/`GeminiEnhancedOcrService` read it directly. This violates threat-model NFR-019/AC-059 ("No API keys in APK or git"). The backend migration created server-side endpoints precisely to avoid this, but the app still calls Gemini directly, so the key remains in the APK.
- **Room DB unencrypted (OPEN — HIGH):** `security-threat-model.md` §6: "Room database is unencrypted until AD-001 is resolved" — High, blocks production launch. SQLCipher evaluation is a steering-file prerequisite that is not done.
- **Undeclared backend Gemini dependency (OPEN):** `ai_agents.py` imports `google.generativeai`, which is absent from `requirements.txt` (§3). Deployed as-is, the AI endpoints silently return fallback text.
- **No production pen-test / legal review (OPEN — blocks launch):** threat model §6 lists pen-test and India DPDP Act review as High/Open, release-blocking.

---

## 10. Highest-Value Next Step

**Build the Android ↔ backend integration layer: an HTTP client (Retrofit or Ktor) with an auth bridge and a sync worker.**

Rationale: Every other open item is downstream of the app being unable to reach the backend.
- Server-side OCR (§2), calling `/process-review` (§4), the AI agent endpoints (§3), and any future cloud backup (§6) all require an Android network client that does not exist.
- Removing the Gemini key from the APK (the highest-severity security item, §9) is only possible once OCR/AI run server-side, which requires the HTTP client.
- Cross-device sync (§5) and production deployment (§8) presuppose a connected client.

This is also the work the migration plan flagged as #1 ("Android HTTP client + auth bridge — Foundational, High effort") and that `architecture.md` §6 prescribes (dirty-flag + `SyncWorker` + 409 conflict). The auth bridge has two viable options, and I recommend the second:

1. **Firebase Custom Token → backend verifies the Firebase ID token and issues its own JWT.** Lowest disruption to the existing Android Firebase flow, but adds a backend verification endpoint and a Firebase Admin SDK dependency.
2. **Migrate Android auth to the backend's own email/password system (`POST /auth/register`, `/auth/login`) and drop Firebase Auth.** The backend already has bcrypt + JWT + refresh tokens fully implemented and tested. This removes a third-party dependency, makes the `SessionManager.KEY_ACCESS_TOKEN` path finally real (it's currently dead in production), and unifies `patient_id` (Firebase UID → backend UUID) so the two DBs can actually sync. Trade-off: the Android login UI must call the backend instead of Firebase; the existing Firebase-only `AuthRepositoryImpl` is replaced.

Recommendation: pursue option 2 — it eliminates the disjoint-auth problem at its root rather than bridging two systems, and the backend auth it needs is already built and covered by tests. Sequence the work as: (a) add Retrofit/Ktor + Hilt `NetworkModule` + `AuthInterceptor`; (b) rewire `AuthRepository` to the backend JWT endpoints and persist the token in `EncryptedSharedPreferences` so `SessionManager.isSessionActive()` becomes true; (c) add a `SyncWorker` (connectivity-constrained) that pushes dirty Room entities to the backend and stores server-assigned IDs; (d) rewire `OcrWorker`/`PostReviewProcessor` to call backend endpoints with on-device fallback; (e) move the Gemini key server-side and remove it from the APK.

---

## Prioritised Top-5

| # | Gap | Impact | Why it's first |
|---|-----|--------|----------------|
| **1** | **Android ↔ backend integration (HTTP client + auth bridge + sync worker)** | Critical | Unblocks everything else; the backend is currently an unused island. No real user can reach any backend endpoint. |
| **2** | **Remove Gemini API key from the APK (server-side OCR + AI)** | High (security) | Violates threat-model NFR-019/AC-059. Only fixable once #1 lets the app call backend Gemini endpoints instead of embedding the key. |
| **3** | **Add `google-generativeai` to backend `requirements.txt` + add `asyncpg` + containerise/deploy** | High (production) | The AI endpoints silently degrade without the package; no Postgres driver or deploy config exists. Needed for any production run. |
| **4** | **Implement Room encryption (SQLCipher, AD-001) and resolve remaining threat-model blockers** | High (security, release-blocking) | Threat model §6 marks unencrypted Room, pen-test, and DPDP Act review as High/Open, blocking production launch. |
| **5** | **Implement cloud backup (F10) opt-in upload once a provider + encryption are chosen** | Medium | F10 ships only local export; cloud backup is unbuilt and gated by an open threat-model item ("backup cloud provider not selected"). Depends on #1 for a network path. |

---

## Notes for Implementers

- The prior `gap-analysis-post-agents-2026-10-29.md` correctly catalogued UI-level stubs (prescription search/sort, consent screen, medicine-count hardcoding, OCR-review navigation) but assessed the backend as "production-ready" without flagging that **no Android client can reach it** — the present scan's central finding.
- The `backend-migration-plan-2026-10-29.md` Area 8 already specifies the exact Android files to create (`data/network/` Retrofit interface, `AuthInterceptor`, `SyncWorker`, Hilt `NetworkModule`) and recommends last-write-wins with `serverUpdatedAt`. That plan remains the blueprint; it was not executed.
- Test counts validated: ~103 backend test functions (brief said 97 — migration added tests), ~495 Android `@Test` annotations (brief said 475).
- `SessionManager.KEY_ACCESS_TOKEN` is the canary for the auth gap: it is written only by tests, never by production code, so `isSessionActive()` is dead in any real build. Wiring it to a backend JWT login is the quickest proof that integration landed.
