# MediHistory AI — Figma vs Compose Implementation Comparison v2

**Date:** 2025-07-15  
**Reviewer:** Automated screen-by-screen analysis (read-only)  
**Baseline:** ~42% match (pre-PR #75)  
**Previous PR #75 additions:** DashboardScreen, MoreScreen, DoctorGroupScreen, full NavHost wiring, bottom-nav restructure, P2 fixes

---

## Overall Summary

**Overall match: ~67%** (weighted by screen importance)

PR #75 delivered real structural improvements — the NavHost is fully wired, the bottom nav has the correct 5-tab structure, and three previously missing screens (Dashboard, More, DoctorGroup) now exist. However, deep content comparison reveals that several screens have critical feature gaps: the Prescription List is a completely different layout paradigm from Figma (flat list vs. doctor-grouped collapsible), the RX Detail screen is drastically simplified versus the rich Figma design, Medicine Detail is missing the duplicate-alert banner and multi-prescription source section, and the Safety Center has a single-panel design versus Figma's dual-tab (Interaction Check + Privacy & Security) design.

---

## Screen-by-Screen Analysis

---

## SCR-01 — Dashboard
**Match: 58%**
**Figma screenshot:** `android/Figma/SCR-01-DASHBOARD/Populated.png`

✅ Matches:
- TopAppBar with "Dashboard" title ✓
- Notification bell icon (trailing) ✓
- Account/profile icon (trailing) ✓
- 2×2 stats grid (Prescriptions, Doctors, Medicines, Pending alerts) ✓
- `warningContainer` amber color on Pending alerts card when count > 0 ✓
- Quick operations row with 4 chips: Add Rx, Duplicates, My History, New Doctor ✓
- "Needs your attention" section (conditional) ✓
- Recent activity section with "See all" link ✓
- "Add Prescription" Extended FAB ✓
- Bottom nav: Dashboard/Prescriptions/Medicines/History/More ✓
- Loading ✓, Error ✓ states implemented
- `PrescriptionCard` reused for recent activity items ✓

⚠️ Minor differences:
- Figma shows the app icon (pharmacy cross in teal circle) left of "Dashboard" in the TopAppBar; code uses plain `Text` title with no leading icon
- Figma shows a greeting header: date, "Good morning, [Name]", waving emoji, avatar "PS" circle — code has none of this (no greeting, no patient name, no date, no avatar)
- Figma stat cards have teal icon + right-arrow chevron inside each card; code uses `headlineMedium` count + `bodySmall` label with no icon/chevron
- Figma "Needs your attention" card has a "3 Tasks" amber badge chip and a left teal border accent; code has none of these decorative elements
- Figma "Needs your attention" includes a third row "Medical history update – Last updated 12 days ago" with an Update button; code only shows OCR pending + duplicate pending rows
- Figma attention rows have distinct icons per row type (amber warning, duplicate icon, calendar sync icon); code uses a single `Icons.Outlined.Warning` for all rows
- Figma recent-activity prescription cards show actual document thumbnail images; code uses a generic `Icons.Outlined.Description` placeholder
- Figma prescription cards show "No conflicts" / "OCR parsing" / "Needs review" status chips inline; code shows StatusBadge but without the "No conflicts" chip variant
- Quick ops chips use square icon+label button style in Figma with larger tap targets; code uses Material 3 `FilterChip`

❌ Missing from code:
- Patient greeting section (date, "Good morning [name]", emoji, avatar)
- App icon/logo in TopAppBar leading position
- "3 Tasks" badge count chip on the attention card
- Left teal vertical bar accent on the attention card
- "Medical history update" attention row (third task type)
- Per-row contextual icons in the attention section
- "Synced" / sync status indicator anywhere on the screen
- Notification badge count on the bell icon (code has semantic description but no visible `Badge` widget)

➕ Extra in code:
- Profile icon (`Icons.Outlined.AccountCircle`) with no Figma equivalent in the Dashboard top bar (Figma shows a teal filled avatar circle, not an outlined icon)

---

## SCR-02 — Prescription List (SCR-02-RX-LIST)
**Match: 38%**
**Figma screenshot:** `android/Figma/SCR-02-RX-LIST/Populated.png`

✅ Matches:
- "Prescriptions" title ✓
- Search icon in top bar ✓
- Sort icon in top bar ✓ (Figma shows filter sliders icon; code uses `Icons.Outlined.Sort` — close)
- Bottom nav present ✓
- Loading ✓, Empty ✓, Error ✓ states implemented

⚠️ Minor differences:
- Figma uses `MediumTopAppBar` with large collapsible title — code correctly uses `MediumTopAppBar` ✓ but Figma also shows "MEDIHISTORY AI" subtitle in small caps above the title; code omits this
- Sort icon differs slightly: Figma uses a filter/tune icon, code uses `Icons.Outlined.Sort`

❌ Missing from code:
- **Doctor-grouped collapsible sections** — Figma organises prescriptions by doctor (Dr. Ananya Krishnamurthy, Dr. Rajan Mehta, Dr. Sunita Bose, Dr. Vikram Iyer) as collapsible groups with a doctor avatar circle (initials), specialty tag, clinic name, prescription count badge, and expand/collapse chevron. Code shows a flat `LazyColumn` of `PrescriptionCard`s with no grouping at all
- **Inline search bar** — Figma shows a persistent search box with microphone icon directly below the top bar. Code uses an icon button that only toggles search active state (no always-visible search bar)
- **Doctor filter chips** — Figma has "All" + per-doctor filter chips ("Dr. Krishnamurthy", "Dr. Mehta", "Dr. Bose") below the search bar. Code has no filter chips
- **"N prescriptions · Synced" count row** with a green "Synced" status chip
- **"1 prescription awaiting verification" action banner** — amber card with ACTION chip, description text, "Review Now →" and "Dismiss" buttons. Code has no attention banner
- Prescription cards inside doctor groups show actual document thumbnail images; code uses icon placeholder
- Prescription cards show AI confidence percentage badges ("AI 78%"); code StatusBadge doesn't include confidence %
- Duplicate alert sub-banner inside cards ("Potential duplicate painkiller flagged — Resolve") — code has no inline duplicate sub-banner per card
- The "Add Prescription" FAB is present in code but Figma doesn't show it on this screen (it may be hidden on scroll)

➕ Extra in code:
- Flat `PrescriptionCard` list (not wrong but structurally different from grouped design)
- `MediumTopAppBar` with search + sort icons in trailing position (sort icon is extra; Figma has search + filter-tune)

---

## SCR-02 — Prescription Detail (SCR-02-RX-DETAIL)
**Match: 32%**
**Figma screenshot:** `android/Figma/SCR-02-RX-DETAIL/Populated.png`

✅ Matches:
- Back arrow navigation ✓
- Document preview placeholder section ✓ (box with icon)
- OCR status badge shown ✓
- Loading ✓, Error ✓ states
- "Extract text" button when OCR PENDING or ERROR ✓

⚠️ Minor differences:
- Figma title is "Prescription — Dr. Mehta" (dynamic doctor name); code uses plain "Prescription"
- Figma has trailing `⋮` options menu icon; code has none

❌ Missing from code:
- **Hospital/clinic name header card** — Figma shows a teal-outlined card "MANIPAL HOSPITAL / Dr. Rajan Mehta · MS (Ortho)" at the very top with a pin-zoom expand button
- **Inline prescription text preview** — Figma shows extracted text overlay ("Tab. Shelcal 500mg OD…") with "Pinch to zoom" affordance and "View document ↗" link
- **"Original document preserved — not modified by MediHistory AI" security badge row**
- **"3 fields need your review" amber action card** with field counter "FIELD 1 OF 3 · Dosage for Shelcal" and big "Confirm '1 tab daily'" and "✏ Edit" buttons
- **Doctor & Clinic Information section** — ElevatedCard with "ATTENDING PHYSICIAN", doctor name, specialty, facility, city chip, "Prescribed Date", "Digitised On" fields with a "✓ Verified" green chip
- **Extracted Medicines section** — detailed per-medicine expanded cards showing active ingredient, strength/form, dosage regimen, route, duration, timing; "Medicine Explorer" and "Edit medicine" buttons; "Add item" button
- **Individual medicine status chips** per extracted medicine (Needs review, Reviewed)
- **"Additional Notes" section** with parsed OCR text
- **Prescription filename footer** with OCR engine version and "Audit log ↗" link
- Doctor info currently shows raw `prescription.doctorId` string (UUID) not doctor name

➕ Extra in code:
- Raw `prescription.doctorId` field label (shows UUID rather than doctor name)
- "Notes" section (minor field in Figma, present in code)

---

## SCR-02 — Doctor Group (SCR-02-DOCTOR-GROUP)
**Match: 62%**
**Figma screenshot:** `android/Figma/SCR-02-DOCTOR-GROUP/Populated.png`

✅ Matches:
- Back arrow + doctor name as TopAppBar title ✓
- Doctor summary card with name, specialty, clinic, prescription count ✓
- Prescription count header text ✓
- Prescription timeline entries with date and StatusBadge ✓
- "View" TextButton per entry ✓
- Loading ✓, Error ✓, Empty ✓ states
- `EmptyStateView` for empty prescriptions with "Go back" action ✓
- Accessibility content descriptions on doctor card and timeline entries ✓

⚠️ Minor differences:
- Figma doctor card shows "MD, DM (Cardiology) · Reg. #KMC-48291" credential line and "Primary Cardiac Provider" green pill badge; code shows only specialty and clinicName
- Figma doctor card has stat chips at bottom: "2 Prescriptions · 5 Active Meds · 14 Jun Last Visit"; code shows only "N prescription(s) recorded"
- Figma shows an "ALLERGY ALERT DOCUMENTED" amber banner on the doctor card; code has no allergy alert
- Figma action row: "Call Clinic", "Directions", "Share Rx" buttons on the doctor card; code has none
- Figma shows a map/location image below the doctor card; code has none
- Figma "Active Medications" section per doctor with medicine list and "Chronic Care" chip; code has no medicine section

❌ Missing from code:
- Doctor credential subtitle (degree, registration number)
- "Primary Cardiac Provider" status badge
- Stats row on doctor card (prescription count, active meds count, last visit date)
- Allergy alert banner (if patient has documented allergies related to this doctor's prescriptions)
- Call Clinic / Directions / Share Rx action buttons
- Map/location card section
- "Active Medications" section listing medicines prescribed by this doctor
- Prescription timeline entries show "AI Extraction: 98% Confidence" sub-text; code omits OCR confidence from timeline
- Prescription timeline entries show actual thumbnail images; code has none

➕ Extra in code:
- Person icon in the summary card (Figma uses initials avatar circle e.g. "AK")

---

## SCR-03 — OCR Review (SCR-03-OCR-REVIEW)
**Match: 72%**
**Figma screenshot:** `android/Figma/SCR-03-OCR-REVIEW/Populated.png`

✅ Matches:
- "Review Prescription" TopAppBar title ✓
- Back arrow ✓
- Document preview placeholder (tappable) ✓
- Status banner: N fields need review (amber) / all reviewed (green) ✓
- "EXTRACTED FIELDS" section header ✓
- Per-field cards with: fieldName label, displayValue, `ConfidenceBadge`, review state chip ✓
- Low-confidence amber border + warning icon + text on PENDING fields below 0.70 ✓
- Confirm / Edit / Reject action buttons for PENDING_REVIEW fields ✓
- Un-review / Un-reject for REVIEWED/REJECTED fields ✓
- "Not extracted" chip for absent fields ✓
- Edit mode with OutlinedTextField + Save/Cancel ✓
- "Review next field" sticky bottom button (scrolls to first pending) ✓
- Loading ✓, Error ✓ states
- SnackbarHost ✓
- Scroll-to-first-pending logic ✓

⚠️ Minor differences:
- Figma shows the doctor name + specialty as a subtitle below the "Review Prescription" title ("Dr. Rajan Mehta · Orthopaedics"); code title is plain "Review Prescription" with no subtitle
- Figma confirm action is a large teal filled `Button` labelled "✓ Confirm field"; code uses `TextButton` (smaller, less prominent)
- Figma edit action is an outlined button "✏ Edit field"; code uses `TextButton`
- Figma shows a page progress indicator at the bottom (step circles); code has no progress stepper
- Figma "High / Review / Missing" confidence chips are coloured pill shapes; code uses `SuggestionChip` + `ConfidenceBadge` (functionally equivalent but visually different)
- Figma shows "Source ↗" chip on each field; code has no per-field source link

❌ Missing from code:
- Doctor name + specialty subtitle in TopAppBar (or below title)
- Per-field "Source ↗" link chip
- Page/step progress indicator at bottom
- Prominent teal Button for Confirm (code uses TextButton which is much less visible)
- "3 fields need your review before saving" wording (code says "N field(s) need your review" — minor text difference)

➕ Extra in code:
- `SnackbarHost` (Figma doesn't explicitly show one, but it's reasonable)

---

## SCR-04 — Medicine Explorer / List (SCR-04-MED-EXPLORER)
**Match: 52%**
**Figma screenshot:** `android/Figma/SCR-04-MED-EXPLORER/Populated.png`

✅ Matches:
- "Medicines" MediumTopAppBar title ✓
- Search icon in top bar ✓
- Filter icon in top bar (Figma uses `Icons.Outlined.FilterList`; code uses same) ✓
- Status filter chips row (All, Current, Past, Unknown) ✓
- "Duplicate alert" filter chip present ✓
- Results count text ✓
- `DockedSearchBar` toggle for search ✓
- `MedicineCard` items with medicine name, active ingredient, strength, date, `MedicineStatusChip` ✓
- Loading ✓, Empty ✓, Error ✓ states

⚠️ Minor differences:
- Figma shows "Medicines / Priya Sharma · Consolidated Regimen" as a subtitle under the title; code has no patient name subtitle
- Figma medicine cards show a circular coloured icon per medicine (drug type icon with colour-coded background); code uses `Icons.Outlined.Medication` with teal primary tint (no coloured circular background)
- Figma shows doctor name ("Dr. A. Krishnamurthy") and a "1 Rx" source chip on each card; code shows prescriptionDate but no doctor name, and no "N Rx" source chip
- Figma shows dosing schedule inline on cards ("Daily", "Post Lunch", "8:00 AM"); code does not
- Figma "All (12)" chip shows total count in parentheses; code shows plain "All"

❌ Missing from code:
- **"2 Ingredient Conflict Overlaps – HIGH PRIORITY" amber alert banner** at the top of the list; code has no list-level duplicate alert banner
- **Coloured circular medicine icon** (each medicine has a unique background tint based on drug class)
- **Doctor name** on each `MedicineCard`
- **"N Rx" source count chip** on each card
- **Dosing schedule** on each card (e.g. "Daily", "Twice daily", "8:00 AM")
- **"Duplicate alert" pill chip on individual cards** with "Overlaps with…" sub-text and "Details" link
- **"All 12 medicines synchronised with cloud records" footer** sync status row
- Patient name / subtitle below the screen title
- Count in parentheses on filter chips ("All (12)", "Duplicate alert (2)")

➕ Extra in code:
- None significant

---

## SCR-04 — Medicine Detail (SCR-04-MED-DETAIL)
**Match: 44%**
**Figma screenshot:** `android/Figma/SCR-04-MED-DETAIL/Populated.png`

✅ Matches:
- Back arrow ✓
- TopAppBar with medicine name as title ✓
- Header card with medicine icon, name, active ingredient, `MedicineStatusChip` ✓
- "Details" section card: strength, formulation, dosage instructions ✓
- "Source prescription" section card: date, doctor ID, "View prescription" button ✓
- Disclaimer text (always visible) ✓
- Loading ✓, Error ✓ states

⚠️ Minor differences:
- Figma title is "Medication Detail" (generic); code uses the actual medicine name as the TopAppBar title — this is actually an improvement
- Figma has a `⋮` options menu and profile avatar in trailing position; code has none
- Figma "Details" section shows fields right-aligned (brand name right, active ingredient left-label); code uses two-column `LabeledRow` (similar but slightly different alignment)
- Figma "Clinical status" shows "Prescribed in 2 recent visits" with a sync icon; code shows `MedicineStatusChip` (CURRENT/PAST/UNKNOWN) which is a different concept
- Figma "View all Paracetamol medicines (2)" search suggestion row; code has no "view all by ingredient" row

❌ Missing from code:
- **"Potential duplicate ingredient" red alert banner** at the top ("Calpol and Dolo 650 share the same active ingredient — Review alert →"); this is the highest-priority safety element on this screen
- **"Prescribed in (N prescriptions)" section** — Figma shows each prescribing instance as an expandable row with doctor, facility, date, dosage, duration, route, notes, prescription filename link; code shows only one "Source prescription" section with a single entry
- **Multiple-dosage conflict banner** ("Multiple dosage instructions found across prescriptions — Non-prescriptive guidance: Please confirm current dosing…")
- **"Safety & Duplicate Details" section** with conflict list and "Compare with X in Duplicate Review" button
- **"Calpol 500mg logged · Set Reminder" sticky bottom bar**
- **"View all [ingredient] medicines (N)"** row under the header card
- `⋮` options menu (trailing TopAppBar icon)
- Source prescription doctor shown as actual name (Figma: "Dr. Sunita Bose"); code shows raw `prescribingDoctorId` UUID

➕ Extra in code:
- None significant

---

## SCR-05 — Duplicate Review (SCR-05-DUPLICATE-REVIEW)
**Match: 65%**
**Figma screenshot:** `android/Figma/SCR-05-DUPLICATE-REVIEW/Populated.png`

✅ Matches:
- "Potential Duplicates" TopAppBar title ✓
- Back arrow ✓
- Amber warning banner for unreviewed alerts ✓
- Show/Hide reviewed toggle filter ✓
- "Potential duplicate" labelling (never "Duplicate medication") ✓
- Expandable/collapsible alert cards ✓
- Match type chip (Exact match / Partial match) ✓
- "Reviewed" chip when alert is reviewed ✓
- Medicine A / Medicine B side cards in expanded view ✓
- "View prescription" button on each medicine side ✓
- Combination note for PARTIAL matches ✓
- Disclaimer banner in expanded view ✓
- "Mark as Reviewed" with confirmation AlertDialog + mandatory disclaimer text ✓
- Loading ✓, Empty ✓, Error ✓ states
- `EmptyStateView` ✓

⚠️ Minor differences:
- Figma has tabs at the top: "All (2)", "Pending (2)", "Reviewed (0)", "Newest first ▾"; code uses a simple show/hide reviewed TextButton (no tabs, no sort)
- Figma duplicate cards show a coloured icon per match type (amber circle for "POTENTIAL DUPLICATE", pink triangle for "COMBINATION OVERLAP"); code uses a single `Icons.Outlined.Warning`
- Figma shows match type as coloured label ("POTENTIAL DUPLICATE", "COMBINATION OVERLAP") above the ingredient name; code shows the ingredient name in the title
- Figma shows a "Why was this flagged?" explanatory section with "Same active drug · Differing strength · Same prescriber" factor chips; code has no explanation section
- Figma shows a progress bar (0 of 2 reviewed, 0%); code has no progress indicator
- Figma has "2 potential duplicate situations found" header card with explanatory text and "Learn how duplicates are detected →" link; code has a simpler amber banner

❌ Missing from code:
- Tabs row: All / Pending / Reviewed with counts, and sort dropdown
- Progress bar (N of N reviewed, %)
- Explanatory header card ("MediHistory AI compares active ingredients…")
- "Why was this flagged?" section with factor chips (Same active drug, Differing strength, Same prescriber)
- Coloured match-type label in card header (POTENTIAL DUPLICATE amber, COMBINATION OVERLAP pink)
- "Learn how duplicates are detected →" link

➕ Extra in code:
- None significant

---

## SCR-06 — Medical History (SCR-06-HISTORY)
**Match: 55%**
**Figma screenshot:** `android/Figma/SCR-06-HISTORY/Populated.png`

✅ Matches:
- "Medical History" TopAppBar title ✓
- Back arrow ✓
- Refresh button in trailing position ✓
- Non-dismissible disclaimer card ✓ (code uses `secondaryContainer`; Figma uses teal `tertiaryContainer` — close)
- AI-generated chip on AI sections ✓
- `HistorySectionView` with section icons ✓
- Source chips ("Source ↗") per entry ✓
- Linear progress bar during generation ✓
- Loading ✓, Empty ✓, Error ✓, Generating ✓ states
- FAB (Refresh) when sections present ✓

⚠️ Minor differences:
- Figma shows "AI Summarized" teal chip next to the title in the TopAppBar; code has no such chip
- Figma shows a share icon (↗) and `⋮` options menu in the TopAppBar trailing; code has only refresh icon
- Figma shows "Last generated: 12 days ago (3 Jul 2025)" with a green dot; code has no last-generated timestamp
- Figma shows prominent "Regenerate / Export PDF / Share" action button row; code has only FAB for refresh
- Figma "Conditions on record" section shows per-condition confidence badge ("Uncertain" amber chip) and source Rx chip inline; code `HistoryEntry` uses `contentTypeIcon` for uncertainty but not per-entry confidence chips
- Figma "Allergies on record" shows a "Critical" red chip for Penicillin allergy; code has no allergy severity chip
- Figma has a visual clinical timeline with year markers (2025, 2024), dot connectors, and month + visit type headers; code uses flat `HistorySectionView` cards with no timeline visualization
- Figma shows "Add a condition not on a prescription" and "Add an allergy" CTA buttons within sections; code has no add-condition/add-allergy CTAs

❌ Missing from code:
- "AI Summarized" badge chip in TopAppBar
- Share icon in TopAppBar
- "Last generated N days ago" timestamp
- "Regenerate / Export PDF / Share" button row
- Allergy severity chip ("Critical")
- Per-condition uncertainty chips inline on entries
- Visual clinical timeline with year markers and dot connectors
- "Add a condition not on a prescription" CTA button
- "Add an allergy" CTA button
- Conditions on record section (Figma has separate Conditions section; code uses generic `SectionType.MEDICINES` / `TIMELINE`)

➕ Extra in code:
- FAB for refresh (Figma has explicit action buttons instead)

---

## SCR-07 — New Doctor Mode (SCR-07-NEW-DOCTOR)
**Match: 58%**
**Figma screenshot:** `android/Figma/SCR-07-NEW-DOCTOR/Populated.png`

✅ Matches:
- "New Doctor Mode" TopAppBar title ✓
- Back arrow ✓
- Explanation card at top of list ✓
- Select all / Deselect all row ✓
- Selectable record cards with checkbox icon, doctor name, date, medicine count ✓
- Consent dialog before PDF generation ✓
- Consent dialog has package contents list and AI disclaimer ✓
- Share via Android share sheet (Intent.ACTION_SEND) ✓
- "Prepare summary" bottom bar button (disabled until selection) ✓
- Loading ✓, Empty ✓, Error ✓ states

⚠️ Minor differences:
- Figma organises selection into collapsible **Conditions (4)**, **Allergies (1)**, **Active Medicines (12)**, **Recent Prescriptions (3)**, **Laboratory Reports (0)** sections; code shows a flat list of prescriptions only
- Figma top bar shows "DOCTOR MODE" mode label above the title; code has plain "New Doctor Mode" title
- Figma shows a "?" help icon and profile avatar in trailing; code has only back arrow
- Figma explanation card shows "End-to-end encrypted · Patient governed" trust badges; code explanation card has no trust badges
- Figma has a "Preview →" button next to the title; code has no preview affordance on the main screen
- Figma shows a duplicate-aware warning banner ("2 selected medicines have potential duplicate alerts"); code has no selection-aware duplicate warning
- Figma shows "4 conditions · 1 allergy · 12 medicines · 3 prescriptions selected — 20 items" summary footer; code shows selected count only in the button text
- Figma medicine items show strength and doctor name inline; code prescription items show doctor name, date, medicine count

❌ Missing from code:
- **Conditions, Allergies, Medicines, Prescriptions, Lab Reports section grouping** (Figma has 5 collapsible sections; code is a flat prescription list)
- "DOCTOR MODE" mode indicator in TopAppBar
- Help (?) icon and profile avatar in TopAppBar trailing
- "End-to-end encrypted · Patient governed" trust badge row on explanation card
- "Preview →" button
- Duplicate-aware warning banner inside the selection view
- Summary footer with full content type breakdown
- Per-medicine strength and doctor name on selection items
- Lab Reports section (even if empty/0)

➕ Extra in code:
- None significant

---

## SCR-07 — PDF Preview (SCR-07-PDF-PREVIEW)
**Match: 62%**
**Figma screenshot:** `android/Figma/SCR-07-PDF-PREVIEW/Populated.png`

✅ Matches:
- "PDF Ready" TopAppBar title ✓
- Back/Cancel arrow ✓
- PDF thumbnail placeholder card with icon ✓
- "PDF ready — tap Share to continue" helper text ✓
- Filename display ✓
- Disclaimer reminder text ✓
- Cancel + Share action button row ✓

⚠️ Minor differences:
- Figma PDF preview shows actual PDF page content (rendered ACTIVE MEDICINES list, prescribers, etc.) with page navigation (Prev / 1 of 3 / Next); code shows a placeholder icon only
- Figma has a "Doctor Credentials" subtitle above "PDF Ready" title; code has plain "PDF Ready"
- Figma shows page indicator dots and Prev/Next navigation; code has no pagination
- Figma Back button is labelled "← Back to edit selection" at the very bottom; code uses Cancel in the action row

❌ Missing from code:
- Actual PDF page rendering (even a basic `PdfRenderer`-based view)
- Page navigation (Prev / Next buttons, page count "1 of 3")
- Page indicator dots
- "Back to edit selection" bottom navigation link

➕ Extra in code:
- None significant

---

## SCR-08 — Reminders (SCR-08-REMINDERS)
**Match: 48%**
**Figma screenshot:** `android/Figma/SCR-08-REMINDERS/Populated.png`

✅ Matches:
- "Reminders" TopAppBar title ✓
- "Set Up Reminder" Extended FAB ✓
- "Today's Medicines" section header ✓
- "All Reminders" section header ✓
- Per-reminder card: medicine name, scheduled time, "Prescribed:" label, Taken/Skipped buttons using `PrimaryButton`/`SecondaryButton` ✓
- Active toggle Switch on All Reminders cards ✓
- "Cancel reminder" `SecondaryButton` ✓
- Loading ✓, Empty ✓, Error ✓ states
- `EmptyStateView` with `Icons.Outlined.NotificationsOff` ✓

⚠️ Minor differences:
- Figma has `+` (add) and `⋮` (options) icons in TopAppBar trailing; code has no trailing actions
- Figma shows a non-dismissible "Based on prescription instructions" info banner at top; code has no such banner
- Figma separates today into **Morning** (with date header "Today — Tuesday, 15 Jul 2025 · 8:45 AM") and **Evening** (with "2 upcoming") sub-groups; code treats all active reminders as "today" with no time-of-day grouping
- Figma dose cards show "Taken at 8:14 AM / Undo" post-action state and "Missed (Scheduled 8:00 AM) / Log now" overdue state; code shows identical Taken/Skipped buttons regardless of dose status
- Figma "All Reminders" section shows inline edit pencil icon per reminder; code has no edit action
- Figma shows a "This week's overview" adherence grid (Mon–Sun calendar with dots and streak badge); code has no adherence/streak section
- Figma shows a "Past due — Monday, 14 Jul 2025 / Show (1 unlogged)" collapsed section; code has no past-due section
- Figma uses "Missed" as a third dose state button; code only has Taken/Skipped

❌ Missing from code:
- `+` (add reminder) icon in TopAppBar trailing
- `⋮` options menu in TopAppBar trailing  
- "Based on prescription instructions" disclaimer info banner
- Time-of-day sub-grouping within "Today's Medicines" (Morning / Evening)
- Date + time header on the Today section
- Post-action dose states: "Taken at HH:MM / Undo" and "Missed / Log now" overdue states
- "Missed" as a third dose-recording action
- Per-reminder edit (pencil) icon in All Reminders section
- "This week's overview" adherence calendar grid with streak badge
- "Past due" collapsed section for unlogged historical doses

➕ Extra in code:
- None significant

---

## SCR-09 — Safety Center (SCR-09-SAFETY-CENTER)
**Match: 45%**
**Figma screenshot:** `android/Figma/SCR-09-SAFETY-CENTER/Populated.png`

✅ Matches:
- "Safety Center" TopAppBar title ✓
- Back arrow ✓
- Duplicate alerts section card with count and "Review duplicates" button ✓
- "No pending alerts" text when count is 0 ✓
- "Based on active ingredients in your reviewed prescriptions" sub-text ✓
- Interaction check section (NotConfigured / Checking / Results / Error states) ✓
- NotConfigured amber card with medical safety statement (no "safe" word) ✓
- Error card with retry ✓
- InteractionResultCard with severity + reviewWarning ✓
- Loading state ✓

⚠️ Minor differences:
- Figma has **two tabs**: "Interaction Check" and "Privacy & Security"; code has a single scrollable layout with no tabs
- Figma shows a reference source row "Ref: MedEx India v2.3 — Active · Change" at the top of the interaction section; code omits the reference source display
- Figma interaction check shows a medicine selector ("Select Medicines to Check — Max 5 items") where the patient picks which medicines to check, and results are per-pair; code's interaction check is automatic (no user selection UI)
- Figma shows "Important Clinical Safeguard" amber banner with multi-line safety text; code omits this banner
- Figma results show "Source: MedEx India Pharmacology Registry" and "View Source Literature ↗" links; code omits source citation
- Figma result has "Check Different Medicines" action at bottom; code has no such reset action
- Figma duplicate alerts card uses `Icons.Outlined.Warning` tinted error red; code uses the same — ✓

❌ Missing from code:
- **"Privacy & Security" tab** (second tab in Figma) — entirely absent
- Reference source display row ("Ref: MedEx India v2.3 · Active · Change")
- **Medicine selector UI** for interaction check (patient selects up to 5 medicines)
- "Important Clinical Safeguard" amber warning banner
- Source citation row per interaction result ("Source: MedEx India Pharmacology Registry")
- "View Source Literature ↗" link per result
- "Check Different Medicines" reset action button
- Tab bar (Interaction Check / Privacy & Security)

➕ Extra in code:
- None significant

---

## Bottom Navigation & More Screen

### MediHistoryBottomBar
**Match: 90%**

✅ Matches:
- 5 tabs: Dashboard / Prescriptions / Medicines / History / More ✓
- Selected = filled icon, unselected = outlined icon ✓
- Badge support via `BadgedBox` + `Badge` ✓ (Figma shows "1" badge on Prescriptions tab)
- String resource labels ✓

⚠️ Minor differences:
- Figma uses teal filled icon on selected tab with teal label; Material 3 `NavigationBarItem` uses `primary` colour which will be teal if the theme is set correctly — probably matches
- Badge in Figma is on Prescriptions tab; code wires badge via `badgeCounts` map (caller needs to pass `Routes.PRESCRIPTIONS → N` from `MainActivity`)

❌ Missing from code:
- Verification that `MainActivity` passes badge counts (grep shows `badgeCounts` is passed but source of count needs confirmation)

### MoreScreen
**Match: 72%**

✅ Matches:
- "More" TopAppBar ✓
- Safety Center row (Shield icon) ✓
- New Doctor Mode row (PersonAdd icon) ✓
- Reminders row (Alarm icon) ✓
- Privacy & Data row (Lock icon) ✓
- Trailing ChevronRight on each row ✓
- ElevatedCard row style ✓
- Subtitle text per row ✓

⚠️ Minor differences:
- Figma shows "More" as a hub with no equivalent top-level TopAppBar — it's accessed via the bottom nav and has the standard bottom nav visible. Code correctly does this ✓
- Figma app shows more decorative row items; code is minimal but structurally correct

❌ Missing from code:
- No "Profile" or "Account" row (Figma may include it in full product; not visible in the captured screenshot)

### MediHistoryNavHost
**Match: 85%**

✅ Matches:
- All 13 screen routes wired ✓
- DoctorGroup route with `doctorId` argument ✓
- OCR review route with `prescriptionId` argument ✓
- Safety Center route with `patientId` argument ✓
- PDF Preview route absent (PdfPreviewScreen exists but not yet wired in NavHost) — see below
- Deep-link from Dashboard → Prescriptions, Duplicates, History, New Doctor all wired ✓

❌ Missing from code:
- `PdfPreviewScreen` is not wired in `MediHistoryNavHost` (the screen exists as a composable but there is no `composable(Routes.PDF_PREVIEW)` entry; it is only triggered inline by `NewDoctorModeScreen` via a `LaunchedEffect`)
- No `Routes.doctorGroup(doctorId)` deep-link from `PrescriptionListScreen` (the doctor-group screen is reachable only if the list screen has doctor-group items, which it currently doesn't)

---

## Top 5 Remaining Gaps to Fix

### Gap 1 — Prescription List: Doctor-Grouped Layout (Critical)
The Figma design's core interaction model for SCR-02-RX-LIST is doctor-grouped collapsible sections with doctor avatars, filter chips, and inline duplicate sub-banners. The current flat `LazyColumn` is the single largest structural deviation. This requires a new grouping data model in `PrescriptionListViewModel` (group by doctor, collapse/expand state) and a new `DoctorGroupHeader` composable.

### Gap 2 — Prescription Detail: Rich OCR + Medicine Sections (Critical)
SCR-02-RX-DETAIL is the most complex screen in the app and currently shows only ~5 of ~20 distinct UI elements from the Figma design. Missing are the hospital header card, inline text preview, per-field review action card, Extracted Medicines section (with full per-medicine expansion), additional notes, and the audit log footer. This is the screen patients use most during onboarding.

### Gap 3 — Dashboard: Patient Greeting + Attention Card Richness (High)
The greeting section (date, patient name, waving emoji, avatar) is the first thing patients see and is entirely absent. The attention card also lacks the "Medical history update" third task type and per-row contextual icons.

### Gap 4 — Medicine Detail: Duplicate Alert Banner + Multi-Prescription Section (High)
The `MedicineDetailScreen` is missing its most important safety element — the "Potential duplicate ingredient" red banner at the top. It also lacks the multi-prescription source section and the Safety & Duplicate Details section, making it appear that medicines have no prescribing history.

### Gap 5 — Safety Center: Medicine Selector + Privacy Tab (Medium)
The Safety Center is missing the "Privacy & Security" second tab entirely, and the interaction check UI has no medicine selector (user cannot choose which medicines to check). The "Important Clinical Safeguard" banner is also absent.

---

## Verdict

**Needs fixes** — several screens have blocking structural gaps before a demo would accurately reflect the Figma design.

| Priority | Screens | Blocker for demo? |
|---|---|---|
| 🔴 Critical | SCR-02-RX-LIST (doctor grouping), SCR-02-RX-DETAIL (rich sections) | Yes |
| 🟠 High | SCR-01-DASHBOARD (greeting), SCR-04-MED-DETAIL (duplicate banner), SCR-07-NEW-DOCTOR (section grouping) | Yes for stakeholder demo |
| 🟡 Medium | SCR-05-DUPLICATE-REVIEW (tabs/progress), SCR-06-HISTORY (timeline), SCR-08-REMINDERS (adherence grid), SCR-09-SAFETY-CENTER (tabs/selector) | Partial demo possible |
| 🟢 Minor | SCR-03-OCR-REVIEW (button prominence), SCR-04-MED-EXPLORER (duplicate banner), SCR-07-PDF-PREVIEW (page nav), SCR-02-DOCTOR-GROUP (allergy/map/meds sections) | Demo-able with caveats |

**Screens that are demo-ready (≥65% match):** SCR-03-OCR-REVIEW, SCR-02-DOCTOR-GROUP, SCR-05-DUPLICATE-REVIEW, SCR-07-PDF-PREVIEW, Bottom Nav, MoreScreen.

**Estimated post-fix overall match target:** ~85% (achievable without backend changes, using hardcoded/mock data for new sections).
