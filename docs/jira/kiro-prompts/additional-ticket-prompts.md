# MediHistory AI — Epic Sprint Planning Guide

This document is a sprint planning coordination guide for the 12 epics of MediHistory AI (MHA-E01 through MHA-E12). It is **not** a code-generation task. Use it to plan sprint scope, order tickets within each epic, and identify cross-epic dependencies before assigning work to Kiro.

Each section describes: the epic's product purpose, its member tickets (from the Jira backlog), an acceptance gate for epic completion, and practical sprint planning notes.

---

## MHA-E01 — Foundation: Repository, CI, DB, Auth, Backend Scaffold

### Purpose
Establishes every shared technical foundation that all other epics depend on: repository structure, Gradle build system, version catalog, Room database, authentication scaffold, and FastAPI backend. Nothing can be built without this epic being complete.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-001 | Repository setup and Gradle project scaffold |
| MHA-002 | Architecture Decision Records (ADRs) |
| MHA-003 | CI scaffold — GitHub Actions android-ci pipeline |
| MHA-004 | Dependency management — pinned versions and security audit |
| MHA-005 | Environment configuration — no secrets in APK |
| MHA-006 | Room database setup — schema, migrations, and test harness |
| MHA-007 | Authentication scaffold — login and session management |
| MHA-008 | FastAPI backend scaffold — project structure and health check |

### Acceptance Gate
All eight tickets are done. `./gradlew assembleDebug` and the android-ci pipeline pass on a clean checkout. FastAPI `/health` returns 200. Room schema version 1 is exported. No hardcoded secrets exist in any committed file. JWT authentication is end-to-end verified.

### Sprint Planning Notes
- **Order strictly:** MHA-001 → MHA-004, MHA-005 → MHA-006, MHA-007, MHA-008 in parallel once MHA-001 is done. MHA-002 and MHA-003 can be done in parallel with MHA-004/005.
- MHA-001 is the gate for everything; do not start any other ticket in this epic until it builds.
- MHA-006 (Room) and MHA-008 (FastAPI) can be parallelised across Android and backend engineers.
- MHA-005 (no secrets in APK) is a prerequisite for any ticket that touches environment config; confirm the pattern is agreed before MHA-007.
- This epic has no external dependencies — it should be Sprint 1.

---

## MHA-E02 — Android UI: Material 3, Navigation, Component Library

### Purpose
Builds the visual and navigation foundation for all Android screens: the Material 3 theme, bottom navigation, shared composable components, design tokens, accessibility baseline, and the top app bar. Every subsequent UI ticket reuses what this epic produces.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-009 | Material 3 theme — colours, typography, and shapes |
| MHA-010 | Navigation scaffold — NavHost and bottom navigation bar |
| MHA-011 | Compose component library — cards, chips, badges, and buttons |
| MHA-012 | Design tokens — spacing, elevation, and radius |
| MHA-013 | Accessibility baseline — touch targets, content descriptions |
| MHA-014 | Top app bar — title, back navigation, and action overflow |
| MHA-015 | Navigation map and screen-to-route mapping documentation |

### Acceptance Gate
All seven tickets are done. The four primary navigation destinations render. All shared components pass AccessibilityChecks. No hardcoded hex or dp values exist outside DesignTokens. Navigation map documentation matches implemented routes.

### Sprint Planning Notes
- **Order:** MHA-009 first (theme), then MHA-010, MHA-011, MHA-012 in parallel, then MHA-013, MHA-014, MHA-015.
- MHA-009 (theme) is the gate for all other E02 tickets.
- MHA-012 (design tokens) should be completed before MHA-013 (accessibility baseline) to ensure the token system is in place.
- MHA-015 (navigation map doc) should be the last ticket — it documents the final state.
- **Dependency note:** MHA-E02 depends on MHA-E01 (MHA-001 for project structure, MHA-009 needs a compiling project). Start E02 Sprint 1 or Sprint 2 alongside E01 completion.

---

## MHA-E03 — Patient and Doctor Data Layer

### Purpose
Defines the core domain entities (Patient, Doctor) with Room DAOs, repositories, CRUD UI screens, shared validation utilities, and offline support. This is the data foundation for all patient-scoped features.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-016 | Patient profile — Room entity, DAO, and repository |
| MHA-017 | Doctor profile — Room entity, DAO, and repository |
| MHA-018 | Patient profile — CRUD UI screens |
| MHA-019 | Doctor profile — CRUD UI screens |
| MHA-020 | Patient-doctor relationship and prescription linkage |
| MHA-021 | Data validation — shared validation utilities |
| MHA-022 | Offline support for patient and doctor data |

