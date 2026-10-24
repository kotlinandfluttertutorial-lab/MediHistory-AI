# Resume Plan — MediHistory AI Artifact Completion

Generated from repository inspection on: 2025-01-31

---

## Current State

### docs/jira/kiro-prompts/ (41 files present)
**Jira ticket prompts (MHA-NNN.md) — 22 exist:**
MHA-001.md through MHA-022.md ✅

**Implementation reports (mhai-NNN-implementation.md) — 20 exist:**
mhai-001-implementation.md through mhai-020-implementation.md ✅

README.md ✅

### .kiro/steering/ (3 files present)
- engineering.md ✅
- privacy-security.md ✅
- product.md ✅

### .kiro/specs/medihistory-mvp/ (3 files present)
- design.md ✅
- requirements.md ✅
- tasks.md ✅

### .kiro/skills/ (2 skill directories, each with SKILL.md)
- medication-safety/SKILL.md ✅
- prescription-ocr-review/SKILL.md ✅

### .github/workflows/ (directory does not exist — 0 files)

### docs/ARTIFACT-COMPLETION-REPORT.md
Does not exist ❌

---

## Epic Map (for MHA-023 through MHA-082 planning)

The existing MHA-001–MHA-022 tickets span three epics:
- **MHA-E01** — Foundation and Project Setup (MHA-001 to MHA-009, ~9 tickets)
- **MHA-E02** — Design System and Navigation (MHA-010 to MHA-015, ~6 tickets)
- **MHA-E03** — Patient and Doctor Data Layer (MHA-016 to MHA-022, ~7 tickets)

Remaining epics to cover per tasks.md (T4–T13):
- **MHA-E04** — Camera Capture and Document Import (T3 continuation, T4)
- **MHA-E05** — OCR Processing and Review (T4, T5)
- **MHA-E06** — Medication History (T6)
- **MHA-E07** — Medicine Reference and Duplicate Detection (T7, T8)
- **MHA-E08** — New Doctor Mode (T9)
- **MHA-E09** — Encrypted Cloud Backup and Sync (T10)
- **MHA-E10** — Consent, Sharing, Export, Deletion (T11)
- **MHA-E11** — Security, Accessibility, and End-to-End Tests (T12)
- **MHA-E12** — Release Readiness (T13)

---

## Stage E — Jira Ticket Prompts to Create

All files go in: `docs/jira/kiro-prompts/`

### Already Exist (skip):
MHA-001.md through MHA-022.md (22 files)

### Must Create (60 files: MHA-023.md through MHA-082.md):

#### Epic MHA-E04 — Camera Capture and Document Import
- [ ] MHA-023.md — CameraX integration: preview and photo capture
- [ ] MHA-024.md — Photo Picker / gallery import (images and PDFs)
- [ ] MHA-025.md — Original file preservation in app-private storage
- [ ] MHA-026.md — Import progress indicator and error handling UI
- [ ] MHA-027.md — Document metadata entity and Room DAO
- [ ] MHA-028.md — Offline support for document import

#### Epic MHA-E05 — OCR Processing and Review
- [ ] MHA-029.md — OCR adapter interface and Google ML Kit integration
- [ ] MHA-030.md — Confidence scoring and source-region metadata
- [ ] MHA-031.md — Raw OCR text storage and structured field extraction
- [ ] MHA-032.md — OCR review screen: display extracted fields
- [ ] MHA-033.md — Patient correction of OCR fields and review-state tracking
- [ ] MHA-034.md — Low-confidence and illegible-field flagging UI
- [ ] MHA-035.md — Source-link: every medicine links to its origin document/page
- [ ] MHA-036.md — OCR re-run and manual override workflow
- [ ] MHA-037.md — OCR WorkManager background job and retry policy
- [ ] MHA-038.md — Offline OCR review and correction

#### Epic MHA-E06 — Medication History
- [ ] MHA-039.md — Medicine entity and Room DAO
- [ ] MHA-040.md — Current / past / unknown status model and state transitions
- [ ] MHA-041.md — Medication history list screen (grouped by status)
- [ ] MHA-042.md — Medicine detail screen with source-prescription link
- [ ] MHA-043.md — Status change confirmation flow (patient-initiated)
- [ ] MHA-044.md — New extraction defaults to unknown status enforcement
- [ ] MHA-045.md — Search and filter in medication history

