# Requirements — Security and Release

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F10
**Jira tickets:** MHA-068, MHA-069, MHA-070, MHA-071, MHA-072, MHA-073, MHA-074, MHA-075, MHA-076, MHA-077, MHA-078, MHA-079, MHA-080, MHA-081, MHA-082
**Functional requirements:** FR-039, FR-040
**Dependencies:** All other specs — this spec hardens and validates what every other spec builds.

---

## 1. Purpose

This spec covers everything required to make the application secure and releasable:

- **Android Keystore key management** — key generation, rotation strategy, invalidation on biometric change
- **Database encryption** — SQLCipher evaluation and integration; encrypted Room database before production
- **Data export** — encrypted ZIP or JSON package of all patient data
- **Account deletion** — full deletion from local storage and backend; clear explanation of backup retention
- **Cloud backup** — opt-in, encrypted before upload, visible status and failure handling
- **Consent management** — audit trail of patient consent events
- **Audit logging** — write-only log of security-relevant events
- **Android test suite** — unit, integration, and UI test coverage gates
- **Backend test suite** — unit and integration coverage gates
- **GitHub Actions CI pipelines** — lint, test, security scan, coverage gates on every PR
- **Release checklist** — gating criteria before any production release

**Critical security rules — these block release:**
1. Database encryption using Android Keystore-derived keys must be active before production release.
2. A qualified legal and security review of applicable India requirements must be obtained before launch. This review is not a compliance certification but is a prerequisite for production.
3. Any cross-account data exposure finding blocks release until resolved.
4. Any critical or high security vulnerability found by OWASP dependency check or manual review blocks release.

---

## 2. Functional requirements

### FR-039 — Data export and deletion
The patient can export all their data as an encrypted ZIP or JSON package from the Settings screen. Full account deletion removes all records, documents, extracted data, reminders, and generated summaries from both local storage and the backend (where sync is enabled). Deletion is confirmed with a plain-language warning. The deletion timeline and any backup retention limitations are explained before confirmation.

### FR-040 — Cloud backup opt-in
Cloud backup is opt-in. The backup status (enabled/disabled, last successful backup, last error) is visible at all times in the Settings screen. Enabling backup requires explicit patient action. Disabling backup explains what happens to existing cloud copies. Backup data is encrypted before upload using a key derived from a patient-controlled secret managed via Android Keystore.

---

## 3. Tickets and acceptance criteria

### MHA-068 — Android Keystore key management
Generate and manage encryption keys in Android Keystore. Document the key derivation strategy (key alias, `StrongBoxBacked` preference, biometric authentication binding). Define key rotation and invalidation policy. Write an ADR: `docs/adr/key-management.md`.

Key ACs:
- Keys are generated in Android Keystore — no key material in SharedPreferences, code, or files.
- Key usage fails gracefully after biometric change (key invalidated) with a re-authentication prompt rather than a crash.
- `EncryptedSharedPreferences` is used for all sensitive preferences (tokens, backup keys).
- ADR is reviewed and committed before the encryption ticket (MHA-069) starts.

### MHA-069 — Database encryption (SQLCipher)
Evaluate and integrate SQLCipher (or the Android `androidx.sqlite` encryption extension) for Room database encryption. The database passphrase is derived from the Android Keystore key from MHA-068. Migration from an unencrypted database (for users upgrading) is documented and tested.

Key ACs:
- A fresh install creates an encrypted database.
- Migration test: unencrypted Room database is encrypted transparently on upgrade without data loss.
- The database passphrase is never stored in plaintext — derived from Keystore at runtime only.
- No Room query results are logged at any log level.
- Security scan (MHA-079) verifies no database file is readable by other apps.

### MHA-070 — Data export
`ExportDataUseCase` produces an encrypted ZIP containing: all prescription images and PDFs, all `PrescriptionMedicine` records (JSON), all `MedicalHistory` records (JSON), all `DuplicateAlert` records (JSON), all `DoseLog` records (JSON). The ZIP is encrypted with a patient-chosen password (PBKDF2 key derivation, AES-256). Export is triggered from Settings.

Key ACs:
- Exported ZIP is written to `getCacheDir()` for the duration of the share; deleted after the share sheet is dismissed.
- Export progress is shown in a `LoadingOverlay` — the patient cannot navigate away during export.
- Unit test verifies the ZIP contains all entity types with correct JSON schema.
- Unit test verifies the ZIP cannot be opened without the correct password (using synthetic fixtures).

### MHA-071 — Account deletion
`DeleteAccountUseCase` performs: (1) delete all local Room records (cascade), (2) delete all app-private files, (3) call `DELETE /account` on the backend when sync is enabled, (4) clear all EncryptedSharedPreferences, (5) sign out. The patient is shown a plain-language warning before confirmation. The backup retention limitation (e.g. "Backups may be retained for up to X days by the cloud provider") is explained.