### Acceptance Gate
All seven tickets are done. Patient and doctor CRUD works fully offline. All DAO queries are patient-scoped (no cross-patient leakage). ValidationUtils has 100% branch coverage. The offline indicator composable renders correctly.

### Sprint Planning Notes
- **Order:** MHA-016 and MHA-017 in parallel (data layer first), then MHA-018 and MHA-019 (UI), then MHA-020 (linkage), MHA-021 (validation), MHA-022 (offline).
- MHA-021 (ValidationUtils) can be done in parallel with MHA-018/019 since it has no UI dependency.
- **Dependency notes:** Requires MHA-006 (Room setup from E01) and MHA-009 (theme from E02) before MHA-018/019.
- This epic is the prerequisite for E04 (Prescription Management and OCR).

---

## MHA-E04 — Prescription Management and OCR

### Purpose
Enables patients to capture or import prescription documents (camera + file picker), runs OCR to extract medicine fields, allows patient review and correction of extracted data, and handles error/retry flows. This is the primary data-ingestion pipeline.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-023 | Prescription — Room entity, DAO, and repository |
| MHA-024 | Prescription list screen |
| MHA-025 | Prescription detail screen |
| MHA-026 | Camera capture with CameraX — confirmation step |
| MHA-027 | Photo Picker and file picker — import prescription |
| MHA-028 | OCR service interface and WorkManager job |
| MHA-029 | Confidence scoring and ExtractedField model |
| MHA-030 | OCR review UI — confirm, edit, and reject extracted fields |
| MHA-031 | OCR error handling and retry |
| MHA-032 | Manual medicine entry screen |
| MHA-033 | Prescription CRUD — backend FastAPI API |

### Acceptance Gate
All eleven tickets are done. A prescription can be captured via camera or imported via file picker, persisted to app-private storage with a SHA-256 hash, OCR-processed by OcrWorker, reviewed field-by-field by the patient, and corrected or rejected. Error and retry flows work. Manual entry is available as a fallback. Backend CRUD is patient-scoped.

### Sprint Planning Notes
- **Order:** MHA-023 (entity) → MHA-024, MHA-025 (list/detail) in parallel, then MHA-026, MHA-027 (capture/import) in parallel, then MHA-028 (OCR service) → MHA-029 (confidence model) → MHA-030 (review UI) → MHA-031 (error/retry) and MHA-032 (manual entry) in parallel.
- MHA-033 (backend API) can be developed in parallel with MHA-026–032 by a backend engineer.
- MHA-029 (confidence scoring/ExtractedField) is a prerequisite for MHA-030 and MHA-031.
- **Dependency notes:** Requires MHA-016 (Patient entity), MHA-006 (Room), MHA-007 (auth) from E01/E03.
- Camera (MHA-026) and file picker (MHA-027) can be parallelised across two engineers.

---

## MHA-E05 — Medicine Explorer and Intelligence

### Purpose
Provides the medicine explorer screen: a browsable, searchable list of all medicines extracted from prescriptions, grouped by status (Current/Past/Unknown), with filtering, source-prescription links, and patient-assigned status management.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-034 | Medicine — Room entity, DAO, and repository |
| MHA-035 | Medicine explorer screen — list with lazy loading |
| MHA-036 | Medicine search and filter — search bar and filter chips |
| MHA-037 | Medicine detail screen — all prescription occurrences |
| MHA-038 | Medicine status assignment — Current, Past, Unknown |
| MHA-039 | Medicine-prescription source link — navigation |
| MHA-040 | Medicine list — backend FastAPI API |

### Acceptance Gate
All seven tickets are done. The medicine explorer lists all medicines grouped by Current/Past/Unknown. Search and filters work with AND logic. Source links navigate to the correct prescription. Status is only patient-assigned (never auto-assigned). Backend API is patient-scoped.

### Sprint Planning Notes
- **Order:** MHA-034 (entity) → MHA-035 (list) → MHA-036 (search/filter) and MHA-037 (detail) in parallel → MHA-038 (status) → MHA-039 (source links).
- MHA-040 (backend API) can be developed in parallel with MHA-035–039.
- **Dependency notes:** Requires MHA-023 (Prescription entity), MHA-029 (ExtractedField) from E04.
- MHA-038 (status assignment) must confirm no auto-assignment logic anywhere — review existing code before closing.

