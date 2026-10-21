# 02 — Prescriptions Screens Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design three related **Prescription Management** screens for MediHistory AI — a native Android application.

**Screen IDs:** SCR-02-RX-LIST, SCR-02-RX-DETAIL, SCR-02-DOCTOR-GROUP
**Features:** F02 — Multi-doctor Prescription Management, F03 — OCR and Prescription Review
**Linked requirements:** FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016
**Acceptance criteria:** AC-010, AC-011, AC-012, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-019, AC-020, AC-021, AC-022, AC-023, AC-024, AC-025

---

## Screen 1: SCR-02-RX-LIST — Prescription List

### Purpose

Show all prescriptions uploaded by the patient, grouped by doctor. Provide search and filter controls, and allow adding new prescriptions via FAB.

### Top App Bar

`MediumTopAppBar` — collapses on scroll.
- Title: "Prescriptions" (`headlineMedium`)
- Trailing: Search icon (🔍), Filter icon (tune ≡)
- No back arrow (top-level nav tab)

### Search Bar

Below the top app bar, a `SearchBar` M3 component (not a `TextField`).
- Placeholder: "Search by doctor, medicine, or date…"
- On expand: full-width search bar with keyboard, showing recent search chips below.
- On collapse: displayed as a pill-shaped inactive bar.

### Filter Chips Row

Horizontally scrollable `FilterChip` row below search bar:

```
[All ✓]  [Dr. Krishnamurthy]  [Dr. Mehta]  [Dr. Bose]  [Dr. Iyer]  [Needs review ⚠]  [Reviewed ✓]
```

- "All" is selected by default.
- Selecting a doctor chip filters the list to that doctor's prescriptions.
- "Needs review" chip filters to prescriptions with pending OCR review.
- Active chip: `FilterChip` in selected state (filled, with `primary` colour tint and leading ✓ icon).

### Prescriptions Grouped by Doctor

The list is organised into collapsible doctor sections. Each section:

