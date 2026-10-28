# MediHistory AI — Figma ↔ Compose UI Match Review

**Date:** 2025-07-15  
**Figma file:** `xLPxdsIl7CBJOJTtliHKj5` (Page 1 — all 13 screens on one canvas)  
**Compose source tree:** `android/app/src/main/java/com/medihistory/ai/ui/`  
**Reviewer:** Kiro automated review agent  

---

## Executive Summary

Overall estimated match: **~42 %**

The colour token system, typography scale, and shared component library (`MediCard`, `StatusBadge`, `ConfidenceBadge`, `MedicineStatusChip`, `PrimaryButton`, `SecondaryButton`, `LoadingIndicator`, `EmptyStateView`) are correctly implemented and align well with the Figma design tokens. All 13 Figma screens have a corresponding Compose file.

The large gap comes almost entirely from **three structural issues**:

1. **SCR-01-DASHBOARD is a placeholder.** The NavHost routes `DASHBOARD` and `MEDICAL_HISTORY` to `PlaceholderScreen(name = "…")`. The dashboard is the most feature-rich screen in Figma (greeting, 4 stat cards, quick-ops row, "Needs Attention" section, recent-activity list, FAB, badge on Prescriptions tab) and has zero real implementation.

2. **Several detail/sub-screens are not wired into the NavHost.** `OcrReviewScreen`, `MedicalHistoryScreen`, `NewDoctorModeScreen`, `PdfPreviewScreen`, `ReminderScreen`, and `PrivacyScreen` all exist as composables but are not reachable from any NavHost route (their `Routes.*` constants exist but the `composable(…)` entries are absent). `PrescriptionDetailScreen` and `MedicineDetailScreen` also exist but their NavHost entries still show `PlaceholderScreen`.

3. **Figma's SCR-02-DOCTOR-GROUP has no Compose counterpart.** The Figma file has a full patient-grouped prescription list (prescriptions grouped under each doctor) that is a different layout from `PrescriptionListScreen`. No `DoctorGroupScreen` or similar composable exists.

No hardcoded `Color(0x…)` values appear anywhere outside `theme/Color.kt` and `theme/ExtendedColors.kt`. The OcrReviewScreen has an unused `import androidx.compose.ui.graphics.Color` (leftover) but does not actually reference `Color.*` directly.

---

## Per-Screen Match Table

