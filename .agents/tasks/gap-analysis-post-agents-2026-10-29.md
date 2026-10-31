# Gap Analysis — Post AI Agent Features (F11–F16)
**Date:** 2026-10-29  
**Scope:** Android app + FastAPI backend, post AI agent implementation  
**Investigator:** Kiro automated investigation step

---

## Executive Summary

The six AI agent features (F11–F16) are substantially complete and correctly wired. The agents themselves are all implemented, tested, and connected to the relevant ViewModels and screens. The backend is **fully implemented** (not a skeleton). The main remaining gaps are:

1. **Prescription search and sort** are still stubbed in `PrescriptionListScreen` — the search/sort icon buttons have `/* TODO */` lambdas. The `PrescriptionListViewModel` does NOT contain any search/filter/sort logic.
2. **Consent management screen** does not exist — `PrivacyScreen` has a `/* TODO: navigate to consent management */` callback with no target screen or route.
3. **Time picker in ReminderAdvisorDialog (F16)** — times are shown but cannot be changed; the dialog explicitly says "Tap to change time (coming soon)".
4. **Dashboard recent activity `medicineCount = 0` is hardcoded** — `DashboardViewModel` emits `medicineCount = 0` for recent prescription summaries. `PrescriptionListViewModel` correctly computes this, but the Dashboard does not.
5. **`DoctorGroupScreen` also hardcodes `medicineCount = 0`** — `DoctorGroupViewModel` explicitly notes "resolved separately in a future ticket."
6. **`PrescriptionDetailScreen` has no medicine list section** — the detail screen shows doctor, date, OCR status, and the F11 "Understand" sheet, but does not display the list of medicines on that prescription.
7. **OCR review navigation goes to prescription list, not back to prescription detail** — after review, `onAllReviewed` navigates to `Routes.PRESCRIPTIONS` (the list), losing the user's context of which prescription they were on.
8. **Medicine list filter button is not wired** — the FilterList icon button in `MedicineListScreen` has `/* Filter bottom sheet — future ticket */`. The "Duplicate alert" filter chip is also not wired.
9. **`NewDoctorModeScreen` missing conditions, allergies, and active medicines sections** — an in-code comment explicitly confirms this (F09/F10 not yet implemented).
10. **Backend export (PDF) and interactions router are intentional stubs** — both return 501, acknowledged by design.

All AI agent files (GeminiService, PrepareForVisitAgent, PrescriptionUnderstandingAgent, MedicineHistoryNarratorAgent, DuplicateExplainerEngine, OcrCorrectionEngine, ReminderAdvisorEngine) are clean — no outstanding TODOs or unwired stubs introduced by those features.

---

## Detailed Findings

### 1. Remaining Dead UI Callbacks

**Impact: Medium**

The exhaustive search of all main `.kt` sources for `/* TODO */`, `/* ... */`, and `/* ... future ... */` patterns found the following live stubs:

| File | Line | Pattern | Description |
|---|---|---|---|
| `PrescriptionListScreen.kt` | 57 | `onClick = { /* TODO: open search */ }` | Search icon — no logic |
| `PrescriptionListScreen.kt` | 63 | `onClick = { /* TODO: open sort */ }` | Sort icon — no logic |
| `PrivacyScreen.kt` | 152 | `onManageConsent = { /* TODO: navigate to consent management */ }` | Consent button — no target |
| `ReminderScreen.kt` | 97 | `onClick = { /* Add reminder — future ticket */ }` | TopAppBar Add icon (duplicate of FAB — low priority) |
| `MedicineListScreen.kt` | 99 | `onClick = { /* Filter bottom sheet — future ticket */ }` | Filter icon in TopAppBar |
| `MedicineListScreen.kt` | 172 | `onClick = { /* Duplicate alert filter — future ticket */ }` | "Duplicate alert" chip |
| `OcrReviewScreen.kt` | 189 | `snackbarHostState.showSnackbar("Document viewer coming soon")` | Document preview is a snackbar stub |

The `ReminderScreen.kt` Add icon in the top bar (line 97) is a duplicate of the "Set Up Reminder" FAB; calling it a dead callback is somewhat harsh — the FAB already covers this.

---

### 2. Prescription Search and Sort

**Impact: Medium**

`PrescriptionListScreen.kt` (lines 57–65) has two `IconButton` widgets — Search and Sort — both with empty `/* TODO */` onClick lambdas.

