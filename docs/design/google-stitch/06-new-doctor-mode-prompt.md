# 06 — New Doctor Mode Screens Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design two **New Doctor Mode** screens for MediHistory AI — a native Android application.

**Screen IDs:** SCR-07-NEW-DOCTOR, SCR-07-PDF-PREVIEW
**Feature:** F07 — New Doctor Mode
**Linked requirements:** FR-032, FR-033, FR-034
**Acceptance criteria:** AC-057, AC-058, AC-059, AC-060

---

## Screen Purpose

New Doctor Mode helps the patient prepare a focused, shareable medical summary for a new doctor consultation. The patient selects which sections and individual records to include, previews a formatted PDF, and then explicitly consents to sharing it. The flow is: Select → Preview → Consent → Share. Nothing is shared without explicit patient confirmation.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- Accessed from: Dashboard quick action "New Doctor", History tab overflow, or SCR-06-HISTORY share action.
- Full-screen flow (modal-style top app bar with close ✕, not bottom nav).
- `LargeTopAppBar` — collapses on scroll to `SmallTopAppBar`.

---

## Screen 1: SCR-07-NEW-DOCTOR — Selection Screen

### Top App Bar

- Title: "New Doctor Mode" (`headlineMedium`)
- Subtitle: "Select records to share" (`bodyMedium`, `outline`)
- Leading: Close ✕ (exits back to previous screen)
- Trailing: "Preview →" (`TextButton`, disabled until at least 1 section selected)

### Introductory Card

`FilledCard` (`secondary-container`) at the top:

```
┌──────────────────────────────────────────────────────────────┐
│  🏥  Prepare for your next consultation                      │
│                                                              │
│  Select the information you want to share with a new doctor. │
│  You control what is included. A formatted PDF will be       │
│  created for you to review before sharing.                   │
└──────────────────────────────────────────────────────────────┘
```

`bodyMedium` text. No action button — just informational.

### Section Selector

The screen is divided into collapsible sections. Each section has a header with a checkbox (to include/exclude the entire section) and an expand chevron to show individual records.

**Section 1: Conditions**

```
☑  Conditions (4)                                          [▾]
```

Expanded:

```
  ☑  Hypertension              [● Documented]  [📎 Rx-Jun-25]
  ☑  Type 2 Diabetes           [⚠ Uncertain]   [📎 Rx-Mar-25]
  ☑  Lumbar spondylosis        [● Documented]  [📎 Rx-Oct-24]
  ☑  Peripheral neuropathy     [● Documented]  [📎 Rx-Feb-25]
```

Each condition row: leading `Checkbox`, condition name (`bodyLarge`), source tag chip, source doc `AssistChip`.
Individual items can be selected/deselected independently. Deselecting all items unchecks the section header.

**Section 2: Allergies**

```
☑  Allergies (1)                                          [▾]
```

Expanded:

```
  ☑  Penicillin               [● Documented]  [📎 Rx-Jun-25]
```

**Section 3: Active Medicines**

```
☑  Active Medicines (12)                                  [▾]
```

Expanded — compact list of all 12 medicines with individual checkboxes:

```
  ☑  Betaloc ZOK — Metoprolol 25mg          Dr. Krishnamurthy
  ☑  Calpol — Paracetamol 500mg             Dr. Bose          [🔔 Alert]
  ☑  Dolo 650 — Paracetamol 650mg           Dr. Bose          [🔔 Alert]
  ☑  Ecosprin — Aspirin 75mg                Dr. Krishnamurthy
  ☑  Metformin — Metformin HCl 500mg        Dr. Bose
  ☑  Neurobion Forte — Vit B complex        Dr. Iyer
  ☑  Pan-D — Pantoprazole+Domperidone       Dr. Bose
  ☑  Pregabalin — Pregabalin 75mg           Dr. Iyer
  ☑  Rozavel — Rosuvastatin 10mg            Dr. Krishnamurthy
  ☑  Shelcal — Calcium+VitD 500mg           Dr. Mehta
  ☑  Telma-H — Telmisartan+HCTZ 40mg       Dr. Krishnamurthy
  ☑  Volini Gel — Diclofenac 1%             Dr. Mehta
```

