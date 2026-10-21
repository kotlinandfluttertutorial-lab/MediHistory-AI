# 05 — Medical History Summarizer Screen Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **AI Medical History Summarizer** screen for MediHistory AI — a native Android application.

**Screen ID:** SCR-06-HISTORY
**Feature:** F06 — AI Medical History Summarizer
**Linked requirements:** FR-027, FR-028, FR-029, FR-030, FR-031
**Acceptance criteria:** AC-050, AC-051, AC-052, AC-053, AC-054, AC-055, AC-056

---

## Screen Purpose

Allow the patient to generate a chronological, doctor-ready summary of their medical history using their reviewed prescription records and other documented information. Every fact in the summary must be tagged with a source type and linked to the originating document. The screen must make it unmistakably clear that this is an AI-generated summary, not a clinician's record, and that all information should be verified with the patient's doctor.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- Accessed from: History tab (bottom navigation) or Dashboard quick action.
- `MediumTopAppBar` — collapses on scroll.
- History tab is selected in the bottom navigation bar.

---

## Top App Bar

- Title: "Medical History" (`headlineMedium`)
- Trailing: Share icon (share), overflow ⋮ (options: Regenerate summary, Export PDF, Settings)
- No back arrow (top-level nav tab)

---

## Section 1: Persistent AI Disclaimer Banner

This banner is **always visible** at the very top of the screen content area, directly below the top app bar. It is **not dismissible**. It must be seen before any generated content.

```
┌──────────────────────────────────────────────────────────────┐
│ ℹ  AI-generated summary                                      │
│    This summary is created from your prescription records.   │
│    It may contain errors or incomplete information. Always   │
│    verify this summary with your doctor before using it.     │
│                                                              │
│    [Learn more about how this summary is created →]         │
└──────────────────────────────────────────────────────────────┘
```

- Background: `secondary-container` (teal tint). NOT amber or red.
- Icon: ℹ (info, not warning).
- Text: `bodyMedium`.
- "Learn more" link: `TextButton` — opens an `AlertDialog` or bottom sheet explaining:
  - Which records were used.
  - What "AI-generated" means in this context.
  - What the patient should do before relying on this document.
- Banner height is proportional — does not take excessive screen space. Multi-line text wraps cleanly.

---

## Section 2: Generate / Regenerate Area

Shown when no summary exists yet, or as a collapsed control when a summary exists.

### No Summary State

A centred `ElevatedCard` (or inline panel) below the disclaimer:

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│         [Illustration: timeline/medical history icon]        │
│                                                              │
│         Based on your 6 prescriptions from 4 doctors         │
│         (last 2 years)                                       │
│                                                              │
│         [    Generate Medical History    ]  (FilledButton)   │
│                                                              │
│    ℹ  Only reviewed prescription records will be included.  │
└──────────────────────────────────────────────────────────────┘
```

- "Based on your N prescriptions from N doctors (last N years)" — `bodyMedium`.
- "Generate Medical History": `FilledButton`, `primary`, centered.
- Note below button: "Only reviewed prescription records will be included." (`bodySmall`, `outline`). Reassures patient that unreviewed OCR data is not silently included.
- Link: "Manage which records are included →" (`TextButton`).

### Summary Exists — Summary Controls

When a summary is already generated, replace the card above with a compact control strip:

```
📄 Last generated: 12 days ago (3 Jul 2025)     [Regenerate] [Export PDF]
```

- `bodySmall` left, two `OutlinedButton`s right.
- "Regenerate": triggers new generation (confirmation dialog first if patient has reviewed records since last generation).
- "Export PDF": exports the current summary as a PDF.

---

## Section 3: Generation Progress

When generation is triggered, show an animated progress panel in place of the generate button:

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  ⏳  Generating your medical history summary…                │
│                                                              │
│  [━━━━━━━━━━━━━━━━━━━━━━━────────────]  65%                 │
│                                                              │
│  Analysing 6 prescriptions from 4 doctors…                  │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

- `CircularProgressIndicator` (indeterminate) or `LinearProgressIndicator` (determinate if progress known).
- Status text updates: "Analysing prescriptions… → Identifying medicines… → Organising timeline… → Generating summary…"
- Cancel button: `TextButton` "Cancel" below progress bar.
- No other content below while generating — prevent partial viewing of incomplete summary.

---

## Section 4: Generated Medical History Content

Shown after successful generation. A chronological timeline with tagged facts.

### Source Type Legend

A small horizontal legend above the timeline (non-dismissible, compact):

```
  [● Documented]  [● Patient-reported]  [⚠ Uncertain]  [✨ AI-generated]
