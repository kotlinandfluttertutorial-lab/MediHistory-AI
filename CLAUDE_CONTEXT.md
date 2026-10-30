# CLAUDE_CONTEXT.md — Project Context for External AI Assistants

> Paste this entire file into Claude Code (or any external AI) to give it enough project
> understanding to answer design questions, review code snippets, or suggest approaches.
> It is a distilled, shareable summary of the steering files and current codebase state.
> It is NOT a substitute for reading the actual source files when writing production code.

---

## 1. What is MediHistory AI?

MediHistory AI is a personal health record app for patients to compile, organise, and share
their complete medication and prescription history.

Tagline: **"All prescriptions. All medicines. One doctor-ready medical history."**

It is a **personal health record tool, NOT a clinical system.** It never diagnoses, never
recommends starting/stopping/changing medicines, and never claims a drug combination is safe.

Primary users: patients managing their own records (multiple doctors, multiple medicines,
need to share history with a new provider).

---

## 2. Stack

### Android app (`android/`)
- **Language:** Kotlin (no Java anywhere)
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Repository pattern + UseCases for business logic
- **DI:** Hilt
- **Local DB:** Room (SQLCipher encryption under evaluation before production)
- **Background:** WorkManager
- **Camera:** CameraX
- **Photo/file import:** Android Photo Picker + system file picker
- **Async:** Kotlin Coroutines + Flow
- **Navigation:** Navigation-Compose, bottom bar with 5 tabs
- **Storage:** app-private storage only (`getFilesDir()` / `getExternalFilesDir()`)
- **Keys:** Android Keystore for encryption keys
- **Sensitive prefs:** EncryptedSharedPreferences (never plaintext for tokens/keys)
- **OCR:** ML Kit text recognition (on-device) + Google Generative AI SDK (Gemini 1.5 Flash)
  as an enhanced OCR service. A `StubOcrService` exists for tests.
- **Auth:** Firebase Auth + Firebase Analytics (BOM-managed)
- **Logging:** Timber (must never log prescription/medicine/doctor text or tokens)
- **Package root:** `com.medihistory.ai`

Key dependency versions (all pinned, no ranges, no `+`):
```
agp=8.4.2  kotlin=1.9.24  compose-bom=2024.06.00  compose-compiler=1.5.14
core-ktx=1.13.1  lifecycle=2.8.2  activity-compose=1.9.0  material3=1.2.1
hilt=2.51.1  room=2.6.1  navigation=2.7.7  coroutines=1.8.1
camerax=1.3.3  accompanist=0.36.0  work=2.9.0  security-crypto=1.1.0-alpha06
timber=5.0.1  firebase-bom=33.1.2  mlkit-text-recognition=16.0.1
google-genai-kotlin=1.8.0  appcompat=1.7.0
```

### Backend (`backend/`)
- **Framework:** FastAPI
- **Schemas:** Pydantic typed for all request/response bodies
- **Validation:** all inputs validated; malformed/oversized input rejected
- **Auth:** JWT-based; auth middleware on every protected route
- **Authorization:** patient-scoped on every object. **Cross-patient access returns HTTP 404
  (not 403)** to avoid leaking resource existence
- **Rate limiting:** `RateLimitMiddleware` (100 req / 60s per IP) on all endpoints
- **Migrations:** Alembic (no manual SQL)
- **API contract:** OpenAPI annotations on every endpoint
- **Secrets:** server-side env vars only — never in APK or source
- **Routers mounted under `/api/v1`:** auth, patients, doctors, prescriptions, medicines,
  duplicates, history, reminders, interactions (stub), export (stub)

---

## 3. Repository Layout

```
MediHistory-AI/
  android/           Android app module
  backend/           FastAPI backend
  docs/              architecture/, design/, jira/, product/, release/
  .kiro/             steering/, specs/, skills/, agents/, workflows/
  .github/workflows/ CI/CD pipelines (android-ci, backend-ci, pr-checks)
```

### Android source layout (package `com.medihistory.ai`)
```
data/
  auth/          session, token storage
  dao/            Room DAOs ([Entity]Dao, e.g. PrescriptionDao)
  database/       MediHistoryDatabase, Converters, DatabaseModule (Hilt)
  di/             Hilt modules
  duplicate/       DuplicateDetector, IngredientNormalizer (deterministic)
  history/        history summarisation
  model/          Room entities: Doctor, Patient, Prescription, Medicine,
                  DuplicateAlert, ExtractedFieldEntity, Reminder, OcrStatus,
                  MedicineStatus, DoctorSpecialty
  ocr/            ExtractedField, ReviewState, OcrService interface + impls
                  (MlKitOcrService, GeminiEnhancedOcrService, StubOcrService),
                  OcrCorrectionEngine, PrescriptionFieldParser, PostReviewProcessor
  privacy/        privacy/consent helpers
  reminders/       reminders & adherence
  repository/      interfaces ([Entity]Repository) + impls (Room[Entity]Repository)
  security/        security helpers
  session/         session state
ui/
  auth/  camera/  components/  dashboard/  doctor/  duplicate/  history/
  medicine/  more/  navigation/  newdoctor/  ocr/  patient/  picker/
  prescription/  privacy/  reminders/  safety/  theme/
worker/          WorkManager workers
```