Each row: `Checkbox`, brand name + active ingredient (`bodyMedium`), doctor name (`bodySmall`, trailing), duplicate alert badge if applicable.

A "Select all / Deselect all" `TextButton` appears below the section header.

**Duplicate alert note within medicines section** (inline, if any alerted medicines are selected):

```
┌──────────────────────────────────────────────────────────────┐
│ 🔔  2 selected medicines have potential duplicate alerts.    │
│    Your new doctor will see this in the summary.            │
└──────────────────────────────────────────────────────────────┘
```

`tertiary-container` (amber) background, informational.

**Section 4: Recent Prescriptions**

```
☑  Recent Prescriptions (3)                               [▾]
```

Expanded:

```
  ☑  Dr. Rajan Mehta — Orthopedics          3 Jul 2025
     [📋 thumbnail]  3 medicines · Reviewed ✓
  ☑  Dr. Ananya Krishnamurthy — Cardiology  14 Jun 2025
     [📋 thumbnail]  5 medicines · Reviewed ✓
  ☑  Dr. Sunita Bose — General              22 Mar 2025
     [📋 thumbnail]  4 medicines · Reviewed ✓
```

Each row: `Checkbox`, prescription thumbnail (32×32dp), doctor name + specialty, date, medicine count, status chip.
Patient can select which prescription documents to include in the PDF (thumbnails included in PDF output).

**Section 5: Laboratory Reports** (optional, if any uploaded)

```
☐  Laboratory Reports (0)                                 [▾]
```

If empty: "(No lab reports uploaded)" `bodySmall`, `outline`. Section header not checked by default.

### Selection Summary Footer

A persistent footer above the keyboard / bottom of screen:

```
  ─────────────────────────────────────────────────────
  ✓ 4 conditions · 1 allergy · 12 medicines · 3 prescriptions selected
  [          Preview Summary →          ]   (FilledButton)
  ─────────────────────────────────────────────────────
```

- Summary text: `bodySmall`, updates in real time as patient selects/deselects.
- "Preview Summary →": `FilledButton`, `primary`. Disabled when nothing selected.
- The entire footer is pinned to the bottom of the screen above the system navigation gesture area.

---

## Screen 2: SCR-07-PDF-PREVIEW — PDF Preview and Share

### Top App Bar

- Title: "Summary Preview" (`titleLarge`)
- Subtitle: "Review before sharing" (`bodyMedium`, `outline`)
- Leading: back arrow (returns to selection screen)
- Trailing: Share icon (share), overflow ⋮ (options: Download PDF, Print)

### AI Disclaimer Banner

Same persistent non-dismissible banner from SCR-06-HISTORY:

```
┌──────────────────────────────────────────────────────────────┐
│ ℹ  AI-generated summary — always verify with your doctor    │
└──────────────────────────────────────────────────────────────┘
```

### PDF Preview Area

A paginated PDF viewer rendered inline:

- Show a white paper-like card (`ElevatedCard` with elevation 4dp) representing the first PDF page.
- Page dimensions approximately A4 proportions (portrait) scaled to fill the screen width at 16dp horizontal padding.
- The PDF content shows:

```
  ┌─────────────────────────────────────────┐
  │  MediHistory AI                         │  (app logo/wordmark)
  │  Medical Summary for: Priya Sharma      │
  │  Generated: 15 Jul 2025                 │
  │  ─────────────────────────────────────  │
  │  ⚠ AI-generated — verify with doctor   │
  │  ─────────────────────────────────────  │
  │                                         │
  │  CONDITIONS                             │
  │  • Hypertension              (Jun 2025) │
  │  • Type 2 Diabetes  ⚠Uncertain(Mar 2025)│
  │  • Lumbar spondylosis        (Oct 2024) │
  │  • Peripheral neuropathy     (Feb 2025) │
  │                                         │
  │  KNOWN ALLERGIES                        │
  │  • Penicillin                (Jun 2025) │
  │                                         │
  │  ACTIVE MEDICINES (12)                  │
  │  • Betaloc ZOK (Metoprolol 25mg)        │
  │    Dr. A. Krishnamurthy                 │
  │  ...                                    │
  └─────────────────────────────────────────┘
```