```

Each tag type has a unique icon AND a colour AND a text label (never colour alone).
- **Documented**: `primary` blue dot — from reviewed prescription records.
- **Patient-reported**: `secondary` teal dot — from patient-entered notes.
- **Uncertain**: `tertiary` amber dot with ⚠ icon — from low-confidence OCR.
- **AI-generated**: `outline` grey with ✨ icon — AI-synthesised text.

### Timeline Layout

Vertical timeline connecting event nodes from most recent (top) to oldest (bottom).

**Timeline node structure:**

```
─── 2025 ────────────────────────────────
  │
  ●  Jun 2025 — Cardiology Follow-up
  │  Dr. Ananya Krishnamurthy
  │
  │  Medicines reviewed:                      [● Documented]
  │  • Betaloc ZOK (Metoprolol) 25mg — continued
  │  • Ecosprin (Aspirin) 75mg — continued
  │  • Rozavel (Rosuvastatin) 10mg — continued
  │  • Telma-H (Telmisartan + HCTZ) 40/12.5mg — continued
  │  • Shelcal (Calcium + Vit D) added          [📎 Rx-Jun-25]
  │
  ●  Mar 2025 — General Physician Visit
  │  Dr. Sunita Bose
  │
  │  New medicines:                            [● Documented]
  │  • Metformin 500mg (Type 2 Diabetes)
  │  • Calpol 500mg PRN (as needed)
  │  • Pan-D 40/10mg                           [📎 Rx-Mar-25]
  │
  │  Noted conditions:                         [⚠ Uncertain]
  │  • "Type 2 DM" — extracted from Rx notes
  │    (low confidence — tap to review)        [📎 Rx-Mar-25]
  │
─── 2024 ────────────────────────────────
  │
  ●  Oct 2024 — Orthopedic Consultation
     Dr. Rajan Mehta
     ...
