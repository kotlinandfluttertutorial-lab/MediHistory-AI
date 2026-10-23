# MediHistory AI — Artifact Completion Report

**Generated:** 2025  
**Repository:** `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI`  
**Status:** All 82 Jira ticket prompt files created; all supporting artifacts in place.

---

## 1. Files Created (by stage)

### Stage E — Jira ticket prompt files (`docs/jira/kiro-prompts/`)

All 82 individual ticket prompts plus supporting files were created in this stage.

| File | Description |
|------|-------------|
| `docs/jira/kiro-prompts/MHA-001.md` | Repository setup and Gradle project scaffold |
| `docs/jira/kiro-prompts/MHA-002.md` | Architecture Decision Records |
| `docs/jira/kiro-prompts/MHA-003.md` | CI scaffold — GitHub Actions android-ci pipeline |
| `docs/jira/kiro-prompts/MHA-004.md` | Dependency management — pinned versions and security audit |
| `docs/jira/kiro-prompts/MHA-005.md` | Environment configuration — no secrets in APK |
| `docs/jira/kiro-prompts/MHA-006.md` | Room database setup — schema |
| `docs/jira/kiro-prompts/MHA-007.md` | Authentication scaffold — login and session management |
| `docs/jira/kiro-prompts/MHA-008.md` | FastAPI backend scaffold — project structure and health check |
| `docs/jira/kiro-prompts/MHA-009.md` | Material 3 theme — colours |
| `docs/jira/kiro-prompts/MHA-010.md` | Navigation scaffold — NavHost and bottom navigation bar |
| `docs/jira/kiro-prompts/MHA-011.md` | Compose component library foundation — cards |
| `docs/jira/kiro-prompts/MHA-012.md` | Design tokens implementation — spacing |
| `docs/jira/kiro-prompts/MHA-013.md` | Accessibility baseline — touch targets |
| `docs/jira/kiro-prompts/MHA-014.md` | Top app bar — title |
| `docs/jira/kiro-prompts/MHA-015.md` | Navigation map and screen-to-route mapping documentation |
| `docs/jira/kiro-prompts/MHA-016.md` | Patient profile — Room entity |
| `docs/jira/kiro-prompts/MHA-017.md` | Doctor profile — Room entity |
| `docs/jira/kiro-prompts/MHA-018.md` | Patient profile — CRUD UI screens |
| `docs/jira/kiro-prompts/MHA-019.md` | Doctor profile — CRUD UI screens |
| `docs/jira/kiro-prompts/MHA-020.md` | Patient-doctor relationship and prescription linkage |
| `docs/jira/kiro-prompts/MHA-021.md` | Data validation — shared validation utilities |
| `docs/jira/kiro-prompts/MHA-022.md` | Offline support for patient and doctor data |
| `docs/jira/kiro-prompts/MHA-023.md` | Prescription — Room entity |
| `docs/jira/kiro-prompts/MHA-024.md` | Prescription list screen |
| `docs/jira/kiro-prompts/MHA-025.md` | Prescription detail screen |
| `docs/jira/kiro-prompts/MHA-026.md` | Camera capture with CameraX — confirmation step |
| `docs/jira/kiro-prompts/MHA-027.md` | Photo Picker and file picker — import prescription |
| `docs/jira/kiro-prompts/MHA-028.md` | OCR service interface and WorkManager job |
| `docs/jira/kiro-prompts/MHA-029.md` | Confidence scoring and ExtractedField model |
| `docs/jira/kiro-prompts/MHA-030.md` | OCR review UI — confirm |
| `docs/jira/kiro-prompts/MHA-031.md` | OCR error handling and retry |
| `docs/jira/kiro-prompts/MHA-032.md` | Manual medicine entry screen |
| `docs/jira/kiro-prompts/MHA-033.md` | Prescription CRUD — backend FastAPI API |
| `docs/jira/kiro-prompts/MHA-034.md` | Medicine — Room entity |
| `docs/jira/kiro-prompts/MHA-035.md` | Medicine explorer screen — list with lazy loading |
| `docs/jira/kiro-prompts/MHA-036.md` | Medicine search and filter — search bar and filter chips |
| `docs/jira/kiro-prompts/MHA-037.md` | Medicine detail screen — all prescription occurrences |
| `docs/jira/kiro-prompts/MHA-038.md` | Medicine status assignment — Current / Past / Unknown |
| `docs/jira/kiro-prompts/MHA-039.md` | Medicine-prescription source link — navigation |
| `docs/jira/kiro-prompts/MHA-040.md` | Medicine list — backend FastAPI API |
| `docs/jira/kiro-prompts/MHA-041.md` | Duplicate detection algorithm — normalised active ingredient matching |
| `docs/jira/kiro-prompts/MHA-042.md` | Duplicate alert — Room entity |
| `docs/jira/kiro-prompts/MHA-043.md` | Duplicate alert list screen |
| `docs/jira/kiro-prompts/MHA-044.md` | Duplicate alert detail screen |
| `docs/jira/kiro-prompts/MHA-045.md` | Mark-as-reviewed action and alert state persistence |
| `docs/jira/kiro-prompts/MHA-046.md` | Combination medicine handling in duplicate detection |
| `docs/jira/kiro-prompts/MHA-047.md` | Duplicate alert — backend FastAPI API |
| `docs/jira/kiro-prompts/MHA-048.md` | Medical history — Room entity and version model |
| `docs/jira/kiro-prompts/MHA-049.md` | History generation service interface and AI summarisation integration |
| `docs/jira/kiro-prompts/MHA-050.md` | Medical history screen — list and detail views |
| `docs/jira/kiro-prompts/MHA-051.md` | History source confidence display |
| `docs/jira/kiro-prompts/MHA-052.md` | History generation trigger and exclusion list |
| `docs/jira/kiro-prompts/MHA-053.md` | Medical history — backend FastAPI API |
| `docs/jira/kiro-prompts/MHA-054.md` | New Doctor Mode — content selection UI |
| `docs/jira/kiro-prompts/MHA-055.md` | PDF generation service — on-device PDF creation |
| `docs/jira/kiro-prompts/MHA-056.md` | PDF preview screen |
| `docs/jira/kiro-prompts/MHA-057.md` | Consent confirmation screen and share action |
| `docs/jira/kiro-prompts/MHA-058.md` | Share and export — backend API for audit logging |
| `docs/jira/kiro-prompts/MHA-059.md` | Reminder — Room entity |
| `docs/jira/kiro-prompts/MHA-060.md` | Reminder setup UI — create and edit reminders |
| `docs/jira/kiro-prompts/MHA-061.md` | WorkManager notification scheduling for reminders |
| `docs/jira/kiro-prompts/MHA-062.md` | Dose logging — taken / skipped / snoozed |
| `docs/jira/kiro-prompts/MHA-063.md` | Reminder list and adherence tracking screen |
| `docs/jira/kiro-prompts/MHA-064.md` | Interaction check service interface and provider configuration |
| `docs/jira/kiro-prompts/MHA-065.md` | Interaction check UI — available state |
| `docs/jira/kiro-prompts/MHA-066.md` | Interaction check UI — unavailable state |
| `docs/jira/kiro-prompts/MHA-067.md` | Provider configuration in settings |
| `docs/jira/kiro-prompts/MHA-068.md` | Android Keystore — key management and encryption evaluation |
| `docs/jira/kiro-prompts/MHA-069.md` | Database encryption evaluation and implementation |
| `docs/jira/kiro-prompts/MHA-070.md` | Data export — encrypted ZIP with all patient data |
| `docs/jira/kiro-prompts/MHA-071.md` | Account deletion — local and backend data removal |
| `docs/jira/kiro-prompts/MHA-072.md` | Cloud backup — opt-in |
| `docs/jira/kiro-prompts/MHA-073.md` | Consent management UI |
| `docs/jira/kiro-prompts/MHA-074.md` | Audit log — metadata only |
| `docs/jira/kiro-prompts/MHA-075.md` | Android unit test suite — ViewModels and repositories |
| `docs/jira/kiro-prompts/MHA-076.md` | Android unit test suite — duplicate detection and validation |
| `docs/jira/kiro-prompts/MHA-077.md` | Android UI test suite — Espresso and Compose testing |
| `docs/jira/kiro-prompts/MHA-078.md` | Backend pytest suite — API endpoints and auth |
| `docs/jira/kiro-prompts/MHA-079.md` | API integration tests — end-to-end contract tests |
| `docs/jira/kiro-prompts/MHA-080.md` | GitHub Actions android-ci — full pipeline with emulator |
| `docs/jira/kiro-prompts/MHA-081.md` | GitHub Actions backend-ci — lint |
| `docs/jira/kiro-prompts/MHA-082.md` | Release checklist and synthetic test data fixtures |
| `docs/jira/kiro-prompts/additional-ticket-prompts.md` | Epic definitions MHA-E01 through MHA-E12 and supplementary prompts |
| `docs/jira/kiro-prompts/README.md` | Index and usage guide for kiro-prompts directory |
| `docs/jira/kiro-prompts/mhai-001-implementation.md` through `mhai-020-implementation.md` | 20 detailed implementation step files for phased delivery |

