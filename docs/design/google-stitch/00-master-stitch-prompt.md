# 00 — Master Context Prompt for MediHistory AI

> **Instructions:** Paste this entire prompt first in every new Google Stitch session, then paste the individual screen prompt that follows. This establishes the shared design system, constraints, and persona that must be consistent across all screens.

---

## Application Identity

**App name:** MediHistory AI
**Tagline:** "All prescriptions. All medicines. One doctor-ready medical history."
**Platform:** Native Android application (phone-first, portrait orientation)
**Purpose:** A personal medical records organiser that helps patients manage prescriptions from multiple doctors, consolidate medicine history, detect potential duplicate active ingredients, and generate a doctor-ready medical history with references to the original documents.
**What the app is NOT:** It is not a clinical decision support tool, a prescribing system, a pharmacy, or a diagnostic aid. It never advises patients to start, stop, or change any medicine. It never invents diagnoses, allergies, or treatment instructions.

---

## Design System

### Material 3 Foundation

Apply Google Material 3 design language throughout. Use the full M3 component library — do not approximate components with custom widgets.

**Color scheme (light theme baseline — dark theme must also be designed):**

| Token | Value | Usage |
|---|---|---|
| `md.sys.color.primary` | `#0077B6` (Medical Blue) | Primary actions, FAB, selected nav |
| `md.sys.color.primary-container` | `#E3F4FD` | Card headers, selected chips |
| `md.sys.color.secondary` | `#00A896` (Teal) | Secondary actions, accent elements |
| `md.sys.color.secondary-container` | `#D4F5F0` | Secondary chips, info panels |
| `md.sys.color.tertiary` | `#F4A261` (Amber) | Low-confidence OCR highlights, warnings |
| `md.sys.color.tertiary-container` | `#FEF0E3` | Confidence warning cards |
| `md.sys.color.error` | `#B3261E` | Errors, critical alerts |
| `md.sys.color.surface` | `#FAFCFF` | Screen backgrounds |
| `md.sys.color.surface-variant` | `#E8EEF4` | Card surfaces, dividers |
| `md.sys.color.on-surface` | `#1A1C1E` | Body text |
| `md.sys.color.outline` | `#72787E` | Borders, secondary text |

**Semantic colours (non-colour cues must always accompany):**

| Meaning | Colour | Additional cue |
|---|---|---|
| High confidence | `#4CAF50` (Green) | ✓ icon |
| Medium confidence | `#FF9800` (Orange) | ⚠ icon |
| Low confidence | `#F44336` (Red) | ✗ icon, amber background |
| Unverified | `#9E9E9E` (Grey) | ? icon |
| Duplicate alert | `#E65100` (Deep Orange) | 🔔 icon + badge |
| Reviewed/acknowledged | `#78909C` (Blue Grey) | ✓ icon, muted style |

### Typography

Use `Roboto` (system default). Apply M3 type scale roles:

- `displaySmall` — screen headlines (e.g., "Your Medical History")
- `headlineMedium` — section titles (e.g., "Active Medicines")
- `titleLarge` — card titles, doctor names
- `titleMedium` — medicine names
- `bodyLarge` — primary body text, extracted field values
- `bodyMedium` — secondary text, dates, labels
- `bodySmall` — captions, disclaimers, source references
- `labelLarge` — button labels, chip text
- `labelSmall` — status chips, badges

**Minimum readable size:** `bodySmall` = 11sp. No text below 11sp.

### Spacing and Layout

- Screen horizontal padding: 16dp
- Card internal padding: 16dp (leading/trailing), 12dp (top/bottom)
- Between cards: 8dp vertical gap
- Between sections: 24dp vertical gap
- Reference phone width: **360dp** (design for this; ensure nothing breaks at 320dp or 480dp)
- Safe areas: respect system status bar (top) and navigation gesture zones (bottom)

### Elevation and Shape

- Cards: `ShapeKeyTokens.CornerMedium` (12dp radius), elevation 1dp (tonal surface)
- Bottom sheet: `ShapeKeyTokens.CornerExtraLargeTop` (28dp top radius)
- Chips: `ShapeKeyTokens.CornerSmall` (8dp radius)
- FAB: `ShapeKeyTokens.CornerLarge` (16dp radius)
- Dialog: `ShapeKeyTokens.CornerExtraLarge` (28dp radius)

---

## Android-Native Interaction Patterns

Apply genuine Android patterns. Do NOT use web navigation metaphors (no browser-style tabs at the top for primary navigation, no sidebar hamburger menus as primary nav on phones).

