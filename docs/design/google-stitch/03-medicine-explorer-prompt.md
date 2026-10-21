# 03 — Medicine Explorer Screens Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design two related **Medicine Explorer** screens for MediHistory AI — a native Android application.

**Screen IDs:** SCR-04-MED-EXPLORER, SCR-04-MED-DETAIL
**Feature:** F04 — Consolidated Medicine Explorer
**Linked requirements:** FR-017, FR-018, FR-019, FR-020, FR-021
**Acceptance criteria:** AC-030, AC-031, AC-032, AC-033, AC-034, AC-035, AC-036, AC-037, AC-038

---

## Screen Purpose

The Medicine Explorer gives the patient a single consolidated view of every medicine ever prescribed across all doctors and all prescriptions. It must handle Priya's 12 active medicines gracefully, and scale to 20+ medicines without breaking the layout. The list is searchable, filterable, and sortable. Each medicine can be tapped for a full detail view with all prescription instances and source links.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- `Medicines` tab is selected in the bottom navigation bar.
- `MediumTopAppBar` — collapses on scroll to `SmallTopAppBar`.

---

## Screen 1: SCR-04-MED-EXPLORER — Medicine List

### Top App Bar

- Title: "Medicines" (`headlineMedium`)
- Trailing: Search icon (🔍), Grid/List toggle icon (view_module / view_list)
- No back arrow (top-level nav tab)

### Search Bar

`SearchBar` M3 component (full-width, pill-shaped when inactive).
- Placeholder: "Search medicines, ingredients…"
- On activation: expands to a full-width search bar with keyboard.
- Real-time filtering: list updates as the patient types.
- Clear (✕) button appears when text is entered.
- Suggestion chips below active search bar: recently searched terms.

### Filter and Sort Row

Below the search bar, a horizontally scrollable row of controls:

**Filter chips (horizontally scrollable):**
```
[All ✓]  [Dr. Krishnamurthy]  [Dr. Mehta]  [Dr. Bose]  [Dr. Iyer]  [Active]  [Duplicate alert 🔔]
```

- "All" = no filter, selected by default.
- Doctor chips filter to medicines prescribed by that doctor only.
- "Active" chip shows only medicines with no end date or confirmed active status.
- "Duplicate alert" chip shows only medicines flagged with a potential duplicate alert.
- Multiple chips can be active simultaneously (AND logic).

**Sort control:**
- Right-aligned `TextButton` with dropdown: "A–Z ▾" (default).
- Sort options (bottom sheet): A–Z, Z–A, Newest first, Oldest first, By doctor, Duplicate alerts first.

**Results count:**
- Below the filter row, left-aligned `bodySmall` text: "Showing 12 medicines" (updates with filter).

### Medicine Card List

**List view (default):**

Each medicine is an `ElevatedCard` with the following layout:

```
┌──────────────────────────────────────────────────────────────┐
│  [💊 48×48dp]  Betaloc ZOK                  [🔔 alert badge] │
│  medicine      Metoprolol Succinate · 25mg                   │
│  icon area     Dr. A. Krishnamurthy · 14 Jun 2025            │
│                [Chip: Cardiology]  [Chip: 1 prescription]    │
└──────────────────────────────────────────────────────────────┘
```

- Leading: medicine icon placeholder (circular, `primary-container` fill, 💊 icon, 48×48dp).
- Title: medicine brand name — `titleMedium`.
- Subtitle line 1: active ingredient + strength — `bodyMedium`, `outline` colour.
- Subtitle line 2: prescribing doctor name + date — `bodySmall`, `outline` colour.
- Bottom chip row: specialty `AssistChip` + "N prescription(s)" `AssistChip`.
- Trailing top-right: duplicate alert badge (🔔 icon in `error` colour) when applicable.
- Trailing: chevron (›) indicating navigation.
- Tap: navigate to SCR-04-MED-DETAIL for that medicine.

**Grid view (alternate, toggle):**

2-column grid, each cell is a compact `ElevatedCard`:
```
┌─────────────┐
│     💊      │
│ Betaloc ZOK │
│ 25mg        │
│ Dr. Krishn… │
│ [🔔]        │
└─────────────┘
```

- Icon centered at top.
- Name: `titleSmall`, centered, max 2 lines with ellipsis.
- Strength: `bodySmall`, centered.
- Doctor name: `bodySmall`, `outline`, truncated with ellipsis.
- Duplicate badge: top-right corner overlay badge.

### Lazy Loading

When the medicine list exceeds the viewport, load additional cards as the patient scrolls down. Show a subtle `LinearProgressIndicator` at the bottom of the list while loading more items. No explicit "Load more" button.