`PrescriptionListViewModel.kt` does **not** contain any search query state, filter logic, or sort order. The data it emits is the full unfiltered list from Room grouped by doctor name.

What it would take to wire this:
- Add `searchQuery: String` and `sortOrder: SortOrder` to `PrescriptionListUiState`.
- Add `onSearchQueryChange(query: String)` and `onSortChange(order: SortOrder)` to the ViewModel; re-filter the combined flow.
- In the screen, open a `DockedSearchBar` (same pattern as `MedicineListScreen`) for search and a `ModalBottomSheet` for sort options.
- This is a well-defined, medium-effort task. The `MedicineListViewModel.onSearchQueryChange` and `MedicineListScreen`'s DockedSearchBar are ready-made patterns to copy.

---

### 3. Consent Management

**Impact: Medium**

`PrivacyScreen.kt` line 152:
```kotlin
ConsentCard(onManageConsent = { /* TODO: navigate to consent management */ })
```

There is **no `ConsentScreen`, no `ConsentRoute`, and no `ConsentViewModel`** anywhere in the codebase. The `Routes.kt` file has no consent-related route. The `MediHistoryNavHost` has no composable entry for consent management.

The `ConsentDialog` in `ui/newdoctor/ConsentDialog.kt` is the *sharing* consent for New Doctor Mode, not a consent management screen for general data usage.

Fix requires: designing what "manage consent" means for the app (likely a screen listing which features/agents the patient has consented to send data to), then building a ConsentScreen + route + ViewModel.

---

### 4. Reminder Time Editing

**Impact: Low**

`ReminderScreen.kt` lines 289 and 335:
```kotlin
// For MVP: times are displayed read-only. Full time-picker is future work.
text = "Tap to change time (coming soon)"
```

The `ReminderAdvisorDialog` (F16) shows suggested times in read-only `Card` composables. There is no `TimePickerDialog` anywhere in the codebase; Material 3's `TimePicker` composable is not imported or used anywhere.

Each time card currently shows the time and the label "Tap to change time (coming soon)". The card does not register an `onClick`, so tapping it does nothing.

Fix: Replace the time `Card` with a `TextButton` that opens Material 3's `TimePickerDialog` in a per-time dialog, updating the `times` list in `rememberSaveable`.

---

### 5. Medicine Count in Dashboard Recent Activity

**Impact: Medium**

`DashboardViewModel.kt` (line ~68):
```kotlin
medicineCount = 0,
```

The `DashboardViewModel.load()` method maps the last 3 prescriptions to `PrescriptionSummary` with `medicineCount = 0`. It observes `medicineRepository.observeByPatient(patientId)` in the same `combine` call but does **not** compute `medicineCountByRx` from it — that count lands in `medicineCount` (total across all medicines in the stats grid tile) but not in the per-prescription `medicineCount` shown on `PrescriptionCard`.

Contrast: `PrescriptionListViewModel` correctly computes:
```kotlin
val medicineCountByRx = medicines.groupingBy { it.prescriptionId }.eachCount()
// ...
medicineCount = medicineCountByRx[rx.id] ?: 0,
```

`DoctorGroupViewModel.kt` (line ~113) has the same issue with an explicit comment:
```kotlin
medicineCount = 0, // resolved separately in a future ticket
```

Fix for Dashboard: mirror the `medicineCountByRx` calculation from `PrescriptionListViewModel` into the Dashboard's mapping lambda — it already has the `medicines` list in scope.

Fix for DoctorGroupViewModel: inject `MedicineRepository`, add it to the `combine`, and compute the count.

---

### 6. Doctor Specialty Display

**Impact: Low (working, one cosmetic issue)**

The specialty selection in `DoctorEditScreen` uses a full `ExposedDropdownMenu` with all `DoctorSpecialty.entries` listed. An `AnimatedVisibility` free-text field appears when `OTHER` is selected.

`DoctorListScreen` correctly shows the specialty using `Doctor.displaySpecialty()` — it unwraps `specialtyOther` when specialty is `OTHER`.

The only issue: `DoctorGroupScreen`'s `DoctorSummaryCard` (line ~148) computes `specialtyLabel` as:
```kotlin
doctor.specialty.name.lowercase().replaceFirstChar { it.uppercase() }
```
It ignores the `specialtyOther` field — if a doctor has specialty `OTHER` with `specialtyOther = "Neurologist"`, the card shows "Other" rather than "Neurologist".

