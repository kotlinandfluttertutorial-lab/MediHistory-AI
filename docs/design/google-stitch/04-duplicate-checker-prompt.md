# 04 — Duplicate Checker Screen Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **Potential Duplicate Ingredient Review** screen for MediHistory AI — a native Android application.

**Screen ID:** SCR-05-DUPLICATE-REVIEW
**Feature:** F05 — Potential Duplicate Ingredient Detection
**Linked requirements:** FR-022, FR-023, FR-024, FR-025, FR-026
**Acceptance criteria:** AC-040, AC-041, AC-042, AC-043, AC-044, AC-045, AC-046, AC-047, AC-048

---

## Screen Purpose

Show the patient all potential duplicate active ingredient situations detected across their prescriptions. Each alert must explain exactly which medicines are involved, why they were flagged, which doctors prescribed them, and on which dates. The patient can expand details and mark an alert as reviewed — this is an organisational action only, not medical clearance. The screen must be calm and informative, not alarming.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- Accessed from: Dashboard quick action, Medicines tab duplicate banner, notification bell.
- `MediumTopAppBar` — collapses on scroll.
- No bottom navigation visible (detail screen accessed from multiple entry points, shows back arrow).

---

## Top App Bar

- Title: "Potential Duplicate Ingredients" (`titleLarge`)
- Subtitle: "2 alerts · 0 reviewed" (`bodyMedium`, `outline`)  ← updates as alerts are reviewed
- Leading: back arrow (returns to previous screen)
- Trailing: overflow ⋮ (options: Mark all as reviewed, Export alert summary)

---

## Screen Header Summary

Below the top app bar, a `FilledCard` (not `error-container` — use a calm informational style with `secondary-container`) summary banner:

```
┌──────────────────────────────────────────────────────────────┐
│  🔔  2 potential duplicate ingredient situations found       │
│                                                              │
│  MediHistory AI compares active ingredients across all your  │
│  prescriptions. Review each alert and discuss with your      │
│  doctor. This is for your awareness — not medical advice.    │
│                                                              │
│  [Learn how duplicates are detected →]                      │
└──────────────────────────────────────────────────────────────┘
```

- Background: `secondary-container` (teal tint). NOT red or amber — this is informational.
- Icon: 🔔 (not ⚠) — alert, not error.
- "Learn how duplicates are detected →": `TextButton` that opens an `AlertDialog` or bottom sheet explaining the matching methodology (ingredient comparison, not clinical assessment).
- Bottom of card: progress row showing "0 of 2 reviewed" with a thin `LinearProgressIndicator`.

---

## Alert Card Design

Each alert is shown as an `ElevatedCard` with an expandable detail section.

### Alert Card — Collapsed State

```
┌───────────────────────────────────────────────────────────────┐
│  🔔  Potential duplicate ingredient                           │
│      Paracetamol                            [Pending review]  │
│                                                               │
│  Calpol 500mg (Dr. Bose)  ↔  Dolo 650 (Dr. Bose)            │
│  Same active ingredient found across 2 prescriptions          │
│                                                               │
│  [Expand for details ▾]                [Mark as reviewed]    │
└───────────────────────────────────────────────────────────────┘
```

Elements:
- Card top-left icon: 🔔 (`error` colour for pending, `outline` colour for reviewed).
- Card title: "Potential duplicate ingredient" (`titleMedium`).
- Matched ingredient name: `bodyLarge`, bold — e.g., "Paracetamol".
- Status chip top-right: `SuggestionChip` — "Pending review" (amber, `tertiary-container`) or "Reviewed ✓" (muted, `surface-variant`).
- Medicine pair row: two medicine names side-by-side connected by ↔ icon.
- Summary sentence: `bodyMedium`, `outline` — "Same active ingredient found across 2 prescriptions."
- Bottom row: "Expand for details ▾" (`TextButton`) left, "Mark as reviewed" (`OutlinedButton`) right.

**Reviewed state:**
- Card border becomes `outline` colour (muted, no emphasis).
- 🔔 icon becomes grey `outline` colour.
- Status chip: "Reviewed ✓" in muted `surface-variant`.
- "Mark as reviewed" button replaced by "Reviewed on [date]" `labelSmall` text.
- Card not removed — stays in list so the patient can refer back to it.