### Duplicate Alert Summary Banner

If any duplicate alerts are active, show a persistent non-dismissible banner above the list:

```
┌─────────────────────────────────────────────────────────────┐
│ 🔔  2 potential duplicate ingredient alerts                  │
│     Tap to review                                       [→] │
└─────────────────────────────────────────────────────────────┘
```

`error-container` background (light red), `on-error-container` text. Tapping navigates to SCR-05-DUPLICATE-REVIEW.

---

## Medicine List — Priya's 12 Medicines (Populated State)

Show all 12 medicines sorted A–Z by default. Cards in order:

1. Betaloc ZOK — Metoprolol Succinate 25mg — Dr. Krishnamurthy — Cardiology — [No alert]
2. Calpol — Paracetamol 500mg — Dr. Bose — General — [🔔 Duplicate alert]
3. Dolo 650 — Paracetamol 650mg — Dr. Bose — General — [🔔 Duplicate alert]
4. Ecosprin — Acetylsalicylic Acid 75mg — Dr. Krishnamurthy — Cardiology — [No alert]
5. Metformin — Metformin HCl 500mg — Dr. Bose — General — [No alert]
6. Neurobion Forte — Vitamin B complex — Dr. Iyer — Neurology — [No alert]
7. Pan-D — Pantoprazole + Domperidone 40/10mg — Dr. Bose — General — [No alert]
8. Pregabalin — Pregabalin 75mg — Dr. Iyer — Neurology — [No alert]
9. Rozavel — Rosuvastatin 10mg — Dr. Krishnamurthy — Cardiology — [No alert]
10. Shelcal — Calcium Carbonate + Vitamin D3 500mg/250IU — Dr. Mehta — Orthopedics — [No alert]
11. Telma-H — Telmisartan + Hydrochlorothiazide 40/12.5mg — Dr. Krishnamurthy — Cardiology — [No alert]
12. Volini Gel — Diclofenac 1% topical — Dr. Mehta — Orthopedics — [No alert]

Items 2 (Calpol) and 3 (Dolo 650) show the 🔔 duplicate badge. The duplicate alert banner is visible above the list.

---

## Screen 2: SCR-04-MED-DETAIL — Medicine Detail

### Purpose

Full detail view for a single medicine, showing all prescription instances across all doctors, source prescription links, and any duplicate alert badge.

### Top App Bar

`MediumTopAppBar` — collapses on scroll.
- Title: medicine brand name (e.g., "Calpol") — `titleLarge`.
- Subtitle: active ingredient + strength (e.g., "Paracetamol · 500mg") — `bodyMedium`, `outline`.
- Leading: back arrow (returns to SCR-04-MED-EXPLORER).
- Trailing: overflow ⋮ (options: Edit, Add note, Remove from list).

### Duplicate Alert Banner (if applicable)

If this medicine is part of a duplicate alert, show a prominent banner at the very top (below the app bar, above all content):

```
┌──────────────────────────────────────────────────────────────┐
│ 🔔  Potential duplicate ingredient detected                  │
│     Calpol (Paracetamol 500mg) and Dolo 650 (Paracetamol     │
│     650mg) share the same active ingredient.                 │
│                                    [Review alert →]         │
└──────────────────────────────────────────────────────────────┘
```

`error-container` background. "Review alert →" = `TextButton` navigating to SCR-05-DUPLICATE-REVIEW.

### Section 1: Medicine Overview Card

`ElevatedCard`:

| Field | Value |
|---|---|
| Brand name | Calpol |
| Active ingredient | Paracetamol (Acetaminophen) |
| Strength | 500mg |
| Formulation | Tablet |
| Route | Oral |
| Status | Active (as of last prescription) |

Each row: `ListItem` with leading label (`labelSmall`, `outline`) and value (`bodyLarge`).

At the bottom of the card, a small `AssistChip` row:
- "Paracetamol" chip — tapping opens a filter showing all medicines with this active ingredient across all prescriptions (useful for seeing duplicates in context).

### Section 2: All Prescription Instances

`ElevatedCard` — "Prescribed in (2 prescriptions)"

Each prescription instance shown as an expandable row:

**Collapsed:**
```
  📋  Dr. Sunita Bose — General Physician      22 Mar 2025
      Calpol 500mg · "1 tablet twice daily"    [Reviewed ✓]
```