### Naming conventions (must follow)
- ViewModel: `[Feature]ViewModel` (e.g. `PrescriptionListViewModel`)
- Repository interface: `[Entity]Repository` (e.g. `PrescriptionRepository`)
- Repository impl: `Room[Entity]Repository` (e.g. `RoomPrescriptionRepository`)
- UseCase: `[Action][Entity]UseCase` (e.g. `GetMedicineListUseCase`)
- DAO: `[Entity]Dao` (e.g. `PrescriptionDao`)
- Screen composable: `[Feature]Screen` (e.g. `PrescriptionListScreen`)
- Component composables: `ui/components/`
- Screens: `ui/screens/` (current code uses `ui/[feature]/`)
- ViewModels: `ui/viewmodel/` (current code keeps them in `ui/[feature]/`)
- Repository files: `data/repository/`
- DAO files: `data/dao/`
- Entity files: `data/model/`

---

## 4. MVP Feature Scope (F01–F10)

- **F01** Dashboard and navigation (bottom bar: Dashboard · Prescriptions · Medicines ·
  History · More)
- **F02** Prescription management (upload, camera, list, detail)
- **F03** OCR extraction with review flow
- **F04** Medicine explorer with search and filters
- **F05** Duplicate detection and alert management
- **F06** Medical history summarisation
- **F07** New Doctor Mode — content selection and PDF export
- **F08** Reminders and adherence tracking
- **F09** Interaction checking (external provider — optional in MVP)
- **F10** Security, privacy, consent, backup, account deletion

### Explicitly EXCLUDED from MVP
Diagnosis / differential diagnosis, prescribing recommendations, dose optimisation,
pharmacy ordering, KYC identity verification, automated medication changes, any feature
that constitutes medical advice.

---

## 5. Domain Concepts You Must Understand

### OCR Review State Machine (enforced in ValidationUtils)
Every extracted field progresses through these states ONLY:
```
PENDING_REVIEW → REVIEWED       (patient confirms or edits)
PENDING_REVIEW → REJECTED       (patient rejects)
REVIEWED       → PENDING_REVIEW  (patient un-reviews)
REJECTED       → PENDING_REVIEW  (patient un-rejects)
```
No other transitions are valid.

`ExtractedField` carries: `fieldName`, `rawValue` (null if absent from source — never fabricate),
`patientValue` (set only after review), `confidence` (0.0–1.0, validated in `init`),
`reviewState`, `sourceReference`.

### Medicine status
`CURRENT`, `PAST`, `UNKNOWN` — patient-assigned only. Defaults to `UNKNOWN`. The app never
sets CURRENT or PAST without an explicit patient action.

### Duplicate detection
- **Deterministic** (not AI/probabilistic) — same input always yields same ordered output.
- Compares every pair of active medicines (`CURRENT` or `UNKNOWN`; `PAST` excluded).
- Medicines with null/blank `activeIngredient` are excluded — never inferred.
- `IngredientNormalizer` handles synonyms (e.g. paracetamol ↔ acetaminophen).
- **EXACT match:** full normalised ingredients are synonyms → confidence ~0.95.
- **PARTIAL match:** combination product ingredient overlap (split on `+`, `/`, `,`)
  → confidence ~0.70, includes a `combinationNote`.
- Alerts are ALWAYS labelled **"Potential duplicate"** — never "Duplicate medication" or
  any clinical term.
- Alerts are informational only — they do NOT block recording medicines.
- Mark-as-reviewed disclaimer required: "Marking as reviewed means you have seen this alert.
  It does not mean this combination has been approved by a doctor."

### Room database
- Current version **8**. Explicit migrations only — NEVER `fallbackToDestructiveMigration()`
  or `allowMainThreadQueries()`.
- Schema exports committed to `android/app/schemas/`.
- Entities: Doctor, Patient, Prescription, ExtractedFieldEntity, Medicine, DuplicateAlert,
  Reminder, DoseRecordEntity.
- Every DAO query is scoped by `patientId` (denormalized on Medicine, DuplicateAlert, etc.).

---

## 6. Hard Rules (Medical Safety)

- **Never infer missing data.** If dose, duration, diagnosis, allergy, or medicine status is
  absent from the source, record it as absent. Do not fabricate.
