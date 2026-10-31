# Gap Fixes — Semantic Code Review
**Date:** 2026-10-29  
**Diff scope:** 8 changed files implementing 5 critical gap fixes in the MediHistory AI Android app  
**Reference:** `gap-analysis-2026-10-29.md`, `plan-gap-fixes-2026-10-29.md`, `gap-fixes-2026-10-29.md`

---

## Summary

Five gaps identified in the gap analysis are addressed: camera capture no longer silently discards the confirmed image; the "Review →" and "Extract text" buttons on `PrescriptionDetailScreen` are wired up; `DuplicateDetector` is triggered after every OCR review; `DoctorListScreen` and `DoctorEditScreen` are reachable from both `MoreScreen` and the Dashboard Doctors tile; and `PatientProfileScreen` is reachable from both `MoreScreen` and the Dashboard profile avatar.

Build verification in `gap-fixes-2026-10-29.md` records `compileDebugKotlin`, `compileDebugUnitTestKotlin`, `testDebugUnitTest`, and `assembleDebug` all passing.

Watch for: a **confirmed** duplicate-alert accumulation bug — because each `DuplicateAlert` gets a new `UUID`, `OnConflictStrategy.IGNORE` never fires, so every OCR review (even ones with no new medicines) inserts fresh duplicate-alert rows for already-detected pairs. Also watch for: a **likely** camera double-fire edge case on process-death restoration, and a **confirmed** plan deviation where `onExtractText` was not exposed as a public parameter.

**Verdict**: APPROVED

---

## High-level view

The camera gap fix uses the standard Jetpack `savedStateHandle` back-result pattern. The CAMERA composable writes two keys onto `previousBackStackEntry.savedStateHandle` then calls `navigateUp()`; the IMPORT composable reads those keys as `StateFlow`s, fires `importViewModel.onCameraImageConfirmed`, and removes the keys. `onCameraImageConfirmed` skips the copy step correctly (CameraX already wrote the file) and goes straight to inserting a `Prescription` row and enqueuing `OcrWorker`. The `LaunchedEffect` key setup works in the happy path but has a recomposition race on process-death restore.

`PrescriptionDetailViewModel` gains `DoctorRepository` and `WorkManager` injections, `load()` resolves `doctorName`, and `triggerOcrExtraction()` guards on null `documentPath`/`documentMimeType`. The screen replaces the raw UUID display with `uiState.doctorName` in both display sites, and wires both buttons. The `onExtractText` callback was wired inline rather than exposed as a public parameter — a plan deviation that is functionally correct but removes a test seam.

`PostReviewProcessor` receives `DuplicateAlertRepository` via constructor injection (no new Hilt module needed — `DuplicateAlertModule` already binds `RoomDuplicateAlertRepository`). At the end of `process()`, all patient medicines are loaded and passed to `DuplicateDetector.detect()`. The resulting candidates are each persisted as a `DuplicateAlert`. The detection block is unconditional — it runs even when no medicines were inserted — and because each alert ID is a fresh UUID the conflict strategy never deduplicates, producing growing alert counts for the same pair on repeated reviews.

Doctor management and patient profile routing is straightforward: new route constants in `Routes`, new composable blocks in `MediHistoryNavHost`, new rows in `MoreScreen`, and `onViewDoctors`/`onViewProfile` callbacks threaded from `DashboardScreen` through `DashboardTopBar` and `StatsGrid`. The DOCTOR_EDIT route correctly uses `hiltViewModel()` and a `LaunchedEffect` to pre-populate the form.

---

<details>
<summary>Issues (3)</summary>

1. **Duplicate alert accumulation** — `DuplicateDetector.detect()` and `duplicateAlertRepository.insert()` run on every `PostReviewProcessor.process()` call regardless of whether any medicines were added. Each alert gets a new `UUID.randomUUID()` id, so `OnConflictStrategy.IGNORE` never fires and each review inserts fresh rows for every already-detected pair. A patient reviewing ten prescriptions (some with no medicine fields) accumulates ten alert rows per pair. Fix: move the detection block inside the `if (medicines.isNotEmpty())` guard, or deduplicate on `(medicineAId, medicineBId)` before inserting. Confidence: **confirmed**.

2. **LaunchedEffect double-fire on process-death restore (camera)** — `LaunchedEffect(cameraResultPath, cameraResultSha)` removes both keys after calling `onCameraImageConfirmed`, but on process-death restoration the effect body executes after recomposition, and the captured state values may still be non-empty when the effect re-runs, creating a second `Prescription` row for the same image. Fix: write `""` to both keys synchronously before calling `onCameraImageConfirmed` so any concurrent recomposition sees empty values. Confidence: **likely**.