### Stage F — Steering, specs, and skills files

**Steering files (`.kiro/steering/`)**

| File | Purpose |
|------|---------|
| `.kiro/steering/engineering.md` | Android and backend engineering constraints |
| `.kiro/steering/medical-safety.md` | OCR fallibility, review state machine, no-clinical-advice rules |
| `.kiro/steering/privacy-security.md` | Privacy and security baseline |
| `.kiro/steering/product.md` | Product purpose, MVP scope, exclusions, and principles |
| `.kiro/steering/structure.md` | Repository layout and naming conventions |
| `.kiro/steering/tech.md` | Android and backend technology stack constraints |
| `.kiro/steering/testing.md` | Test strategy, synthetic data rules, CI gate requirements |

**Spec files (`.kiro/specs/`)**

| File | Domain |
|------|--------|
| `.kiro/specs/android-ui/requirements.md` | Android UI — screens, navigation, design system |
| `.kiro/specs/foundation/requirements.md` | Project foundation — build, CI, architecture |
| `.kiro/specs/medical-history/requirements.md` | Medical history summarisation |
| `.kiro/specs/medication-safety/requirements.md` | Duplicate detection and safety alerts |
| `.kiro/specs/medicine-intelligence/requirements.md` | Medicine explorer, search, status |
| `.kiro/specs/medihistory-mvp/design.md` | MVP design overview |
| `.kiro/specs/medihistory-mvp/requirements.md` | MVP functional requirements |
| `.kiro/specs/medihistory-mvp/tasks.md` | MVP task breakdown |
| `.kiro/specs/prescription-management/requirements.md` | Prescription CRUD and OCR flow |
| `.kiro/specs/security-and-release/requirements.md` | Security, privacy, and release readiness |