```

**Node elements:**
- Year separator: `titleSmall`, `outline` colour, full-width divider.
- Node dot: filled circle, `primary` colour, on a `outline` colour vertical line.
- Month + event type: `titleMedium`.
- Doctor name: `bodyMedium`, `outline`.
- Fact rows: `bodyMedium`. Each row has a trailing source tag chip and/or source document `AssistChip`.

**Source tags (per fact):**

Each fact row ends with either:
- A `FilterChip`-style source tag: e.g., "[● Documented]", "[⚠ Uncertain]", "[✨ AI-generated]" in the corresponding colour.
- A source document `AssistChip` with 📎 icon: e.g., "[📎 Rx-Jun-25]". Tapping navigates to SCR-02-RX-DETAIL.

**Uncertain facts:**
- Amber background row.
- ⚠ icon leading.
- Trailing: "tap to review" in `labelSmall`.
- Tapping navigates to the source prescription for manual review.

### Documented Conditions Section

Above the timeline, a compact `ElevatedCard` — "Conditions on record":

```
┌──────────────────────────────────────────────────────────────┐
│  Conditions on record                                        │
│                                                              │
│  • Hypertension           [● Documented]  [📎 Rx-Jun-25]    │
│  • Type 2 Diabetes        [⚠ Uncertain]   [📎 Rx-Mar-25]    │
│  • Lumbar spondylosis     [● Documented]  [📎 Rx-Oct-24]    │
│  • Peripheral neuropathy  [● Documented]  [📎 Rx-Feb-25]    │
│                                                              │
│  [+] Add a condition not on a prescription                  │
└──────────────────────────────────────────────────────────────┘
```

### Documented Allergies Section

`ElevatedCard` — "Allergies on record":

```
┌──────────────────────────────────────────────────────────────┐
│  Allergies on record                                         │
│                                                              │
│  • Penicillin             [● Documented]  [📎 Rx-Jun-25]    │
│                                                              │
│  [+] Add an allergy not on a prescription                   │
└──────────────────────────────────────────────────────────────┘
```

Note: "MediHistory AI does not verify allergy information clinically. This is recorded as documented on prescriptions." (`bodySmall`, `outline`)

---

## Screen States

### State 1: No Summary Yet (first use)

Disclaimer banner + generate card with illustration and `FilledButton`. No timeline. No conditions/allergy cards.

### State 2: Generating

Disclaimer banner + animated progress panel. Nothing else shown below.

### State 3: Generated — with Priya's data

Full layout: disclaimer banner, summary controls strip (last generated, regenerate, export), conditions card, allergies card, chronological timeline with Priya's 2 years of history (4 doctors, 6 prescriptions, tagged facts, source links).

### State 4: Error — Generation Failed

Disclaimer banner stays.
Error panel replaces progress:
- Error icon + `titleMedium`: "Summary could not be generated"
- `bodyMedium`: "This may be due to insufficient reviewed records or a temporary error."
- `OutlinedButton`: "Try again"
- `TextButton`: "What do I need to generate a summary?"

---

## Share and Export Actions

From overflow ⋮ or the "Export PDF" button:
- "Export as PDF" — exports the full formatted summary to a PDF file.
- "Share" — triggers a consent confirmation bottom sheet before sharing:

```
┌──────────────────────────────────────────────────────────────┐
│  Share your medical history?                                 │
│                                                              │
│  You are about to share your medical history summary         │
│  outside MediHistory AI. This file contains sensitive        │
│  personal health information.                                │
│                                                              │
│  The summary includes:                                       │
│  • 4 documented conditions                                  │
│  • 1 documented allergy                                     │
│  • 12 active medicines                                      │
│  • Records from Jun 2023 – Jul 2025                         │
│                                                              │
│             [Cancel]    [Share →]                           │
└──────────────────────────────────────────────────────────────┘
```

"Share →" button opens the Android system share sheet.

---

## Accessibility Specifications

- Disclaimer banner: "Important notice: This is an AI-generated summary. It may contain errors. Always verify with your doctor before using it."
- Generate button: "Generate medical history summary based on 6 reviewed prescriptions from 4 doctors."
- Timeline nodes: "June 2025, Cardiology follow-up with Dr. Ananya Krishnamurthy. [list facts]."
- Source tags: "Source: Documented — from reviewed prescription." / "Source: Uncertain — low confidence OCR extraction. Tap to review."
- Source document chips: "Source document: Prescription from Dr. Krishnamurthy, June 2025. Tap to view."
- Progress: "Generating medical history summary. 65% complete."
- Year separators: announced as "Year: 2025."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Persistent non-dismissible disclaimer | FR-027 | AC-050: Disclaimer always visible on summary screen |
| Generate from reviewed records only | FR-028 | AC-051: Only reviewed Rx data used in generation |
| Source tags on every fact | FR-029 | AC-052: Every fact tagged with documented/uncertain/AI |
| Source document links on facts | FR-029 | AC-053: Each fact links to source document |
| Conditions and allergies sections | FR-030 | AC-054: Conditions and allergies shown separately |
| No fabricated facts | FR-031 | AC-055: Uncertain fields shown, not filled with guesses |
| Export and share with consent dialog | FR-034 | AC-056: Consent shown before sharing |
