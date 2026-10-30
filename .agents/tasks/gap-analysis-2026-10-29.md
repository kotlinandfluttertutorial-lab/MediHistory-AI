# MediHistory AI — Gap Analysis Report
**Date:** 2026-10-29  
**Scope:** Android app codebase, all MVP features F01–F10  
**Method:** Static code inspection — all findings are grounded in actual file content

---

## Executive Summary

The app is substantially further along than a skeleton. Firebase authentication (F10), OCR import + review flow (F03), prescription list / detail (F02), medicine explorer (F04), duplicate alerts (F05), medical history (F06), New Doctor Mode / PDF export (F07), reminders (F08), privacy & data deletion (F10) all have real Room-backed ViewModels and functioning screens. The biggest remaining gaps are:

1. **OCR provider is a stub** — `StubOcrService` is wired in production; no real provider is integrated.
2. **DuplicateDetector is never triggered** — the detector exists and is tested, but nothing calls it after OCR review completes.
3. **`DoctorListScreen` and `PatientProfileScreen` are orphaned** — both screens and ViewModels are fully implemented but are not registered in the `NavHost` or `Routes`. They are unreachable at runtime.
4. **`PrescriptionDetailScreen` — "Review →" button and "Extract text" button are dead** — both `onClick` handlers are empty lambdas.
5. **Reminders FAB and active-toggle switch are unimplemented stubs**.
6. **`MoreScreen` has no entry point to Doctor management or Patient profile**.
7. **`SyntheticPrescriptions.kt` lives in the main source set** — it's sample data that should only be in the test source set or previews.
8. **AI summary (F06) is a permanent stub** — `HistoryCompiler` emits a hardcoded "AI provider not configured" text entry.
9. **Interaction checking (F09) is permanently `NotConfigured`** — no provider path exists.
10. **CI has a coverage gate comment but no real JaCoCo threshold** enforcement.

---

## 1. Navigation Completeness

### Routes Defined (`Routes.kt`)
All of the following routes are defined:
`DASHBOARD`, `PRESCRIPTIONS`, `MEDICINE_EXPLORER`, `DUPLICATE_ALERTS`, `MEDICAL_HISTORY`, `REMINDERS`, `IMPORT`, `CAMERA`, `PRESCRIPTION_DETAIL`, `MEDICINE_DETAIL`, `OCR_REVIEW`, `NEW_DOCTOR_MODE`, `PDF_PREVIEW`, `SAFETY_CENTER`, `PRIVACY`, `MORE`, `DOCTOR_GROUP`

### Routes NOT in `NavHost`
| Missing Route | Screen exists? | Impact |
|---|---|---|
| `PDF_PREVIEW` | `PdfPreviewScreen.kt` exists | **Medium** — F07 PDF preview step skipped; share sheet fires directly |
| `DOCTOR_LIST` / `DOCTOR_EDIT` | `DoctorListScreen.kt` + `DoctorEditScreen.kt` exist | **High** — Doctor management is completely unreachable from the running app |
| `PATIENT_PROFILE` | `PatientProfileScreen.kt` exists | **High** — Patient can never edit their profile (name, DOB, blood group, allergies) |

### Routes in `NavHost` — Callback wiring status
| Route | All callbacks wired? | Notes |
|---|---|---|
| `DASHBOARD` | ✅ Yes | All 6 `on*` lambdas navigate to real routes |
| `PRESCRIPTIONS` | ⚠️ Partial | Search `IconButton` = `/* TODO: open search */`; Sort `IconButton` = `/* TODO: open sort */` |
| `PRESCRIPTION_DETAIL` | ⚠️ Partial | "Review →" `Button.onClick` = empty lambda; "Extract text" `OutlinedButton.onClick` = empty lambda |
| `MEDICINE_EXPLORER` | ⚠️ Partial | Filter `IconButton` = `/* Filter bottom sheet — future ticket */`; "Duplicate alert" chip = no-op |
| `MEDICINE_DETAIL` | ✅ Yes | Back + view-prescription wired |
| `DUPLICATE_ALERTS` | ✅ Yes | |
| `MEDICAL_HISTORY` | ✅ Yes | |
| `OCR_REVIEW` | ✅ Yes | `onAllReviewed` → Prescriptions list |
| `NEW_DOCTOR_MODE` | ✅ Yes | |
| `CAMERA` | ⚠️ Partial | `onImageConfirmed` = `{ _, _ -> navigateUp() }` — captured image is **silently discarded**; never fed to the import flow |
| `IMPORT` | ✅ Yes | Navigates to OCR review on success |
| `SAFETY_CENTER` | ✅ Yes | |
| `REMINDERS` | ✅ Yes | |
| `PRIVACY` | ✅ Yes | `onManageConsent` = `/* TODO */` |
| `MORE` | ✅ Yes | |
| `DOCTOR_GROUP` | ✅ Yes | |

