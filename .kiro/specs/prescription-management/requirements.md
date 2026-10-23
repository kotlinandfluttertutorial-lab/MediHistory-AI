# Requirements — Prescription Management

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F02, F03, F10
**Jira tickets:** MHA-016, MHA-017, MHA-018, MHA-019, MHA-020, MHA-021, MHA-022, MHA-023, MHA-024, MHA-025, MHA-026, MHA-027, MHA-028, MHA-029, MHA-030, MHA-031, MHA-032, MHA-033
**Functional requirements:** FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016, FR-039
**Dependencies:** foundation spec (Room, auth, FastAPI scaffold), android-ui spec (theme, navigation, components)

---

## 1. Purpose

This spec covers the core prescription data lifecycle from first capture through OCR review to persistent storage:

- **Patient profile** — name, date of birth, patient notes
- **Doctor profile** — name, specialty, clinic/hospital, contact notes
- **Prescription upload** — camera capture via CameraX and file import via Photo Picker / system file picker
- **OCR extraction** — background WorkManager job, structured field extraction, confidence scores
- **OCR review flow** — per-field review, confidence indicator, patient confirm/edit/reject
- **Manual medicine entry** — fallback when OCR fails or is unavailable
- **Prescription CRUD backend API** — create, read, update, delete endpoints with patient-scoped auth
- **Offline support** — local-first storage, sync status indicator, conflict handling

Original prescription images and PDFs are never modified after capture. OCR output is never overwritten by patient edits — both values are stored.

---

## 2. Functional requirements

### FR-005 — Add prescription via camera capture
The patient captures a prescription image with CameraX. The capture flow confirms readability before submission. The original image is stored in app-private storage (`getFilesDir()`) unchanged.

### FR-006 — Add prescription via file picker
The patient selects a JPEG, PNG, or PDF from device storage using Android Photo Picker or the system file picker. The original file is copied to app-private storage unchanged.

### FR-007 — Doctor profile management
Create, edit, and delete doctor profiles with: name, specialty (predefined list), clinic/hospital name, contact notes. Each prescription is linked to exactly one doctor profile.

### FR-008 — Prescription metadata entry
After upload, the patient enters or confirms: prescription date, linked doctor, and optional notes. All fields are editable after initial entry.

### FR-009 — Prescription list and detail
Scrollable list of all prescriptions with doctor name, date, and `StatusBadge` (Pending / Extracted / Needs Review / Error). Detail screen shows original document alongside extracted medicines and metadata.

### FR-010 — Prescription deletion
Deletion requires explicit confirmation. Cascades to linked `PrescriptionMedicine` records. Triggers recalculation of duplicate alerts. Reflected in history summaries. Patient data deletion also covered by FR-039.

### FR-011 — OCR trigger and status
OCR is triggered automatically after upload and manually from the detail screen. A progress indicator is shown while OCR runs. The WorkManager job runs in the background and does not block the UI.

### FR-012 — Structured extraction fields
Fields extracted per medicine: doctor name, clinic info, prescription date, medicine name (brand), active ingredient (when reliably identifiable), strength, formulation, dosage instructions, frequency, route, duration. Fields absent from the document are left empty — never inferred or fabricated.

### FR-013 — Confidence score and review state
Each extracted field stores a confidence score (0.0–1.0) and a review state: `PENDING_REVIEW`, `REVIEWED`, or `REJECTED`. Fields below a configurable threshold are highlighted. A prescription is not fully processed until all `PENDING_REVIEW` items are resolved.

### FR-014 — Patient review interface
The review screen shows each field with its confidence indicator. The patient can confirm, edit, or reject each value. Patient edits are stored in `patientValue`; the original OCR output is preserved in `rawValue`. Both are retained permanently.

### FR-015 — OCR error handling
On OCR failure (service unavailable, unreadable image, timeout), the prescription is marked `ERROR`. A plain-language explanation is shown with options to retry or enter details manually. The original document is always retained.

### FR-016 — Manual medicine entry
The patient can add medicine entries manually without OCR. Manually entered fields are tagged `source = MANUAL`. Manual entries follow the same review-state model as extracted entries.

### FR-039 — Data export and deletion (prescription scope)
Deletion of a prescription removes the source document and all extracted data from both local storage and the backend when sync is enabled. The patient is shown a plain-language warning before confirmation. Deletion timeline and backup retention limitations are explained.

---

## 3. Tickets and acceptance criteria

### MHA-016 — Patient profile entity and screen
Room entity and DAO for `Patient`. Profile screen for name and date of birth. Hilt-injected `PatientViewModel`.

Key ACs:
- Patient is created on first launch if none exists.
- Profile edits are persisted immediately and reflected on next app open.
- Profile screen passes `AccessibilityChecks`.

### MHA-017 — Doctor profile entity and screen
Room entity and DAO for `Doctor`. List and edit screens. Specialty picker uses a predefined list.