**Skills files (`.kiro/skills/`)**

| File | Domain |
|------|--------|
| `.kiro/skills/android-development/SKILL.md` | Android Kotlin/Compose development patterns |
| `.kiro/skills/backend-development/SKILL.md` | FastAPI backend development patterns |
| `.kiro/skills/medical-data-security/SKILL.md` | Medical data encryption and privacy |
| `.kiro/skills/medical-history-summarization/SKILL.md` | AI-generated history and disclaimer rules |
| `.kiro/skills/medication-safety/SKILL.md` | Duplicate detection and safety labelling |
| `.kiro/skills/medicine-normalization/SKILL.md` | Active ingredient normalisation |
| `.kiro/skills/prescription-ocr/SKILL.md` | OCR extraction and confidence scoring |
| `.kiro/skills/prescription-ocr-review/SKILL.md` | OCR review UI and state machine |
| `.kiro/skills/testing-and-quality/SKILL.md` | Test strategy and synthetic fixture guidance |

### Stage G — GitHub Actions CI workflows (`.github/workflows/`)

| File | Purpose |
|------|---------|
| `.github/workflows/android-ci.yml` | Android lint, unit tests, and (full pipeline) emulator tests |
| `.github/workflows/backend-ci.yml` | Backend lint, type checking, and pytest suite |
| `.github/workflows/pr-checks.yml` | PR gate — security grep, coverage threshold, lint |

### Stage H — This report

| File | Purpose |
|------|---------|
| `docs/ARTIFACT-COMPLETION-REPORT.md` | This file — final artifact completion report |

---

## 2. Files That Already Existed (skipped)

No files were skipped in this run. The file `docs/ARTIFACT-COMPLETION-REPORT.md` did not exist before this run (confirmed via filesystem check).

The following files in `docs/jira/kiro-prompts/` were created in a prior workflow stage and carried forward as existing artifacts (they were not overwritten by this stage):

- All `MHA-NNN.md` files (MHA-001 through MHA-082)
- `additional-ticket-prompts.md`
- `README.md`
- All `mhai-NNN-implementation.md` files

These are fully present and valid — they are listed under Stage E above for completeness.

---

## 3. Files That Could Not Be Created

None — all files created successfully.