---

## 2. Screen-by-Screen Functional Gaps

### DashboardScreen (`ui/dashboard/DashboardScreen.kt`)
- **Data source:** Real Room via `DashboardViewModel` — combines `PrescriptionRepository`, `MedicineRepository`, `DoctorRepository`, `DuplicateAlertDao`. ✅
- **Empty state:** ✅ `DashboardEmptyState` for zero prescriptions.
- **Error state:** ✅ error text + Retry button.
- **Loading state:** ✅ shimmer skeleton.
- **Gaps:** Doctor count tile `onClick = {}` (no navigation). Recent-prescriptions cards use `onClick = onViewPrescriptions` (always goes to list, not to the tapped prescription). Profile avatar in top bar is a non-interactive initial-letter circle — no navigation to `PatientProfileScreen`.

### PrescriptionListScreen (`ui/prescription/PrescriptionListScreen.kt`)
- **Data source:** Real Room via `PrescriptionListViewModel`. ✅
- **Empty / error state:** ✅
- **Gaps:** Search icon `/* TODO: open search */`; Sort icon `/* TODO: open sort */`.

### PrescriptionDetailScreen (`ui/prescription/PrescriptionDetailScreen.kt`)
- **Data source:** Real Room via `PrescriptionDetailViewModel`. ✅
- **Empty / error state:** ✅
- **Gaps — High impact:**
  - "Review →" `Button.onClick = { /* OCR review nav — wired in NavHost */ }` — **dead button**. Nothing happens when tapped even though the route exists.
  - "Extract text" `OutlinedButton.onClick = { /* OCR trigger — implemented in a future ticket */ }` — **dead button**.
  - Doctor field shows raw `doctorId` UUID string, not the doctor's name — a regression from the detail-screen design intent.

### OcrReviewScreen / OcrReviewViewModel
- **Data source:** Real `ExtractedFieldDao` + `PostReviewProcessorInterface`. ✅
- **State machine:** ✅ All valid transitions enforced.
- **Gaps:** None critical; flow works end-to-end with `StubOcrService`.

### ImportScreen / ImportViewModel
- **Data source:** Real file I/O + `PrescriptionRepository` + `OcrWorker`. ✅
- **Gaps:** None significant; all three paths (gallery, PDF, camera) are wired. Camera path navigates to `CameraScreen` correctly.

### CameraScreen / CameraViewModel / CameraPreviewContent
- **Status:** Functional CameraX preview with shutter capture and confirm/retake flow. ✅
- **Gap — High:** `NavHost` wires `onImageConfirmed = { _, _ -> navController.navigateUp() }` — the captured file path and hash are **never passed to `ImportViewModel`**. Camera capture is fully disconnected from the import flow. The patient can take a photo, confirm it, and the image is silently discarded.

### MedicineListScreen / MedicineDetailScreen
- **Data source:** Real Room. ✅
- **Search:** ✅ `DockedSearchBar` wired to `MedicineListViewModel.onSearchQueryChange`.
- **Filter chips:** ✅ Status filter works client-side.
- **Gaps:** Filter FAB = no-op; "Duplicate alert" chip = no-op.

### DoctorListScreen + DoctorEditScreen (`ui/doctor/`)
- **Data source:** `DoctorViewModel` injects `DoctorRepository` — real Room. ✅
- **Fully implemented:** List, add, edit, delete with confirmation dialog. ✅
- **Gap — Critical:** Neither screen is registered in `NavHost` or `Routes`. Unreachable from anywhere in the running app. The `MoreScreen` does not include a "My Doctors" entry.