Key ACs:
- Confirmation dialog is a two-step process: first confirm intent, then type "DELETE" or equivalent to confirm (prevents accidental deletion).
- After deletion, the app returns to the sign-up/sign-in screen with a fresh state.
- Integration test verifies `DELETE /account` returns HTTP 200 and all patient data is removed from the backend (using synthetic test account).
- Backend cascades delete to: prescriptions, medicines, alerts, history, audit log entries owned by the patient.

### MHA-072 — Cloud backup (Android WorkManager job)
`BackupWorker` encrypts the local database export and uploads it to the configured backend endpoint. Backup runs on a daily schedule when enabled. Backup status (last success timestamp, last error) is stored in `EncryptedSharedPreferences` and displayed in Settings.

Key ACs:
- Backup is disabled by default — requires explicit opt-in in Settings.
- Backup upload uses HTTPS only; HTTP connections are rejected.
- `TestWorkerBuilder` test covers: successful backup, upload failure (retry), encryption failure (error state without crash).
- Backup status UI shows: "Backup enabled — last backup: [date]", "Backup enabled — last backup failed: [date] — Retry", "Backup disabled."

### MHA-073 — Cloud backup settings UI
Settings screen section for backup: toggle to enable/disable, last backup status, a "Back up now" button, and a plain-language explanation of what is included in the backup and what happens when backup is disabled.

Key ACs:
- Enabling backup shows a consent dialog explaining what data will be sent and to which service.
- Disabling backup shows a dialog: "Your existing cloud backup will remain until it expires. New changes will not be backed up."
- Backup status is readable by TalkBack (labelled correctly).
- Screen passes `AccessibilityChecks`.

### MHA-074 — Consent management entity and UI
Room entity for `ConsentRecord` with fields: id, patientId, consentType, consentVersion, grantedAt, revokedAt (nullable). `ConsentType` enum covers: `BACKUP_ENABLED`, `HISTORY_SHARED`, `TERMS_ACCEPTED`. A consent history screen is accessible from Settings.

Key ACs:
- Schema export updated and committed.
- Consent is recorded when the patient enables backup (MHA-073) and when the patient confirms sharing (MHA-057).
- Consent revocation (disabling backup) sets `revokedAt` — the record is not deleted.
- Consent history screen lists all consent events in reverse chronological order.

### MHA-075 — Audit logging (complete)
Extend the `AuditLog` entity (introduced in MHA-058) to cover all security-relevant events: `LOGIN`, `LOGOUT`, `BACKUP_ENABLED`, `BACKUP_DISABLED`, `EXPORT`, `ACCOUNT_DELETED`, `HISTORY_SHARED`, `ALERT_REVIEWED`. Each entry: patientId, event, timestamp, metadata (no content fields). Backend endpoint `POST /audit` persists entries server-side when sync is enabled.

Key ACs:
- Audit log entries are write-only — no delete or update path exists in the DAO or API.
- No content fields (medicine names, document text, summary text) appear in any audit entry.
- Integration test verifies each event type produces a correctly structured audit entry.

### MHA-076 — Android unit test suite coverage gate
Establish coverage gate: ≥ 80% line coverage for the data layer; 100% branch coverage for `DuplicateDetector` and `ValidationUtils`. Coverage is measured by JaCoCo and reported in CI.

Key ACs:
- JaCoCo configuration is committed to the build files.
- CI workflow fails if coverage drops below the gate threshold.
- Coverage report is uploaded as a CI artifact on every PR.

### MHA-077 — Android integration and UI test suite
`HiltAndroidRule`-based UI tests for all critical flows: add prescription, OCR review, mark duplicate as reviewed, generate history, export data. `AccessibilityChecks.enable()` is active in the base test class. Mock CameraX output is used for camera tests.

Key ACs:
- All critical flow UI tests pass in CI on every PR to `main`.
- Every UI test that touches a `StatusBadge` verifies it passes `AccessibilityChecks`.
- No UI test fixture contains real patient names, real medicine names, or real document content.

### MHA-078 — Backend test suite coverage gate
Establish coverage gate: ≥ 90% line coverage for the backend API layer. Coverage measured by `pytest-cov` and reported in CI.

Key ACs:
- `pytest-cov` configuration is committed.
- CI workflow fails if backend coverage drops below 90%.
- Coverage report uploaded as CI artifact on every PR.

### MHA-079 — Security CI jobs
GitHub Actions jobs: (1) OWASP dependency check for Android dependencies, (2) `pip-audit` for Python dependencies, (3) grep for hardcoded secrets (API keys, passwords, tokens) in source files — fails on any match, (4) verify no API keys are present in `local.properties` or `gradle.properties` patterns.

