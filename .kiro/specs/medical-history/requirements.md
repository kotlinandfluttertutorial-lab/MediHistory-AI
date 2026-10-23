# Requirements — Medical History

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F06, F07
**Jira tickets:** MHA-048, MHA-049, MHA-050, MHA-051, MHA-052, MHA-053, MHA-054, MHA-055, MHA-056, MHA-057, MHA-058
**Functional requirements:** FR-027, FR-028, FR-029, FR-030, FR-031, FR-032, FR-033, FR-034
**Dependencies:** prescription-management spec (reviewed PrescriptionMedicine records), medicine-intelligence spec (MedicineEntity with status assignments)

---

## 1. Purpose

This spec covers generation, display, and patient-controlled sharing of medical history summaries:

- **Medical history entity** — versioned summary record with data snapshot reference
- **AI summarisation service interface** — contract between the Android app and the backend summarisation job; the underlying AI provider is swappable
- **History screens** — list of summary versions, current summary view, source confidence display
- **History generation trigger** — patient-initiated, from reviewed records only
- **PDF export** — locally generated, includes patient name, summary, source references, selected prescription images, and a mandatory disclaimer
- **New Doctor Mode content selection** — patient chooses what to include before sharing
- **Consent screen** — preview of exact content to be shared, plain-language consent, destination app name
- **Share audit logging** — destination app name, timestamp, document ID (no document content logged)
- **History and share backend APIs** — create, read, version management, with patient-scoped authorisation

Every statement in a generated summary must link to its source document. AI-generated sections are labelled as such. The summary is not a clinical report.

---

## 2. Functional requirements

### FR-027 — History generation
The patient triggers generation from the History screen. Only `REVIEWED` and confirmed `PrescriptionMedicine` records are included. Unreviewed or `REJECTED` extractions are excluded. Each significant statement links to its source document.

### FR-028 — Summary content structure
The summary covers: documented conditions (from prescription context only — not AI-inferred), patient-reported allergies, medicines with dates and prescribing doctor, any patient-attached laboratory results, and prescription dates with doctor information. Each section is labelled: Documented / Patient-reported / Uncertain / AI-generated.

### FR-029 — No clinical invention
The summarisation process never invents diagnoses, allergies, treatment instructions, or clinical history. Missing fields are omitted or marked "not available." The AI system prompt enforces this rule and must be tested with adversarial inputs.

### FR-030 — Source reference display
Each statement that derives from a source document shows an inline `SourceReferenceChip`. Tapping navigates to the linked prescription. References use the format `[DocID]` or a human-readable equivalent.

### FR-031 — Summary versioning
Each generated summary is saved as a separate version with a timestamp and the data snapshot it was generated from. The patient can view previous versions. Generating a new summary does not delete previous versions.

### FR-032 — New Doctor Mode flow
Step-by-step flow: review summary → select prescriptions/medicines to include → confirm content → generate PDF → share via Android share sheet with explicit consent confirmation. The patient can exit at any point without sending anything.

### FR-033 — PDF generation
PDF includes: patient name (as entered by the patient), summary content with source references, selected medicine list, selected prescription images as appendices, and a mandatory disclaimer stating the document is a patient-compiled record and not a clinical report. PDF is generated locally on device.

### FR-034 — Sharing consent and privacy
Before sharing: show a preview of exact content, a plain-language consent statement, and the destination app name. Patient taps a distinct confirmation button. The share action is logged in the audit log (destination app name, timestamp, document ID — no document content).

---

## 3. Tickets and acceptance criteria

### MHA-048 — MedicalHistory entity and Room DAO
Room entity for `MedicalHistory` with fields: id, patientId, generatedAt, dataSnapshotJson (serialised list of source record IDs), summaryMarkdown, versionNumber, status (`GENERATING` / `READY` / `ERROR`).

Key ACs:
- Schema export JSON is updated and committed.
- DAO tests cover: insert, queryByPatient (ordered by `generatedAt` desc), queryById.
- `dataSnapshotJson` is immutable after creation — update path is blocked in the DAO.
- `summaryMarkdown` is not logged in Logcat or server logs at any log level.

### MHA-049 — AI summarisation service interface
Define a `SummarisationService` interface in the backend with: `summarise(patientId, recordIds) -> SummaryResult`. `SummaryResult` is a Pydantic model: `summaryMarkdown`, `sourceReferences` (list of `{statementId, prescriptionId}`), `generatedAt`. The interface is backed by a real AI provider (configurable via `AI_PROVIDER` env var) and a deterministic stub for tests.

Key ACs:
- The stub always produces a valid `SummaryResult` from synthetic inputs.
- Adversarial input tests verify the AI provider's system prompt is enforced: the stub must reject attempts to generate diagnoses, invent allergies, or fabricate clinical history.
- `SummaryResult` schema passes mypy strict mode.
- Provider selection is controlled by an environment variable — no provider API keys in the APK.

### MHA-050 — History generation backend endpoint
`POST /history/generate` accepts `{patientId, recordIds}`. Validates that all `recordIds` belong to the requesting patient and are in `REVIEWED` state. Enqueues the summarisation job. Returns a `MedicalHistory` record in `GENERATING` status.

Key ACs:
- Unreviewed record IDs in the request return HTTP 422 with a clear error message.
- Cross-patient record IDs return HTTP 404.
- Job completion updates `MedicalHistory.status` to `READY` or `ERROR`.
- `TestClient` tests cover: valid generation, unreviewed records included, cross-patient records included, job failure.

### MHA-051 — History list and detail screens (Android)
`HistoryListScreen` shows all summary versions ordered by `generatedAt` descending. `HistoryDetailScreen` renders `summaryMarkdown` with inline `SourceReferenceChip` components. A `GeneratingIndicator` is shown while `status = GENERATING`.