### PatientProfileScreen (`ui/patient/PatientProfileScreen.kt`)
- **Data source:** `PatientViewModel` injects `PatientRepository` — real Room. ✅
- **Fully implemented:** Name, DOB picker, blood group dropdown, allergy chips. ✅
- **Gap — Critical:** Not registered in `NavHost` or `Routes`. The Dashboard avatar and `MoreScreen` have no entry point to it.

### DuplicateReviewScreen / DuplicateReviewViewModel
- **Data source:** Real `DuplicateAlertDao` + `MedicineRepository`. ✅
- **Gaps:** `onViewMedicines` action button in empty state = no-op `{ /* Caller handles navigation */ }`.

### MedicalHistoryScreen / MedicalHistoryViewModel + HistoryCompiler
- **Data source:** Real Room — only `REVIEWED` prescriptions included. ✅
- **Gap — Medium:** `AI_SUMMARY` section is a permanent stub: `"AI summary generation requires a configured AI provider. Reviewed records compiled above."` No AI provider integration exists.

### ReminderScreen / ReminderViewModel
- **Data source:** Real `ReminderRepository` + `DoseRecordDao`. ✅
- **Gaps:**
  - FAB `onClick = { /* TODO: navigate to medicine list to pick a medicine */ }` — **dead**. There is no way to create a reminder from this screen.
  - Add `IconButton` in top bar = `{ /* Add reminder — future ticket */ }` — **dead**.
  - Active-toggle `Switch.onCheckedChange = { /* Active toggle — wired in a follow-up ticket */ }` — **dead**. Toggling active state does nothing.

### SafetyCenterScreen / SafetyCenterViewModel
- **Data source:** Real `DuplicateAlertDao`. ✅
- **Interaction check:** Always `NotConfigured` — gated behind `BuildConfig.ENABLE_INTERACTION_CHECK` which is always `false` in CI and in production builds. ✅ medical-safety correctly implemented.

### NewDoctorModeScreen / NewDoctorViewModel + PdfGenerator
- **Data source:** Real `PrescriptionRepository` + `MedicineRepository`. ✅
- **PDF generation:** `PdfGenerator.generate()` exists; FileProvider URI delivery works. ✅
- **Gaps:** `PdfPreviewScreen.kt` exists but the route `PDF_PREVIEW` is not in the `NavHost` and `NewDoctorModeScreen` skips to the share sheet directly. The PDF preview step is missing.

### PrivacyScreen / PrivacyViewModel
- **Data source:** `DataDeletion` + `DataExporter` — real Room. ✅
- **Gap:** `ConsentCard.onManageConsent = { /* TODO: navigate to consent management */ }` — dead button.

### MoreScreen
- **Data source:** None — navigation hub only.
- **Gap — High:** Does not include "My Doctors" entry (no route to `DoctorListScreen`) and no "My Profile" entry (no route to `PatientProfileScreen`).

---

## 3. ViewModel Gaps

| ViewModel | Real Repository? | Notable gaps |
|---|---|---|
| `DashboardViewModel` | ✅ Yes | `medicineCount` shows total, not active-only |
| `PrescriptionListViewModel` | ✅ Yes | None |
| `PrescriptionDetailViewModel` | ✅ Yes | Loads prescription but doesn't resolve `doctorId` → name |
| `OcrReviewViewModel` | ✅ Yes | None |
| `ImportViewModel` | ✅ Yes | None |
| `CameraViewModel` | ✅ Yes | onConfirm result is ignored by NavHost |
| `MedicineListViewModel` | ✅ Yes | None |
| `MedicineDetailViewModel` | ✅ Yes | None |
| `DuplicateReviewViewModel` | ✅ Yes | None |
| `MedicalHistoryViewModel` | ✅ Yes | AI summary is a permanent stub |
| `ReminderViewModel` | ✅ Yes | `createReminder` exists but no UI path calls it |
| `SafetyCenterViewModel` | ✅ Yes | Interaction check always NotConfigured |
| `NewDoctorViewModel` | ✅ Yes | `doctorId` UUID used as display name in PDF header |
| `DoctorViewModel` | ✅ Yes | Fully implemented; orphaned |
| `PatientViewModel` | ✅ Yes | Fully implemented; orphaned |
| `LoginViewModel` | ✅ Firebase | None |
| `PrivacyViewModel` | ✅ Yes | None |