- **OCR output is untrusted input.** Sanitise and validate all OCR output before persisting.
- **Store confidence, review state, and source reference** for every extracted field.
- **Preserve originals.** OCR never replaces the source document. Originals stored unchanged.
- **Link to source.** Every extracted fact links to its source prescription document.
- **Show confidence and review state** always. Low-confidence fields visually highlighted.
- **Separate medication status** — Current/Past/Unknown kept separate, patient-assigned only.
- **Active ingredient identity** drives duplicate matching, not brand text alone.
- **Never advise on medicines** — never start/stop/change.
- Every AI-generated section must be labelled as AI-generated. Every summary includes a
  disclaimer that it is a patient-compiled record, not a clinical report.
- Combination products decomposed into constituent ingredients for matching. Partial matches
  produce a reduced-confidence alert with a note naming both medicines and the matching
  ingredient — no clinical claim.
- Synthetic data only in all tests and demos. No real patient names, real doctor names, or
  real Indian medicine brands. Use names like "Test Patient Alpha", "Dr. Synthetic",
  "Synthamol 500mg", "Testocillin 250mg".

---

## 7. Hard Rules (Security & Privacy)

- No API keys, tokens, or secrets in any Kotlin source, resource, or committed config file.
- Backend base URL and feature flags via `BuildConfig` from `local.properties` (git-ignored)
  or CI env vars.
- Never log prescription text, medicine names, doctor names, access tokens, or document
  contents in Logcat or server logs.
- HTTPS only. Encrypt at rest (Keystore-derived keys).
- Sharing requires explicit patient action and preview — private by default.
- Cloud backup is opt-in with visible status, failures, and recovery implications.
- Offline access is core — all core patient data accessible offline. Sync is a supplement.
- Backend: least privilege for service accounts and DB roles.
- A formal threat model review is required before production release.
- Cross-account exposure or critical/high security issues block release.

---

## 8. Testing Conventions

- **Kotlin tests:** `given_[state]_when_[action]_then_[outcome]` or
  `should_[outcome]_when_[condition]`
- **Python tests:** `test_[outcome]_[condition]` or `test_[feature]_[scenario]`
- Unit: ViewModel state transitions, Repository impls with in-memory Room, UseCase logic with
  fake repos, ValidationUtils boundary values, DuplicateDetector with synthetic fixtures.
- Integration: full flows (create → OCR → review → alert), Room DAOs with real in-memory DB,
  WorkManager with TestWorkerBuilder, FastAPI with TestClient + in-memory SQLite.
- UI: Compose/Espresso, HiltAndroidRule for DI, AccessibilityChecks enabled, mock CameraX
  output.
- Coverage targets: ≥80% data layer; 100% branches for DuplicateDetector & ValidationUtils;
  ≥90% backend API layer.
- Must test OCR confidence at/above/below threshold, all review transitions, edit stores
  patientValue without changing rawValue, reject does not delete the field.
- Must test duplicate edge cases: same ingredient different brands, different ingredients
  similar names, combination vs single (partial), combination vs same combination (full),
  paracetamol/acetaminophen synonym, unreviewed records excluded, determinism.
- Must test offline/sync: create/edit in airplane mode, offline indicator, no crash on toggle.
- Must test accessibility: ≥48×48dp touch targets, StatusBadge passes AccessibilityChecks,
  image composables have contentDescription or marked decorative, TalkBack announces nav.

---

## 9. How to Use This Document

When you (the external AI) are asked to:
- **Review a code snippet:** check it against the rules in sections 6 and 7. Flag any
  fabricated/inferred missing data, any clinical-sounding duplicate label, any secret in
  source, any unscoped patient query, any logging of prescription text.
- **Suggest an approach:** respect the stack (Kotlin/Compose/Hilt/Room; FastAPI/Pydantic),
  naming conventions (section 3), and the review state machine (section 5).
- **Write code:** match the existing style (data classes with Room annotations, Hilt
  `@Inject`, repository interface + Room impl pattern, sealed `NavDestination`). Never use
  Java. Never use range or `+` dependency versions.
- **Generate tests:** use synthetic data only, follow the naming conventions, and cover the
  edge cases listed in section 8.

If the user pastes a file, treat it as the source of truth for that file's current state. If
something here contradicts the pasted file, the file wins (the codebase evolves faster than
this summary).

---

## 10. Where to Look for More (in the real repo)

- Steering files: `.kiro/steering/*.md` (tech, product, structure, testing, privacy-security,
  medical-safety, engineering)
- Feature specs: `.kiro/specs/[domain]/requirements.md`
- Architecture docs: `docs/architecture/` (ADRs, data model, API contracts, threat model)
- Design docs: `docs/design/` (screen inventory, navigation map, design system, Figma refs)
- Product docs: `docs/product/` (PRD, user stories, feature specs)
- CI: `.github/workflows/` (android-ci, backend-ci, pr-checks)
- Room schema exports: `android/app/schemas/`
