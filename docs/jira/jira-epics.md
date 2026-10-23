# Jira Epics — MediHistory AI

**Document version:** 1.0  
**Status:** Draft — pending stakeholder review  
**Last updated:** 2025

> **Important:** The Epic IDs below (MHA-E01 … MHA-E12) are proposed local identifiers.  
> They do not yet exist in Jira. Jira will assign its own numeric IDs during import.  
> Never use these IDs as real Jira URLs or issue keys.

---

## MHA-E01 — Project Foundation and Architecture

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E01 |
| **Epic Name** | Project Foundation and Architecture |
| **Goal** | Establish a production-ready project skeleton with a working build pipeline, architecture decision records, dependency management, CI scaffold, Room database, authentication scaffold, and a FastAPI backend skeleton. All subsequent epics build on this foundation. |
| **Scope** | Repository setup, Gradle configuration, MVVM architecture scaffold, Room schema, Android Keystore evaluation baseline, FastAPI project skeleton, environment configuration (no hardcoded credentials), GitHub Actions CI baseline. |
| **Linked Features** | F10 (Privacy and Security — foundational infrastructure) |
| **Linked FRs** | FR-039, FR-040 (storage and authentication are foundation-level) |
| **Success Criteria** | Android project builds without errors. Backend FastAPI server starts and responds to a health check. CI pipeline runs on every pull request. No API keys or secrets are present in any committed file. Architecture decision records exist for all major technical choices. |

---

## MHA-E02 — Android Design System and Navigation

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E02 |
| **Epic Name** | Android Design System and Navigation |
| **Goal** | Implement a Material 3 design system with reusable components, design tokens, accessibility baseline, bottom navigation, and top app bar so that all subsequent UI epics build on a consistent, accessible foundation. |
| **Scope** | Material 3 theme (colours, typography, shapes), design token implementation, reusable Compose component library (cards, chips, badges, buttons, status indicators), bottom navigation bar (4 primary destinations), top app bar, accessibility baseline (touch targets ≥ 48 dp, content descriptions, semantic labels). |
| **Linked Features** | F01, F02, F03, F04, F05, F06, F07, F08, F09, F10 (all features use the design system) |
| **Linked FRs** | FR-003 (bottom navigation), FR-009 (prescription list UI patterns) |
| **Success Criteria** | All components pass accessibility checks (TalkBack, touch target size). Theme switches correctly between light and dark mode. Navigation scaffold renders all four bottom destinations. Zero hardcoded colour or dimension values — all use design tokens. |

---

## MHA-E03 — Patient and Doctor Records

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E03 |
| **Epic Name** | Patient and Doctor Records |
| **Goal** | Implement the data models, CRUD operations, and UI for patient profiles and doctor profiles. These entities are the structural foundation for prescription linkage. |
| **Scope** | Patient profile Room entity and DAO, doctor profile Room entity and DAO, patient CRUD UI, doctor CRUD UI, specialty picker (predefined list + Other with free text), patient-doctor relationship, offline support, data validation (required fields). |
| **Linked Features** | F02 (doctor profile management) |
| **Linked FRs** | FR-007 (doctor profile), FR-008 (prescription metadata) |
| **Success Criteria** | Patient and doctor records persist across app restarts. Deleting a doctor does not delete linked prescriptions; prescriptions show "Doctor removed" reference. Specialty list includes all required values. All CRUD operations work offline. |

---

## MHA-E04 — Prescription Management and OCR

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E04 |
| **Epic Name** | Prescription Management and OCR |
| **Goal** | Implement the full prescription lifecycle: capture or import, secure storage, background OCR extraction, confidence scoring, patient review interface, and CRUD API. Prescriptions are the primary data object in the application. |
| **Scope** | Prescription Room entity and DAO, prescription list and detail screens, CameraX camera capture with confirmation step, Android Photo Picker / file picker, app-private document storage, OCR service interface (provider-independent), WorkManager OCR job, confidence scoring per field, review state machine (PENDING_REVIEW → REVIEWED / REJECTED), patient review UI, manual medicine entry, OCR error handling and retry, prescription CRUD backend API. |
| **Linked Features** | F02, F03 |
| **Linked FRs** | FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016 |
| **Success Criteria** | Original document is never modified (hash check passes). OCR runs in the background without blocking UI. Confidence scores are stored per field. Patient edits are stored alongside (not replacing) OCR output. Manual entry works without OCR. Deletion removes all linked data and recalculates duplicate alerts. |

