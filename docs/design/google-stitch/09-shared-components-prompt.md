# 09 — Shared Components Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **Shared Component Library** for MediHistory AI — a native Android application. These components appear across multiple screens and must be specified as reusable design tokens.

**Component set:** All shared UI components across all screens
**Applies to:** All screen IDs (SCR-01 through SCR-09)
**Accessibility standards:** WCAG AA, Android TalkBack, 48dp minimum touch targets

---

## Component Purpose

This prompt generates the component design system — not a single screen, but a component sheet showing every shared, reusable UI component in all its states. Each component is shown on a component artboard with:
- All relevant states (default, pressed, disabled, loading, selected, error)
- Exact dimensions, colour tokens, and typography roles
- Accessibility annotations
- Usage context

---

## Component 1: Bottom Navigation Bar

### Specification

M3 `NavigationBar` with 5 items.

```
  ┌──────────┬──────────┬──────────┬──────────┬──────────┐
  │  🏠       │  📋       │  💊       │  📄       │  ⋯       │
  │Dashboard │Prescripts│Medicines │ History  │  More    │
  └──────────┴──────────┴──────────┴──────────┴──────────┘
```

**States per item:**
- **Unselected:** Icon (outlined), label below, `on-surface-variant` colour.
- **Selected:** Icon (filled), indicator pill behind icon, `on-secondary-container` icon, `secondary-container` indicator, label `on-surface`.
- **Badge — count:** Red filled circle badge top-right of icon, white number inside, `error` colour, `labelSmall` font.
- **Badge — dot:** Small dot (no number) for generic unread indicator.

**Dimensions:**
- Height: 80dp
- Icon size: 24×24dp
- Indicator pill: 64×32dp (selected)
- Touch target per item: minimum 48dp height

**Accessibility:**
- Each item: `contentDescription` = "[Tab name] tab. [If badge: N unread alerts]. Double tap to navigate."
- Selected state: "Currently on [tab name] tab."

**Badge states:**
- Prescriptions tab: "1" badge (1 prescription needs OCR review)
- Dashboard tab: no badge
- Medicines tab: no badge in default, can show count if medicines need review
- History tab: no badge
- More tab: aggregate count if reminders or safety items pending

---

## Component 2: Confidence Badge

Shown on every extracted field to indicate OCR extraction confidence.

### States

| State | Label | Icon | Colour scheme | Background |
|---|---|---|---|---|
| High | "Verified ✓" | check_circle | `secondary` green | `secondary-container` |
| Medium | "Review ⚠" | warning_amber | `tertiary` amber | `tertiary-container` |
| Low | "Needs review ✗" | error | `error` red | `error-container` |
| Unverified | "Unverified ?" | help_outline | `outline` grey | `surface-variant` |
| Patient confirmed | "Confirmed by you ✓" | person_check | `primary` blue | `primary-container` |

### Anatomy

```
  ┌───────────────────────────┐
  │  [icon]  [label text]     │
  └───────────────────────────┘
```

- Shape: `ShapeKeyTokens.CornerSmall` (8dp radius) — `AssistChip` style.
- Height: 32dp.
- Icon: 16×16dp, leading.
- Label: `labelLarge`.
- Horizontal padding: 12dp leading/trailing.
- Icon is NEVER the sole indicator — always accompanied by label text.

### Accessibility

`contentDescription` = "Confidence: [state label]. [Contextual: e.g., 'This field was reviewed by you.' or 'This field needs your review.']"

### Usage

Shown on:
- Individual extracted fields in SCR-02-RX-DETAIL.
- Medicine cards in SCR-04-MED-EXPLORER (if extraction is uncertain).
- Facts in SCR-06-HISTORY timeline.
- Conditions and allergies in SCR-07-NEW-DOCTOR selection.

---

## Component 3: Source Link Chip

A tappable chip that links to a source prescription document.

### Anatomy

```
  ┌──────────────────────────────┐
  │  📎  Rx — Dr. Mehta, Jul 25  │
  └──────────────────────────────┘
```

- Icon: attach_file (📎), 16×16dp, `primary` colour.
- Label: `labelLarge`, `primary` colour. Format: "Rx — [Doctor abbreviation], [Mon YY]".
- Shape: `ShapeKeyTokens.CornerSmall`.
- Style: `AssistChip` — outlined, `outline` border, `surface` background, `primary` icon and text.
- Height: 32dp.
- Tap: navigates to SCR-02-RX-DETAIL for that prescription.
- Long-press tooltip: "Source: Prescription from [Dr. full name], [full date]. Tap to view."

### States

- **Default:** outlined chip as above.
- **Pressed:** filled with `primary-container` background.
- **Unavailable:** `outline` colour, no icon, label "Source unavailable", `contentDescription` = "Source document not available."

### Accessibility