**Doctor Section Header:**
- `ListItem` style with leading avatar (doctor's initials in a coloured circle — each doctor has a unique `primary/secondary/tertiary/error` container colour).
- Leading avatar colour per doctor: Dr. Krishnamurthy = primary-container, Dr. Mehta = secondary-container, Dr. Bose = tertiary-container, Dr. Iyer = error-container.
- Doctor name: `titleMedium`.
- Specialty badge: `AssistChip` to the right of the name (e.g., "Cardiology").
- Hospital name: `bodySmall`, `outline` colour.
- Trailing: prescription count badge ("3 prescriptions") + expand/collapse chevron.
- Tapping the header collapses or expands that doctor's prescriptions.
- Tap-and-hold or separate "View doctor" option navigates to SCR-02-DOCTOR-GROUP.

**Prescription Cards (within each section):**

Each prescription appears as an `ElevatedCard`:

```
┌──────────────────────────────────────────────────────────┐
│  [Rx thumbnail 72×72dp]  Dr. Ananya Krishnamurthy         │
│  [image, rounded 8dp]    Cardiology follow-up            │
│                          📅 14 Jun 2025 · 5 medicines     │
│                          [Chip: Reviewed ✓]               │
└──────────────────────────────────────────────────────────┘
```

Layout: thumbnail left (72×72dp, `OutlinedCard` with rounded 8dp corners), content right.
- Line 1 (title): doctor name — `titleMedium`.
- Line 2: prescription label / visit type — `bodyMedium`, `outline`.
- Line 3: date + medicine count — `bodySmall`, leading 📅 icon.
- Line 4: OCR status chip — see chip spec below.
- Trailing: right-facing chevron (›) — indicates navigable card.
- Tap: navigates to SCR-02-RX-DETAIL.
- Long-press: shows context menu (View, Edit details, Delete prescription).

**OCR Status Chips:**

| Status | Chip label | Icon | Colour |
|---|---|---|---|
| Pending | "OCR Pending" | hourglass_empty | surface-variant |
| In Progress | "Extracting…" | animated spinner | primary-container |
| Needs Review | "Needs review ⚠" | warning | tertiary-container (amber) |
| Reviewed | "Reviewed ✓" | check_circle | secondary-container (green tint) |
| Rejected | "Extraction failed" | error | error-container |

### FAB

`FloatingActionButton` — lower-right, `add` icon.
`contentDescription`: "Add prescription".
On tap: expands to a bottom sheet with two options:
- 📷 "Take a photo" (camera capture via CameraX).
- 🖼 "Choose from gallery" (Photo Picker).
- 📄 "Import PDF" (file picker for PDF).

### Sort Control

Small `TextButton` "Sort: Newest first ▾" at the top-right of the list area (above the filter chips row, inline with a "6 prescriptions" count label). Tap opens a `ModalBottomSheet` sort menu:
- Newest first (default, ✓ selected)
- Oldest first
- By doctor (A–Z)
- Needs review first

---

## Screen 1 States

### Loading
Full skeleton: 2 doctor section headers (shimmer) each with 2 skeleton prescription cards beneath.

### Empty — No Prescriptions
Centred illustration (prescription pad with + symbol, line-art, friendly).
- Title: "No prescriptions yet" (`headlineMedium`)
- Body: "Tap the button below to add your first prescription." (`bodyMedium`)
- `ExtendedFloatingActionButton`: `+  Add prescription`
- No filter chips row shown.

### Populated (Priya's data)
Doctor sections: Dr. Krishnamurthy (2 prescriptions: Reviewed, Reviewed), Dr. Mehta (2 prescriptions: Reviewed, Needs review ⚠), Dr. Bose (1 prescription: Reviewed), Dr. Iyer (1 prescription: Reviewed).
All sections expanded by default.

### OCR In Progress
One prescription card (Dr. Mehta's most recent) shows "Extracting…" chip with animated spinner. Card is non-interactive (greyed trailing chevron) while OCR is running. A snackbar at the bottom reads "Extracting prescription — this takes a few seconds."

---

## Screen 2: SCR-02-RX-DETAIL — Prescription Detail

### Purpose

Show the original prescription image alongside all extracted fields with confidence indicators. Allow the patient to review and correct individual fields. Preserve the original document unchanged.

### Top App Bar

`MediumTopAppBar` — collapses on scroll.
- Title: "Prescription — Dr. Mehta" (`titleLarge`).
- Subtitle: "3 Jul 2025 · Orthopedics" (`bodyMedium`, outline colour).
- Leading: back arrow.
- Trailing: overflow menu ⋮ (options: Edit details, Share, Delete, View original).

### Section 1: Original Document Viewer

At the top of the screen, a full-width document viewer:
- Shows the prescription image thumbnail at 16:9 aspect ratio (or the actual image aspect ratio).
- "Pinch to zoom" gesture hint overlay (appears for 2 seconds on first open, then fades).
- Overlay controls: fullscreen expand icon (top-right of image), "View original" link below.
- Caption below image: "Original document preserved — not modified by MediHistory AI." (`bodySmall`, `outline` colour, centered).
- If a PDF: show the first page as a rendered thumbnail with a PDF badge overlay.

### Section 2: Extraction Status Banner

If any fields have low confidence:

```
┌─────────────────────────────────────────────────────────────┐
│ ⚠  3 fields need your review                                │
│    Tap each highlighted field to confirm or correct it.     │
│                                        [Review all fields →]│
└─────────────────────────────────────────────────────────────┘
```

Amber (`tertiary-container`) background. Non-dismissible while unreviewed fields remain. Dismissed automatically once all fields are reviewed.

If all fields reviewed: replaced with a green `secondary-container` banner: "✓ All fields reviewed".

### Section 3: Doctor and Clinic Information

`ElevatedCard` — "Doctor & Clinic"

Fields displayed as `ListItem` rows:
- Doctor name: "Dr. Rajan Mehta"
- Specialty: "Orthopedics"
- Clinic/Hospital: "Manipal Hospital Whitefield"
- Date of prescription: "3 Jul 2025"
- Date added to app: "6 Jul 2025 (3 days ago)"

Each field has a leading label (`labelSmall`, `outline` colour) and a value (`bodyLarge`).
If a field was extracted with low confidence: amber highlight background + ⚠ icon + "Needs review" label + tap-to-edit affordance.
Reviewed fields: ✓ icon in `secondary` colour.

### Section 4: Extracted Medicines

`ElevatedCard` — "Medicines on this prescription"

Sub-heading: "3 medicines extracted"

Each medicine shown as an expandable `ListItem`:

**Collapsed view:**
```
  💊  Shelcal              Calcium Carbonate + Vitamin D3  500mg/250IU
      [Chip: Reviewed ✓]
```

**Expanded view (tap to expand):**
```
  💊  Shelcal
      Active ingredient:   Calcium Carbonate + Vitamin D3
      Strength:            500mg / 250IU
      Form:                Tablet
      Dosage:              ⚠ 1 tablet once daily (Needs review)
      Route:               Oral
      Duration:            ⚠ Not extracted
      Instructions:        After food
      [Edit this medicine]  [View in Medicine Explorer]
```

Amber highlight on "Dosage" field (low confidence OCR).
"Not extracted" shown in `outline` colour italic — never a fabricated value.
"Edit this medicine" button: `OutlinedButton`.
"View in Medicine Explorer" button: `TextButton`.

Each medicine card: expandable using an `ExpandableListItem` pattern (chevron toggles).

### Section 5: Additional Notes

`OutlinedCard` — "Additional notes"
- Free-text area: any other text extracted from the prescription that didn't fit a structured field.
- Shown in `bodyMedium`, `surface-variant` background.
- Label above: "Unstructured text from prescription" (`labelSmall`, `outline`).
- If none: "No additional notes extracted." (`bodyMedium`, `outline`, italics).

### Section 6: Source Traceability Footer

Small section at the bottom:
- "Source document: prescription_mehta_20250703.jpg" (file name, as `AssistChip` with 📎 icon — tap to view original).
- "Extracted on: 6 Jul 2025 at 14:32" (`bodySmall`).
- "Extraction model: OCR Service v1.2" (`bodySmall`, `outline`).
- Link: "View full extraction log" — `TextButton` navigating to a log detail screen.

### Review Mode

When "Review all fields →" is tapped, the screen enters a step-by-step review mode:
- A review banner at top: "Field 1 of 3 needs review".
- The current unreviewed amber field is highlighted with a pulsing amber border.
- Below the field: "Confirm" (`FilledButton`) and "Edit" (`OutlinedButton`).
- After confirming or editing, advances to the next unreviewed field.
- After last field: banner changes to "✓ All fields reviewed. Thank you."

---

## Screen 2 States

### Loading
Skeleton for document viewer area, doctor card fields, medicine list items.

### OCR In Progress
Document viewer shows the image.
Below image: animated progress bar with label "Extracting prescription details — this may take a moment."
Medicine section and doctor section show skeleton shimmers.
Banner: "Extraction in progress — please wait."

### Populated — Needs Review
Amber banner with 3 fields needing review. Dr. Mehta's prescription with Shelcal dosage and two other fields highlighted amber.

### Populated — All Reviewed
Green banner "All fields reviewed". No amber fields.

### Extraction Failed
Error state with `error-container` banner: "Extraction could not be completed for some fields." Shows partial extracted data where available. Manual entry option for missing fields. `OutlinedButton`: "Enter details manually".

---

## Screen 3: SCR-02-DOCTOR-GROUP — Doctor Group View

### Purpose

Show a focused view of one doctor's prescriptions in a timeline layout, with a doctor summary card at the top.

### Top App Bar

`MediumTopAppBar`.
- Title: "Dr. Ananya Krishnamurthy" (`titleLarge`).
- Subtitle: "Cardiologist · Apollo Hospitals Jayanagar".
- Leading: back arrow.
- Trailing: overflow ⋮.

### Doctor Summary Card

`ElevatedCard` at top:
- Doctor avatar (large, 56×56dp, initials "AK" in `primary-container`).
- Name: `headlineMedium`.
- Specialty + hospital: `bodyMedium`, `outline`.
- Stats row: "2 prescriptions · 5 medicines · Last visit: 14 Jun 2025".
- Allergy note (if present): `AssistChip` with ⚠ icon: "Penicillin allergy on file".

### Prescription Timeline

Vertical timeline (connected dots on a left-aligned vertical line):

```
●  14 Jun 2025 — Cardiology follow-up
   5 medicines · Reviewed ✓ · [View prescription →]

●  22 Mar 2025 — Initial consultation
   5 medicines · Reviewed ✓ · [View prescription →]
```

Each timeline node: date (`titleSmall`, `primary` colour), visit label (`bodyMedium`), medicine count (`bodySmall`), status chip, tappable link "View prescription →".

Timeline line: 2dp `outline` colour vertical line connecting nodes.

---

## Accessibility Specifications

- Prescription cards `contentDescription`: "Prescription from Dr. Rajan Mehta, Orthopedics, dated 3 July 2025, 3 medicines, status: Needs review."
- OCR status chips: "OCR status: Needs review — tap prescription to review extracted fields."
- Amber highlighted fields: announced as "[field name]. Low confidence — needs your review. Double tap to edit."
- "View original" link: "View original unmodified prescription document."
- Medicine expand chevron: "Expand to see full details for Shelcal."
- Image viewer: "Prescription image from Dr. Mehta, 3 July 2025. Pinch to zoom."
- FAB: "Add new prescription."
- Doctor section headers expand/collapse: "Dr. Krishnamurthy section, 2 prescriptions, expanded. Double tap to collapse."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Prescription list grouped by doctor | FR-005, FR-006 | AC-010, AC-011 |
| Original document preserved, viewer | FR-007 | AC-012: Original image unchanged |
| OCR status chips | FR-008 | AC-013: Status visible on all cards |
| Low-confidence amber highlighting | FR-009 | AC-014: Amber field highlight shown |
| Field-level review and edit | FR-010 | AC-015: Patient can confirm/edit each field |
| "Not extracted" — no fabrication | FR-011 | AC-016: Missing fields shown as not extracted |
| FAB add prescription | FR-012 | AC-017: FAB present, opens add flow |
| Doctor grouping and timeline | FR-013 | AC-018: Timeline view available per doctor |
| Search and filter | FR-014 | AC-019: Search filters by doctor/medicine |
| Multi-doctor support | FR-015 | AC-020: Multiple doctor groups displayed |
| Extraction source traceability | FR-016 | AC-025: Source doc link on every extracted field |