Key ACs:
- Doctor list is empty-stated with a call-to-action.
- Deleting a doctor that has linked prescriptions shows a warning; deletion is blocked until prescriptions are reassigned or deleted.
- Specialty list is not editable at runtime (data-driven from a string resource array).

### MHA-018 — Prescription entity and Room DAO
Room entity for `Prescription` with fields: id, patientId, doctorId, date, sourceFilePath, status (`PENDING`, `PROCESSING`, `EXTRACTED`, `NEEDS_REVIEW`, `ERROR`), createdAt, updatedAt.

Key ACs:
- Schema export JSON is updated and committed.
- DAO unit tests cover: insert, queryByPatient, queryByDoctor, deleteById (cascade verified).
- `sourceFilePath` stores a relative path within app-private storage — never an absolute path.

### MHA-019 — Camera capture flow
CameraX integration for prescription capture. Preview screen with capture button. Post-capture confirmation showing the image with options to accept or retake.

Key ACs:
- UI test uses mock CameraX output — no real camera required.
- Accepted image is written to `getFilesDir()/prescriptions/<uuid>.jpg`.
- Retake discards the previous capture without writing to storage.

### MHA-020 — File picker import
Android Photo Picker integration for JPEG/PNG. System file picker for PDF. Imported file is copied to app-private storage.

Key ACs:
- Original file is not modified; only the copy in app-private storage is used.
- Unsupported file types show an error message without crashing.
- Files larger than a configurable limit (default: 20 MB) show an error message.

### MHA-021 — Prescription metadata entry screen
Form screen for: prescription date (date picker), doctor (dropdown from doctor list), and patient notes (text field). All fields are pre-populated from OCR where available.

Key ACs:
- Date picker enforces past-or-today constraint.
- Doctor dropdown is pre-filtered to the patient's existing doctor list; new doctor can be created inline.
- Unsaved edits are preserved on navigation away and restored on return.

### MHA-022 — Prescription list screen
`PrescriptionListScreen` with `LazyColumn`, `StatusBadge` per item, sort by date descending. `PrescriptionViewModel` exposes a `Flow<List<PrescriptionSummary>>`.

Key ACs:
- List updates reactively when a prescription is added, updated, or deleted.
- Empty state uses the shared `EmptyState` composable.
- Each list item has a touch target ≥ 48×48 dp.

### MHA-023 — Prescription detail screen
Displays original document (zoomable image or PDF viewer), extracted medicine list, metadata, and status badge. Manual entry button is always visible.

Key ACs:
- PDF viewer handles password-protected PDFs by showing an error (not a crash).
- Delete action requires a confirmation dialog using `ConfirmationDialog` component.
- Source document is visible without network access (offline support).

### MHA-024 — PrescriptionMedicine entity and Room DAO
Room entity for `PrescriptionMedicine` with fields: id, prescriptionId, rawValue (per field map), patientValue (per field map), confidenceScore, reviewState, source (`OCR` / `MANUAL`), createdAt.

Key ACs:
- Schema export updated and committed.
- `reviewState` transitions enforced in `ValidationUtils` — invalid transitions throw `IllegalStateException`.
- DAO tests cover all valid state transitions and verify invalid transitions are rejected.

### MHA-025 — OCR WorkManager job
`OcrWorker` implemented with `CoroutineWorker`. Accepts `prescriptionId`. Updates prescription status to `PROCESSING` on start, `EXTRACTED` or `NEEDS_REVIEW` on completion, `ERROR` on failure. Retries up to three times on transient failure.

Key ACs:
- `TestWorkerBuilder` test verifies status transitions for success, partial-confidence, and failure cases.
- Worker never logs prescription text or extracted medicine content.
- Extracted fields with missing values are stored as absent — not fabricated.

### MHA-026 — Structured extraction and confidence scoring
OCR pipeline produces a typed `ExtractionResult` Pydantic model on the backend. Each field carries `rawValue`, `confidenceScore`, and `fieldType`. Backend validates the model before persisting.

Key ACs:
- `ExtractionResult` schema passes mypy strict mode.
- Fields missing from the document are `null` in the response — not empty strings or placeholder text.
- Backend unit tests cover: all fields present, partial fields, zero fields extracted.

### MHA-027 — OCR review screen (Android)
`OcrReviewScreen` shows each `PrescriptionMedicine` field with `ConfidenceIndicator`. Patient can tap a field to confirm, edit (opens inline text field), or reject. Save updates `patientValue` and `reviewState`. Original `rawValue` is never modified.

Key ACs:
- Editing a field stores the new value in `patientValue`; `rawValue` is unchanged (verified in DAO test).
- Rejecting a field sets `reviewState = REJECTED`; the field remains visible with a strikethrough style.
- Screen passes `AccessibilityChecks`.

### MHA-028 — Confidence threshold configuration
A `confidenceThreshold` value (default: 0.75) is defined in `BuildConfig` and overridable via remote config. Fields below threshold are highlighted with `ConfidenceIndicator` in the amber/warning state.