#### Epic MHA-E07 — Medicine Reference and Duplicate Detection
- [ ] MHA-046.md — Medicine reference adapter interface and data source
- [ ] MHA-047.md — Active-ingredient normalization and verified-ID matching
- [ ] MHA-048.md — Combination-product representation
- [ ] MHA-049.md — Duplicate detection engine (deterministic, non-AI)
- [ ] MHA-050.md — Ambiguous-mapping handling ("Unable to confirm ingredient")
- [ ] MHA-051.md — Duplicate review screen and safety disclaimer
- [ ] MHA-052.md — Potential-duplicate label and "Potential duplicate ingredient" copy
- [ ] MHA-053.md — Duplicate review state persistence and DAO

#### Epic MHA-E08 — New Doctor Mode
- [ ] MHA-054.md — New Doctor Mode report data model and builder
- [ ] MHA-055.md — Patient-entered profile and allergy/condition entry
- [ ] MHA-056.md — Selectable medicine and document list for report
- [ ] MHA-057.md — Report preview screen with unconfirmed-fact labelling
- [ ] MHA-058.md — PDF export of New Doctor Mode report
- [ ] MHA-059.md — Source index and OCR-limitation note in report
- [ ] MHA-060.md — Sharing intent with explicit preview and consent step

#### Epic MHA-E09 — Encrypted Cloud Backup and Sync
- [ ] MHA-061.md — Opt-in backup consent screen and preference storage
- [ ] MHA-062.md — Encrypted backup adapter interface and WorkManager job
- [ ] MHA-063.md — Sync status indicator (idle / syncing / error / offline)
- [ ] MHA-064.md — Conflict resolution: no silent overwrite of confirmed edits
- [ ] MHA-065.md — Backup failure notification and recovery instructions
- [ ] MHA-066.md — Cloud-backup retention explanation in settings
- [ ] MHA-067.md — Restore from backup flow

#### Epic MHA-E10 — Consent, Sharing, Export, and Deletion
- [ ] MHA-068.md — Export selected records (JSON / PDF)
- [ ] MHA-069.md — Full deletion (local + backup) with retention explanation
- [ ] MHA-070.md — Sharing: explicit preview, selection, and consent step
- [ ] MHA-071.md — Privacy settings screen (private-by-default enforcement)
- [ ] MHA-072.md — Cloud-backup enable/disable and status in Privacy settings

#### Epic MHA-E11 — Security, Accessibility, and End-to-End Tests
- [ ] MHA-073.md — Android Keystore key management and encrypted Room setup
- [ ] MHA-074.md — No-credentials-in-APK audit and secrets management
- [ ] MHA-075.md — Scalable text and screen-reader accessibility pass
- [ ] MHA-076.md — Colour-contrast and no-colour-only-meaning audit
- [ ] MHA-077.md — End-to-end test suite: import → OCR → history → duplicate → export
- [ ] MHA-078.md — Offline scenario tests (import, review, sync recovery)
- [ ] MHA-079.md — Synthetic-fixture test data set

#### Epic MHA-E12 — Release Readiness
- [ ] MHA-080.md — Threat model and open-risk documentation
- [ ] MHA-081.md — India privacy / legal requirements checklist
- [ ] MHA-082.md — Release readiness review and sign-off

**Total to create in Stage E: 60 files**

---

## Stage F — Steering, Spec Requirements, and Skill Files

### Steering files (.kiro/steering/)

#### Already Exist (skip):
- engineering.md ✅
- privacy-security.md ✅
- product.md ✅

#### Must Create (3 files):
- [ ] `.kiro/steering/tech.md` — Android tech-stack reference (Kotlin versions, Compose BOM, Room, CameraX, WorkManager, ML Kit versions; architecture layers; build commands). Referenced in nearly every MHA-NNN.md "Steering Files to Follow" list.
- [ ] `.kiro/steering/structure.md` — Directory/package layout rules (module structure, naming conventions, package paths). Referenced in nearly every MHA-NNN.md.
- [ ] `.kiro/steering/medical-safety.md` — Medical-safety guardrails (no clinical advice, no inferred doses, disclaimer copy, safety-language requirements). Referenced in nearly every MHA-NNN.md.

### Spec requirements files (.kiro/specs/medihistory-mvp/)

#### Already Exist (skip):
- design.md ✅
- requirements.md ✅
- tasks.md ✅

#### Must Create (0 files):
All three spec files are present. No new spec files needed for the MVP scope.

### Skill SKILL.md files (.kiro/skills/)

#### Already Exist (skip):
- medication-safety/SKILL.md ✅
- prescription-ocr-review/SKILL.md ✅