---

## MHA-E05 — Medicine Explorer

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E05 |
| **Epic Name** | Medicine Explorer |
| **Goal** | Build a consolidated, searchable view of all medicines across all prescriptions. Patients can explore their full medicine history, grouped by status and linked to source documents. |
| **Scope** | Medicine Room entity and DAO, medicine explorer screen (lazy loading, ≥20 items), medicine detail screen (all prescription occurrences), search bar (name + active ingredient), filter chips (doctor, specialty, date range, review state), Current/Past/Unknown status grouping (patient-assigned), source prescription links, medicine-prescription join table, medicine list backend API. |
| **Linked Features** | F04 |
| **Linked FRs** | FR-017, FR-018, FR-019, FR-020, FR-021 |
| **Success Criteria** | All medicines from all prescriptions appear in the list. Status grouping is patient-controlled. Search returns correct results for name and active ingredient. Source links navigate to the correct prescription detail. Lazy loading handles 50+ items without UI freeze. |

---

## MHA-E06 — Duplicate Ingredient Detection

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E06 |
| **Epic Name** | Duplicate Ingredient Detection |
| **Goal** | Implement a deterministic, active-ingredient-based duplicate detection engine that produces explainable alerts, handles combination products, and allows patients to mark alerts as reviewed without implying clinical clearance. |
| **Scope** | Duplicate detection algorithm (normalised active ingredient matching, deterministic — no AI inference), DuplicateAlert Room entity and DAO, duplicate alert list screen (unreviewed first), duplicate alert detail screen (all required fields + disclaimer), alert generation workflow (triggers on review state change and new medicine addition), combination medicine handling (partial match alerts), alert backend API, patient mark-as-reviewed action (timestamp only, no deletion). |
| **Linked Features** | F05 |
| **Linked FRs** | FR-022, FR-023, FR-024, FR-025, FR-026 |
| **Success Criteria** | Brand-name matching does not produce false positives. Same active ingredient under different brands produces one alert. Combination product partial matches produce reduced-confidence alerts with correct content. Reviewed alert shows disclaimer. Alert list sorts unreviewed first. |

---

## MHA-E07 — Doctor-Ready Medical History

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E07 |
| **Epic Name** | Doctor-Ready Medical History |
| **Goal** | Generate a versioned, source-referenced medical history summary from reviewed prescription records only. Never invent diagnoses, allergies, or clinical history. |
| **Scope** | MedicalHistory Room entity and DAO, history generation service interface (AI summarisation — provider-independent), history list screen (versions with timestamps), history detail screen (sections labelled by type: Documented / Patient-reported / Uncertain / AI-generated), inline source reference indicators (tappable, ≥48 dp), adversarial input tests for the summariser, history backend API. |
| **Linked Features** | F06 |
| **Linked FRs** | FR-027, FR-028, FR-029, FR-030, FR-031 |
| **Success Criteria** | Summary uses only REVIEWED records. Source reference taps navigate to correct prescription. No diagnosis or allergy is invented from absent data. Adversarial prompt test passes. Previous versions are retained and accessible. |

---

## MHA-E08 — New Doctor Mode and Export

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E08 |
| **Epic Name** | New Doctor Mode and Export |
| **Goal** | Allow patients to prepare a selected, consented summary for a new doctor consultation as a PDF, and share it via the Android share sheet with explicit consent at each step. |
| **Scope** | Content selection UI (per-prescription and per-medicine checkboxes), PDF generation service interface (on-device, no external service by default), PDF preview screen, consent confirmation screen (preview + consent statement + destination app name + explicit "Share now" button), Android share sheet integration, share action backend audit log (no document content), disclaimer in PDF. |
| **Linked Features** | F07 |
| **Linked FRs** | FR-032, FR-033, FR-034 |
| **Success Criteria** | Flow cannot skip to Share without completing all prior steps. PDF contains disclaimer. Share action requires consent screen tap. Audit log records destination app name and timestamp — no document content. PDF generated on-device without network when offline. |