Key ACs:
- Changing the threshold in tests correctly re-classifies fields above/below/at the boundary (boundary value tests required).
- Threshold is not hardcoded as a magic number anywhere other than the default definition.

### MHA-029 — Manual medicine entry screen
`ManualMedicineEntryScreen` with fields matching the structured extraction schema. All fields optional except medicine name. Saved entries are tagged `source = MANUAL` and enter `PENDING_REVIEW` state.

Key ACs:
- No field is pre-filled from OCR when source is `MANUAL`.
- Manual entries appear in the prescription detail medicine list with a "Manual" label.
- Validation rejects an empty medicine name with an inline error.

### MHA-030 — OCR error state and retry
When `OcrWorker` returns `ERROR`, the prescription detail screen shows an `ErrorBanner` with a plain-language message and a retry button. Retry re-enqueues the `OcrWorker`.

Key ACs:
- Retry button is accessible (≥ 48×48 dp, labelled for TalkBack).
- Retry does not create a duplicate `OcrWorker` if one is already enqueued.
- Original document is visible and downloadable in the error state.

### MHA-031 — Prescription CRUD backend API
FastAPI endpoints: `POST /prescriptions`, `GET /prescriptions`, `GET /prescriptions/{id}`, `PATCH /prescriptions/{id}`, `DELETE /prescriptions/{id}`. All endpoints require authentication and enforce patient-scoped authorisation.

Key ACs:
- `DELETE /prescriptions/{id}` cascades to `PrescriptionMedicine` records.
- Cross-patient access returns HTTP 404 (not 403).
- `TestClient` integration tests cover: create, read, update, delete, and cross-patient access attempt.

### MHA-032 — Prescription medicines backend API
FastAPI endpoints: `POST /prescriptions/{id}/medicines`, `GET /prescriptions/{id}/medicines`, `PATCH /prescriptions/{id}/medicines/{mid}`, `DELETE /prescriptions/{id}/medicines/{mid}`. Supports review state updates.

Key ACs:
- `PATCH` endpoint validates `reviewState` transitions server-side; invalid transitions return HTTP 422.
- `rawValue` field is immutable after creation — `PATCH` cannot modify it.
- All endpoints return HTTP 404 for cross-patient access.

### MHA-033 — Offline support and sync status
`PrescriptionRepository` is local-first: reads always come from Room, writes are enqueued for sync when offline. A `SyncStatusBanner` composable shows: Online / Offline / Syncing / Sync Error. Unsent edits are preserved across app restarts.

Key ACs:
- In airplane-mode test: create prescription, verify it is stored in Room, toggle network on, verify sync completes without data loss.
- `SyncStatusBanner` is visible on the prescription list and detail screens.
- No crash when toggling airplane mode during an active save operation.

---

## 4. Implementation order

1. **MHA-016** — Patient profile (identity anchor for all other entities)
2. **MHA-017** — Doctor profile (required before prescriptions can be created)
3. **MHA-018** — Prescription entity and Room DAO (schema foundation for this spec)
4. **MHA-024** — PrescriptionMedicine entity and Room DAO (depends on prescription entity)
5. **MHA-019** — Camera capture flow (input path 1)
6. **MHA-020** — File picker import (input path 2)
7. **MHA-021** — Prescription metadata entry screen (follows capture/import)
8. **MHA-022** — Prescription list screen (requires prescription entity)
9. **MHA-023** — Prescription detail screen (requires list screen navigation)
10. **MHA-025** — OCR WorkManager job (requires PrescriptionMedicine entity)
11. **MHA-026** — Structured extraction and confidence scoring (backend; requires MHA-008)
12. **MHA-027** — OCR review screen (requires extraction output and PrescriptionMedicine DAO)
13. **MHA-028** — Confidence threshold configuration (applied during review screen work)
14. **MHA-029** — Manual medicine entry screen (parallel to OCR review)
15. **MHA-030** — OCR error state and retry (finalises OCR flow)
16. **MHA-031** — Prescription CRUD backend API (requires FastAPI scaffold MHA-008)
17. **MHA-032** — Prescription medicines backend API (depends on MHA-031)
18. **MHA-033** — Offline support and sync status (integrates all of the above)

---

## 5. Out of scope

- Medicine explorer and status grouping (medicine-intelligence spec).
- Duplicate detection algorithm and alerts (medication-safety spec).
- History summarisation and PDF generation (medical-history spec).
- Reminders and dose logging (medication-safety spec).
- Database encryption at rest (security-and-release spec).

---

## 6. Notes and constraints

- `sourceFilePath` is always a path within `getFilesDir()` — never a content URI or absolute external path.
- OCR worker must never log prescription text, extracted medicine names, or document content.
- `rawValue` is immutable after first write; any edit path must write to `patientValue` only.
- All test fixtures use synthetic data (e.g. "Synthamol 500mg", "Dr. Synthetic", "Test Patient Alpha").
- Review state transitions are enforced in `ValidationUtils` and tested with 100% branch coverage.
- The confidence threshold boundary (exactly at threshold) must be a named test case.