`contentDescription` = "Source document: Prescription from [Dr. full name], [full date]. Tap to view original."

---

## Component 4: Duplicate Alert Badge

A visual badge shown on medicine cards and list items where a potential duplicate ingredient has been detected.

### Anatomy

```
  ┌──────────────────┐
  │  🔔  Duplicate ⚡ │
  └──────────────────┘
```

- Icon: notifications (🔔), `error` colour, 16×16dp.
- Label: "Duplicate" or "Alert" — `labelSmall`, `error` colour.
- Background: `error-container` (pale red).
- Shape: `ShapeKeyTokens.CornerSmall` (pill shape variant).
- Used as a compact badge on medicine cards.

**Icon-only badge variant** (for grid view or tight layouts):

```
  ┌────┐
  │ 🔔 │  ← floating badge on top-right of medicine card
  └────┘
```

Round badge, 24×24dp, `error` colour background, `on-error` icon.

### States

- **Pending review:** `error-container`, `error` icon, full label visible.
- **Reviewed:** `surface-variant`, `outline` icon, label "Reviewed", muted.

### Accessibility

- Full badge: `contentDescription` = "Potential duplicate ingredient alert — tap to review."
- Icon-only: same `contentDescription`.
- Reviewed: "Duplicate alert — reviewed."

---

## Component 5: OCR Status Chip

Shown on prescription cards and in the detail view to indicate the current OCR extraction status.

### States

| Status | Label | Icon | Colour scheme |
|---|---|---|---|
| Pending | "OCR Pending" | hourglass_empty | `surface-variant` / `on-surface-variant` |
| In Progress | "Extracting…" | (animated spinner) | `primary-container` / `primary` |
| Needs Review | "Needs review ⚠" | warning | `tertiary-container` / `tertiary` |
| Reviewed | "Reviewed ✓" | check_circle | `secondary-container` / `secondary` |
| Failed | "Extraction failed ✗" | error_outline | `error-container` / `error` |

### Anatomy

```
  ┌─────────────────────────────┐
  │  [animated spinner]  Extracting…  │
  └─────────────────────────────┘
```

- `AssistChip` style.
- Height: 32dp.
- Icon: 16×16dp (animated for "In Progress" state — circular progress indicator 16dp).
- Label: `labelLarge`.
- Shape: `ShapeKeyTokens.CornerSmall`.

**"In Progress" animation:**
Replace icon with `CircularProgressIndicator` (16×16dp, stroke 2dp, `primary` colour). The chip label changes from "Extracting…" through stages: "Analysing…", "Identifying…", "Finishing…".

### Accessibility

`contentDescription` = "OCR status: [state label]." + contextual: "Tap to review" for Needs Review state.

---

## Component 6: Safety Disclaimer Banner

A persistent, non-dismissible informational banner shown on screens with AI-generated or extracted medical content.

### Variants

**Variant A — AI Summary Disclaimer (blue/teal):**

```
┌──────────────────────────────────────────────────────────────┐
│ ℹ  AI-generated summary — always verify with your doctor    │
│    Tap to learn more                                        │
└──────────────────────────────────────────────────────────────┘
```

- Background: `secondary-container`.
- Icon: info_outline, `on-secondary-container`.
- Text: `bodyMedium`, `on-secondary-container`.
- "Tap to learn more": `TextButton` in `secondary` colour.
- Used on: SCR-06-HISTORY, SCR-07-NEW-DOCTOR, SCR-07-PDF-PREVIEW.

**Variant B — Prescription Instruction Disclaimer (blue/teal):**

```
┌──────────────────────────────────────────────────────────────┐
│ ℹ  Based on prescription instructions — not medical advice  │
└──────────────────────────────────────────────────────────────┘
```

- Same colours as Variant A but shorter, single line.
- Used on: SCR-08-REMINDERS.

**Variant C — Interaction Check Warning (amber):**

```
┌──────────────────────────────────────────────────────────────┐
│ ⚠  Absence of a result does NOT mean the combination is safe│
│    Always consult your doctor or pharmacist.                │
└──────────────────────────────────────────────────────────────┘
```

- Background: `tertiary-container` (amber).
- Icon: warning, `tertiary`.
- Text: `bodyMedium`, `on-tertiary-container`.
- Used on: SCR-09-SAFETY-CENTER (Interaction Check tab).

**Common properties for all variants:**
- Not dismissible — no close ✕ button.
- Height: auto (text wraps as needed, minimum 48dp).
- Horizontal padding: 16dp.
- Vertical padding: 12dp.
- Positioned: directly below top app bar, before any screen content.
- Does NOT scroll away — sticks at top on scroll (sticky).

### Accessibility

- Variant A: "Important notice: This is an AI-generated summary. Verify with your doctor. Tap for more information."
- Variant B: "Notice: Based on prescription instructions, not medical advice."
- Variant C: "Warning: Absence of an interaction check result does not mean the combination is safe. Consult your doctor."