---

## 4. Data Layer Completeness

All CRUD interfaces are implemented with real Room delegates. No method is stubbed out.

| Repository Interface | Implementation | Fully implemented? |
|---|---|---|
| `PrescriptionRepository` | `RoomPrescriptionRepository` | ✅ Yes |
| `MedicineRepository` | `RoomMedicineRepository` | ✅ Yes |
| `DoctorRepository` | `RoomDoctorRepository` | ✅ Yes |
| `PatientRepository` | `RoomPatientRepository` | ✅ Yes |
| `DuplicateAlertRepository` | `RoomDuplicateAlertRepository` | ✅ Yes |
| `ReminderRepository` | `RoomReminderRepository` | ✅ Yes |
| `AuthRepository` | `AuthRepositoryImpl` (Firebase) | ✅ Yes |

**Single data layer gap:** `DuplicateDetector.detect()` exists as a well-tested pure function (`duplicate/DuplicateDetector.kt`) but is **never called** in the production code path. `PostReviewProcessor` creates `Medicine` rows but does not call `DuplicateDetector` to generate `DuplicateAlert` rows. The duplicate alert list will therefore always be empty for new users.

---

## 5. MVP Feature Coverage (F01–F10)

| Feature | Status | Key gaps |
|---|---|---|
| **F01 Dashboard** | ✅ Functional | Doctor tile not navigable; recent-prescriptions tap goes to list not detail |
| **F02 Prescription management** | ⚠️ Mostly done | "Review →" and "Extract text" buttons dead on detail screen |
| **F03 OCR extraction with review** | ⚠️ Functional but stub OCR | `StubOcrService` used in production; no real provider |
| **F04 Medicine explorer** | ⚠️ Mostly done | Filter bottom sheet and duplicate-alert chip not implemented |
| **F05 Duplicate detection & alerts** | ⚠️ UI ready but broken | `DuplicateDetector` never triggered; alerts never populated automatically |
| **F06 Medical history summarisation** | ⚠️ Deterministic part done | AI summary is a permanent stub |
| **F07 New Doctor Mode / PDF** | ⚠️ Mostly done | PDF preview route missing from NavHost |
| **F08 Reminders** | ⚠️ Partial | Cannot create reminders from UI; toggle inactive does nothing |
| **F09 Interaction checking** | ⚠️ Intentionally deferred | Correctly shows `NotConfigured`; no provider path |
| **F10 Security / Privacy / Auth** | ✅ Mostly done | Firebase auth ✅; data export ✅; deletion ✅; consent management TODO |

---

## 6. Camera Screen

`CameraScreen.kt` + `CameraPreviewContent.kt` are **fully functional**:
- CameraX `Preview` + `ImageCapture` bound to lifecycle ✅
- Confirm/retake flow with temp-file management ✅
- SHA-256 hash computed on confirm ✅
- Permission denied handling ✅

**Critical gap:** The `NavHost` discards the result:

```kotlin
// MediHistoryNavHost.kt line ~120
composable(Routes.CAMERA) {
    CameraScreen(
        onImageConfirmed = { _, _ -> navController.navigateUp() },  // ← path + hash silently dropped
        onDismiss = { navController.navigateUp() },
    )
}
```

The confirmed photo is never handed to `ImportViewModel`; no prescription row is created after a camera capture.

---

## 7. Doctor Management UI

`DoctorListScreen` and `DoctorEditScreen` are **fully implemented** with real Room data but are **completely unreachable** at runtime:

- No route constant `DOCTOR_LIST` or `DOCTOR_EDIT` in `Routes.kt`
- Neither screen appears in `MediHistoryNavHost.kt`
- `MoreScreen` lists: Safety Center, New Doctor Mode, Reminders, Privacy — **no "My Doctors" entry**
- The Dashboard "Doctors" stat tile has `onClick = {}` (empty lambda)