All 82 MHA ticket prompt files, 7 steering files, 10 spec files, 9 skills files, 3 CI workflow files, and this report were verified present on the filesystem before this report was written.

---

## 4. External Tool Actions Required

The following actions require manual steps outside the repository. They cannot be automated by Kiro.

**Google Stitch**
The prompts in `docs/design/google-stitch/` must be run manually through Google Stitch. Each prompt produces UI mockups that feed the Figma workflow. The prompts are:
- `00-master-stitch-prompt.md` — master layout and design language
- `01-dashboard-prompt.md` — Patient Dashboard
- `02-prescriptions-prompt.md` — Prescription List, Detail, OCR Review
- `03-medicine-explorer-prompt.md` — Medicine Explorer, Detail
- `04-duplicate-checker-prompt.md` — Duplicate Alert Review
- `05-medical-history-prompt.md` — Medical History Summary
- `06-new-doctor-mode-prompt.md` — New Doctor Mode, PDF Preview
- `07-reminders-prompt.md` — Medication Reminders
- `08-safety-center-prompt.md` — Safety Center
- `09-shared-components-prompt.md` — Shared component library

**Figma**
Import the generated mockups using the guide in `docs/design/figma/figma-import-and-handoff.md`. Update all `[FIGMA_URL_TBD]` and `[FIGMA_NODE_TBD]` placeholders in `docs/design/screen-inventory.md` and any kiro-prompt files once Figma URLs are assigned.

**Jira CSV import**
Import `docs/jira/jira-ticket-master.csv` (or `docs/jira/medihistory-jira-simple-import.csv` for simpler tools) using the steps in `docs/jira/jira-import-guide.md`. The epic IDs MHA-E01 through MHA-E12 are local placeholders — Jira will assign its own numeric IDs on import.

**GitHub Actions**
The CI workflows in `.github/workflows/` require a real GitHub remote with Actions enabled. They will not run in a local-only repository. Ensure repository secrets are configured in GitHub Settings before enabling the full pipeline (backend API keys, signing keystores, and coverage upload tokens are not committed to this repository).

---

## 5. Validation Findings

### FR coverage

Functional requirements FR-001 through FR-040 are defined in `docs/product/product-requirements.md`.

Scanning all files in `docs/jira/kiro-prompts/` against FR-NNN patterns:

- **FR-003 through FR-040**: all present in at least one prompt file. ✓
- **FR-001** (patient data model — basic profile): not explicitly referenced by FR-001 ID in any prompt file. The concept is covered by MHA-016 (patient entity), MHA-018 (patient CRUD), and MHA-020, but the FR-001 identifier is absent from the prompt text.
- **FR-002** (doctor profile — basic profile): not explicitly referenced by FR-002 ID in any prompt file. The concept is covered by MHA-017 (doctor entity) and MHA-019, but the FR-002 identifier is absent from the prompt text.

**Action required:** When implementing MHA-016, MHA-017, MHA-018, and MHA-019, verify that the acceptance criteria in `docs/product/product-requirements.md` under FR-001 and FR-002 are fully satisfied. The tickets cover the correct scope; the traceability tag is missing from the prompt text only.

### Screens without features

`docs/design/screen-inventory.md` defines 13 screen IDs:

| Screen ID | Coverage |
|-----------|----------|
| SCR-01-DASHBOARD | MHA-010, MHA-014 (navigation and app bar) ✓ |
| SCR-02-RX-LIST | MHA-024 ✓ |
| SCR-02-RX-DETAIL | MHA-025 ✓ |
| SCR-02-DOCTOR-GROUP | MHA-024 (grouped list view) ✓ |
| SCR-03-OCR-REVIEW | MHA-030 ✓ |
| SCR-04-MED-EXPLORER | MHA-035, MHA-036 ✓ |
| SCR-04-MED-DETAIL | MHA-037, MHA-038, MHA-039 ✓ |
| SCR-05-DUPLICATE-REVIEW | MHA-043, MHA-044 ✓ |
| SCR-06-HISTORY | MHA-050, MHA-051, MHA-052 ✓ |
| SCR-07-NEW-DOCTOR | MHA-054 ✓ |
| SCR-07-PDF-PREVIEW | MHA-056, MHA-057 ✓ |
| SCR-08-REMINDERS | MHA-060, MHA-062, MHA-063 ✓ |
| SCR-09-SAFETY-CENTER | MHA-073 (consent), MHA-065, MHA-066 (interaction check) ✓ |

