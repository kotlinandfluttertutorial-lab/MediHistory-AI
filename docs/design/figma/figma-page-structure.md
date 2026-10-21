# Figma File Page Structure — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma file:** [FIGMA_URL_TBD]

---

## Overview

The MediHistory AI Figma file is organized into 18 pages. Every page follows a consistent internal layout with a title frame at the top-left and frames arranged left-to-right, top-to-bottom in reading order.

**Internal page layout convention:**
- Top row: page title frame (`1440 × 100`)
- Second row: reference notes frame (if applicable)
- Main area: screen frames arranged in a grid, 80dp gap between frames

---

## Page 1 — Cover & Legend

**Purpose:** Project orientation for anyone opening the file for the first time.

**Contents:**
- Project title: "MediHistory AI"
- Tagline: "All prescriptions. All medicines. One doctor-ready medical history."
- File version and last-updated date
- Team contacts (placeholder names)
- Status legend:

| Status label | Color | Meaning |
|---|---|---|
| 🔵 In Progress | Blue | Being designed |
| 🟡 Review | Amber | Awaiting stakeholder sign-off |
| 🟢 Ready for Dev | Green | Approved, ready to implement |
| 🔴 Blocked | Red | Blocked by open decision |
| ⚪ Not Started | Grey | Not yet designed |

- Screen ID reference table (all 13 screen IDs and their names)
- Link to this repository's `docs/design/` folder: [REPO_URL_TBD]

---

## Page 2 — Design Tokens

**Purpose:** Single source of truth for all design decisions. Every token must appear here before it is used anywhere else in the file.

**Frame groups on this page:**

### 2.1 Color Tokens
Frame name: `Tokens/Colors`  
Arrange as a grid: token name | swatch | hex value | usage note

Tokens covered: all entries from `figma-design-tokens.md` Section 1 (Color Tokens).

### 2.2 Typography Tokens
Frame name: `Tokens/Typography`  
Show each style as a live text sample with the style name, size, weight, line-height.

Tokens covered: all entries from `figma-design-tokens.md` Section 2 (Typography).

### 2.3 Spacing Scale
Frame name: `Tokens/Spacing`  
Visual ruler showing each spacing value (4, 8, 12, 16, 20, 24, 32, 40, 48, 64 dp).

### 2.4 Elevation Levels
Frame name: `Tokens/Elevation`  
Show a card at each elevation level (0–5) with the tonal surface overlay percentage labeled.

### 2.5 Shape Tokens
Frame name: `Tokens/Shape`  
Show each corner radius token on a filled rectangle with the dp value labeled.

### 2.6 Touch Targets
Frame name: `Tokens/TouchTargets`  
Diagram showing 48 × 48 dp minimum tap area vs. visual component size.

---

## Page 3 — Component Library

**Purpose:** All reusable components, organized by tier. Developers and designers both reference this page.

### Atoms
Frame: `Library/Atoms`  
Contents: color swatches, type ramp specimens, icon grid, divider, badge (notification dot).

### Molecules
Frame: `Library/Molecules`  
Contents (one component frame per row, all variants shown side by side):

| Component | Variants |
|---|---|
| `ConfidenceBadge` | High / Medium / Low / Unreviewed |
| `OcrStatusChip` | Extracted / Pending / Failed / Reviewing |
| `SourceLinkChip` | Default / Pressed / Focused |
| `DoseLogButton` | Taken / Skipped / Missed / Unrecorded |
| `ReminderRow` | Active / Snoozed / Disabled |
| `SafetyBanner` | Warning / Info / Error |

### Organisms
Frame: `Library/Organisms`  
Contents:

| Component | Variants |
|---|---|
| `PrescriptionCard` | Default / Expanded / Loading skeleton |
| `MedicineCard` | Default / With duplicate alert / Loading |
| `DuplicateAlertCard` | Unreviewed / Reviewed / Dismissed |
| `NavigationBar` | Dashboard selected / Prescriptions / Medicines / History / Safety |
| `TopAppBar` | Default / With back / With search / With overflow menu |
| `ConsentDialog` | Share consent / Export consent / Delete confirmation |
| `LoadingSkeleton` | Card skeleton / List skeleton / Detail skeleton |
| `EmptyState` | No prescriptions / No medicines / No alerts / No reminders |
| `ErrorState` | Network error / OCR failed / Sync failed |

---

## Pages 4–16 — Screen Pages

Each screen page follows this internal layout:

```
Row 1 (top): [Screen ID] / Loading  |  [Screen ID] / Empty  |  [Screen ID] / Error
Row 2:       [Screen ID] / Populated
Row 3+:      Additional states (Skeleton, Offline, Partial, etc.) if applicable
Row 4:       Annotation overlay frame (red boxes with callout labels)
```

### Page 4 — SCR-01 Dashboard

**Feature:** F01 — Patient Dashboard  
**Frame prefix:** `SCR-01-DASHBOARD`

