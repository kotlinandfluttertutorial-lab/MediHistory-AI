# Architecture — MediHistory AI

**Document version:** 1.0  
**Status:** Draft — open decisions marked TODO

---

## 1. System overview

MediHistory AI has three deployment modes:

| Mode | Components active |
|------|------------------|
| Local only | Android app + local Room database |
| With backend sync | Android app + local Room database + FastAPI backend + PostgreSQL |
| With AI services | Any of the above + provider-independent OCR/AI service interfaces |

All modes share the same Android codebase. Feature availability degrades gracefully based on which external services are configured (see NFR-020).

```
┌──────────────────────────────────────────────────────────────────────┐
│  Android Application                                                  │
│  ┌────────────────┐  ┌──────────────────┐  ┌───────────────────────┐│
│  │   Compose UI   │  │   ViewModels     │  │  WorkManager Jobs     ││
│  │   (Material 3) │  │   (MVVM state)   │  │  (OCR, Sync, Export)  ││
│  └───────┬────────┘  └────────┬─────────┘  └──────────┬────────────┘│
│          │                   │                         │             │
│  ┌───────▼───────────────────▼─────────────────────────▼────────────┐│
│  │                    Repository Layer                               ││
│  │         (interfaces — domain layer never imports framework)       ││
│  └────────────────────────────┬──────────────────────────────────────┘│
│           ┌────────────────────┴──────────────────────┐              │
│  ┌────────▼────────┐                        ┌─────────▼─────────────┐│
│  │   Room / SQLite │                        │  Retrofit / Ktor HTTP ││
│  │   Local DB      │                        │  Backend API client   ││
│  └─────────────────┘                        └─────────┬─────────────┘│
└──────────────────────────────────────────────────────┼───────────────┘
                                                        │
              ┌─────────────────────────────────────────┤
              │                                         │
   ┌──────────▼──────────┐              ┌───────────────▼────────────┐
   │  FastAPI Backend     │              │  AI / OCR Services         │
   │  (optional sync)     │              │  (provider-independent     │
   │  PostgreSQL          │              │   interfaces)              │
   └──────────────────────┘              └────────────────────────────┘
              │
   ┌──────────▼──────────┐
   │  Cloud Storage       │
   │  (opt-in backup)     │
   └──────────────────────┘
```

---

## 2. Android application architecture

### 2.1 Layered structure

```
app/
├── ui/                  # Compose screens and components (Material 3)
│   ├── dashboard/
│   ├── prescriptions/
│   ├── medicines/
│   ├── duplicates/
│   ├── history/
│   ├── newdoctor/
│   ├── reminders/
│   ├── interactions/
│   └── settings/
├── viewmodel/           # ViewModels — one per screen or feature area
├── domain/
│   ├── model/           # Domain data classes (no framework dependencies)
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Use cases (optional thin layer for complex logic)
├── data/
│   ├── local/
│   │   ├── database/    # Room database, DAOs, entities
│   │   └── storage/     # App-private file storage helpers
│   ├── remote/          # Retrofit/Ktor services, DTOs
│   └── repository/      # Repository implementations
└── worker/              # WorkManager workers (OcrWorker, SyncWorker, ExportWorker)
```

### 2.2 MVVM pattern

- **UI layer (Compose):** Observes `StateFlow` or `LiveData` from ViewModel. Emits user events back to ViewModel. No business logic.
- **ViewModel:** Holds UI state. Delegates all data operations to use cases or repositories via coroutines. No direct Room or API access.
- **Repository (interface):** Defined in `domain/repository`. Returns domain models. Implementations in `data/repository` wire Room DAOs and remote APIs together.
- **Use cases:** Optional — used where a ViewModel would otherwise depend on more than one repository. Not required for simple CRUD flows.

### 2.3 Local database — Room

Tables: `patients`, `doctors`, `prescriptions`, `prescription_medicines`, `medicines`, `duplicate_alerts`, `medical_history_summaries`, `medication_reminders`, `dose_logs`, `audit_log_entries`.

Key Room decisions:
- All multi-table writes are wrapped in `@Transaction`.
- Foreign keys are enforced with `onDelete = CASCADE` for child records (e.g. PrescriptionMedicines cascade on Prescription deletion).
- Migrations are versioned; no `fallbackToDestructiveMigration()` in production.
- Database encryption: evaluate SQLCipher or `EncryptedSharedPreferences`-style approach before production (see NFR-006 and threat model).

TODO: Decide on encrypted Room DB library and key rotation strategy before implementing the data layer.

### 2.4 Document storage

Prescription images and PDFs are stored in the app's private internal storage (`Context.filesDir/prescriptions/{prescriptionId}/`). Files are not accessible to other apps without explicit sharing. File names do not contain patient-identifiable information. File integrity is verified using a stored SHA-256 hash on read (see AC-006).