Key ACs:
- Empty state directs the patient to complete OCR review for at least one prescription before generating a summary.
- `SourceReferenceChip` tap navigates to the correct prescription detail screen (verified in navigation test).
- Screen passes `AccessibilityChecks`.
- Markdown rendering must not allow HTML injection (use a safe Markdown renderer).

### MHA-052 — Source confidence display
Each section of the summary detail screen carries a provenance label: Documented / Patient-reported / Uncertain / AI-generated. AI-generated sections are visually distinguished (e.g. a distinct background tint and an icon). The label is announced by TalkBack.

Key ACs:
- No section is labelled "Documented" unless its content is directly traceable to a source document in `dataSnapshotJson`.
- UI test verifies the provenance label changes when a section's source is changed in a synthetic fixture.
- AI-generated label passes `AccessibilityChecks` (contrast, touch target if interactive).

### MHA-053 — History generation trigger UI
A "Generate Summary" button on `HistoryListScreen` is enabled only when at least one `REVIEWED` prescription medicine record exists. Tapping calls `POST /history/generate`. A `LoadingOverlay` is shown during generation.

Key ACs:
- Button is disabled (with a tooltip/explanation) when no reviewed records exist.
- Generation in progress prevents a second concurrent generation request (button disabled while `GENERATING`).
- On error, an `ErrorBanner` shows a plain-language message and a retry option.

### MHA-054 — Summary versioning
`HistoryListScreen` lists all versions. Each version card shows: version number, `generatedAt` timestamp, and number of source records. Tapping opens the version's detail screen. No version is deleted when a new one is generated.

Key ACs:
- Version numbers are assigned sequentially by the backend (1, 2, 3…).
- Deleting a prescription that was a source for a prior version marks that version's status as `STALE` with a visible indicator.
- DAO test verifies all versions are returned in descending order.

### MHA-055 — PDF generation (Android)
`GeneratePdfUseCase` produces a PDF using Android's `PdfDocument` API or an equivalent local library. PDF layout: title ("Patient Medical Record"), patient name, generated date, mandatory disclaimer, summary sections, medicine table, prescription image appendices.

Key ACs:
- Disclaimer text is hardcoded and not user-editable: "This document is a patient-compiled record and is not a clinical report. It should be reviewed by a qualified healthcare professional."
- PDF is written to a temporary file in `getCacheDir()` — not to shared external storage.
- PDF generation does not require network access (offline capable).
- Generated PDF file is deleted from cache after the share sheet is dismissed.

### MHA-056 — New Doctor Mode content selection
`NewDoctorModeFlow` is a multi-step Compose flow: (1) review summary, (2) select prescriptions to include (multi-select list), (3) select medicines to include (pre-selected from chosen prescriptions), (4) confirm content preview, (5) generate PDF, (6) share.

Key ACs:
- The patient can exit the flow at any step without triggering a share.
- Back navigation within the flow returns to the previous step, preserving selections.
- Step progress is visually indicated (step indicator component).
- No network request is made until the PDF share is confirmed.

### MHA-057 — Consent screen
Before the Android share sheet is invoked: display a `ConsentScreen` showing the exact document title, a summary of what is included (n prescriptions, n medicines), the destination app name (from the share intent), and a plain-language consent statement. A distinct "Share" confirmation button is required; "Cancel" exits without sharing.

Key ACs:
- "Share" button is visually distinct from "Cancel" (different colour or weight).
- Consent screen passes `AccessibilityChecks`.
- If the destination app name is unavailable, show "Unknown app" — do not silently proceed.

### MHA-058 — Share audit logging
On share confirmation, write an `AuditLog` record: patientId, event = `HISTORY_SHARED`, destinationApp, documentId, timestamp. No document content, summary text, or medicine names are included in the log entry.

Key ACs:
- `AuditLog` Room entity and DAO are created in this ticket.
- DAO test verifies no content fields are present on the `AuditLog` entity.
- Backend audit log endpoint `POST /audit` accepts the same fields and persists them server-side when sync is enabled.
- Audit log entries are never deleted by the patient (they are not part of data export deletion scope — this must be explained in the deletion warning).

---

## 4. Implementation order

1. **MHA-048** — MedicalHistory entity and Room DAO (data model anchor)
2. **MHA-049** — AI summarisation service interface (backend contract; required before generation endpoint)
3. **MHA-050** — History generation backend endpoint (requires MHA-049 and MHA-031)
4. **MHA-051** — History list and detail screens (requires entity and backend endpoint)
5. **MHA-052** — Source confidence display (extends detail screen)
6. **MHA-053** — History generation trigger UI (completes the generation flow)
7. **MHA-054** — Summary versioning (extends list screen)
8. **MHA-055** — PDF generation (required before New Doctor Mode)
9. **MHA-056** — New Doctor Mode content selection (requires PDF generation)
10. **MHA-057** — Consent screen (final step before share)
11. **MHA-058** — Share audit logging (accompanies consent screen)

---

## 5. Out of scope

- Medication reminders and dose logging (medication-safety spec).
- Duplicate detection and interaction checking (medication-safety spec).
- Database encryption at rest (security-and-release spec).
- Lab result interpretation (MVP exclusion).

---

## 6. Notes and constraints

- The AI system prompt is a server-side configuration — it is never embedded in the APK or committed to source control as plain text.
- `summaryMarkdown` is never logged at any log level, in Logcat or server logs.
- Adversarial input tests for the summarisation service must be part of the definition of done for MHA-049.
- Audit log entries are write-only from the patient's perspective; they are not surfaced in the UI in the MVP.
- PDF cache files must be cleaned up after share dismissal; a `FileCleanupWorker` or equivalent mechanism is required.
- All test fixtures use synthetic patient names, doctor names, and medicine names.