---

## Component 7: Loading Skeleton Variants

Three skeleton variants matching the three primary card types.

### Skeleton A — Prescription Card Skeleton

```
  ┌──────────────────────────────────────────────────────────┐
  │  [■■■■■■■■]  [━━━━━━━━━━━━━━━━━━━━━━]  shimmer ←        │
  │  72×72dp     [━━━━━━━━━━━━━━]                             │
  │  shimmer     [━━━━━━━━━━━━━━━━━]                          │
  │              [━━━━━━━━]                                   │
  └──────────────────────────────────────────────────────────┘
```

Matches SCR-02-RX-LIST card layout. Shimmer lines match the expected text widths (100%, 60%, 80%, 40% of available width).

### Skeleton B — Medicine Card Skeleton

```
  ┌──────────────────────────────────────────────────────────┐
  │  [●●●●●●]  [━━━━━━━━━━━━━━━━━━━━━━━━━━━━━]  shimmer ←  │
  │  48dp      [━━━━━━━━━━━━━━━━━━━]                         │
  │  circle    [━━━━━━━━━━━━━━━━━━━━━━━━━]                   │
  │            [━━━━━━━] [━━━━━━━━━━━]                        │
  └──────────────────────────────────────────────────────────┘
```

Matches SCR-04-MED-EXPLORER card layout.

### Skeleton C — Stats Tile Skeleton

```
  ┌─────────────────────┐
  │  [━━━]              │   ← icon shimmer
  │  [━━━━━━━━]         │   ← number shimmer
  │  [━━━━━━━━━━━━]     │   ← label shimmer
  └─────────────────────┘
```

Matches SCR-01-DASHBOARD stats tile layout.

**Shimmer animation:** A gradient sweep from left to right, repeating every 1.5 seconds. Gradient: `surface-variant` → `surface` → `surface-variant`. Smooth `LinearEasing`.

### Accessibility

Container `contentDescription`: "Loading" — announced once. Individual shimmer elements: no announcement (focusable = false).

---

## Component 8: Empty State Template

Reusable empty state for any screen with no data.

### Anatomy

```
                    [Illustration — 200×200dp]

                    [Primary title]
                    headlineMedium, centered, on-surface

                    [Supporting body text]
                    bodyMedium, centered, outline colour
                    max 2 lines

                    [     Primary CTA Button     ]
                    ExtendedFloatingActionButton or FilledButton
```

- Illustration area: 200×200dp, centred. SVG/vector illustration, each screen has a unique illustration matching its context.
- Illustration style: outline/line-art, friendly, not clinical, `primary` and `secondary` colour accents.
- Title: 1 line, `headlineMedium`.
- Body: up to 2 lines, `bodyMedium`, `outline` colour.
- CTA: single primary action button (do not show multiple actions in empty state).
- Vertical layout, all centred.
- Positioned in the middle of the remaining screen space (after app bar, before bottom nav).

### Screen-specific illustrations

| Screen | Illustration | Title | Body |
|---|---|---|---|
| SCR-02-RX-LIST | Prescription pad with + | "No prescriptions yet" | "Add your first prescription to get started." |
| SCR-04-MED-EXPLORER | Empty pill bottle | "No medicines yet" | "Medicines will appear here after you review a prescription." |
| SCR-05-DUPLICATE-REVIEW | Two medicines with ✓ | "No duplicate alerts" | "No potential duplicate ingredients were found." |
| SCR-06-HISTORY | Timeline with sparkle | "No history yet" | "Generate a medical history after adding prescriptions." |
| SCR-08-REMINDERS | Alarm clock + capsule | "No reminders yet" | "Set reminders based on your prescription instructions." |

### Accessibility

Illustration: `contentDescription` = "[contextual description of illustration]".
Title: announced as `headlineMedium`.
CTA button: full descriptive label.

---

## Component 9: Error State with Retry

Reusable error state for data load failures, network errors, or processing failures.

### Anatomy

```
                    [Error icon — 64×64dp]
                    error_outline, outline colour

                    [Error title]
                    titleMedium, on-surface, centered

                    [Error detail]
                    bodyMedium, outline colour, centered

                    [  Retry  ]
                    OutlinedButton, centered
```

- Icon: `error_outline` (outline, not filled — reduces alarm), 64×64dp, `outline` colour (not `error` red — this is recoverable).
- Title: e.g., "Couldn't load prescriptions" — `titleMedium`.
- Detail: e.g., "Check your connection and try again." — `bodyMedium`, `outline`.
- Retry button: `OutlinedButton` "Retry", centred.
- Optional secondary action: `TextButton` "Contact support" below Retry (only for persistent errors).

### States