| Frame name | State | Description |
|---|---|---|
| `SCR-01-DASHBOARD / Loading` | Loading | Skeleton cards for all dashboard sections |
| `SCR-01-DASHBOARD / Empty` | Empty | First-run state, no records added yet |
| `SCR-01-DASHBOARD / Error` | Error | Sync or data load failure |
| `SCR-01-DASHBOARD / Populated` | Populated | Patient summary with stats, recent prescriptions, duplicate alert banner |
| `SCR-01-DASHBOARD / Populated — Alert Active` | Populated variant | Dashboard with prominent duplicate alert card visible |

**Key UI regions to annotate:**
- Summary stats row (total prescriptions, doctors, medicines, alerts)
- Recent prescriptions section (last 3)
- Duplicate alert banner (amber, dismissible)
- Bottom navigation bar

---

### Page 5 — SCR-02 Rx List

**Feature:** F02 — Prescription Management  
**Frame prefix:** `SCR-02-RX-LIST`, `SCR-02-DOCTOR-GROUP`

| Frame name | State | Description |
|---|---|---|
| `SCR-02-RX-LIST / Loading` | Loading | Skeleton list |
| `SCR-02-RX-LIST / Empty` | Empty | No prescriptions, add CTA |
| `SCR-02-RX-LIST / Error` | Error | Load failure with retry |
| `SCR-02-RX-LIST / Populated` | Populated | List of prescription cards, FAB for add |
| `SCR-02-DOCTOR-GROUP / Populated` | Populated | Grouped by doctor view |

---

### Page 6 — SCR-02 Rx Detail

**Feature:** F02 — Prescription Management  
**Frame prefix:** `SCR-02-RX-DETAIL`

| Frame name | State | Description |
|---|---|---|
| `SCR-02-RX-DETAIL / Loading` | Loading | Skeleton detail |
| `SCR-02-RX-DETAIL / Empty` | Empty | No extracted data yet |
| `SCR-02-RX-DETAIL / Error` | Error | Load error |
| `SCR-02-RX-DETAIL / Populated` | Populated | Full detail: image thumbnail, doctor info, medicines list, source document link |

---

### Page 7 — SCR-03 OCR Review

**Feature:** F03 — OCR and Prescription Review  
**Frame prefix:** `SCR-03-OCR-REVIEW`

| Frame name | State | Description |
|---|---|---|
| `SCR-03-OCR-REVIEW / Loading` | Loading | OCR in progress indicator |
| `SCR-03-OCR-REVIEW / Empty` | Empty | No fields extracted |
| `SCR-03-OCR-REVIEW / Error` | Error | OCR extraction failed |
| `SCR-03-OCR-REVIEW / Populated` | Populated | Side-by-side or stacked layout: image + extracted fields with confidence badges |
| `SCR-03-OCR-REVIEW / Populated — Low Confidence` | Populated variant | Multiple amber confidence badges visible, review prompts active |

---

### Page 8 — SCR-04 Med Explorer

**Feature:** F04 — Consolidated Medicine Explorer  
**Frame prefix:** `SCR-04-MED-EXPLORER`

| Frame name | State | Description |
|---|---|---|
| `SCR-04-MED-EXPLORER / Loading` | Loading | Skeleton medicine cards |
| `SCR-04-MED-EXPLORER / Empty` | Empty | No medicines yet |
| `SCR-04-MED-EXPLORER / Error` | Error | Load error |
| `SCR-04-MED-EXPLORER / Populated` | Populated | Searchable list, filter chips, medicine cards |
| `SCR-04-MED-EXPLORER / Populated — Search Active` | Populated variant | Search bar expanded, results filtered |

---

### Page 9 — SCR-04 Med Detail

**Feature:** F04 — Consolidated Medicine Explorer  
**Frame prefix:** `SCR-04-MED-DETAIL`

| Frame name | State | Description |
|---|---|---|
| `SCR-04-MED-DETAIL / Loading` | Loading | Skeleton detail |
| `SCR-04-MED-DETAIL / Empty` | Empty | No detail available |
| `SCR-04-MED-DETAIL / Error` | Error | Load error |
| `SCR-04-MED-DETAIL / Populated` | Populated | Active ingredient, strength, doctors who prescribed, prescription links, duplicate warning if applicable |

---

### Page 10 — SCR-05 Duplicate Review

**Feature:** F05 — Potential Duplicate Ingredient Detection  
**Frame prefix:** `SCR-05-DUPLICATE-REVIEW`

| Frame name | State | Description |
|---|---|---|
| `SCR-05-DUPLICATE-REVIEW / Loading` | Loading | Skeleton alert list |
| `SCR-05-DUPLICATE-REVIEW / Empty` | Empty | No alerts detected |
| `SCR-05-DUPLICATE-REVIEW / Error` | Error | Detection service error |
| `SCR-05-DUPLICATE-REVIEW / Populated` | Populated | List of duplicate alert cards with explainability detail |
| `SCR-05-DUPLICATE-REVIEW / Populated — Detail Expanded` | Populated variant | One alert card expanded showing full ingredient comparison |

---

### Page 11 — SCR-06 History

**Feature:** F06 — AI Medical History Summarizer  
**Frame prefix:** `SCR-06-HISTORY`