All 13 screen IDs are covered by at least one ticket. No screen is unaddressed.

### Tickets without Acceptance Criteria

Zero tickets in `jira-ticket-master.csv` have an empty Acceptance Criteria column. All 82 tickets have ACs defined.

### Tickets without Test Requirements

44 tickets have an empty Test Requirements column in the CSV. These tickets have ACs that describe expected behaviour, and the corresponding prompt files (`MHA-NNN.md`) include inline test guidance within the prompt body. The Test Requirements CSV column is supplementary. The 44 tickets with empty Test Requirements columns are:

MHA-001, MHA-002, MHA-003, MHA-004, MHA-005, MHA-006, MHA-007, MHA-008, MHA-010, MHA-012, MHA-013, MHA-014, MHA-015, MHA-020, MHA-021, MHA-022, MHA-028, MHA-029, MHA-033, MHA-039, MHA-040, MHA-041, MHA-045, MHA-046, MHA-047, MHA-048, MHA-049, MHA-053, MHA-058, MHA-061, MHA-064, MHA-067, MHA-068, MHA-069, MHA-071, MHA-074, MHA-075, MHA-076, MHA-077, MHA-078, MHA-079, MHA-080, MHA-081, MHA-082

**Action required:** Populate the Test Requirements column in Jira for these tickets after import, referencing the test guidance in the corresponding `MHA-NNN.md` prompt files. This is a documentation gap, not a functional gap.

### Prompt files referencing nonexistent ticket IDs

All `MHA-NNN.md` files from MHA-001 through MHA-082 exist in the filesystem. Dependencies declared in the CSV `Dependencies` column reference only IDs within this range. No prompt references a nonexistent ticket ID.

### Missing tests for Acceptance Criteria

Test coverage must be verified during implementation. The `testing.md` steering file specifies coverage targets (≥80% line coverage for the data layer, 100% branch coverage for DuplicateDetector and ValidationUtils, ≥90% for the backend API layer). These targets are enforced by the CI gate in `.github/workflows/pr-checks.yml`. Tickets with empty Test Requirements columns (listed above) require test requirements to be populated in Jira before implementation begins.

### Medical safety gaps

**OCR tickets (MHA-028 through MHA-032)**
All five OCR-related prompt files (`MHA-028.md` through `MHA-032.md`) were confirmed to reference `medical-safety.md`. ✓

**Duplicate detection tickets (MHA-041 through MHA-046)**
Five of the six duplicate detection prompts include the "potential duplicate" labelling requirement:
- MHA-041 ✓ — references "potential duplicate"
- MHA-042 ✓ — references "potential duplicate"
- MHA-043 ✓ — references "potential duplicate"
- MHA-044 ✓ — references "potential duplicate"
- MHA-045 ⚠ — does not contain the exact phrase "potential duplicate". The prompt does include a disclaimer instruction ("Do not imply that 'reviewed' means a doctor has cleared the combination; the disclaimer from MHA-044 must remain") but the phrase "potential duplicate" is absent from the body text.
- MHA-046 ✓ — references "potential duplicate"

**Action required for MHA-045:** When implementing the mark-as-reviewed flow, ensure the alert label reads "Potential duplicate" in the UI — not "Duplicate" or "Reviewed duplicate". The MHA-044 disclaimer must persist in the alert detail screen even after the alert is marked reviewed. This is a labelling requirement from `medical-safety.md` that the implementer must apply even though the MHA-045 prompt does not state the exact phrase.

---

## 6. Ticket-to-Prompt Mapping Table

The table below maps all 82 Jira tickets (MHA-001 through MHA-082) to their prompt file. Epics MHA-E01 through MHA-E12 are covered in `docs/jira/kiro-prompts/additional-ticket-prompts.md`.