### 2.5 CameraX and Photo Picker

- Camera capture uses CameraX `ImageCapture` with a confirmation screen before storing (AC-007).
- File import uses Android `PhotoPickerContract` for images and `OpenDocument` for PDFs.
- Both paths store the original file unchanged before triggering OCR.

### 2.6 WorkManager

Background jobs:
- `OcrWorker`: Triggered after a prescription is stored. Sends the document to the OCR service and stores the structured result. Uses `NetworkType.CONNECTED`. Retries up to 3 times with exponential backoff.
- `SyncWorker`: Periodic sync of local changes to the backend (when sync is enabled). Uses `NetworkType.CONNECTED` and `BatteryNotLow`.
- `ExportWorker`: Generates the PDF or ZIP export package. Runs one-time, on demand.

### 2.7 Android Keystore

Android Keystore is used for:
- Encrypting the local database key (if SQLCipher or equivalent is used).
- Signing export packages.
- Deriving encryption keys for cloud backup.

No key material leaves the device. Keys are hardware-backed where the device supports it.

### 2.8 Dependency injection

Use Hilt (Dagger-Hilt) for dependency injection. Repository implementations, ViewModels, and WorkManager workers are all injected. No service locators or static singletons.

TODO: Confirm Hilt vs. Koin preference before starting implementation.

---

## 3. Backend architecture

### 3.1 Overview

The backend is optional — the app functions fully in local-only mode. When enabled, the backend provides:
- Persistent cross-device sync.
- OCR and AI service proxying (keeping API keys server-side).
- PDF export orchestration for large documents.
- Encrypted cloud backup coordination.

### 3.2 FastAPI application structure

```
backend/
├── app/
│   ├── main.py              # FastAPI app, lifespan, middleware
│   ├── api/
│   │   ├── auth.py          # /auth/* routes
│   │   ├── patients.py      # /patients/me routes
│   │   ├── prescriptions.py
│   │   ├── medicines.py
│   │   ├── duplicates.py
│   │   ├── history.py
│   │   ├── export.py
│   │   ├── reminders.py
│   │   └── interactions.py
│   ├── core/
│   │   ├── config.py        # Settings from environment variables only
│   │   ├── security.py      # JWT creation, validation
│   │   └── dependencies.py  # Shared FastAPI dependencies (get_current_patient)
│   ├── models/              # SQLAlchemy ORM models
│   ├── schemas/             # Pydantic request/response schemas (typed, validated)
│   ├── services/            # Business logic (OCR service, duplicate engine, summarizer)
│   └── db/
│       ├── session.py       # Async SQLAlchemy session
│       └── migrations/      # Alembic migration scripts
└── tests/
    ├── fixtures/            # Synthetic data only
    └── test_*.py
```

### 3.3 Authentication and authorisation

- All patient-data endpoints require a valid JWT in the `Authorization: Bearer` header.
- The `get_current_patient` dependency validates the JWT and loads the patient record. It is applied to every route that handles patient data — there are no exceptions.
- Patient-scoped authorisation: every database query is filtered by `patient_id` derived from the JWT. No patient can access another patient's data. This is enforced at the service layer, not just the route layer.
- JWT algorithm: HS256 (configurable). Access token TTL: 30 minutes (configurable via `ACCESS_TOKEN_EXPIRE_MINUTES`). Refresh token TTL: 7 days.
- Passwords are hashed using bcrypt. Plain-text passwords are never logged or stored.

### 3.4 Typed schemas and input validation

- All request and response bodies are defined as Pydantic v2 models with explicit field types, validators, and constraints.
- OCR output received from external AI services is validated against a strict Pydantic schema before being stored. Invalid output is rejected with a detailed error (see AC-018).
- No raw OCR text or prescription content is included in error responses, logs, or exception tracebacks.

### 3.5 Rate limiting

Applied using `slowapi` (or equivalent ASGI middleware). Per-endpoint limits are defined in `core/config.py`. Auth endpoints have the strictest limits. See docs/architecture/api-contracts.md for per-endpoint limits.

### 3.6 Database

- PostgreSQL with async SQLAlchemy and asyncpg driver.
- All migrations managed by Alembic. No destructive migrations without an explicit rollback plan.
- Database credentials are loaded exclusively from environment variables. No credentials in source code.
- Connection pooling configured with sensible max pool size for the deployment environment.

TODO: Define connection pool size and timeout for production deployment.

### 3.7 AI/OCR service proxy

The backend acts as a proxy for AI/OCR service calls to keep API keys server-side. The Android app sends the document to the backend; the backend calls the AI/OCR provider and returns structured results. Provider-specific code is isolated behind a service interface (`OcrService`, `ExtractionService`, `NormalizationService`, `SummarizationService`) so providers can be swapped without changing routes or schemas.