**Expanded:**
```
  📋  Dr. Sunita Bose — General Physician      22 Mar 2025
      Dosage:    1 tablet twice daily
      Duration:  ⚠ Not extracted
      Route:     Oral
      Notes:     After meals
      Source:    [📎 prescription_bose_20250322.jpg]
      [View full prescription →]
```

- Prescription thumbnail: 40×40dp, rounded, leading the row.
- Source document `AssistChip` with 📎 icon.
- "View full prescription →" TextButton navigates to SCR-02-RX-DETAIL.
- "Not extracted" shown in italic `outline` colour — never fabricated.

Show all prescription instances for this medicine (even from different doctors). If prescribed by 2 different doctors, show 2 separate expandable rows.

### Section 3: Dosage Summary

`OutlinedCard` — "Dosage instructions across prescriptions"

If multiple prescriptions with different dosage instructions exist for the same medicine:

```
┌────────────────────────────────────────────────────────────┐
│ ⚠  Multiple dosage instructions found                      │
│    Dr. Bose (Mar 2025):    1 tablet twice daily            │
│    Dr. Bose (Oct 2024):    1 tablet once daily             │
│                                                            │
│    Review with your doctor if uncertain.                    │
└────────────────────────────────────────────────────────────┘
```

Amber `tertiary-container` background. Non-prescriptive — shows the information but does not recommend which one to follow.

### Section 4: Related Alerts

`ElevatedCard` — "Alerts" (only shown if alerts exist for this medicine)

List related duplicate alerts by medicine name, with "Review →" links.

---

## Screen States

### Loading (both screens)
Skeleton shimmer in place of all cards and text. List shows 5 skeleton medicine cards. Detail shows skeleton for all sections.

### Empty — No Medicines
Centred illustration (empty pill bottle, friendly line-art).
- Title: "No medicines yet" (`headlineMedium`).
- Body: "Medicines will appear here after you add and review a prescription." (`bodyMedium`).
- `FilledButton`: "Go to prescriptions".

### Filtered — No Results
When filter chips return zero results:
- Illustration: search with no results (magnifying glass with X).
- Title: "No medicines match your filter" (`headlineMedium`).
- Body: "Try removing a filter or searching by a different term." (`bodyMedium`).
- `TextButton`: "Clear filters".

### Populated — 12 medicines
All 12 medicines listed, Calpol and Dolo 650 showing duplicate badges. Duplicate alert banner visible above the list.

---

## Interactions and Navigation

| Trigger | Destination |
|---|---|
| Tap medicine card (list or grid) | SCR-04-MED-DETAIL |
| Tap "Duplicate alert" banner | SCR-05-DUPLICATE-REVIEW |
| Tap "Review alert →" in detail banner | SCR-05-DUPLICATE-REVIEW |
| Tap source prescription chip | SCR-02-RX-DETAIL |
| Tap "View full prescription →" | SCR-02-RX-DETAIL |
| Tap doctor filter chip | Filter list by that doctor |
| Tap "Duplicate alert" filter chip | Show only alerted medicines |
| Tap active ingredient chip | Filter to all medicines with same active ingredient |
| Toggle grid/list | Switch list layout in-place |

---

## Accessibility Specifications

- Medicine cards `contentDescription`: "[Brand name], [active ingredient], [strength], prescribed by [doctor], [date]. [If alert: Potential duplicate ingredient alert active.]"
- Duplicate badge: "Potential duplicate ingredient alert" — not just visual badge.
- "Not extracted" fields: "Duration not extracted from prescription."
- Source chip: "Source: prescription from Dr. Bose, 22 March 2025. Tap to view."
- Lazy load indicator: "Loading more medicines."
- Filter chips: "Filter: [chip name]. Currently [selected/unselected]. Double tap to [select/deselect]."
- Grid/list toggle: "Switch to [grid/list] view."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Consolidated medicine list from all Rx | FR-017 | AC-030: All medicines from all prescriptions shown |
| Search by name and ingredient | FR-018 | AC-031: Search returns relevant results in real time |
| Filter by doctor and status | FR-019 | AC-032: Doctor filter narrows list correctly |
| 20+ medicines without layout break | FR-020 | AC-033: List lazy-loads, no layout overflow |
| Medicine detail with all Rx instances | FR-021 | AC-034: All prescription instances shown in detail |
| Source prescription links | FR-021 | AC-035: Each instance links to source Rx |
| Duplicate badge on affected medicines | FR-022 | AC-036: Duplicate badge shown where applicable |
| "Not extracted" shown, not fabricated | FR-011 | AC-037: Missing fields display as not extracted |
| Active ingredient chip for filtering | FR-018 | AC-038: Ingredient chip filters to same-ingredient medicines |