Key ACs:
- All four security jobs run on every PR to `main`.
- OWASP report is uploaded as a build artifact.
- Secret grep pattern covers: `api_key`, `apiKey`, `password`, `secret`, `token`, `Authorization` in non-test source files.
- Any critical or high CVSS vulnerability in the OWASP report fails the build.

### MHA-080 — GitHub Actions — Android CI pipeline
Workflow: checkout → setup JDK → restore Gradle cache → lint → unit tests → integration tests → UI tests (emulator) → JaCoCo coverage gate → upload artifacts. Runs on every PR to `main`.

Key ACs:
- Workflow file is at `.github/workflows/android-ci.yml`.
- All jobs must pass before a PR can be merged (branch protection rule documented in README).
- Emulator UI tests run in parallel across API levels 29 and 34.

### MHA-081 — GitHub Actions — Backend CI pipeline
Workflow: checkout → setup Python → install dependencies → mypy strict → flake8/ruff → pytest with coverage → coverage gate → upload artifacts. Runs on every PR to `main`.

Key ACs:
- Workflow file is at `.github/workflows/backend-ci.yml`.
- mypy and ruff failures block the PR.
- All tests use in-memory SQLite — no external database dependency in CI.

### MHA-082 — Release checklist
Produce and commit `docs/release/release-checklist.md`. The checklist gates every production release. Items include:

- [ ] All spec acceptance criteria verified.
- [ ] Android CI and backend CI pass on the release branch.
- [ ] Database encryption active (SQLCipher integrated, Keystore key management confirmed).
- [ ] No critical or high vulnerabilities in OWASP report or `pip-audit`.
- [ ] No hardcoded secrets found by secret grep job.
- [ ] Cross-patient access integration test passes with two distinct synthetic accounts.
- [ ] Qualified legal and security review of India requirements completed (sign-off documented).
- [ ] Privacy policy reviewed and up to date.
- [ ] Data export and account deletion flows tested end-to-end.
- [ ] Cloud backup opt-in flow tested end-to-end.
- [ ] Accessibility: all critical flows pass `AccessibilityChecks`; manual TalkBack test completed.
- [ ] All mandatory disclaimers present and hardcoded (duplicate alerts, dose log, interaction check, PDF).
- [ ] No real patient data, real medicine names, or real doctor names in any test fixture or demo account.

Key ACs:
- Checklist is reviewed and signed off by the project lead before each production release.
- Any unchecked item blocks release.
- The checklist is version-controlled; changes require a PR.

---

## 4. Implementation order

1. **MHA-068** — Android Keystore key management (required before any encryption work)
2. **MHA-069** — Database encryption — SQLCipher (requires MHA-068)
3. **MHA-074** — Consent management entity (required by backup and sharing)
4. **MHA-072** — Cloud backup WorkManager job (requires MHA-068 for key management)
5. **MHA-073** — Cloud backup settings UI (requires MHA-072)
6. **MHA-070** — Data export (requires MHA-068 for encryption)
7. **MHA-071** — Account deletion (requires MHA-070 and all data layer specs)
8. **MHA-075** — Audit logging — complete (extends MHA-058; requires all event sources)
9. **MHA-076** — Android unit test coverage gate (enforced once all feature specs complete)
10. **MHA-077** — Android integration and UI test suite (requires all feature screens)
11. **MHA-078** — Backend test coverage gate (enforced once all backend endpoints complete)
12. **MHA-079** — Security CI jobs (can start after MHA-003; run continuously)
13. **MHA-080** — GitHub Actions Android CI pipeline (extends MHA-003 with coverage and UI tests)
14. **MHA-081** — GitHub Actions backend CI pipeline (extends MHA-003 with coverage)
15. **MHA-082** — Release checklist (produced last; documents all of the above)

---

## 5. Out of scope

- New application features — this spec is about hardening and release readiness only.
- UI/UX changes to existing feature screens (unless required to add a missing mandatory disclaimer).
- Caregiver or multi-patient accounts (post-MVP).
- KYC or identity verification (MVP exclusion).

---

## 6. Notes and constraints

- Database encryption (MHA-069) and Keystore key management (MHA-068) **block production release** — they are not optional.
- Qualified legal and security review of India requirements (regulatory, data localisation, health data rules) blocks production release. This is not a compliance certification; it is a prerequisite for informed decision-making.
- Cross-account exposure or any critical/high security finding blocks release.
- All test fixtures, demo accounts, and synthetic data must use fake names — no real patient, doctor, or medicine names.
- The release checklist (MHA-082) is the final gate; nothing ships without it being fully checked off and signed off.
- Audit log entries are write-only and are explicitly excluded from the data deletion scope for `DoseLog` and `ConsentRecord` — the patient deletion warning must explain this distinction.