### Alert Card — Expanded State

When patient taps "Expand for details ▾", the card expands to show full details:

```
┌───────────────────────────────────────────────────────────────┐
│  🔔  Potential duplicate ingredient                           │
│      Paracetamol                            [Pending review]  │
│                                                               │
│  ┌───────────────────────┐  ┌───────────────────────────┐    │
│  │  💊  Calpol            │  │  💊  Dolo 650              │   │
│  │  Paracetamol 500mg    │  │  Paracetamol 650mg         │   │
│  │  Dr. Sunita Bose      │  │  Dr. Sunita Bose           │   │
│  │  22 Mar 2025          │  │  12 Jan 2025               │   │
│  │  General Physician    │  │  General Physician         │   │
│  │  [View prescription]  │  │  [View prescription]       │   │
│  └───────────────────────┘  └───────────────────────────┘    │
│                                                               │
│  ─────────── Why was this flagged? ──────────────            │
│  Both medicines contain Paracetamol as the active            │
│  ingredient. They were prescribed on different dates by the  │
│  same doctor. Strength differs: 500mg vs 650mg.              │
│                                                              │
│  Match type:  [Chip: Same active ingredient]                 │
│               [Chip: Different strength]                     │
│               [Chip: Same prescriber]                        │
│                                                              │
│  ──── This is not a clinical assessment ────                 │
│  MediHistory AI does not determine whether taking these      │
│  medicines together is safe or unsafe. Discuss with your     │
│  doctor or pharmacist.                                        │
│                                                              │
│  [Collapse details ▲]            [Mark as reviewed]          │
└──────────────────────────────────────────────────────────────┘
```

**Side-by-side medicine detail cards (two-column):**
- Each column: `OutlinedCard`.
- Content: medicine icon, brand name (`titleSmall`), active ingredient + strength (`bodyMedium`), prescribing doctor (`bodySmall`), date (`bodySmall`), specialty (`bodySmall`).
- "View prescription" link: `TextButton`, navigates to SCR-02-RX-DETAIL for that prescription.

**"Why was this flagged?" section:**
- `bodyMedium` plain-language explanation. Never says "dangerous", "unsafe", or gives clinical guidance.
- Match type chips: `AssistChip` row — e.g., "Same active ingredient", "Different strength", "Same prescriber", "Combination product".

**Possible match type chips:**

| Chip label | Icon | When shown |
|---|---|---|
| Same active ingredient | = | Exact same active ingredient |
| Different strength | ≠ | Same ingredient, different dose |
| Possible overlap | ~ | Active ingredient found in combination product |
| Combination product | 💊 | One is a multi-ingredient product |
| Same prescriber | 👤 | Both from same doctor |
| Different prescribers | 👥 | From different doctors |
| Ingredient match uncertain | ⚠ | Low confidence in ingredient identification |

**Disclaimer footer within expanded card:**
- `bodySmall`, `outline` italic: "MediHistory AI does not determine whether taking these medicines together is safe or unsafe. Discuss with your doctor or pharmacist."
- Always visible — not collapsible.

---

## Alert 2 — Combination Product Overlap

Show a second alert card for Alert 2 (Telma-H overlap):

**Collapsed:**
```
┌───────────────────────────────────────────────────────────────┐
│  🔔  Potential duplicate ingredient                           │
│      Hydrochlorothiazide (component)       [Pending review]   │
│                                                               │
│  Telma-H 40/12.5mg (Dr. Krishnamurthy) ↔ possible overlap    │
│  Combination product — ingredient check required              │
│                                                               │
│  [Expand for details ▾]                [Mark as reviewed]    │
└───────────────────────────────────────────────────────────────┘
```

When expanded, this alert shows:
- Match type chips: "Combination product", "Ingredient match uncertain", "Different prescribers".
- Explanation: "Telma-H contains Hydrochlorothiazide as one of its active components. If another prescription also contains a diuretic or Hydrochlorothiazide, this may be relevant. Please discuss with your doctor."
- Uncertainty note: "The active ingredient overlap in combination products may not always be clinically significant. This alert is for your awareness only."

