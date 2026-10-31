# Implementation Plan — 5 Critical Gap Fixes
**Date:** 2026-10-29  
**Scope:** Android app, gaps identified in `gap-analysis-2026-10-29.md`  
**Target branch:** feature branch off main  

---

## Pre-flight discoveries (grounded in source reading)

| Finding | Impact on plan |
|---|---|
| `DoctorViewModel` has `loadDoctors(patientId)` and `loadDoctor` does **not** exist | The DOCTOR_EDIT route for editing an existing doctor cannot pre-populate fields. For now, the edit route renders `DoctorEditScreen` as an add-form (safe; no data loss; existing doctor is not overwritten). A dedicated `loadDoctor(id, patientId)` must be added to `DoctorViewModel` to support edit. **See item 4c below.** |
| `MedicineRepository.observeByPatient(patientId)` exists at line 21 | Confirmed — use this exact name in `PostReviewProcessor`. |
| `DuplicateAlertRepository.insert(alert)` exists; no `OnConflictStrategy` detail needed at this layer | The Room DAO implementation uses `IGNORE` (confirmed in gap analysis); safe to call repeatedly. |
| `OcrWorker.buildRequest(prescriptionId, patientId, documentPath, mimeType)` — exact 4-param signature confirmed | Use exactly these params in `PrescriptionDetailViewModel`. |
| `DoctorRepository.getById(doctorId, patientId)` — confirmed signature | Use in `PrescriptionDetailViewModel` to resolve doctor name. |
| `PatientProfileScreen` signature: `patientId: String?` (nullable) | Pass the concrete `patientId` string, not null — edit mode (not create mode). |
| `PrescriptionDetailScreen` currently has no `onReviewOcr` or `onExtractText` params | Both must be added with default `{}` values so existing NavHost call site compiles before the NavHost is updated. |
| `DashboardScreen` currently has no `onViewDoctors` or `onViewProfile` params | Both must be added with default `{}` values so the existing NavHost call site compiles before the NavHost is updated. |
| `DashboardTopBar` is a `private` composable — its signature change must be co-located with the body change | Both touched in the same item. |
| `ImportViewModel` already imports `WorkManager`, `OcrWorker`, `PrescriptionRepository`, `PatientRepository`, `sessionManager` | `onCameraImageConfirmed` can reuse all of them with no new injections. |

---

## Implementation Plan