---

## MHA-E06 — Duplicate Detection and Alerts

### Purpose
Implements deterministic (non-AI) duplicate detection across a patient's reviewed medicines by normalising active ingredients, with an alert list and detail screen. All findings are labelled as "potential duplicates" — never clinical conclusions.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-041 | Duplicate detection algorithm — normalised active ingredient matching |
| MHA-042 | Duplicate alert — Room entity, DAO, and generation workflow |
| MHA-043 | Duplicate alert list screen |
| MHA-044 | Duplicate alert detail screen |
| MHA-045 | Mark-as-reviewed action and alert state persistence |
| MHA-046 | Combination medicine handling in duplicate detection |
| MHA-047 | Duplicate alert — backend FastAPI API |

### Acceptance Gate
All seven tickets are done. DuplicateDetector is deterministic, handles synonyms and combination products. Alert list shows correct ordering. Detail screen displays all required fields with "Potential duplicate" label and disclaimer. Mark-as-reviewed is reversible. Backend API is patient-scoped.

### Sprint Planning Notes
- **Order:** MHA-041 (algorithm) → MHA-042 (entity + generation workflow) → MHA-043 (list) → MHA-044 (detail) → MHA-045 (mark as reviewed) and MHA-046 (combination handling) in parallel. MHA-047 (backend) in parallel with MHA-043–046.
- MHA-041 must be done first — the algorithm is the foundation.
- MHA-046 (combination handling) extends MHA-041; read MHA-041 carefully before starting MHA-046.
- **Dependency notes:** Requires MHA-029 (ExtractedField, reviewState) from E04.
- The "Potential duplicate" label and disclaimer text are product requirements — do not soften them.

---

## MHA-E07 — Medical History and AI Summarisation

### Purpose
Generates versioned medical history summaries from reviewed prescription and medicine data, using an AI summarisation service with strict guardrails: no invented diagnoses, no invented allergies, all statements linked to source records. Displays sections by type (DOCUMENTED, PATIENT_REPORTED, UNCERTAIN, AI_GENERATED).

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-048 | Medical history — Room entity and version model |
| MHA-049 | History generation service interface and AI summarisation integration |
| MHA-050 | Medical history screen — list and detail views |
| MHA-051 | History source confidence display |
| MHA-052 | History generation trigger and exclusion list |
| MHA-053 | Medical history — backend FastAPI API |

### Acceptance Gate
All six tickets are done. History generation only uses REVIEWED records. Exclusion list is shown before confirming. AI_GENERATED sections are clearly labelled. No diagnoses, allergies, or treatment details are invented when absent from source data. Source references are tappable. Versions are immutable.

### Sprint Planning Notes
- **Order:** MHA-048 (entity) → MHA-049 (service interface + AI integration) → MHA-050 (list/detail) → MHA-051 (confidence display) and MHA-052 (trigger/exclusion) in parallel. MHA-053 (backend) in parallel with MHA-050–052.
- MHA-049 is the highest-risk ticket — the AI system prompt adversarial tests are critical. Allocate extra review time.
- **Dependency notes:** Requires MHA-029 (reviewState from E04) and MHA-034 (Medicine from E05).
- Consider scheduling a product safety review of MHA-049's system prompt before the sprint demo.

---

## MHA-E08 — New Doctor Mode and PDF Export

### Purpose
Allows patients to compile a shareable report for a new doctor: select prescriptions and medicines, generate a PDF on-device, preview it, add consent confirmation, and share via the Android share sheet. All sharing requires explicit patient action and a consent preview step.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-054 | New Doctor Mode — content selection UI |
| MHA-055 | PDF generation service — on-device PDF creation |
| MHA-056 | PDF preview screen |
| MHA-057 | Consent confirmation screen and share action |
| MHA-058 | Share and export — backend API for audit logging |

### Acceptance Gate
All five tickets are done. PDF is generated on-device without network. Disclaimer is present in every PDF. Consent screen is shown before every share. Audit log entry created on share. Sharing without explicit confirmation is impossible.