| Ticket ID | Summary | Prompt File | Notes |
|-----------|---------|-------------|-------|
| MHA-001 | Repository setup and Gradle project scaffold | `docs/jira/kiro-prompts/MHA-001.md` | |
| MHA-002 | Architecture Decision Records | `docs/jira/kiro-prompts/MHA-002.md` | |
| MHA-003 | CI scaffold — android-ci pipeline | `docs/jira/kiro-prompts/MHA-003.md` | |
| MHA-004 | Dependency management — pinned versions | `docs/jira/kiro-prompts/MHA-004.md` | |
| MHA-005 | Environment configuration — no secrets in APK | `docs/jira/kiro-prompts/MHA-005.md` | |
| MHA-006 | Room database setup — schema | `docs/jira/kiro-prompts/MHA-006.md` | |
| MHA-007 | Authentication scaffold — login and session | `docs/jira/kiro-prompts/MHA-007.md` | |
| MHA-008 | FastAPI backend scaffold — project structure | `docs/jira/kiro-prompts/MHA-008.md` | |
| MHA-009 | Material 3 theme — colours | `docs/jira/kiro-prompts/MHA-009.md` | |
| MHA-010 | Navigation scaffold — NavHost and bottom nav | `docs/jira/kiro-prompts/MHA-010.md` | |
| MHA-011 | Compose component library — cards | `docs/jira/kiro-prompts/MHA-011.md` | |
| MHA-012 | Design tokens — spacing | `docs/jira/kiro-prompts/MHA-012.md` | |
| MHA-013 | Accessibility baseline — touch targets | `docs/jira/kiro-prompts/MHA-013.md` | |
| MHA-014 | Top app bar — title | `docs/jira/kiro-prompts/MHA-014.md` | |
| MHA-015 | Navigation map and screen-to-route mapping | `docs/jira/kiro-prompts/MHA-015.md` | |
| MHA-016 | Patient profile — Room entity | `docs/jira/kiro-prompts/MHA-016.md` | |
| MHA-017 | Doctor profile — Room entity | `docs/jira/kiro-prompts/MHA-017.md` | |
| MHA-018 | Patient profile — CRUD UI screens | `docs/jira/kiro-prompts/MHA-018.md` | |
| MHA-019 | Doctor profile — CRUD UI screens | `docs/jira/kiro-prompts/MHA-019.md` | |
| MHA-020 | Patient-doctor relationship and prescription linkage | `docs/jira/kiro-prompts/MHA-020.md` | |
| MHA-021 | Data validation — shared validation utilities | `docs/jira/kiro-prompts/MHA-021.md` | |
| MHA-022 | Offline support for patient and doctor data | `docs/jira/kiro-prompts/MHA-022.md` | |
| MHA-023 | Prescription — Room entity | `docs/jira/kiro-prompts/MHA-023.md` | |
| MHA-024 | Prescription list screen | `docs/jira/kiro-prompts/MHA-024.md` | |
| MHA-025 | Prescription detail screen | `docs/jira/kiro-prompts/MHA-025.md` | |
| MHA-026 | Camera capture with CameraX | `docs/jira/kiro-prompts/MHA-026.md` | |
| MHA-027 | Photo Picker and file picker — import prescription | `docs/jira/kiro-prompts/MHA-027.md` | |
| MHA-028 | OCR service interface and WorkManager job | `docs/jira/kiro-prompts/MHA-028.md` | |
| MHA-029 | Confidence scoring and ExtractedField model | `docs/jira/kiro-prompts/MHA-029.md` | |
| MHA-030 | OCR review UI — confirm | `docs/jira/kiro-prompts/MHA-030.md` | |
| MHA-031 | OCR error handling and retry | `docs/jira/kiro-prompts/MHA-031.md` | |
| MHA-032 | Manual medicine entry screen | `docs/jira/kiro-prompts/MHA-032.md` | |
| MHA-033 | Prescription CRUD — backend FastAPI API | `docs/jira/kiro-prompts/MHA-033.md` | |
| MHA-034 | Medicine — Room entity | `docs/jira/kiro-prompts/MHA-034.md` | |
| MHA-035 | Medicine explorer screen — list with lazy loading | `docs/jira/kiro-prompts/MHA-035.md` | |
| MHA-036 | Medicine search and filter | `docs/jira/kiro-prompts/MHA-036.md` | |
| MHA-037 | Medicine detail screen | `docs/jira/kiro-prompts/MHA-037.md` | |
| MHA-038 | Medicine status assignment — Current / Past / Unknown | `docs/jira/kiro-prompts/MHA-038.md` | |
| MHA-039 | Medicine-prescription source link — navigation | `docs/jira/kiro-prompts/MHA-039.md` | |
| MHA-040 | Medicine list — backend FastAPI API | `docs/jira/kiro-prompts/MHA-040.md` | |
| MHA-041 | Duplicate detection algorithm | `docs/jira/kiro-prompts/MHA-041.md` | |
| MHA-042 | Duplicate alert — Room entity | `docs/jira/kiro-prompts/MHA-042.md` | |
| MHA-043 | Duplicate alert list screen | `docs/jira/kiro-prompts/MHA-043.md` | |
| MHA-044 | Duplicate alert detail screen | `docs/jira/kiro-prompts/MHA-044.md` | |
| MHA-045 | Mark-as-reviewed action and alert state | `docs/jira/kiro-prompts/MHA-045.md` | ⚠ See medical safety finding |
| MHA-046 | Combination medicine handling in duplicate detection | `docs/jira/kiro-prompts/MHA-046.md` | |
| MHA-047 | Duplicate alert — backend FastAPI API | `docs/jira/kiro-prompts/MHA-047.md` | |
| MHA-048 | Medical history — Room entity and version model | `docs/jira/kiro-prompts/MHA-048.md` | |
| MHA-049 | History generation service interface and AI summarisation | `docs/jira/kiro-prompts/MHA-049.md` | |
| MHA-050 | Medical history screen — list and detail views | `docs/jira/kiro-prompts/MHA-050.md` | |
| MHA-051 | History source confidence display | `docs/jira/kiro-prompts/MHA-051.md` | |
| MHA-052 | History generation trigger and exclusion list | `docs/jira/kiro-prompts/MHA-052.md` | |
| MHA-053 | Medical history — backend FastAPI API | `docs/jira/kiro-prompts/MHA-053.md` | |
| MHA-054 | New Doctor Mode — content selection UI | `docs/jira/kiro-prompts/MHA-054.md` | |
| MHA-055 | PDF generation service — on-device PDF creation | `docs/jira/kiro-prompts/MHA-055.md` | |
| MHA-056 | PDF preview screen | `docs/jira/kiro-prompts/MHA-056.md` | |
| MHA-057 | Consent confirmation screen and share action | `docs/jira/kiro-prompts/MHA-057.md` | |
| MHA-058 | Share and export — backend API for audit logging | `docs/jira/kiro-prompts/MHA-058.md` | |
| MHA-059 | Reminder — Room entity | `docs/jira/kiro-prompts/MHA-059.md` | |
| MHA-060 | Reminder setup UI — create and edit reminders | `docs/jira/kiro-prompts/MHA-060.md` | |
| MHA-061 | WorkManager notification scheduling for reminders | `docs/jira/kiro-prompts/MHA-061.md` | |
| MHA-062 | Dose logging — taken / skipped / snoozed | `docs/jira/kiro-prompts/MHA-062.md` | |
| MHA-063 | Reminder list and adherence tracking screen | `docs/jira/kiro-prompts/MHA-063.md` | |
| MHA-064 | Interaction check service interface | `docs/jira/kiro-prompts/MHA-064.md` | |
| MHA-065 | Interaction check UI — available state | `docs/jira/kiro-prompts/MHA-065.md` | |
| MHA-066 | Interaction check UI — unavailable state | `docs/jira/kiro-prompts/MHA-066.md` | |
| MHA-067 | Provider configuration in settings | `docs/jira/kiro-prompts/MHA-067.md` | |
| MHA-068 | Android Keystore — key management | `docs/jira/kiro-prompts/MHA-068.md` | |
| MHA-069 | Database encryption evaluation | `docs/jira/kiro-prompts/MHA-069.md` | |
| MHA-070 | Data export — encrypted ZIP | `docs/jira/kiro-prompts/MHA-070.md` | |
| MHA-071 | Account deletion — local and backend | `docs/jira/kiro-prompts/MHA-071.md` | |
| MHA-072 | Cloud backup — opt-in | `docs/jira/kiro-prompts/MHA-072.md` | |
| MHA-073 | Consent management UI | `docs/jira/kiro-prompts/MHA-073.md` | |
| MHA-074 | Audit log — metadata only | `docs/jira/kiro-prompts/MHA-074.md` | |
| MHA-075 | Android unit test suite — ViewModels and repositories | `docs/jira/kiro-prompts/MHA-075.md` | |
| MHA-076 | Android unit test suite — duplicate detection and validation | `docs/jira/kiro-prompts/MHA-076.md` | |
| MHA-077 | Android UI test suite — Espresso and Compose | `docs/jira/kiro-prompts/MHA-077.md` | |
| MHA-078 | Backend pytest suite — API endpoints and auth | `docs/jira/kiro-prompts/MHA-078.md` | |
| MHA-079 | API integration tests — end-to-end contract tests | `docs/jira/kiro-prompts/MHA-079.md` | |
| MHA-080 | GitHub Actions android-ci — full pipeline with emulator | `docs/jira/kiro-prompts/MHA-080.md` | |
| MHA-081 | GitHub Actions backend-ci — lint and pytest | `docs/jira/kiro-prompts/MHA-081.md` | |
| MHA-082 | Release checklist and synthetic test data fixtures | `docs/jira/kiro-prompts/MHA-082.md` | |
| MHA-E01 | Project Foundation and Architecture (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E02 | Android Design System and Navigation (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E03 | Patient and Doctor Records (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E04 | Prescription Management and OCR (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E05 | Medicine Explorer and Tracking (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E06 | Duplicate Detection and Safety Alerts (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E07 | Medical History Summarisation (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E08 | New Doctor Mode and PDF Export (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E09 | Reminders and Adherence Tracking (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E10 | Interaction Checking (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E11 | Security, Privacy, and Consent (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |
| MHA-E12 | Testing, CI/CD, and Release (epic) | `docs/jira/kiro-prompts/additional-ticket-prompts.md` | covered in additional-ticket-prompts.md |

---

## 7. Recommended Next Steps

An ordered action list for the team:

1. **Import jira-ticket-master.csv into Jira** using `docs/jira/jira-import-guide.md`. Use `docs/jira/medihistory-jira-simple-import.csv` if your Jira instance requires the simplified format. After import, update cross-ticket Dependencies fields with the real Jira issue keys that replace the MHA-NNN local IDs.

2. **Run Google Stitch prompts** in `docs/design/google-stitch/` to generate UI mockups. Start with `00-master-stitch-prompt.md` to establish the design language, then run prompts 01–09 in order.

3. **Import mockups into Figma** and follow `docs/design/figma/figma-import-and-handoff.md`. Update all `[FIGMA_URL_TBD]` and `[FIGMA_NODE_TBD]` placeholders in `docs/design/screen-inventory.md` and in any kiro-prompt files once Figma URLs are assigned.

4. **Review and merge the steering files** (`.kiro/steering/`) — team agreement is required on `tech.md`, `testing.md`, `medical-safety.md`, and `privacy-security.md` before implementation begins. These files are injected into every Kiro session and govern all code generation.

5. **Implement MHA-E01 (Foundation) tickets first**, in this order: MHA-001 (repo scaffold), MHA-004 (dependency management), MHA-005 (environment config), MHA-006 (Room schema), MHA-008 (FastAPI scaffold), MHA-007 (auth scaffold), MHA-003 (CI baseline). Do not start feature tickets until CI is green and the project builds cleanly.

6. **Implement MHA-E02 (Android Design System and Navigation)** after the foundation is stable. MHA-009, MHA-010, MHA-011, MHA-012, MHA-013, MHA-014 must all be complete before any feature screen is built.

7. **Proceed sprint by sprint following epic ordering** in `docs/jira/kiro-prompts/additional-ticket-prompts.md`: E03 → E04 → E05 → E06 → E07 → E08 → E09 → E10 → E11 → E12.

8. **Before implementing each ticket**, read the corresponding `docs/jira/kiro-prompts/MHA-NNN.md` file and follow the prompt precisely. The prompts include: file placement rules, acceptance criteria, test requirements, medical safety constraints, and references to the relevant skill guides.

9. **Populate empty Test Requirements columns** in Jira (44 tickets) by referencing the test guidance in the corresponding `MHA-NNN.md` prompt files. This should be done during sprint planning before a ticket enters development.

10. **Before production release**, conduct all of the following — none can be skipped:
    - Qualified security and privacy review covering applicable India data protection requirements (DPDP Act 2023 and any sector-specific health data rules). This is not a compliance certification and does not substitute for qualified legal review.
    - Formal threat model review (`docs/architecture/security-threat-model.md`).
    - Full accessibility audit with TalkBack on a real device covering all 13 screen IDs in `docs/design/screen-inventory.md`.
    - SQLCipher encryption evaluation and key derivation strategy decision (see `MHA-068.md` and `MHA-069.md`).
    - Review of all `[FIGMA_URL_TBD]` placeholders — none should remain unresolved in a release build.
    - Never deploy to production or incur cloud costs without explicit team authorisation and security sign-off.

---

*This report was generated by inspecting the live filesystem at `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI`. All file counts and validation findings reflect the actual state of the repository at the time of generation.*