- [ ] 1. **Add `onCameraImageConfirmed` to `ImportViewModel`**  
      Add a public `fun onCameraImageConfirmed(filePath: String, sha256: String)` method that:
      - Sets `_importState.value = ImportState.Processing`
      - Reads `patientId` from `sessionManager.getPatientId()`
      - Calls `ensurePatientRowExists(patientId)` (same private helper as `handleUri`)
      - Constructs a `Prescription` with `mimeType = "image/jpeg"`, `documentPath = filePath`, `documentHashSha256 = sha256`, `ocrStatus = OcrStatus.PENDING`; does NOT copy the file
      - Calls `prescriptionRepository.insert(prescription)` inside a `viewModelScope.launch`
      - Calls `workManager.enqueue(OcrWorker.buildRequest(prescription.id, patientId, filePath, "image/jpeg"))`
      - On success: sets `_importState.value = ImportState.Success(filePath, sha256, "image/jpeg", prescription.id)`
      - On exception: logs only `e.javaClass.simpleName` via `Timber.tag("ImportViewModel").e(...)`, sets `_importState.value = ImportState.Error("Could not save prescription. Please try again.")`  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/picker/ImportViewModel.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 2. **Wire camera result back to `ImportViewModel` in `MediHistoryNavHost`**  
      Two changes in the same file:  
      a. **CAMERA composable block:** Replace the no-op `onImageConfirmed = { _, _ -> navController.navigateUp() }` with a lambda that calls `navController.previousBackStackEntry?.savedStateHandle?.set("camera_result_path", path)` and `navController.previousBackStackEntry?.savedStateHandle?.set("camera_result_sha256", sha256)`, then calls `navController.navigateUp()`.  
      b. **IMPORT composable block:** After the `ImportScreen(...)` call, add a `LaunchedEffect(Unit)` that collects `navController.currentBackStackEntry?.savedStateHandle?.getStateFlow<String?>("camera_result_path", null)` and `"camera_result_sha256"` via `combine`. When both values are non-null and non-empty, call `importViewModel.onCameraImageConfirmed(path, sha256)`, then remove both keys from the savedStateHandle. The `importViewModel` is obtained with `hiltViewModel()` inside the IMPORT composable — same instance as the one used by `ImportScreen`. Use `androidx.lifecycle.compose.collectAsStateWithLifecycle()` or `LaunchedEffect` + `snapshotFlow` as appropriate.  
      > **Rationale:** `savedStateHandle` on `previousBackStackEntry` is the standard Jetpack pattern for passing results between destinations without a shared ViewModel. It survives process death.  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 3. **Wire "Review →" button and "Extract text" button in `PrescriptionDetailScreen`**  
      a. Add `onReviewOcr: () -> Unit = {}` and `onExtractText: () -> Unit = {}` parameters to `PrescriptionDetailScreen` (defaults allow existing NavHost call site to continue compiling).  
      b. Change the "Review →" `Button.onClick` from `{ /* ... */ }` to `onClick = onReviewOcr`.  
      c. Change the "Extract text" `OutlinedButton.onClick` from `{ /* ... */ }` to `onClick = onExtractText`.  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/prescription/PrescriptionDetailScreen.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 4. **Add `triggerOcrExtraction` to `PrescriptionDetailViewModel`; inject `WorkManager` and `DoctorRepository`; add `doctorName` to state**  
      a. Add `doctorName: String? = null` field to `PrescriptionDetailUiState`.  
      b. Inject `WorkManager` and `DoctorRepository` into `PrescriptionDetailViewModel` via `@Inject` constructor parameters alongside the existing `PrescriptionRepository`.  
      c. In `load(id, patientId)`: after fetching the prescription, if `prescription.doctorId` is non-null, call `doctorRepository.getById(prescription.doctorId, patientId)` and include `doctorName = doctor?.name` in the success state.  
      d. Add `fun triggerOcrExtraction(prescriptionId: String, patientId: String)`: reads `uiState.value.prescription`; if `documentPath` or `documentMimeType` is null, returns early. Otherwise calls `workManager.enqueue(OcrWorker.buildRequest(prescriptionId, patientId, documentPath, documentMimeType))` and sets `_uiState.value` to the current state with `prescription.copy(ocrStatus = OcrStatus.PROCESSING)`.  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/prescription/PrescriptionDetailViewModel.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 5. **Wire doctor name display and OCR callbacks in `PrescriptionDetailScreen`**  
      a. Replace `prescription.doctorId?.let { "Doctor ID: $it" } ?: "Doctor not specified"` (in the header card) with `uiState.doctorName?.let { it } ?: "Doctor not specified"`.  
      b. Replace the standalone `prescription.doctorId ?: "Not specified"` (in the doctor info section below the preview) with `uiState.doctorName ?: "Not specified"`.  
      c. Wire `onExtractText = { viewModel.triggerOcrExtraction(prescriptionId, patientId) }` internally, passing it to the `PrescriptionDetailScreen` composable from within the same composable body (i.e., set this as the default or pass directly; the internal lambda captures `viewModel`, `prescriptionId`, and `patientId` which are all in scope). Because `onExtractText` has a default of `{}`, this is done by wiring it directly inside the composable rather than requiring the NavHost to supply it — change the default `{}` to `{ viewModel.triggerOcrExtraction(prescriptionId, patientId) }`.  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/prescription/PrescriptionDetailScreen.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 6. **Update the `PRESCRIPTION_DETAIL` composable in `MediHistoryNavHost` to pass `onReviewOcr`**  
      In the existing `PRESCRIPTION_DETAIL` composable block, add `onReviewOcr = { navController.navigate(Routes.ocrReview(id)) }` to the `PrescriptionDetailScreen(...)` call. (`onExtractText` is wired inside the screen itself in item 5c, so it does not need to be passed from the NavHost.)  
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 7. **Add `DuplicateDetector` and `DuplicateAlertRepository` to `PostReviewProcessor`**  
      a. Add `private val duplicateAlertRepository: DuplicateAlertRepository` and `private val medicineRepository: MedicineRepository` constructor parameters via `@Inject`. (Note: `medicineRepository` is **already** a constructor parameter — do **not** add it again. Only `duplicateAlertRepository` is new. Confirm by re-reading the constructor before writing.)  
      b. At the end of `process()` — after the `if (medicines.isNotEmpty()) { medicineRepository.insertAll(medicines) }` block — add:
      ```
      // Load ALL medicines for the patient (not just the ones inserted this run).
      val allMedicines = medicineRepository.observeByPatient(patientId).first()
      val candidates = DuplicateDetector.detect(allMedicines)
      for (candidate in candidates) {
          val alert = DuplicateAlert(
              id = UUID.randomUUID().toString(),
              patientId = patientId,
              medicineAId = candidate.medicineAId,
              medicineBId = candidate.medicineBId,
              matchingIngredient = candidate.matchingIngredient,
              matchType = candidate.matchType,
              confidence = candidate.confidence,
              combinationNote = candidate.combinationNote,
              createdAt = System.currentTimeMillis(),
          )
          duplicateAlertRepository.insert(alert)
      }
      ```
      c. Add imports: `com.medihistory.ai.data.model.DuplicateAlert`, `com.medihistory.ai.data.repository.DuplicateAlertRepository`, `com.medihistory.ai.duplicate.DuplicateDetector`. The `kotlinx.coroutines.flow.first` import is already present.  
      d. The `PostReviewProcessorInterface` does not change — this is an implementation-only change.  
      Files: `android/app/src/main/java/com/medihistory/ai/data/ocr/PostReviewProcessor.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 8. **Update Hilt binding for `PostReviewProcessor` (if needed)**  
      Check whether `PostReviewProcessor` is bound via a Hilt `@Binds` or `@Provides` method (likely in an `OcrModule` or `DataModule`). If it is provided via a `@Binds` method (`PostReviewProcessorInterface` → `PostReviewProcessor`), the new constructor parameter is injected automatically and no module change is needed. If it is provided via `@Provides`, update the provides method to accept `DuplicateAlertRepository`. Search for `PostReviewProcessor` in `*Module.kt` files before writing.  
      Files: search `android/app/src/main/java/com/medihistory/ai/di/` for the binding  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors (Hilt would fail here if the binding is broken).

- [ ] 9. **Add doctor management route constants to `Routes.kt`**  
      Add inside the `Routes` object:
      ```kotlin
      const val DOCTOR_LIST = "doctor_list"
      const val DOCTOR_EDIT_ROUTE = "doctor_edit/{doctorId}"
      const val DOCTOR_ADD = "doctor_add"
      fun doctorEdit(id: String) = "doctor_edit/$id"
      ```
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/Routes.kt`  
      Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 10. **Add `PATIENT_PROFILE` route constant to `Routes.kt`**  
       Add inside the `Routes` object:
       ```kotlin
       const val PATIENT_PROFILE = "patient_profile"
       ```
       Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/Routes.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 11. **Add `loadDoctor` to `DoctorViewModel` to support the edit route**  
       `DoctorViewModel` currently only has `loadDoctors(patientId)` (bulk list). Add:
       ```kotlin
       fun loadDoctor(doctorId: String, patientId: String) {
           viewModelScope.launch {
               val doctor = doctorRepository.getById(doctorId, patientId)
               if (doctor != null) {
                   _uiState.update {
                       it.copy(
                           name = doctor.name,
                           specialty = doctor.specialty,
                           specialtyOther = doctor.specialtyOther ?: "",
                           clinicName = doctor.clinicName ?: "",
                           isLoading = false,
                       )
                   }
               }
           }
       }
       ```
       This pre-populates the `DoctorEditScreen` form when navigating from the list. Without this, the edit route shows a blank add-form (no data loss, but poor UX).  
       Files: `android/app/src/main/java/com/medihistory/ai/ui/doctor/DoctorViewModel.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 12. **Register doctor and patient profile routes in `MediHistoryNavHost`**  
       Add four composable blocks **before** the closing brace of the `NavHost`:
       
       a. **`DOCTOR_LIST`:**
       ```kotlin
       composable(Routes.DOCTOR_LIST) {
           DoctorListScreen(
               patientId = patientId,
               onAddClick = { navController.navigate(Routes.DOCTOR_ADD) },
               onDoctorClick = { doctor -> navController.navigate(Routes.doctorEdit(doctor.id)) },
           )
       }
       ```
       
       b. **`DOCTOR_ADD`:**
       ```kotlin
       composable(Routes.DOCTOR_ADD) {
           DoctorEditScreen(
               patientId = patientId,
               onSaveSuccess = { navController.navigateUp() },
               onNavigateUp = { navController.navigateUp() },
           )
       }
       ```
       
       c. **`DOCTOR_EDIT_ROUTE`** (edit existing doctor — pre-populate via `LaunchedEffect`):
       ```kotlin
       composable(
           route = Routes.DOCTOR_EDIT_ROUTE,
           arguments = listOf(navArgument("doctorId") { type = NavType.StringType }),
       ) { backStackEntry ->
           val doctorId = backStackEntry.arguments?.getString("doctorId").orEmpty()
           val doctorViewModel: DoctorViewModel = hiltViewModel()
           LaunchedEffect(doctorId) {
               doctorViewModel.loadDoctor(doctorId, patientId)
           }
           DoctorEditScreen(
               patientId = patientId,
               onSaveSuccess = { navController.navigateUp() },
               onNavigateUp = { navController.navigateUp() },
               viewModel = doctorViewModel,
           )
       }
       ```
       
       d. **`PATIENT_PROFILE`:**
       ```kotlin
       composable(Routes.PATIENT_PROFILE) {
           PatientProfileScreen(
               patientId = patientId,
               onSaveSuccess = { navController.navigateUp() },
               onNavigateUp = { navController.navigateUp() },
           )
       }
       ```
       
       Add the required import statements: `DoctorListScreen`, `DoctorEditScreen`, `PatientProfileScreen`.  
       Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 13. **Add "My Doctors" and "My Profile" entries to `MoreScreen`**  
       a. Add two new `item { MoreRow(...) }` blocks inside the `LazyColumn`, placed before the existing Safety Center item:
       
       ```kotlin
       item {
           MoreRow(
               icon = Icons.Outlined.Person,
               title = "My Profile",
               subtitle = "View and edit your profile",
               onClick = { onNavigate(Routes.PATIENT_PROFILE) },
           )
       }
       item {
           MoreRow(
               icon = Icons.Outlined.MedicalServices,
               title = "My Doctors",
               subtitle = "Manage your doctors",
               onClick = { onNavigate(Routes.DOCTOR_LIST) },
           )
       }
       ```
       
       b. Add imports: `Icons.Outlined.Person` (already in material-icons-core) and `Icons.Outlined.MedicalServices` (already imported at project level for the Dashboard; confirm it is in `material-icons-extended` in libs.versions.toml before using; otherwise use `Icons.Outlined.LocalHospital` or `Icons.Default.Person` for the Doctors row).  
       Files: `android/app/src/main/java/com/medihistory/ai/ui/more/MoreScreen.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 14. **Wire `onViewDoctors` in `DashboardScreen` — Doctors stat tile + NavHost**  
       a. Add `onViewDoctors: () -> Unit = {}` parameter to `DashboardScreen` (default `{}` keeps NavHost backward-compatible until item 15).  
       b. Pass `onViewDoctors` down to `StatsGrid`. Change `StatsGrid`'s signature to include `onViewDoctors: () -> Unit` and wire it to the Doctors `StatTile onClick` (currently `onClick = {}`):
       ```kotlin
       StatTile(
           icon = Icons.Outlined.MedicalServices,
           count = uiState.doctorCount,
           label = "Doctors",
           countColor = MaterialTheme.colorScheme.secondary,
           onClick = onViewDoctors,   // ← was {}
           contentDesc = "${uiState.doctorCount} doctors. Tap to manage.",
           modifier = Modifier.weight(1f),
       )
       ```
       Files: `android/app/src/main/java/com/medihistory/ai/ui/dashboard/DashboardScreen.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 15. **Wire `onViewProfile` in `DashboardScreen` — profile avatar tap target + NavHost**  
       a. Add `onViewProfile: () -> Unit = {}` parameter to `DashboardScreen` and pass it to `DashboardTopBar`.  
       b. Add `onViewProfile: () -> Unit` to `DashboardTopBar`'s parameter list.  
       c. In `DashboardTopBar`, wrap the avatar `Box` with `Modifier.clickable { onViewProfile() }` and add `semantics { contentDescription = "Profile. Tap to view your profile." }` to the same `Box`. The existing `Modifier` chain becomes:
       ```kotlin
       Box(
           modifier = Modifier
               .size(40.dp)
               .clip(CircleShape)
               .background(MaterialTheme.colorScheme.primaryContainer)
               .clickable(onClickLabel = "View profile") { onViewProfile() }
               .semantics { contentDescription = "Profile. Tap to view your profile." },
           contentAlignment = Alignment.Center,
       )
       ```
       Files: `android/app/src/main/java/com/medihistory/ai/ui/dashboard/DashboardScreen.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 16. **Update the `DASHBOARD` composable in `MediHistoryNavHost` to pass new callbacks**  
       In the `DASHBOARD` composable block, add the two new lambdas to `DashboardScreen(...)`:
       ```kotlin
       onViewDoctors = { navController.navigate(Routes.DOCTOR_LIST) },
       onViewProfile = { navController.navigate(Routes.PATIENT_PROFILE) },
       ```
       Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`  
       Verify: `.\gradlew :app:compileDebugKotlin` — no compile errors.

- [ ] 17. **Full compile + unit test verification**  
       Run in order:
       ```
       cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android
       .\gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
       ```
       All compilation must succeed with zero errors. Then:
       ```
       .\gradlew :app:testDebugUnitTest
       ```
       All existing unit tests must pass. If any test breaks due to the new `PostReviewProcessor` constructor parameter (`DuplicateAlertRepository`), update the test to supply a fake/mock `DuplicateAlertRepository` — do not remove test coverage.  
       Files: any test that instantiates `PostReviewProcessor` directly (search `src/test/` for `PostReviewProcessor`)  
       Verify: `BUILD SUCCESSFUL` for both gradle invocations.

- [ ] 18. **Debug APK assembly**  
       ```
       cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android
       .\gradlew :app:assembleDebug
       ```
       Must produce `app/build/outputs/apk/debug/app-debug.apk` with `BUILD SUCCESSFUL`.  
       Verify: exit code 0, APK exists.

---

## Dependency order summary

Items must be done in order because:
- Items 1–2 are self-contained (camera gap); 2 depends on 1 to exist.
- Items 3–6 are self-contained (detail screen); 5 depends on 4's new state field; 6 depends on 3's new parameter.
- Items 7–8 are self-contained (duplicate detection); 8 just verifies wiring.
- Items 9–16 are the navigation wiring; 9 and 10 must come before 12 (route constants used in composable blocks); 11 must come before 12c; 14 and 15 must come before 16 (new params must exist before NavHost passes them).
- Items 17–18 are final verification gates — run only after all code changes are complete.

---

## Files changed — summary

| File | Gaps addressed |
|---|---|
| `ui/picker/ImportViewModel.kt` | Gap 1 — `onCameraImageConfirmed` |
| `ui/navigation/MediHistoryNavHost.kt` | Gap 1, 2, 4 (CAMERA result, IMPORT listener, PRESCRIPTION_DETAIL `onReviewOcr`, doctor/profile routes, dashboard callbacks) |
| `ui/prescription/PrescriptionDetailScreen.kt` | Gap 2 — button wiring, doctor name display |
| `ui/prescription/PrescriptionDetailViewModel.kt` | Gap 2 — `triggerOcrExtraction`, `doctorName`, WorkManager injection |
| `data/ocr/PostReviewProcessor.kt` | Gap 3 — `DuplicateDetector` + `DuplicateAlertRepository` |
| `ui/doctor/DoctorViewModel.kt` | Gap 4 — `loadDoctor` for edit route |
| `ui/navigation/Routes.kt` | Gaps 4, 5 — `DOCTOR_LIST`, `DOCTOR_EDIT_ROUTE`, `DOCTOR_ADD`, `PATIENT_PROFILE` constants |
| `ui/more/MoreScreen.kt` | Gaps 4, 5 — "My Doctors" and "My Profile" rows |
| `ui/dashboard/DashboardScreen.kt` | Gaps 4, 5 — `onViewDoctors`, `onViewProfile`, avatar tap target |