- **Transient error (network):** Icon + title + "Check your connection…" + Retry.
- **Extraction failed:** Icon + title "Extraction failed" + "We couldn't read this prescription clearly. Try retaking the photo or enter details manually." + `OutlinedButton` "Try again" + `TextButton` "Enter manually".
- **Critical error:** Same structure but `error` colour icon for genuinely unrecoverable states.

### Accessibility

Error icon: `contentDescription` = "Error."
Title: announced as `titleMedium`.
Detail: announced as `bodyMedium`.
Retry button: "Retry loading."

---

## Component 10: Consent Confirmation Dialog

A reusable dialog pattern for any action that shares, deletes, or irreversibly changes data.

### Anatomy

```
  ┌──────────────────────────────────────────────────────────┐
  │  [Dialog title]                                          │
  │  titleLarge                                              │
  │                                                          │
  │  [Supporting text describing what will happen]           │
  │  bodyMedium                                              │
  │                                                          │
  │  [Content summary (optional) — bullet list]              │
  │  bodyMedium                                              │
  │                                                          │
  │  [Warning note if applicable — tertiary-container block] │
  │  bodySmall, amber background, ⚠ icon                    │
  │                                                          │
  │                    [Cancel]   [Confirm action]           │
  │                    TextButton    FilledButton             │
  └──────────────────────────────────────────────────────────┘
```

**Variants:**

| Use case | Confirm button style | Confirm label |
|---|---|---|
| Share medical summary | `FilledButton` (`primary`) | "Share →" |
| Mark alert as reviewed | `FilledButton` (`primary`) | "Mark as reviewed" |
| Delete account | `FilledButton` (`error`) | "Delete permanently" |
| Enable cloud backup | `FilledButton` (`primary`) | "Enable backup" |
| Export data | `FilledButton` (`primary`) | "Export" |

**Rules:**
- Dialog is never dismissible by tapping outside (use `dismissOnClickOutside = false`).
- Cancel always available.
- Confirm button is disabled when additional input is required (e.g., typing "DELETE").
- Warning block (amber) only shown for sharing and deletion — not for non-destructive confirmations.
- Dialog must not truncate — allow scrolling if content is long.

### Accessibility

- Dialog announced as "Dialog: [title]."
- Cancel: "Cancel. Closes this dialog."
- Confirm: "[Confirm label]. [Brief consequence: e.g., 'This will share your medical records.']"
- If confirm is disabled: "[label]. Disabled. [reason]."
- Warning block: read as part of dialog content in order.

---

## Component Traceability

| Component | Used on screens | Requirement |
|---|---|---|
| Bottom Navigation Bar | All SCR-01–SCR-04, SCR-06 | FR-001, all navigation |
| Confidence Badge | SCR-02-RX-DETAIL, SCR-04-MED-DETAIL, SCR-06-HISTORY, SCR-07-NEW-DOCTOR | FR-009, FR-029 |
| Source Link Chip | SCR-02-RX-DETAIL, SCR-04-MED-DETAIL, SCR-05-DUPLICATE-REVIEW, SCR-06-HISTORY, SCR-07-NEW-DOCTOR | FR-016, FR-029 |
| Duplicate Alert Badge | SCR-04-MED-EXPLORER, SCR-04-MED-DETAIL, SCR-07-NEW-DOCTOR | FR-022 |
| OCR Status Chip | SCR-02-RX-LIST, SCR-02-RX-DETAIL | FR-008 |
| Safety Disclaimer Banner | SCR-06-HISTORY, SCR-07-NEW-DOCTOR, SCR-07-PDF-PREVIEW, SCR-08-REMINDERS, SCR-09-SAFETY-CENTER | FR-027, FR-031, FR-037 |
| Loading Skeleton | All screens | FR-001–FR-040 (UX) |
| Empty State Template | SCR-02-RX-LIST, SCR-04-MED-EXPLORER, SCR-05-DUPLICATE-REVIEW, SCR-06-HISTORY, SCR-08-REMINDERS | All empty states |
| Error State with Retry | All screens | All error states |
| Consent Confirmation Dialog | SCR-05-DUPLICATE-REVIEW, SCR-06-HISTORY, SCR-07-PDF-PREVIEW, SCR-09-SAFETY-CENTER | FR-026, FR-034, FR-038, FR-040 |

---

## Global Accessibility Notes

All components comply with:
- **Minimum touch target:** 48×48dp for all interactive elements.
- **Colour contrast:** ≥ 4.5:1 normal text, ≥ 3:1 large text (WCAG AA).
- **Non-colour indicators:** Every status communicated by both colour and icon+label.
- **TalkBack order:** Left-to-right, top-to-bottom logical reading order.
- **Focus ring:** Visible M3 focus indicator on all interactive components.
- **Dynamic type:** All text reflows at 1.0–1.3× font scale without truncation.
- **Reduced motion:** Shimmer and animations respect system reduced-motion setting.