---

## 4. AI/OCR pipeline

See docs/architecture/ai-safety.md for the full safety model.

### 4.1 Provider-independent interfaces

Each AI capability is defined as an abstract Python class with a typed interface:
- `OcrService.extract_text(document_bytes) -> OcrResult`
- `ExtractionService.extract_fields(ocr_text, schema) -> ExtractionResult`
- `NormalizationService.normalize_medicine(name, brand) -> NormalizationResult`
- `SummarizationService.summarize(records, schema) -> SummaryResult`

Implementations for specific providers (e.g. Google Document AI, AWS Textract, Azure Form Recogniser) are injected via configuration.

### 4.2 Confidence scoring

Every field in `ExtractionResult` and `NormalizationResult` carries a confidence score (0.0–1.0). Scores are stored in the database with the extracted value. The default review threshold is 0.7 (configurable). Fields below threshold are flagged for patient review.

### 4.3 Deterministic duplicate matching

Duplicate detection is a deterministic algorithm (not an AI model) to ensure predictable, auditable results:
1. Load all PrescriptionMedicine records in REVIEWED state for the patient.
2. Normalize active ingredients using the `NormalizationService`.
3. Group by normalized active ingredient identity.
4. For each group with more than one entry, create or update a `DuplicateAlert`.
5. Apply combination product and formulation adjustments to set alert confidence.

Deterministic matching results are stored separately from AI-generated summaries. Changes to the AI summarizer model do not affect duplicate alert logic.

---

## 5. Security model summary

Full threat model in docs/architecture/security-threat-model.md.

Key security controls:
- Android Keystore for local key management.
- App-private storage for prescription documents.
- Database encryption at rest (to be implemented before production — see NFR-006).
- HTTPS/TLS 1.2+ for all API communication.
- JWT with short expiry; refresh token rotation.
- Patient-scoped authorisation on every backend endpoint.
- No API keys in the APK or git history.
- Rate limiting on all public endpoints.
- Audit logging: metadata only, no health content.
- OCR input treated as untrusted; output validated before storage.

---

## 6. Offline-first strategy

Room is the single source of truth for the Android app. The app is fully functional without a network connection for all read operations and for write operations that do not require AI processing.

Write flow with sync enabled:
1. Patient action writes to Room (immediate).
2. Room triggers a dirty flag on the modified entity.
3. WorkManager `SyncWorker` (constrained to network available) picks up dirty entities and pushes to the backend.
4. On success, the dirty flag is cleared and a server-assigned ID is stored.
5. On conflict (same entity modified on two devices), the server returns a 409. The SyncWorker stores the conflict for patient resolution.

The patient can always view the current Room state. Sync status (synced, pending, conflict, error) is shown in the UI.

---

## 7. Data flow — add prescription end to end

1. Patient captures image (CameraX) or picks file (Photo Picker).
2. App validates file type and size.
3. Original file is stored in app-private storage; SHA-256 hash stored in Room.
4. Patient enters metadata (doctor, date) — prescription record created in Room with status = PENDING_OCR.
5. `OcrWorker` is enqueued (network required).
6. `OcrWorker` sends document to backend OCR proxy.
7. Backend calls OCR provider, validates structured output against Pydantic schema.
8. Structured result returned to `OcrWorker`; stored as `PrescriptionMedicine` records in Room with status = PENDING_REVIEW.
9. Patient reviews extracted fields in the review UI; review state transitions to REVIEWED or REJECTED.
10. After review, `DuplicateDetectionWorker` recalculates alerts.
11. Newly detected alerts appear in the dashboard and duplicate alerts screen.
12. Patient can trigger summary generation from reviewed records.

---

## 8. Open architectural decisions

| ID | Decision | Options | Target date |
|----|---------|---------|-------------|
| AD-001 | Encrypted Room DB library | SQLCipher, Jetpack Security EncryptedFile per-record, unencrypted + file-level encryption | Before data layer implementation |
| AD-002 | DI framework | Hilt vs. Koin | Before first Android module |
| AD-003 | HTTP client | Retrofit vs. Ktor client | Before first API integration |
| AD-004 | OCR provider selection | Google Document AI, AWS Textract, Azure Form Recogniser, open-source | Before OCR implementation |
| AD-005 | AI summarization provider | OpenAI GPT, Google Gemini, Anthropic Claude, local model | Before summarization implementation |
| AD-006 | Medication reference source (F09) | Licensed API provider — TBD | Before F09 implementation |
| AD-007 | Connection pool size | Depends on expected concurrency | Before production deployment |

---

*This document should be reviewed after each open decision is resolved. Architecture diagrams are in docs/architecture/architecture-diagram.mmd.*