3. **`onExtractText` absent from `PrescriptionDetailScreen` public signature** — The plan (item 3a) required adding `onExtractText: () -> Unit = {}` as an injectable parameter. The implemented code wires it inline (`onClick = { viewModel.triggerOcrExtraction(prescriptionId, patientId) }`) without the parameter, removing the test seam. Tests that want to stub OCR triggering without a real `WorkManager` cannot do so through the composable's API. Confidence: **confirmed**.

</details>

---

<details>
<summary>Details</summary>

## Camera result wiring and LaunchedEffect key semantics

The CAMERA composable sets `camera_result_path` and `camera_result_sha256` on `previousBackStackEntry.savedStateHandle` then calls `navigateUp()`. The IMPORT composable reads both as `StateFlow<String>` with an empty-string default and passes them as keys to `LaunchedEffect`. When both are non-empty the effect calls `importViewModel.onCameraImageConfirmed(cameraResultPath, cameraResultSha)` and removes both keys.

`savedStateHandle.remove<String>(key)` triggers the `StateFlow` to re-emit to its default `""`, which changes the `LaunchedEffect` keys. In normal operation Compose debounces `LaunchedEffect` cancellation to the end of the composition pass, so the remove calls from inside the running effect are safe. On process-death restoration, however, the effect body executes after recomposition — the state values captured at launch time may still be the original non-empty strings when the effect re-runs, and a second call to `onCameraImageConfirmed` with the same path/sha256 creates a second `Prescription` row pointing to the same file.

Fix: before calling `onCameraImageConfirmed`, write `""` to both keys (`backStackEntry.savedStateHandle.set("camera_result_path", "")`). Any concurrent recomposition then keys on `""` and skips the body.

## Duplicate detection accumulation

The detection block in `PostReviewProcessor.process()` sits outside the `if (medicines.isNotEmpty())` guard:

```kotlin
if (medicines.isNotEmpty()) {
    medicineRepository.insertAll(medicines)
}

// This block always runs:
val allMedicines = medicineRepository.observeByPatient(patientId).first()
val candidates = DuplicateDetector.detect(allMedicines)
for (candidate in candidates) {
    val alert = DuplicateAlert(
        id = UUID.randomUUID().toString(),   // ← new UUID every time
        ...
    )
    duplicateAlertRepository.insert(alert)
}
```

Because each `DuplicateAlert.id` is a fresh `UUID`, the DAO's `OnConflictStrategy.IGNORE` keyed on `id` never deduplicates. A patient who reviews a doctor-name-only field ten times accumulates ten identical alert rows per pair. The duplicate alert list screen will show ten entries for the same pair.

## `PrescriptionDetailScreen` API deviation

The plan (item 3a) required `onExtractText: () -> Unit = {}` as a publicly injectable parameter, with item 5c describing wiring the default to `viewModel.triggerOcrExtraction`. The coder instead wired it inline and omitted the parameter entirely. At runtime the behaviour is identical to the plan's intent. The missing seam matters only for composable-level tests that want to verify the "Extract text" tap path without injecting a real `WorkManager` — they currently have no hook to do so through `PrescriptionDetailScreen`'s public API.

</details>

---

<details>
<summary>File map</summary>

| File | Change |
|---|---|
| `ui/picker/ImportViewModel.kt` | Added `onCameraImageConfirmed` — no file copy, inserts Prescription, enqueues OcrWorker |
| `ui/navigation/MediHistoryNavHost.kt` | CAMERA: savedStateHandle result passing; IMPORT: LaunchedEffect reader; PRESCRIPTION_DETAIL: `onReviewOcr` wired; doctor routes (LIST, ADD, EDIT); patient profile route; DASHBOARD callbacks |
| `ui/navigation/Routes.kt` | Added `DOCTOR_LIST`, `DOCTOR_EDIT_ROUTE`, `DOCTOR_ADD`, `PATIENT_PROFILE`, `doctorEdit()` |
| `ui/prescription/PrescriptionDetailScreen.kt` | `onReviewOcr` parameter added; "Review →" wired; "Extract text" wired inline; `doctorName` from uiState in both display sites |
| `ui/prescription/PrescriptionDetailViewModel.kt` | `doctorName` in state; `DoctorRepository` + `WorkManager` injected; `load()` resolves doctor name; `triggerOcrExtraction()` added |
| `data/ocr/PostReviewProcessor.kt` | `DuplicateAlertRepository` injected; duplicate detection + alert insertion added unconditionally at end of `process()` |
| `ui/more/MoreScreen.kt` | "My Profile" and "My Doctors" rows added before Safety Center |
| `ui/dashboard/DashboardScreen.kt` | `onViewDoctors`, `onViewProfile` parameters; avatar tap target with `clickable` + `semantics`; Doctors `StatTile` wired to `onViewDoctors` |

Full diff: `git diff main` against the `feature/Bug-Fix` branch.

</details>