| Frame name | State | Description |
|---|---|---|
| `SCR-06-HISTORY / Loading` | Loading | Skeleton summary |
| `SCR-06-HISTORY / Empty` | Empty | Not enough records to generate |
| `SCR-06-HISTORY / Error` | Error | Generation failed |
| `SCR-06-HISTORY / Populated` | Populated | Chronological summary with source citations, confidence labels |
| `SCR-06-HISTORY / Populated — Generating` | Populated variant | AI generation in progress indicator |

---

### Page 12 — SCR-07 New Doctor

**Feature:** F07 — New Doctor Mode  
**Frame prefix:** `SCR-07-NEW-DOCTOR`

| Frame name | State | Description |
|---|---|---|
| `SCR-07-NEW-DOCTOR / Loading` | Loading | Skeleton |
| `SCR-07-NEW-DOCTOR / Empty` | Empty | No records selected |
| `SCR-07-NEW-DOCTOR / Error` | Error | Preparation error |
| `SCR-07-NEW-DOCTOR / Populated` | Populated | Record selection UI, consent toggle, generate PDF button |
| `SCR-07-NEW-DOCTOR / Populated — Consent` | Populated variant | Consent dialog overlay |

---

### Page 13 — SCR-07 PDF Preview

**Feature:** F07 — New Doctor Mode  
**Frame prefix:** `SCR-07-PDF-PREVIEW`

| Frame name | State | Description |
|---|---|---|
| `SCR-07-PDF-PREVIEW / Loading` | Loading | PDF generating spinner |
| `SCR-07-PDF-PREVIEW / Error` | Error | Generation failed |
| `SCR-07-PDF-PREVIEW / Populated` | Populated | PDF preview with share button and selected-records summary |

---

### Page 14 — SCR-08 Reminders

**Feature:** F08 — Medication Reminders  
**Frame prefix:** `SCR-08-REMINDERS`

| Frame name | State | Description |
|---|---|---|
| `SCR-08-REMINDERS / Loading` | Loading | Skeleton |
| `SCR-08-REMINDERS / Empty` | Empty | No reminders set |
| `SCR-08-REMINDERS / Error` | Error | Load error |
| `SCR-08-REMINDERS / Populated` | Populated | Reminder list with dose log buttons |

---

### Page 15 — SCR-09 Safety Center

**Feature:** F09 — Medication Interaction Checks / F10 — Privacy  
**Frame prefix:** `SCR-09-SAFETY-CENTER`

| Frame name | State | Description |
|---|---|---|
| `SCR-09-SAFETY-CENTER / Loading` | Loading | Skeleton |
| `SCR-09-SAFETY-CENTER / Empty` | Empty | No interaction data (reference not configured) |
| `SCR-09-SAFETY-CENTER / Error` | Error | Reference source unavailable |
| `SCR-09-SAFETY-CENTER / Populated` | Populated | Interaction check results with explanatory banners |
| `SCR-09-SAFETY-CENTER / Source Unavailable` | Populated variant | Clear "checking could not be completed" message |

---

## Page 17 — Flows and Navigation Map

**Purpose:** Show how screens connect, not what they look like in detail.

**Contents:**
- Full navigation flow diagram (mirroring `navigation-map.md`)
- Bottom navigation tab flows
- Modal/bottom sheet flows
- Deep link entry points from notifications
- Back stack illustration for key flows (e.g. Dashboard → Rx List → Rx Detail → OCR Review)

Use Figma connector arrows between thumbnail-sized screen frames. Label each arrow with the triggering action (e.g. "Tap Rx card", "FAB pressed").

---

## Page 18 — Prototype Links

**Purpose:** Clickable prototype for stakeholder walkthroughs and usability testing.

**Contents:**
- Full-size screen frames (copies from screen pages) linked with Figma prototype connections
- Entry point: `SCR-01-DASHBOARD / Populated`
- Cover flow: Dashboard → Rx List → Rx Detail → OCR Review → Dashboard
- Duplicate flow: Dashboard → Duplicate Review → Medicine Detail
- New Doctor flow: History → New Doctor → PDF Preview → Share

Prototype link: [FIGMA_URL_TBD]

---

## Naming Conventions Quick Reference

| Object | Convention | Example |
|---|---|---|
| Page | `SCR-XX Name` or `NN — Title` | `SCR-01 Dashboard` |
| Frame (screen) | `[SCREEN-ID] / [State]` | `SCR-01-DASHBOARD / Populated` |
| Component | `Tier/Name` | `Organism/PrescriptionCard` |
| Component variant | Property=Value | `State=Loading, Variant=Default` |
| Color style | `color/token-name` | `color/primary` |
| Text style | `type/level-size` | `type/title-large` |
| Effect style | `elevation/levelN` | `elevation/level2` |

---

## Related Documents

- [`figma-import-and-handoff.md`](figma-import-and-handoff.md)
- [`figma-component-spec.md`](figma-component-spec.md)
- [`figma-design-tokens.md`](figma-design-tokens.md)
- [`../screen-inventory.md`](../screen-inventory.md)