### Navigation Structure

```
Bottom Navigation Bar (persistent, 5 items):
  ┌────────────┬──────────────┬────────────┬───────────┬────────┐
  │ 🏠 Dashboard│ 📋 Prescripts │ 💊 Medicines│ 📄 History │ ⋯ More │
  └────────────┴──────────────┴────────────┴───────────┴────────┘
```

- Bottom nav uses `NavigationBar` M3 component.
- "More" opens a `ModalNavigationDrawer` or a secondary screen (Reminders, Safety Center, Settings, Privacy).
- Each tab has a badge for unread alerts (duplicate alerts, pending OCR review).
- Selected indicator uses `NavigationBarItem` indicator pill.

### Common Components (must appear consistently across all screens)

**Top App Bar:**
- `TopAppBar` (small) for list screens — scrolls away on scroll.
- `MediumTopAppBar` for detail screens — collapses on scroll.
- `LargeTopAppBar` for the Dashboard — shrinks as user scrolls.
- Always include: back navigation arrow (detail screens), overflow menu (⋮) for screen-level actions.

**FAB (Floating Action Button):**
- Use `ExtendedFloatingActionButton` on empty states (shows label + icon).
- Use `FloatingActionButton` on populated list screens (shows icon only).
- Primary action: "Add Prescription" (+ icon) on Prescriptions and Dashboard screens.
- FAB hides on downward scroll, reappears on upward scroll.

**Cards:**
- Use `ElevatedCard` for prescription and medicine items.
- Use `OutlinedCard` for summary stats and info panels.
- Use `FilledCard` for primary call-to-action panels (onboarding, empty state CTA).

**Chips:**
- `FilterChip` for search filters (doctor, specialty, status).
- `AssistChip` for source document links and quick actions.
- `SuggestionChip` for review actions.

**Dialogs:**
- `AlertDialog` for destructive actions and consent confirmations.
- `BasicDialog` for informational messages.
- Full-screen dialogs for complex flows (e.g., adding a prescription, OCR review).

---

## Universal Screen States

Every screen must show all four states. Design each state thoughtfully — do not show a white blank for empty/loading.

### Loading State — Skeleton

- Replace text with rounded grey shimmer rectangles matching the expected text width.
- Replace images/thumbnails with shimmer rectangles matching the expected image size.
- Animate shimmer left-to-right (shimmer sweep).
- Show 2–3 skeleton cards to suggest content is coming.
- Do not show a progress spinner alone.

### Empty State — Illustration + CTA

- Centred illustration (medical/document theme, friendly, not clinical).
- `headlineMedium` title: e.g., "No prescriptions yet"
- `bodyMedium` subtitle: e.g., "Add your first prescription to get started"
- `ExtendedFloatingActionButton` or `FilledButton` as the primary CTA.
- No clutter — single clear action only.

### Error State — Retry

- Error icon (outline, not filled, to reduce alarm).
- `titleMedium` message: e.g., "Couldn't load prescriptions"
- `bodyMedium` detail: e.g., "Check your connection and try again"
- `OutlinedButton` "Retry" — centred below the message.

### Populated State — Real Data

- Use Priya Sharma's synthetic data (see persona below).
- No "Lorem ipsum". All visible text must be realistic medical record data.

---

## Safety and Accuracy UI Patterns

These patterns must appear on every screen that shows extracted or AI-generated information. They are non-negotiable product safety requirements.

### OCR Confidence Highlighting

Low-confidence extracted fields must be highlighted with an amber (`#FEF0E3`) background and a ⚠ icon. The field remains editable. The amber background is accompanied by the text "Needs review" in `labelSmall`. This is the primary mechanism for flagging uncertain data.

```
┌─────────────────────────────────────────────────┐
│ ⚠  Medicine name              [Needs review]    │
│    Metoprolol Succ               ← tap to edit  │
└─────────────────────────────────────────────────┘
```

### Disclaimer Banners

Screens that show AI-generated summaries (F06 Medical History, F07 New Doctor Mode) must display a persistent, non-dismissible disclaimer banner at the top of the generated content:

```
┌─────────────────────────────────────────────────────────┐
│ ℹ  AI-generated summary — always verify with your doctor│
│    Tap to learn more                                    │
└─────────────────────────────────────────────────────────┘
```

Colour: `secondary-container` background, `on-secondary-container` text. Not amber (not an error). Must have an accessible contrast ratio ≥ 4.5:1.

### Duplicate Alert Cards

Potential duplicate alerts are labelled "Potential duplicate ingredient" — never "Duplicate medicine", "Unsafe", "Dangerous", or any clinical language.