To reach doctor management the user would need to type a deep link URL — not something any patient can do.

---

## 8. Patient Profile

`PatientProfileScreen` and `PatientViewModel` are **fully implemented** (name, DOB, blood group, allergies with chip input, save with validation) but are **unreachable**:

- No route constant in `Routes.kt`
- Not registered in `MediHistoryNavHost.kt`
- Dashboard top bar profile avatar is a non-interactive circle; no tap target
- `MoreScreen` has no "My Profile" entry

The patient's name shown in the Dashboard "Good morning, X" greeting comes from the email prefix set during login registration — the patient can never update it.

---

## 9. Backend / Auth Wiring

`AuthRepositoryImpl.kt` is **fully Firebase-backed**:

```kotlin
// AuthRepositoryImpl.kt
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository { ... }
```

- `login()` calls `firebaseAuth.signInWithEmailAndPassword().await()` ✅
- `register()` calls `firebaseAuth.createUserWithEmailAndPassword().await()` ✅
- `logout()` calls `firebaseAuth.signOut()` ✅
- `isLoggedIn()` / `currentUserId()` delegate to `firebaseAuth.currentUser` ✅
- No HTTP stub remaining; no legacy network auth code present ✅

The `SessionManager` uses `EncryptedSharedPreferences` to persist the patient ID (the Firebase UID). All downstream Room queries use this ID as the patient scope.

---

## 10. OCR Provider

`OcrModule.kt` explicitly provides `StubOcrService()` in production:

```kotlin
// OcrModule.kt
@Provides
@Singleton
fun provideOcrService(): OcrService = StubOcrService()
// TODO(ADR-006): replace StubOcrService with the selected provider adapter.
```

`StubOcrService` returns synthetic fixtures (`"Dr. Synthetic"`, `"Synthamol 500mg"`, `confidence = 0.62f`) with a 500ms artificial delay. It is the **only OCR implementation**. No Google ML Kit, Firebase ML, or cloud-OCR adapter exists anywhere in the codebase.

**Impact:** Every prescription imported by a real user will receive synthetic extracted fields regardless of the actual document content. The OCR review flow will always show `"Dr. Synthetic"` and `"Synthamol 500mg"` as suggested values.

---

## 11. Unimplemented TODO Comments (main source only)

| File | Location | Comment | Impact |
|---|---|---|---|
| `OcrModule.kt:27` | `provideOcrService()` | `// TODO(ADR-006): replace StubOcrService` | **High** — No real OCR in production |
| `ReminderScreen.kt:107` | FAB `onClick` | `/* TODO: navigate to medicine list to pick a medicine */` | **High** — Cannot create reminders |
| `PrivacyScreen.kt:147` | `ConsentCard.onManageConsent` | `/* TODO: navigate to consent management */` | **Medium** — F10 consent management missing |
| `PrescriptionListScreen.kt:57` | Search `IconButton` | `/* TODO: open search */` | **Medium** — Search UI not implemented |
| `PrescriptionListScreen.kt:63` | Sort `IconButton` | `/* TODO: open sort */` | **Low** — Sort not implemented |

Additional non-`TODO` stubs worth noting:
- `PrescriptionDetailScreen.kt` "Review →" button: `onClick = { /* OCR review nav — wired in NavHost */ }` — **not** wired
- `PrescriptionDetailScreen.kt` "Extract text" button: `onClick = { /* OCR trigger — implemented in a future ticket */ }` — not implemented
- `ReminderScreen.kt` active toggle: `onCheckedChange = { /* Active toggle — wired in a follow-up ticket */ }`
- `MediHistoryNavHost.kt` camera route: `onImageConfirmed = { _, _ -> navController.navigateUp() }`
- `DashboardScreen.kt` doctors tile: `onClick = {}`

Also worth noting: `SyntheticPrescriptions.kt` lives in `ui/prescription/` inside the **main source set**. It contains synthetic fixtures (`"synth-rx-001"`, `"Dr. Synthetic Mehta"`) intended for previews/tests. It is not actively used at runtime (no production ViewModel references it) but should be moved to the test source set or a `@Preview`-only file to avoid confusion.