### Sprint Planning Notes
- **Order:** MHA-054 (content selection) → MHA-055 (PDF generation) → MHA-056 (preview) → MHA-057 (consent + share). MHA-058 (backend audit log API) in parallel with MHA-056–057.
- MHA-055 involves an offline PDF library evaluation — resolve the iText licensing question before writing the ticket implementation.
- **Dependency notes:** Requires MHA-050 (history screen from E07) and MHA-034 (Medicine from E05).
- The consent confirmation screen (MHA-057) is a privacy-critical step — do not allow any path to bypass it.

---

## MHA-E09 — Reminders and Adherence

### Purpose
Enables patients to set medicine reminders based on prescription dosage instructions, receive notifications, log dose events (Taken/Skipped/Missed), and view a 7-day adherence summary. All data is patient-reported — never fed into the history summariser as clinically documented data.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-059 | Reminder — Room entity, DAO, and repository |
| MHA-060 | Reminder setup UI — create and edit reminders |
| MHA-061 | WorkManager notification scheduling for reminders |
| MHA-062 | Dose logging — taken, skipped, and missed |
| MHA-063 | Reminder list and adherence tracking screen |

### Acceptance Gate
All five tickets are done. Reminders fire at the correct time, include the medicine name and prescription date in the notification text, respect Do Not Disturb. Dose log entries always have patientReported = true. Adherence summary is accurate. Disabling a reminder cancels the WorkManager job.

### Sprint Planning Notes
- **Order:** MHA-059 (entity) → MHA-060 (setup UI) and MHA-061 (notification scheduling) in parallel → MHA-062 (dose logging) → MHA-063 (list/adherence screen).
- MHA-061 (WorkManager notifications) requires a 1-minute test reminder to verify timing — plan for device/emulator time in the sprint.
- **Dependency notes:** Requires MHA-034 (Medicine from E05) for the medicine picker in MHA-060.
- Adherence data must NEVER appear as "documented" data in the history summariser — confirm this boundary with the team at sprint kick-off.

---

## MHA-E10 — Interaction Checking

### Purpose
Provides an optional, provider-configured drug interaction check screen. When the provider is not configured, shows a required disclaimer rather than suggesting interactions are absent. Never claims any medicine combination is safe. Integrates with provider configuration in settings.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-064 | Interaction check service interface and provider configuration |
| MHA-065 | Interaction check UI — available state |
| MHA-066 | Interaction check UI — unavailable state |
| MHA-067 | Provider configuration in settings |

### Acceptance Gate
All four tickets are done. The unavailable state shows the exact required message from FR-038 verbatim. No "safe" language appears anywhere. The available state recommends discussing with a doctor/pharmacist on every result. Provider configuration is correctly reflected in the service.

### Sprint Planning Notes
- **Order:** MHA-064 (service interface) → MHA-065 and MHA-066 in parallel → MHA-067 (settings).
- MHA-064 is the lowest story-point ticket in this epic but is the critical gate.
- The exact required message from FR-038 must be confirmed with the product owner before sprint start — it must not be paraphrased.
- **Dependency notes:** Requires MHA-034 (Medicine/status from E05) for the current medicines list.
- This epic is Low priority — consider scheduling after E08 to avoid blocking higher-priority security work.

---

## MHA-E11 — Security, Privacy, and Data Management

### Purpose
Implements the security and privacy features required for production: Android Keystore key management, database encryption, data export (encrypted ZIP), account deletion, opt-in cloud backup, consent management UI, and the local audit log. All features are necessary before any production launch.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-068 | Android Keystore — key management and encryption evaluation |
| MHA-069 | Database encryption evaluation and implementation |
| MHA-070 | Data export — encrypted ZIP with all patient data |
| MHA-071 | Account deletion — local and backend data removal |
| MHA-072 | Cloud backup — opt-in, status, and encryption |
| MHA-073 | Consent management UI |
| MHA-074 | Audit log — metadata only, no document content |

### Acceptance Gate
All seven tickets are done. Room database is encrypted and not readable as plaintext. Export produces a passphrase-encrypted ZIP. Account deletion removes all local data. Cloud backup is disabled by default. Consent records are local-only. Audit log contains no document content. Threat model document exists.