Fix: use the same `displaySpecialty()` extension used by `DoctorListScreen`.

---

### 7. Navigation Back from OCR Review to Prescription Detail

**Impact: Medium**

`MediHistoryNavHost.kt` line ~115:
```kotlin
onAllReviewed = {
    // All fields confirmed — go to the prescriptions list so the
    // patient can see the updated card (doctor, date, medicines).
    navController.navigate(Routes.PRESCRIPTIONS) {
        popUpTo(Routes.PRESCRIPTIONS) { inclusive = true }
    }
},
```

After reviewing all OCR fields, the app navigates to the **prescription list** (`Routes.PRESCRIPTIONS`), not back to the specific prescription detail. The comment acknowledges this as a deliberate choice but it discards the user's navigation context.

The patient arrives at the list from which they can navigate into the detail — but they lose the prescription-specific context (e.g., they may not know which one they were reviewing). The Figma spec (SCR-03-OCR-REVIEW) should specify where post-review navigation goes.

Recommended fix: navigate to `Routes.prescriptionDetail(id)` instead, with `popUpTo(Routes.OCR_REVIEW)` to remove the OCR review from the back stack. This preserves context.

---

### 8. PrescriptionDetailScreen — No Medicine List Section

**Impact: High**

`PrescriptionDetailScreen.kt` displays:
- Section 1: Doctor/hospital header card with date and OCR status
- Section 2: OCR review prompt (when `NEEDS_REVIEW`)
- Section 3: Document preview placeholder
- Doctor name, prescription date, notes (plain text)
- "Extract text" button (when pending/error)
- "✨ Understand this prescription" button (when reviewed, F11)

It does **not** render a list of medicines on that prescription. After OCR is reviewed, the patient has no way to see which medicines were extracted directly on the detail screen — they must navigate to the Medicine Explorer and filter by source.

`PrescriptionDetailViewModel` injects `MedicineRepository` and uses it for F11 (`generateUnderstanding`), so the medicines are already available in the ViewModel.

Fix: add a "Medicines" section below the OCR status, pulling from `medicineRepository.observeByPatient(patientId).filter { it.prescriptionId == prescriptionId }`. When OCR is PENDING or not yet reviewed, show "Review OCR fields to see extracted medicines."

---

### 9. Backend Status

**Impact: None (fully implemented)**

The backend at `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\backend` is **not a skeleton**. It has:
- `app/main.py` — FastAPI entry point with all 10 routers mounted at `/api/v1`
- Rate limiting middleware (`RateLimitMiddleware`)
- JWT auth (`get_current_patient` dependency on every protected route)
- Full routers: `auth`, `patients`, `doctors`, `prescriptions`, `medicines`, `duplicates`, `history`, `reminders`, plus `export` and `interactions` as intentional 501 stubs
- Full service layer: `auth_service`, `doctor_service`, `duplicate_service`, `history_service`, `medicine_service`, `patient_service`, `prescription_service`, `reminder_service`
- Full Pydantic schemas for every domain
- SQLAlchemy models for all entities

The two stub routers are explicitly and intentionally stubbed:
- `export.py` — PDF export returns 501; noted as "not available in MVP"
- `interactions.py` — Drug interaction check returns a safe "unavailable" response; full integration with a reference provider is deferred

These are known design decisions, not gaps.

---

### 10. New TODOs / Stubs in AI Agent Files

**Impact: None (all agents are clean)**

A grep of all AI agent files for `TODO`, `stub`, `future`, `not implemented`, `coming soon` found only:
- `MedicineHistoryNarratorAgent.kt` — uses "stub" only as a **method name** (`stubSection()`, `stubEntry()`) for the graceful Gemini-unavailable fallback. This is correct and intentional, not a gap.

All six agent files are fully implemented with no open stubs:

| File | Status |
|---|---|
| `GeminiService.kt` | Complete — API key from BuildConfig, null-safe, no logging |
| `PrescriptionUnderstandingAgent.kt` (F11) | Complete — wired to `PrescriptionDetailViewModel` |
| `PrepareForVisitAgent.kt` (F12) | Complete — wired to `DashboardViewModel.prepareForVisit()` |
| `DuplicateExplainerEngine.kt` (F13) | Complete — called in `PostReviewProcessor` on alert creation, stored in `DuplicateAlert.explanation`, rendered in `DuplicateAlertCard` |
| `OcrCorrectionEngine.kt` (F14) | Complete — called in `OcrReviewViewModel.load()`, stored in `OcrReviewUiState.suggestedValues`, rendered as `SuggestionChip` in `ExtractedFieldCard` |
| `MedicineHistoryNarratorAgent.kt` (F15) | Complete — called in `MedicalHistoryViewModel.generateHistory()`, replaces stub AI_SUMMARY section |
| `ReminderAdvisorEngine.kt` (F16) | Complete — called in `ReminderViewModel.onMedicineSelected()`, result shown in `ReminderAdvisorDialog` |

The only F16 gap is the time-picker UI (see item 4 above) — the engine itself is complete.

---

## Additional Gaps Found

### 11. MedicineListScreen Filter Button Not Wired
**Impact: Low**

`MedicineListScreen.kt` line 99:
```kotlin
onClick = { /* Filter bottom sheet — future ticket */ }
```
The search bar (`DockedSearchBar`) is correctly wired to `viewModel::onSearchQueryChange`. The Filter icon button and "Duplicate alert" chip (line 172) are stubbed. A filter bottom sheet showing advanced options (e.g., filter by prescription, by duplicate status) was likely intended but not built.

### 12. OcrReviewScreen Document Viewer Is a Snackbar
**Impact: Low**

`OcrReviewScreen.kt` line 189 shows a snackbar "Document viewer coming soon" instead of opening the actual document. This means patients cannot view the original document during OCR review, which is important for verifying extracted values.

### 13. NewDoctorModeScreen Missing Content Sections
**Impact: Medium**

`NewDoctorModeScreen.kt` lines 226–232 include a comment:
```
// Stub sections for Conditions, Allergies, and Active Medicines are
// intentionally not shown here — these features (F09/F10) are not yet implemented.
```
The PDF generated by `PdfGenerator.kt` will therefore lack these sections until F09/F10 are built.

---

## Prioritised Top-10 Gaps

| Priority | Gap | Impact | Effort |
|---|---|---|---|
| **1** | **PrescriptionDetailScreen: No medicine list** | High — patients can't see their prescription's medicines inline | Medium |
| **2** | **Dashboard recent activity `medicineCount = 0`** | Medium — all prescription cards on dashboard show "0 medicines" | Low (mirrors PrescriptionListViewModel logic) |
| **3** | **PrescriptionListScreen: Search and sort not wired** | Medium — core navigation feature advertised with icon buttons but dead | Medium |
| **4** | **OCR review navigation goes to list, not detail** | Medium — breaks user context after review | Low (1 line change) |
| **5** | **DoctorGroupScreen `medicineCount = 0`** | Medium — prescription timeline shows "0 medicines" | Low (inject MedicineRepository) |
| **6** | **Consent management screen missing** | Medium — Privacy screen has a live button with no destination | High (new screen required) |
| **7** | **MedicineListScreen Filter button dead** | Low — chip row has filter chip with no action | Medium |
| **8** | **ReminderAdvisorDialog: time picker not implemented** | Low — times are visible but not editable | Low (Material 3 TimePicker) |
| **9** | **OcrReviewScreen document viewer is a snackbar** | Low — patients can't view original during review | Medium (open FileProvider URI) |
| **10** | **DoctorGroupScreen ignores `specialtyOther`** | Low — cosmetic, shows "Other" instead of actual specialty | Trivial (1 line fix) |

---

## Notes for Implementers

- **Priority 4 (navigation fix)** is a 1-line change in `MediHistoryNavHost.kt`: replace `Routes.PRESCRIPTIONS` with `Routes.prescriptionDetail(id)` in the OCR review `onAllReviewed` callback.
- **Priority 2 (Dashboard medicine count)** is a 3-line change in `DashboardViewModel`: compute `medicineCountByRx` from the already-observed `medicines` flow before building `recentSummaries`.
- **Priority 10 (DoctorGroupScreen specialty)** is a 1-line change: replace the `specialty.name...` expression with a call to `doctor.displaySpecialty()` (the same extension already used in `DoctorListScreen`).
- All AI agent features (F11–F16) are production-ready pending the Gemini API key being set in `local.properties`. No implementation gaps exist in the agent code itself.
- The backend is production-ready for MVP features. The export and interactions stubs are intentional scope decisions.