| # | Figma screen | Compose file | Match | Key differences | Missing from code | Missing from Figma |
|---|---|---|---|---|---|---|
| 1 | SCR-01-DASHBOARD / Populated | `NavHost → PlaceholderScreen("Dashboard")` | **0 %** | Entire screen is a text-only placeholder | Greeting + date, 4 stat cards, quick-ops row (Add Rx / Duplicates / My History / New Doctor), "Needs Your Attention" section with 3 action rows, "Recent Activity" list with OCR-status chips, extended FAB "Add Prescription", badge on Prescriptions bottom-nav tab | — |
| 2 | SCR-02-RX-LIST / Populated | `PrescriptionListScreen.kt` | **75 %** | `MediumTopAppBar` ✓, FAB ✓, `PrescriptionCard` with `StatusBadge` ✓, Loading/Empty/Error ✓. Missing: search icon and sort icon in top-bar trailing actions; no doctor-specialty sub-line on card (but doctorSpecialty field exists in PrescriptionSummary) | Trailing search + sort icons in `MediumTopAppBar`; "Reviewed" / "OCR parsing" / "Needs review" status chips visible directly on the list cards (not just inside PrescriptionCard header) | — |
| 3 | SCR-02-RX-DETAIL / Populated | `PrescriptionDetailScreen.kt` | **55 %** | Top-bar ✓, back arrow ✓, loading/error ✓. Document thumbnail placeholder ✓. Missing: Figma shows doctor name + specialty + clinic section (Section 3), full medicines-prescribed list (Section 4), dosage-variation notice, safety/duplicate card (Section 4), source-traceability footer; code shows only doctorId raw string and basic date/OCR status | Doctor & clinic info card (name, specialty, clinic); medicines-prescribed list within the prescription; dosage-variation notice; duplicate/safety section; source-traceability footer | — |
| 4 | SCR-03-OCR-REVIEW / Populated | `OcrReviewScreen.kt` | **85 %** | Review list ✓, `ExtractedFieldCard` ✓, `ConfidenceBadge` ✓, amber low-confidence border ✓, state machine ✓, document placeholder ✓, "Review next" bottom bar ✓, pending banner ✓. Missing: Figma shows field names labelled with icons (MEDICINE, DOSE, DURATION, DOCTOR, DATE, FREQUENCY); the source-document view is a proper A4 sheet canvas in Figma vs. a simple rectangular placeholder in code; no pagination controls on document | Field-type icons beside field name labels; A4 document simulation (pagination controls); "5 fields remaining" counter on sticky bottom bar | — |
| 5 | SCR-04-MED-EXPLORER / Populated | `MedicineListScreen.kt` | **70 %** | `MediumTopAppBar` ✓, search + filter icons ✓, `DockedSearchBar` ✓, filter chips (All / Current / Past / Unknown / Duplicate alert) ✓, loading/error/empty ✓. Missing: sort affordance (count-and-sort row); Figma shows a "Sort" button/icon in the count row; `MedicineCard` does not show `MedicineStatusChip` inline (it's only in `MedicineDetailScreen`) | Inline `MedicineStatusChip` on `MedicineCard` in the list; count-and-sort row with sort icon | — |
| 6 | SCR-04-MED-DETAIL / Populated | `MedicineDetailScreen.kt` | **80 %** | Header card with icon + name + active ingredient + `MedicineStatusChip` ✓; details section (strength, formulation, dosage) ✓; source-prescription section ✓; disclaimer ✓; loading/error ✓. Missing: Figma shows an "All prescription instances" card (a prescription timeline/history for this medicine across multiple Rx); also shows a "Conditions and Allergies" section and a duplicate-alert chip if applicable | "All prescription instances" timeline card; conditions/allergies on record note; duplicate-alert chip on detail screen | — |
| 7 | SCR-05-DUPLICATE-REVIEW / Populated | `DuplicateReviewScreen.kt` + `DuplicateAlertCard.kt` | **80 %** | `TopAppBar` ✓, back arrow ✓, warning banner ✓, `DuplicateAlertCard` ✓, expand/collapse ✓, match-type chip ✓, disclaimer banner ✓, "Mark as Reviewed" with dialog ✓, loading/empty/error ✓. Missing: Figma's populated state shows a Summary Banner with total alert count + "N reviewed / M unreviewed"; show/hide reviewed toggle is code-only (not in Figma) | Summary banner with reviewed vs. unreviewed count; full-width "N Potential duplicates" hero header | Show/hide reviewed toggle (code has it, Figma does not) |
| 8 | SCR-06-HISTORY / Populated | `NavHost → PlaceholderScreen("Medical History")` | **0 %** | Entire screen is a placeholder | Disclaimer card ✓ (code has it but unreachable); history sections (Conditions, Allergies, Active Medicines, Recent Prescriptions, Participating Prescribers); timeline with year dividers; share FAB; generating-progress state | — |
| 9 | SCR-07-NEW-DOCTOR / Populated | `NewDoctorModeScreen.kt` | **75 %** | `TopAppBar` ✓, back arrow ✓, explanation card ✓, selectable record cards ✓, select all/deselect all ✓, consent dialog ✓, sticky "Prepare summary" button ✓, loading/empty/error ✓. Missing: Figma shows a "Included Summary Details" preview card showing what's included (medicines list, doctor list, date range); also has a search/filter bar at top | "Included summary details" preview card (dynamic content list); search/date filter on records | — |
| 10 | SCR-07-PDF-PREVIEW / Populated | `PdfPreviewScreen.kt` | **70 %** | `TopAppBar` "PDF Ready" ✓, back/cancel ✓, Share button ✓, disclaimer ✓, filename display ✓, PDF placeholder card ✓. Missing: Figma shows a scrollable A4 document canvas (a rendered simulation of the PDF content), a watermark micro-header, and pagination controls | A4 document canvas simulation with content preview; pagination controls; watermark | — |
| 11 | SCR-08-REMINDERS / Populated | `ReminderScreen.kt` | **65 %** | `TopAppBar` ✓, loading/empty/error ✓, `ReminderCard` with medicine name + scheduled time + Taken/Skipped buttons ✓. Missing: Figma has 3 distinct sections — "Today's Medicines" (dose cards stack with Taken/Missed states), "Reminders Configuration List", "Adherence Overview (non-clinical)". Code has a single flat list. No adherence section. No "Past due reminders" collapsible section. No FAB to add a reminder. | 3-section layout (Today's medicines / Config list / Adherence overview); past-due collapsible section; FAB "+ Set Up Reminders"; per-dose Taken/Missed visual states (logged vs unlogged) | — |
| 12 | SCR-09-SAFETY-CENTER / Populated | `SafetyCenterScreen.kt` | **75 %** | `TopAppBar` ✓, back arrow ✓, duplicate-alerts section ✓, interaction section (NotConfigured/Checking/Results/Error) ✓, "Review duplicates" button ✓. Missing: Figma shows an "ACTIVE REFERENCE SOURCE STATUS BAR" at the top (amber banner with source name + last-updated date); also has a "PRE-CHECK INFORMATIONAL BANNER" (teal) and "RESULT CARD: INTERACTIONS FOUND" with severity chips | Active-reference-source status bar; pre-check informational teal banner; interaction-result cards with severity chips and review-warning text (code has `InteractionResultCard` but it uses `MaterialTheme.colorScheme.error` for severity — Figma uses severity-specific chips) | — |
| 13 | SCR-AUX-SETTINGS/PRIVACY (named "Privacy & Security" in Figma nav) | `PrivacyScreen.kt` | **60 %** | `TopAppBar` "Privacy and Data" ✓, export card ✓, cloud backup card ✓, delete card ✓, two-step deletion dialog ✓, loading spinner during deletion ✓. Missing: Figma shows a tabbed layout ("Privacy & Security Tab") with additional sections for consent management, cloud-backup toggle with visible backup status (last synced, failures), and an account-deletion section with cloud retention explanation | Consent management section; cloud-backup status with last-synced date and failure indicator; tabbed navigation inside the screen | — |

> **SCR-02-DOCTOR-GROUP** (prescriptions grouped under each doctor) exists in Figma with full Populated/Loading/Empty/Error states. **No corresponding Compose screen exists.** This is an entirely missing screen.

---

## Component Usage Table

| Component | Used in screens | Not used (should be) |
|---|---|---|
| `MediCard` | `PrescriptionCard` (SCR-02-RX-LIST) | Should be used in `DuplicateAlertCard` (uses `ElevatedCard` directly), `MedicineDetailScreen` section cards (uses `ElevatedCard`), `ReminderCard` (uses `Card`) — all functionally equivalent but inconsistent |
| `StatusBadge` | `PrescriptionCard`, `PrescriptionDetailScreen` | Not on Dashboard (not yet built) |
| `ConfidenceBadge` | `ExtractedFieldCard` (SCR-03-OCR-REVIEW) | Correct — only needed on OCR review |
| `MedicineStatusChip` | `MedicineDetailScreen` | Missing from `MedicineCard` in SCR-04-MED-EXPLORER list items — Figma shows status chip inline on each list card |
| `PrimaryButton` | `EmptyStateView` | Not used directly in any screen body; screens use `Button` directly. Should replace direct `Button` calls in `NewDoctorModeScreen`, `PdfPreviewScreen`, `PrivacyScreen` for consistency |
| `SecondaryButton` | Not used anywhere (0 screens) | Should replace `OutlinedButton` in `ReminderCard` (Taken/Skipped), `PdfPreviewScreen` (Cancel), `SafetyCenterScreen`, `PrivacyScreen` |
| `LoadingIndicator` | `PrescriptionListScreen`, `PrescriptionDetailScreen`, `MedicineListScreen`, `MedicineDetailScreen`, `DuplicateReviewScreen`, `MedicalHistoryScreen`, `NewDoctorModeScreen`, `ReminderScreen`, `SafetyCenterScreen` | Correct — most screens use it. `OcrReviewScreen` uses `CircularProgressIndicator` directly instead of `LoadingIndicator` |
| `EmptyStateView` | `PrescriptionListScreen`, `MedicineListScreen`, `DuplicateReviewScreen`, `MedicalHistoryScreen`, `NewDoctorModeScreen`, `ReminderScreen`, `SafetyCenterScreen` | Correct usage |

---

## Color Token Compliance

All UI color references in screen files use `MaterialTheme.colorScheme.*` or `MaterialTheme.extendedColors.*`. **No hardcoded `Color(0x…)` values were found outside theme files.**

| Finding | Detail |
|---|---|
| ✅ Primary teal `#006874` | Defined as `MhPrimary` in `Color.kt`; used via `MaterialTheme.colorScheme.primary` everywhere. |
| ✅ Surface `#F5FAFB` | Defined as `MhSurface` / `MhBackground` in `Color.kt`; applied in `Theme.kt`. |
| ✅ Warning amber `#7D5700` | Defined as `MhWarning` in `Color.kt`; accessible via `MaterialTheme.extendedColors.warning`. |
| ✅ Warning container `#FFDEA8` | Defined as `MhWarningContainer`; used in `StatusBanner`, `DuplicateAlertCard`, `ExtractedFieldCard`, `StatusBadge`. |
| ⚠️ Unused `import Color` in `OcrReviewScreen.kt` | Line 43 imports `androidx.compose.ui.graphics.Color` but no `Color.*` reference follows. Dead import — no runtime impact, but lint will flag it. |
| ⚠️ `SafetyCenterScreen` severity color | Interaction result severity text uses `MaterialTheme.colorScheme.error` (red) for all severities. Figma shows severity-level chips with distinct colors per severity tier. |

---

## Medical Safety Text Compliance

| Screen | Disclaimer required? | Present in code? | Notes |
|---|---|---|---|
| `MedicalHistoryScreen` (SCR-06) | **Yes** | ✅ Yes | `DisclaimerCard` is non-dismissible and always shown before content. |
| `PdfPreviewScreen` (SCR-07-PDF-PREVIEW) | **Yes** | ✅ Yes | "Patient-compiled record. Not a clinical report." shown in body. |
| `DuplicateReviewScreen` (SCR-05) | **Yes** | ✅ Yes | `DuplicateAlertCard` expanded view shows required amber banner. "Mark as Reviewed" dialog shows required disclaimer. |
| `MedicineDetailScreen` (SCR-04-MED-DETAIL) | Yes (extraction disclaimer) | ✅ Yes | `DISCLAIMER_TEXT` constant; always visible; in semantic tree. |
| `SafetyCenterScreen` (SCR-09) | Yes (no-safety-word rule) | ✅ Yes | "safe" does not appear in any string; `NotConfiguredCard` explicitly says "does not mean your medicines are approved for combined use." |
| `ReminderScreen` (SCR-08) | Yes (dose recording ≠ prescription) | ✅ Yes | "Prescribed:" label visually separated from dose buttons. |
| `DashboardScreen` (SCR-01) | Yes (AI-generated sections) | ❌ No | Entire screen is a placeholder — disclaimer not present. |

---

## Bottom Navigation

Figma shows 5 tabs: **Dashboard · Prescriptions · Medicines · History · More** (with a badge dot on Prescriptions and "More" opens a drawer or sub-screen with additional features).

Code (`NavDestination.kt`) defines 5 tabs: **Dashboard · Prescriptions · Medicine Explorer · Duplicate Alerts · Medical History**.

| Issue | Detail |
|---|---|
| ⚠️ Tab 4 label mismatch | Figma tab 4 label: **"History"** (routes to medical history). Code tab 4: **"Duplicate Alerts"** (routes to `DuplicateReviewScreen`). Medical history is code tab 5. |
| ⚠️ Tab 5 label mismatch | Figma tab 5: **"More"** (a disclosure tab that opens Safety Center, New Doctor Mode, Reminders, Privacy, Settings). Code tab 5: **"Medical History"**. |
| ⚠️ "More" tab missing | The Figma "More" tab is a standard Android disclosure hub for secondary destinations (Safety Center, New Doctor Mode, Reminders, Privacy). Code has no "More" tab — these screens are reachable only via direct NavHost navigation without a UI entry point. |
| ✅ Badge on Prescriptions tab | Figma shows a badge dot on the Prescriptions tab when there are unreviewed prescriptions. `NavDestination.kt` has `labelRes = R.string.nav_prescriptions` but no badge rendering. `MediHistoryBottomBar` does not render badges on any tab. |
| ✅ Filled/outlined icon pair | Code correctly uses `selectedIcon` (filled) and `unselectedIcon` (outlined) per Figma pattern. |

---

## Priority Fixes

### P1 — Blocks release

| ID | Issue | File(s) |
|---|---|---|
| P1-01 | **SCR-01-DASHBOARD not implemented.** Routes to `PlaceholderScreen`. The dashboard is the first screen users see and the primary navigation hub. | `MediHistoryNavHost.kt` |
| P1-02 | **SCR-06-HISTORY not wired.** Routes to `PlaceholderScreen`. `MedicalHistoryScreen.kt` exists but is unreachable. | `MediHistoryNavHost.kt` |
| P1-03 | **SCR-02-RX-DETAIL not wired.** NavHost entry shows `PlaceholderScreen` instead of calling `PrescriptionDetailScreen`. The composable exists but is never rendered. | `MediHistoryNavHost.kt` |
| P1-04 | **SCR-04-MED-DETAIL not wired.** NavHost entry shows `PlaceholderScreen` instead of calling `MedicineDetailScreen`. | `MediHistoryNavHost.kt` |
| P1-05 | **OCR Review route not in NavHost.** `Routes.OCR_REVIEW` is defined but no `composable(Routes.OCR_REVIEW)` entry exists. Patients cannot reach the review flow after upload. | `MediHistoryNavHost.kt` |
| P1-06 | **SCR-02-DOCTOR-GROUP has no Compose screen.** Figma shows a complete doctor-grouped prescriptions screen. No `DoctorGroupScreen.kt` exists. | Missing file |
| P1-07 | **Tab 4/5 labels and routes mismatch Figma.** Figma uses "History" and "More" as tab 4/5. Code uses "Duplicate Alerts" and "Medical History". The "More" hub screen is missing entirely, leaving Safety Center, New Doctor Mode, Reminders, and Privacy unreachable from the main nav. | `NavDestination.kt`, `MediHistoryNavHost.kt` |
| P1-08 | **Medical history disclaimer absent (screen unreachable / placeholder).** When SCR-01-DASHBOARD is built it must include appropriate AI-generated content disclaimers. | Future `DashboardScreen.kt` |

### P2 — Should fix before launch

| ID | Issue | File(s) |
|---|---|---|
| P2-01 | **`SecondaryButton` component is unused.** All screens use `OutlinedButton` directly, bypassing the 48 dp touch-target enforcement in `SecondaryButton`. Replace in `ReminderCard`, `PdfPreviewScreen`, `SafetyCenterScreen`. | Multiple screen files |
| P2-02 | **`MedicineStatusChip` missing from `MedicineCard`.** Figma shows status chip inline on each medicine list item. `MedicineDetailScreen` has it but `MedicineCard` (the list item) does not. | `medicine/MedicineCard.kt` |
| P2-03 | **`PrimaryButton` not used directly in screens.** Screens call `Button(…)` directly (bypassing the 48 dp default min-height). `EmptyStateView` uses it correctly; all other call sites should be updated. | `NewDoctorModeScreen.kt`, `PdfPreviewScreen.kt`, `PrivacyScreen.kt`, others |
| P2-04 | **Bottom-nav badge missing.** Figma shows a badge dot on the Prescriptions tab when unreviewed prescriptions exist. No badge rendering in `MediHistoryBottomBar`. | `MediHistoryBottomBar.kt` |
| P2-05 | **Search and sort icons missing from `PrescriptionListScreen` top bar.** Figma shows search and sort trailing actions on `SCR-02-RX-LIST`. | `PrescriptionListScreen.kt` |
| P2-06 | **`ReminderScreen` flat list vs. Figma 3-section layout.** The reminder screen needs a "Today's Medicines" section, a configuration section, and a non-clinical adherence section. Current single flat list diverges significantly from Figma. | `ReminderScreen.kt` |
| P2-07 | **`OcrReviewScreen` uses raw `CircularProgressIndicator` instead of `LoadingIndicator`.** Inconsistent with every other screen. | `OcrReviewScreen.kt` |
| P2-08 | **Dead `import androidx.compose.ui.graphics.Color` in `OcrReviewScreen`.** Unused import — lint warning. | `OcrReviewScreen.kt` |
| P2-09 | **Interaction result cards in `SafetyCenterScreen` use error color for all severities.** Should use severity-specific chips. | `SafetyCenterScreen.kt` |
| P2-10 | **`PrivacyScreen` missing consent management and cloud-backup status sections.** Figma shows consent history and live backup status with last-synced date. | `PrivacyScreen.kt` |

### P3 — Nice to have

| ID | Issue | File(s) |
|---|---|---|
| P3-01 | **`PrescriptionDetailScreen` missing doctor/clinic card, medicines list, and source-traceability footer.** Core detail is present but much of the Figma content is absent. | `PrescriptionDetailScreen.kt` |
| P3-02 | **`MedicineDetailScreen` missing "all prescription instances" timeline card.** Useful for patients on long-term medication. | `MedicineDetailScreen.kt` |
| P3-03 | **`NewDoctorModeScreen` missing "Included summary details" preview card.** Figma shows a dynamic preview of what will be in the PDF before generating. | `NewDoctorModeScreen.kt` |
| P3-04 | **`PdfPreviewScreen` shows file-name only instead of A4 document canvas preview.** Figma shows a simulation of the rendered PDF with pagination. Not strictly blocking but reduces trust for the sharing action. | `PdfPreviewScreen.kt` |
| P3-05 | **`OcrReviewScreen` field-type icons absent.** Figma shows unique icons for MEDICINE, DOSE, DURATION, DOCTOR, DATE, FREQUENCY fields. | `ExtractedFieldCard.kt` |
| P3-06 | **`SafetyCenterScreen` missing active-reference-source status bar and pre-check informational banner.** These orient the patient before they see results. | `SafetyCenterScreen.kt` |
| P3-07 | **`MediCard` not consistently used.** `DuplicateAlertCard` and `ReminderCard` use `ElevatedCard`/`Card` directly rather than the `MediCard` wrapper, creating inconsistent elevation defaults (1 dp vs. Material default). | `DuplicateAlertCard.kt`, `ReminderCard` in `ReminderScreen.kt` |
| P3-08 | **`DuplicateReviewScreen` show/hide reviewed toggle has no Figma counterpart.** Minor divergence — code added a useful feature not shown in Figma. Document or add to Figma. | `DuplicateReviewScreen.kt` |

---

## Screens That Need the Most Work

1. **SCR-01-DASHBOARD** — 0 % implemented. Highest priority screen. Needs: greeting/date header, 4 stat cards (prescriptions, doctors, medicines, pending alerts with amber glow on non-zero count), quick-ops row (Add Rx, Duplicates, My History, New Doctor), "Needs Your Attention" section with actionable rows, "Recent Activity" list with `PrescriptionCard`-style items, extended FAB, badge on Prescriptions nav tab.

2. **SCR-06-HISTORY (MedicalHistoryScreen)** — 0 % reachable (PlaceholderScreen in NavHost). `MedicalHistoryScreen.kt` is well-written but unreachable. Wire it in and add: sections for Conditions / Allergies / Active Medicines / Recent Prescriptions / Participating Prescribers, year-divider timeline, share FAB, AI-disclaimer banner (already coded), generating progress state (already coded).

3. **Bottom-nav restructure** — The tab order and labels must be aligned with Figma: Dashboard / Prescriptions / Medicines / History / More. The "More" disclosure hub is the entry point for Safety Center, New Doctor Mode, Reminders, and Privacy — all currently unreachable from any nav UI.

4. **SCR-08-REMINDERS** — 65 % match. The 3-section layout (Today's doses / Config list / Adherence overview) is entirely different from the current flat list. The "Past Due" collapsible section and FAB are absent.

5. **SCR-02-RX-DETAIL / SCR-02-DOCTOR-GROUP** — Detail screen is wired to a placeholder; doctor-group screen is entirely missing. Both are P1.

---

*Report generated by read-only investigation. No code was modified.*