---

## 12. CI/CD

Three workflow files are present:

### `.github/workflows/android-ci.yml`
- Android Lint → unit tests → instrumented tests (emulator) → artifact upload ✅
- **Gap:** Coverage check is a comment: `"Unit tests passed - coverage gate pending instrumented test setup"`. No JaCoCo report is generated or threshold enforced. The steering rule calls for ≥80% data layer coverage and 100% branch coverage for `DuplicateDetector`/`ValidationUtils`.

### `.github/workflows/pr-checks.yml`
- Secrets grep ✅ | `google-services.json` check ✅ | lint ✅ | unit tests ✅ | backend CI ✅ | PR gate ✅
- Backend coverage gate: `--cov-fail-under=90` ✅
- **Gap:** No Android JaCoCo threshold; no UI/Espresso tests in the PR gate; `AccessibilityChecks` not run in CI.

### `.github/workflows/backend-ci.yml`
- Ruff lint + black format + pytest with 90% coverage gate ✅
- **Gap:** No migration smoke test (Alembic) step.

---

## Top 10 Prioritised Gaps to Fix Next

| # | Gap | Feature | Impact | Effort |
|---|---|---|---|---|
| 1 | **Camera → Import disconnect** — `onImageConfirmed = { _, _ -> navigateUp() }` in `MediHistoryNavHost.kt` silently drops captured images. Fix: pass `filePath` + `sha256` to a new `CameraImportViewModel` or reuse `ImportViewModel` with a `onCameraResult(path, sha256)` method. | F02/F03 | **Critical** | Small — 1 file |
| 2 | **Register `DoctorListScreen` + `DoctorEditScreen` in NavHost and add "My Doctors" to MoreScreen** — all logic exists; just needs route constants + composable blocks + a MoreRow entry. | F02 | **High** | Small — 2 files |
| 3 | **Register `PatientProfileScreen` in NavHost; add "My Profile" to MoreScreen and tap target on Dashboard avatar** — all logic exists. | F10 | **High** | Small — 2 files |
| 4 | **Wire "Review →" and "Extract text" buttons in `PrescriptionDetailScreen`** — both should navigate using the existing `Routes.ocrReview(prescriptionId)` route and trigger `OcrWorker`. The NavHost already has this route. | F03 | **High** | Small — 1 file |
| 5 | **Wire `DuplicateDetector` after `PostReviewProcessor`** — call `DuplicateDetector.detect(allMedicinesForPatient)` at the end of `PostReviewProcessor.process()` and persist resulting `DuplicateAlert` rows via `DuplicateAlertRepository`. Without this, duplicate alerts are never populated. | F05 | **High** | Medium — 2 files |
| 6 | **Replace `StubOcrService` with a real OCR provider** — integrate Google ML Kit Text Recognition (`com.google.android.gms:play-services-mlkit-text-recognition`) or Firebase ML. Update `OcrModule` per ADR-006. | F03 | **High** | Large |
| 7 | **Wire the Reminders "Set Up Reminder" FAB** — navigate to `MedicineListScreen` so the patient can pick a medicine, then call `ReminderViewModel.createReminder()`. Also implement the active-toggle switch. | F08 | **High** | Medium — 2 files |
| 8 | **Resolve `doctorId` to doctor name in `PrescriptionDetailScreen`** — the screen currently shows the raw UUID. `PrescriptionDetailViewModel` should also load the doctor via `DoctorRepository.getById()`. | F02 | **Medium** | Small |
| 9 | **Move `SyntheticPrescriptions.kt` out of the main source set** — it belongs in `src/test/` or a `@Preview`-only file. Its presence in main may cause confusion about whether the app shows stub data at runtime. | Architecture | **Low** | Trivial |
| 10 | **Add JaCoCo coverage threshold to CI** — add `jacocoTestReport` + `jacocoTestCoverageVerification` tasks to `build.gradle.kts` with the steering-mandated thresholds (≥80% line for data layer, 100% branch for `DuplicateDetector`/`ValidationUtils`) and fail the `android-ci` job if they are not met. | Quality | **Medium** | Small |

---

*Report generated by read-only static analysis. No code was modified.*