### "Mark as Reviewed" Actions

Buttons that acknowledge an alert must say "Mark as reviewed" — never "Clear", "Dismiss as safe", "Approve", or any label that implies clinical clearance.

---

## Accessibility Requirements

Every screen must meet these baseline requirements. Call them out explicitly in each component design.

| Requirement | Specification |
|---|---|
| Touch targets | Minimum 48×48dp for all interactive elements |
| Colour contrast | ≥ 4.5:1 for normal text, ≥ 3:1 for large text (WCAG AA) |
| Colour-only cues | Never use colour as the sole indicator — always pair with icon, label, or pattern |
| Content descriptions | All icons, images, and status indicators must have `contentDescription` defined |
| Screen reader order | Logical top-to-bottom, left-to-right reading order for TalkBack |
| Focus indicators | Visible focus ring on all interactive components |
| Dynamic type | Text must reflow without truncation at font scale 1.0–1.3× |
| One-handed use | Critical actions reachable within thumb zone (bottom 60% of screen) |

---

## Synthetic Patient Persona

Use this data consistently across ALL screens. Never use real patient data.

**Patient:** Priya Sharma
**Age:** 52
**Location:** Bengaluru, India
**Doctors:**

| Doctor | Specialty | Hospital |
|---|---|---|
| Dr. Ananya Krishnamurthy | Cardiologist | Apollo Hospitals Jayanagar |
| Dr. Rajan Mehta | Orthopedist | Manipal Hospital Whitefield |
| Dr. Sunita Bose | General Physician | Sparsh Hospital |
| Dr. Vikram Iyer | Neurologist | Fortis Hospital Cunningham Road |

**Active Medicines (12):**

| # | Brand Name | Active Ingredient | Strength | Prescribed By |
|---|---|---|---|---|
| 1 | Telma-H | Telmisartan + Hydrochlorothiazide | 40/12.5mg | Dr. Krishnamurthy |
| 2 | Betaloc ZOK | Metoprolol Succinate | 25mg | Dr. Krishnamurthy |
| 3 | Ecosprin | Aspirin (Acetylsalicylic Acid) | 75mg | Dr. Krishnamurthy |
| 4 | Rozavel | Rosuvastatin | 10mg | Dr. Krishnamurthy |
| 5 | Shelcal | Calcium Carbonate + Vitamin D3 | 500mg/250IU | Dr. Mehta |
| 6 | Calpol | Paracetamol | 500mg | Dr. Bose |
| 7 | Dolo 650 | Paracetamol | 650mg | Dr. Bose |
| 8 | Neurobion Forte | B1+B6+B12 (Vitamin B complex) | — | Dr. Iyer |
| 9 | Pregabalin | Pregabalin | 75mg | Dr. Iyer |
| 10 | Volini Gel | Diclofenac (topical) | 1% | Dr. Mehta |
| 11 | Pan-D | Pantoprazole + Domperidone | 40/10mg | Dr. Bose |
| 12 | Metformin | Metformin HCl | 500mg | Dr. Bose |

**Potential duplicate alerts (2):**

| Alert | Medicines | Reason |
|---|---|---|
| Alert 1 | Calpol 500mg (Dr. Bose) + Dolo 650 (Dr. Bose) | Same active ingredient: Paracetamol — different brand and strength |
| Alert 2 | Telma-H (contains HCTZ) + possible overlap with BP medicines | Combination product ingredient overlap |

**Prescription documents:** 6 prescription images, uploaded as JPEG thumbnails (simulate with placeholder thumbnails).
**Medical conditions:** Hypertension, Type 2 Diabetes (Dr. Bose), Lumbar spondylosis (Dr. Mehta), Peripheral neuropathy (Dr. Iyer).
**Allergies (documented):** Penicillin (documented on Dr. Krishnamurthy's prescription).
**Last prescription added:** 3 days ago (Dr. Mehta — orthopedic follow-up).

---

## Traceability

This master prompt applies to all screens and links to all features:
- F01 Patient Dashboard
- F02 Multi-doctor Prescription Management
- F03 OCR and Prescription Review
- F04 Consolidated Medicine Explorer
- F05 Potential Duplicate Ingredient Detection
- F06 AI Medical History Summarizer
- F07 New Doctor Mode
- F08 Medication Reminders and Adherence
- F09 Medication Interaction Checks
- F10 Privacy and Security

All screens implement the accessibility, safety, and design standards defined here.

---

*End of master context prompt. Paste the individual screen prompt below this line.*