---

## "Mark as Reviewed" Confirmation

When the patient taps "Mark as reviewed":

An `AlertDialog` appears:

```
┌──────────────────────────────────────────────────────────────┐
│                   Mark as reviewed?                          │
│                                                              │
│  This records that you have seen this alert. It is not       │
│  medical clearance and does not change your medicines.       │
│                                                              │
│  You can always view this alert again by scrolling down.     │
│                                                              │
│                       [Cancel]  [Mark as reviewed]          │
└──────────────────────────────────────────────────────────────┘
```

- Confirm button: "Mark as reviewed" (`FilledButton`, `primary`).
- Cancel button: "Cancel" (`TextButton`).
- Dialog body: always includes the explicit statement that this is not medical clearance.

After confirmation: alert card transitions to muted reviewed state (smooth animation, card does NOT disappear). Subtitle updates to "2 alerts · 1 reviewed". Progress bar updates.

---

## Filter and Sort Controls

Above the alert list, a filter row:
```
[All (2)]  [Pending review (2)]  [Reviewed (0)]
```

`FilterChip` row. "All" selected by default.

Sort: `TextButton` "Newest first ▾" → bottom sheet with: Newest first, Oldest first, Pending first.

---

## Screen States

### State 1: No Alerts

Centred illustration (two medicine capsules with a ✓ between them, friendly line-art).
- Title: "No duplicate alerts" (`headlineMedium`).
- Body: "No potential duplicate active ingredients were found across your prescriptions." (`bodyMedium`).
- Sub-body: "Alerts will appear here when similar ingredients are found in prescriptions from different doctors or dates." (`bodySmall`, `outline`).
- No action button — this is a positive state.

### State 2: Alerts Pending Review (Priya's data)

Both alerts shown in pending state (amber chips, dark 🔔 icons, "Mark as reviewed" buttons active).
Header summary shows "2 potential duplicate ingredient situations found."

### State 3: All Reviewed

All alert cards in muted reviewed state.
Progress bar full (green).
Header summary changes to:
- "2 alerts · 2 reviewed" (`titleMedium`).
- `bodyMedium`: "All alerts have been reviewed. Remember to discuss with your doctor."
- A new `FilledCard` appears at top: "✓ All alerts reviewed. No new alerts since your last review."

---

## Accessibility Specifications

- Alert card `contentDescription` (collapsed): "Potential duplicate ingredient alert. Paracetamol detected in Calpol 500mg and Dolo 650. Status: Pending review. Double tap to expand details."
- "Mark as reviewed" button: "Mark this Paracetamol duplicate alert as reviewed."
- Medicine detail cards in expanded view: "Calpol, Paracetamol 500mg, prescribed by Dr. Sunita Bose on 22 March 2025."
- Match type chips: "Match type: [chip label]. [brief description]."
- Disclaimer text: always in the reading order after the alert details.
- Progress bar: "Reviewed 0 of 2 alerts."
- Reviewed state card: "Paracetamol duplicate alert. Reviewed on [date]. Tap to expand."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Alert cards with ingredient matching | FR-022 | AC-040: Alerts shown for matching active ingredients |
| Side-by-side medicine detail in alert | FR-023 | AC-041: Both medicines shown with source Rx links |
| Match type chips with explanation | FR-024 | AC-042: Match type explained in plain language |
| Combination product handling | FR-025 | AC-043: Combination products flagged with uncertainty note |
| "Mark as reviewed" flow with dialog | FR-026 | AC-044: Review dialog shows not-medical-clearance text |
| Card stays after review (not deleted) | FR-026 | AC-045: Reviewed alerts remain visible |
| No clinical language (safe/unsafe) | FR-026 | AC-046: No clinical assessment language in UI |
| No alerts state with positive message | FR-022 | AC-047: No-alert empty state shown |
| Disclaimer always visible in expanded | FR-026 | AC-048: Disclaimer non-collapsible in expanded card |