- Show "Page 1 of 3" indicator below the page.
- Horizontal swipe OR left/right arrow buttons to navigate pages.
- Page shadow (subtle) behind the white page card.

### Preview Controls

Below the PDF page preview:

- `TextButton` "← Back to edit selection" — returns to SCR-07-NEW-DOCTOR.
- `FilledButton` "Share this summary →" — triggers consent dialog.

### Consent Confirmation Dialog

When "Share this summary →" is tapped:

```
┌──────────────────────────────────────────────────────────────┐
│              Share your medical summary?                     │
│                                                              │
│  You are about to share your medical records summary         │
│  outside MediHistory AI.                                     │
│                                                              │
│  This summary contains:                                      │
│  • Personal details: Priya Sharma                           │
│  • 4 conditions, 1 allergy, 12 medicines                    │
│  • Prescription records from 3 doctors                      │
│  • Period: Mar 2025 – Jul 2025                              │
│                                                              │
│  ⚠  This is an AI-generated summary. It may not be          │
│     complete or accurate. Your doctor should review          │
│     original documents for clinical decisions.               │
│                                                              │
│  By tapping Share, you confirm you want to share this        │
│  summary and understand it is AI-generated.                  │
│                                                              │
│             [Cancel]          [Share →]                     │
└──────────────────────────────────────────────────────────────┘
```

- "Share →": `FilledButton`, opens Android system share sheet.
- "Cancel": `TextButton`.
- Dialog NOT dismissible by tapping outside — patient must tap Cancel or Share explicitly.
- The AI-generated disclaimer (⚠ block) is in `tertiary-container` (amber) within the dialog, distinct from the rest of the dialog content.
- Scroll required if the dialog content is too long for the screen height.

---

## Screen States

### State: Empty Selection (nothing checked)

"Preview Summary →" button is disabled (greyed out, `outline` colour).
Footer text: "Select at least one section to continue."

### State: Partial Selection

"Preview Summary →" enabled with count summary. Duplicate alert note shown if alerted medicines are selected.

### State: Fully Populated (Priya's data)

All sections expanded with pre-selected items. Footer: "4 conditions · 1 allergy · 12 medicines · 3 prescriptions selected."

### State: PDF Generating

After tapping "Preview Summary →", a brief generating state:
- Screen transitions to PDF preview screen.
- Before PDF loads: animated placeholder (paper/document loading animation, `CircularProgressIndicator` centered over page area).
- Duration: 1–3 seconds for a typical summary.

### State: PDF Preview Loaded

Full PDF page view with page navigation and share button.

### State: After Sharing

Return to SCR-07-NEW-DOCTOR. Show a `Snackbar`: "Summary shared successfully."
Add a "Last shared: [date/time]" note below the summary controls on SCR-06-HISTORY.

---

## Accessibility Specifications

- Section header checkboxes: "Section: [name], [N] items. Currently [checked/unchecked]. Double tap to [include/exclude] entire section."
- Individual item checkboxes: "[Condition/Medicine/Prescription name]. Currently [selected/deselected]. Double tap to [select/deselect]."
- Duplicate alert note: "Note: 2 selected medicines have potential duplicate ingredient alerts."
- "Preview Summary →" (disabled): "Preview Summary — disabled. Select at least one section to continue."
- PDF preview: "PDF preview, page 1 of 3. Swipe left for next page or use navigation arrows."
- Consent dialog: "Confirm sharing dialog. [full dialog text read in order]. Cancel button. Share button."
- Share button: "Share medical summary. This will open the share menu."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Section selector with checkboxes | FR-032 | AC-057: Patient can select/deselect individual sections |
| Individual record selection | FR-032 | AC-057: Patient can select individual conditions, medicines, Rx |
| Duplicate alert note in selection | FR-032 | AC-057: Alert note shown if alerted medicine selected |
| PDF preview before sharing | FR-033 | AC-058: Patient sees PDF before share is triggered |
| AI disclaimer in preview and dialog | FR-033 | AC-058: Disclaimer shown in preview and consent dialog |
| Explicit consent dialog | FR-034 | AC-059: Share only after consent dialog confirmation |
| Summary of what will be shared | FR-034 | AC-059: Dialog lists what is included |
| Snackbar confirmation after share | FR-034 | AC-060: Confirmation message after sharing |