---

## MHA-E09 — Medication Reminders

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E09 |
| **Epic Name** | Medication Reminders |
| **Goal** | Provide optional medication reminders based on reviewed prescription instructions and a dose logging feature that distinguishes patient-reported adherence from prescribed instructions. |
| **Scope** | Reminder Room entity and DAO, reminder setup UI (override schedule, based-on-prescription-date label), WorkManager notification scheduling, Android notification channels, dose log Room entity (patient_reported flag), dose log UI ("Taken / Skipped / Missed"), adherence summary view per medicine, reminder-prescription linkage. |
| **Linked Features** | F08 |
| **Linked FRs** | FR-035, FR-036 |
| **Success Criteria** | Reminders fire at scheduled times. Reminder UI shows prescription date it is based on. Dose log entries have patient_reported = true. Dose log data is not included in medical history without patient-reported label. Editing a reminder does not modify the source prescription. |

---

## MHA-E10 — Medication Interaction Safety

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E10 |
| **Epic Name** | Medication Interaction Safety |
| **Goal** | Implement a conditional interaction-checking feature that is only available when a licensed, appropriately maintained medication reference source is configured, and shows a safe unavailability message otherwise. |
| **Scope** | Interaction check service interface (provider-independent, configured via MED_REFERENCE_PROVIDER environment variable), interaction check UI (available and unavailable states), result display (medicines involved, interaction description, recommend clinician review), unavailability message (exact required text per FR-038 / AC-053), provider configuration in settings. |
| **Linked Features** | F09 |
| **Linked FRs** | FR-037, FR-038 |
| **Success Criteria** | Feature is inaccessible (or shows unavailable state) when MED_REFERENCE_PROVIDER is not set. Unavailable message contains exact required text. No "no interactions found" message is shown when provider is absent. Results show medicines and interaction description. |

---

## MHA-E11 — Security and Privacy

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E11 |
| **Epic Name** | Security and Privacy |
| **Goal** | Implement the privacy and security features that are non-negotiable for a medical records application: encryption, export, deletion, cloud backup opt-in, consent management, and metadata audit logging. |
| **Scope** | Android Keystore key management evaluation and implementation, encrypted database evaluation (SQLCipher or equivalent), data export (encrypted ZIP / JSON with all patient data), full account deletion (local + backend, with explanation of retention limitations), cloud backup opt-in (visible status, explicit enable/disable, encryption before upload), consent management UI, audit log (metadata only: action type, timestamp, document ID — no content). |
| **Linked Features** | F10 |
| **Linked FRs** | FR-039, FR-040 |
| **Success Criteria** | No API keys in APK or committed files. Export package is complete and decryptable. Deletion removes all local files and notifies backend. Cloud backup requires explicit opt-in. Audit log does not contain prescription text or medicine details. Database encryption evaluation is documented. |

---

## MHA-E12 — Testing, CI/CD, and Release Readiness

| Field | Value |
|-------|-------|
| **Epic ID** | MHA-E12 |
| **Epic Name** | Testing, CI/CD, and Release Readiness |
| **Goal** | Establish comprehensive automated test coverage across Android and backend, configure CI/CD pipelines, produce synthetic test fixtures, and complete the release checklist for all cross-cutting quality requirements. |
| **Scope** | Android unit test suite (ViewModels, repositories, use cases, duplicate detection algorithm), Android UI test suite (Espresso + Compose testing, all critical flows), backend pytest suite (all API endpoints, auth, validation), API integration tests (end-to-end contract tests), GitHub Actions android-ci (build, lint, unit tests, UI tests), GitHub Actions backend-ci (lint, type-check, pytest, coverage), synthetic test data fixtures (no real PII), release checklist (security review, accessibility audit, privacy review, traceability verification). |
| **Linked Features** | F01–F10 (cross-cutting) |
| **Linked FRs** | All FR-001–FR-040 (test coverage required for all requirements) |
| **Success Criteria** | All tests pass in CI. Coverage meets defined thresholds. No real PII in test fixtures. Release checklist is complete. Security and accessibility findings are documented and resolved or accepted. |
