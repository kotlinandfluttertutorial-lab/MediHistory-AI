# Requirements — Foundation

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F10 (security/config)
**Jira tickets:** MHA-001, MHA-002, MHA-003, MHA-004, MHA-005, MHA-006, MHA-007, MHA-008
**Functional requirements:** FR-039, FR-040
**Dependencies:** None — this is the base layer all other specs depend on.

---

## 1. Purpose

This spec covers every cross-cutting concern that must exist before feature work can start:

- Android and backend **repository scaffold** (project structure, module layout, version catalogue)
- **CI/CD pipeline** (GitHub Actions: build, lint, test, security scan)
- **Room database** schema, migrations, and DAOs
- **Authentication** (Firebase Auth or equivalent — JWT-based, backend-verified)
- **FastAPI backend scaffold** (typed schemas, middleware, health endpoint)
- **Environment configuration** (server-side secrets, Android BuildConfig flags, no API keys in APK)
- **Architecture Decision Records** (ADRs documenting key technical choices)

This is the foundation layer. Every other spec builds on top of it.

---

## 2. Functional requirements

### FR-039 — Data export and deletion
Patients can export all their data and request full account deletion from both local storage and the backend. The deletion timeline and any backup retention limitations are explained before confirmation.

### FR-040 — Cloud backup opt-in
Cloud backup is opt-in. The feature is wired up at the foundation layer (WorkManager job, encrypted upload) so all feature specs can rely on it; the patient-visible settings surface is delivered in the security-and-release spec.

---

## 3. Tickets and acceptance criteria

### MHA-001 — Android repository scaffold
Set up the Android project in the agreed module structure (`app`, `core`, `feature/*`). Apply the Kotlin version catalogue. Configure lint and detekt. Verify the project builds cleanly with no warnings treated as errors.

Key ACs:
- `./gradlew build` passes on a clean checkout with no local config.
- Module graph has no circular dependencies.
- Lint and detekt configurations are committed.

### MHA-002 — Architecture Decision Records
Create the `docs/adr/` directory with at least the following ADRs: storage strategy (Room + app-private files), authentication approach, OCR provider selection rationale, and encryption key management.

Key ACs:
- Each ADR follows the agreed template (context, decision, consequences).
- ADRs are linked from the repository README.

### MHA-003 — CI/CD pipeline
Configure GitHub Actions workflows for: Android build + unit tests, backend build + unit tests, lint/static analysis, and OWASP dependency check. Workflow runs on every pull request to `main`.

Key ACs:
- All jobs pass on the initial scaffold commit.
- A failing test causes the workflow to fail (not warn).
- OWASP dependency check report is uploaded as a build artifact.

### MHA-004 — Dependency version catalogue
Define all library versions in `libs.versions.toml`. No version strings outside the catalogue. Agree on Compose BOM, Room, WorkManager, Retrofit/Ktor, Hilt versions.

Key ACs:
- `./gradlew dependencyUpdates` produces a report.
- Zero duplicate version declarations across build files.

### MHA-005 — Environment configuration
Define environment variables for backend (database URL, secret key, OCR provider endpoint, JWT secret). Document each variable in `.env.example`. Android `BuildConfig` contains only non-secret flags (e.g. `BASE_URL` pointing at the backend). No secrets in the APK or in version control.

Key ACs:
- `.env.example` is committed; `.env` is git-ignored.
- A `./scripts/check-env.sh` script validates all required variables are set at startup.
- Android build fails if `BASE_URL` is empty.

### MHA-006 — Room database schema
Define the initial Room schema covering: `Patient`, `Doctor`, `Prescription`, `PrescriptionMedicine`, `MedicineEntity`, `DuplicateAlert`, `MedicalHistory`, `AuditLog`, `DoseLog`. Apply migrations framework. Schema export JSON is committed.

Key ACs:
- Schema version starts at 1; migration tests exist for future version 2 paths.
- All entities have primary keys and appropriate indices.
- Room schema export JSON is committed to `schemas/`.
- DAO unit tests cover insert, query, and cascade-delete behaviours.

### MHA-007 — Authentication
Implement sign-up and sign-in flows (email/password). Backend validates the JWT on every request. Patient-scoped authorization: each protected endpoint verifies the requesting patient owns the resource. Refresh token rotation is supported.

Key ACs:
- Unauthenticated requests to protected endpoints return HTTP 401.
- Cross-patient resource access returns HTTP 403 (verified by integration test with two distinct test accounts).
- Tokens are stored in Android Keystore-backed `EncryptedSharedPreferences`, not plain SharedPreferences.
- Sign-out clears all tokens from device storage.

### MHA-008 — FastAPI backend scaffold
Bootstrap the FastAPI app with: Pydantic v2 typed schemas, SQLAlchemy async ORM, Alembic migrations, a `/health` endpoint, CORS policy, rate limiting middleware, and structured JSON logging. No prescription text, medicine details, or access tokens are logged.

Key ACs:
- `GET /health` returns `{"status": "ok"}` without authentication.
- Structured logs contain request ID, timestamp, method, path, and response code — no body content.
- Rate limiting returns HTTP 429 with a `Retry-After` header.
- All Pydantic schemas pass mypy strict mode.

---

## 4. Implementation order

Dependencies flow top-to-bottom; do not start a task until all tasks above it are complete.

1. **MHA-001** — Android repository scaffold (establishes project structure)
2. **MHA-004** — Dependency version catalogue (required before any library can be added)
3. **MHA-005** — Environment configuration (required before backend or CI can run)
4. **MHA-006** — Room database schema (required by all Android feature specs)
5. **MHA-007** — Authentication (requires Room for token storage and MHA-008 for the backend)
6. **MHA-008** — FastAPI backend scaffold (requires MHA-005 for config)
7. **MHA-002** — Architecture Decision Records (documents choices made in steps 1–6)
8. **MHA-003** — CI/CD pipeline (validates the full scaffold once all pieces exist)

---

## 5. Out of scope

- Any UI screens or user-visible flows (delivered in android-ui spec and feature specs).
- OCR pipeline integration (prescription-management spec).
- Medicine data or duplicate detection logic (medicine-intelligence and medication-safety specs).
- PDF generation or sharing (medical-history spec).
- Encryption at rest (security-and-release spec — evaluated against Android Keystore before production).
- Any feature above the foundation layer.

---

## 6. Notes and constraints

- Android Keystore key management and database encryption are **evaluated** here (ADR written) but **enforced** in the security-and-release spec before production.
- Never log prescription text, medicine details, or access tokens (enforced in MHA-008 logging config).
- All test fixtures must use synthetic data — no real patient names, medicine names, or document content.
- Cross-account exposure or critical/high security issues block release (see security-and-release spec).