#### Must Create (6 skill directories + SKILL.md):
Referenced in MHA-NNN.md files (MHA-001.md references `kiro-android-setup`; MHA-022.md references `kiro-android-offline`):
- [ ] `.kiro/skills/kiro-android-setup/SKILL.md` — Gradle scaffold, version catalog, baseline security setup instructions.
- [ ] `.kiro/skills/kiro-android-offline/SKILL.md` — Offline-first patterns: ConnectivityObserver, Room-only path, WorkManager retry.
- [ ] `.kiro/skills/kiro-android-navigation/SKILL.md` — NavHost, NavGraph, bottom navigation bar, back-stack rules.
- [ ] `.kiro/skills/kiro-android-camera/SKILL.md` — CameraX capture, Photo Picker integration, app-private storage.
- [ ] `.kiro/skills/kiro-android-backup/SKILL.md` — Opt-in encrypted WorkManager backup, sync status, conflict resolution.
- [ ] `.kiro/skills/kiro-android-accessibility/SKILL.md` — Compose a11y: content descriptions, scalable text, contrast, TalkBack.

**Total to create in Stage F: 3 steering + 0 spec + 6 skill = 9 files**

---

## Stage G — GitHub Actions Workflow YAMLs

### Directory state:
`.github/workflows/` — directory does not exist.

### Must Create (directory + files):
- [ ] `.github/workflows/ci.yml` — Pull-request CI: checkout, Java setup, Gradle cache, `./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug`, upload lint report artifact.
- [ ] `.github/workflows/instrumented-tests.yml` — Instrumented/connected tests on an Android emulator via GitHub Actions (AVD setup, `./gradlew :app:connectedDebugAndroidTest`). Triggered on `push` to `main` and manual dispatch.
- [ ] `.github/workflows/dependency-review.yml` — Dependency review action on PRs to catch new vulnerable or unlicensed dependencies.
- [ ] `.github/workflows/codeql.yml` — CodeQL static analysis for Android/Kotlin (security scanning). Runs on push to `main` and PRs.
- [ ] `.github/workflows/release.yml` — Release build pipeline: assembleRelease, sign APK (secrets from GitHub Actions secrets — never in APK), upload to GitHub Release. Triggered on version tag push (`v*`).

**Total to create in Stage G: 5 files (+ create the .github/workflows/ directory)**

---

## Stage H — Completion Report

### Must Create (1 file):
- [ ] `docs/ARTIFACT-COMPLETION-REPORT.md` — Summary report covering:
  - All artifacts created across Stages A–G
  - Traceability: each MHA ticket → requirement → epic
  - Steering files coverage
  - Skill files coverage
  - CI/CD pipeline summary
  - Known gaps and open items
  - Privacy and security review checklist status

**Total to create in Stage H: 1 file**

---

## Grand Summary

| Stage | What | Files to Create | Files Already Present |
|-------|------|-----------------|-----------------------|
| E | MHA-023.md – MHA-082.md in docs/jira/kiro-prompts/ | 60 | 22 (MHA-001–022) + 20 (mhai impl) + 1 README |
| F | Steering files | 3 | 3 (engineering, privacy-security, product) |
| F | Spec requirements files | 0 | 3 (design, requirements, tasks) |
| F | Skill SKILL.md files | 6 | 2 (medication-safety, prescription-ocr-review) |
| G | GitHub Actions YAMLs | 5 | 0 |
| H | ARTIFACT-COMPLETION-REPORT.md | 1 | 0 |
| **Total** | | **75 files** | **51 files** |

---

## Notes

1. **tech.md, structure.md, medical-safety.md are blocking**: Every MHA ticket from MHA-001 onward lists these three steering files in "Steering Files to Follow." They must be created before the MHA-023–MHA-082 prompts are used for implementation.
2. **Skill directories**: The existing skill names in MHA-001.md (`kiro-android-setup`) and MHA-022.md (`kiro-android-offline`) don't match the two existing skill directory names (`medication-safety`, `prescription-ocr-review`). Both sets are needed.
3. **GitHub Actions**: The `.github/` directory does not exist at all — create the full path `.github/workflows/`.
4. **MHA ticket numbering**: MHA-023 through MHA-082 covers 60 tickets across 9 epics (E04–E12). Adjust ticket count per epic if the product backlog is refined.
5. **Implementation report files** (mhai-NNN-implementation.md): 20 already exist (mhai-001 through mhai-020). The pattern suggests one implementation report per ticket is generated after implementation — these are not pre-created artifacts like the MHA-NNN.md prompt files.