### Sprint Planning Notes
- **Order:** MHA-068 (Keystore evaluation) → MHA-069 (DB encryption) in parallel with MHA-074 (audit log) → MHA-070 (export) and MHA-071 (deletion) and MHA-072 (backup) in parallel → MHA-073 (consent UI).
- MHA-068 is the gate for MHA-069, MHA-070, and MHA-072 (all depend on the Keystore key).
- MHA-074 (audit log) can be started in parallel with MHA-068 since it has no Keystore dependency.
- **Cross-epic dependency note:** MHA-069 (DB encryption) may require changes to MHA-006 (Room setup from E01) — review the migration path before sprint start.
- **Security gate:** Cross-account exposure or critical/high security findings block release. Do not close E11 until the threat model (MHA-068) has been reviewed by a qualified reviewer.
- A qualified legal/security review of India privacy requirements is recommended before launch (not a compliance certification — see privacy-security steering).

---

## MHA-E12 — Testing, CI/CD, and Release Readiness

### Purpose
Establishes the full automated test suite (Android unit, UI, and backend), extends CI/CD pipelines to include emulator tests and backend coverage enforcement, and produces the release checklist and synthetic fixture library needed for a production-ready release.

### Linked Tickets
| Ticket | Summary |
|--------|---------|
| MHA-075 | Android unit test suite — ViewModels and repositories |
| MHA-076 | Android unit test suite — duplicate detection and validation |
| MHA-077 | Android UI test suite — Espresso and Compose testing |
| MHA-078 | Backend pytest suite — API endpoints and auth |
| MHA-079 | API integration tests — end-to-end contract tests |
| MHA-080 | GitHub Actions android-ci — full pipeline with emulator |
| MHA-081 | GitHub Actions backend-ci — lint, type-check, pytest, coverage |
| MHA-082 | Release checklist and synthetic test data fixtures |

### Acceptance Gate
All eight tickets are done. Android unit coverage ≥80% for the data layer. Backend coverage ≥90% for the API layer. DuplicateDetector and ValidationUtils have 100% branch coverage. Full android-ci pipeline (including emulator) and backend-ci pipeline pass on every PR. Release checklist exists with all six categories and is linked from README. Synthetic fixture library contains no real PII.

### Sprint Planning Notes
- **Order:** MHA-075 and MHA-076 in parallel → MHA-077 (UI tests, depends on implemented screens) in parallel with MHA-078 (backend tests). MHA-079 (integration tests) after MHA-078. MHA-080 after MHA-077. MHA-081 after MHA-078. MHA-082 last (requires all tests to be in place for the checklist).
- MHA-075–076 (unit tests) can start as soon as E04–E06 implementations are complete.
- MHA-077 (UI tests) requires all five flows to be implemented — do not start until E04, E06, and E07 screens exist.
- **Dependency note:** MHA-080 (emulator CI) depends on MHA-003 (CI scaffold from E01) — extend, do not replace.
- MHA-082 (release checklist) is the final epic gate — it must be the last ticket closed in E12 and in the overall project.
- Fixture data in MHA-082 must be reviewed by two team members before the checklist is marked complete.

---

## Epic Dependency Summary

```
E01 (Foundation)
 └─► E02 (Android UI)
 └─► E03 (Data Layer)
      └─► E04 (Prescription / OCR)
           └─► E05 (Medicine Explorer)
                └─► E06 (Duplicate Detection)
                └─► E07 (History / AI)
                     └─► E08 (New Doctor Mode / PDF)
                └─► E09 (Reminders)
                └─► E10 (Interaction Checking)
E11 (Security / Privacy) — depends on E01 (Room), can be developed in parallel with E04–E10
E12 (Testing / CI) — depends on E04–E11 implementations being in place
```

## How to Use This Guide in Sprint Planning

1. **Sprint 1:** Close E01. Begin E02 and E03 in parallel (data layer can start day 1 of sprint 1 once MHA-001 is done).
2. **Sprint 2:** Finish E02 and E03. Begin E04 (Prescription / OCR).
3. **Sprint 3–4:** Complete E04. Begin E05 and start E11 security evaluation (MHA-068) in parallel.
4. **Sprint 5:** Complete E05. Begin E06 and E07 in parallel.
5. **Sprint 6:** Complete E06 and E07. Begin E08 and E09.
6. **Sprint 7:** Complete E08, E09, E10. Begin E11 (remaining tickets after MHA-068) and E12 unit tests.
7. **Sprint 8:** Complete E11 and E12. Run final release checklist review.
8. **Security gate:** E11 must be reviewed by a qualified reviewer before any production release. Do not advance E12 acceptance gate until E11 security review is resolved.
9. **Each ticket:** Use the corresponding `MHA-NNN.md` prompt file in this directory as the Kiro implementation prompt. Do not modify prompt files after implementation begins — create a new version if requirements change.
