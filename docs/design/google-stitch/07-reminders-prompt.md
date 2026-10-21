# 07 — Medication Reminders Screen Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **Medication Reminders and Adherence** screen for MediHistory AI — a native Android application.

**Screen ID:** SCR-08-REMINDERS
**Feature:** F08 — Medication Reminders and Adherence
**Linked requirements:** FR-035, FR-036
**Acceptance criteria:** AC-062, AC-063, AC-064, AC-065

---

## Screen Purpose

Allow the patient to set optional medication reminders based on their reviewed prescription instructions, log each dose as taken, skipped, or missed, and see a simple adherence overview. The screen must clearly distinguish between what was prescribed (from the prescription record) and what the patient actually did (dose log). It must never imply clinical guidance or tell the patient whether their adherence is medically acceptable.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- Accessed from: "More" tab → Reminders, or notification tap.
- `MediumTopAppBar` — collapses on scroll.
- No bottom navigation visible (secondary screen accessed via More menu).

---

## Top App Bar

- Title: "Reminders" (`headlineMedium`)
- Trailing: Add icon (`add`, `+`), overflow ⋮ (options: Pause all reminders, Notification settings)
- Leading: back arrow (returns to More/Settings screen or Dashboard)

---

## Persistent Disclaimer Banner

Non-dismissible, below the top app bar:

```
┌──────────────────────────────────────────────────────────────┐
│ ℹ  Based on prescription instructions                        │
│    These reminders are based on documented prescription      │
│    instructions. They are not medical advice.                │
│    Always follow your doctor's instructions.                 │
└──────────────────────────────────────────────────────────────┘
```

`secondary-container` background, ℹ icon, `bodySmall` text.

---

## Section 1: Today's Medicines

Header: "Today — Tuesday, 15 Jul 2025" (`titleMedium`, with a current date chip).

Each medicine that has an active reminder for today appears as a reminder card. Show Priya's morning and evening reminders.

### Reminder Card — Not Yet Logged

```
┌──────────────────────────────────────────────────────────────┐
│  💊  Betaloc ZOK                             🕐 8:00 AM     │
│      Metoprolol Succinate 25mg               Morning         │
│      1 tablet daily · Prescribed by Dr. Krishnamurthy        │
│                                                              │
│  [  Taken ✓  ]    [  Skipped  ]    [  Missed  ]             │
└──────────────────────────────────────────────────────────────┘
```

Elements:
- Leading: medicine icon (💊, 40×40dp, `primary-container`).
- Title: brand name — `titleMedium`.
- Subtitle: active ingredient + strength — `bodyMedium`, `outline`.
- Body line: dosage instruction from prescription — `bodySmall`, `outline`. Prefixed with "Prescribed:".
- Time chip: `AssistChip` showing scheduled time (e.g., "8:00 AM") + period label ("Morning").
- Doctor attribution: `bodySmall`, `outline` — "Prescribed by Dr. Krishnamurthy."
- Bottom action row: three equal-width `OutlinedButton` buttons:
  - "Taken ✓" — logs dose as taken.
  - "Skipped" — logs as skipped (patient chose not to take).
  - "Missed" — logs as missed (forgot or unavailable).

### Reminder Card — Taken State

```
┌──────────────────────────────────────────────────────────────┐
│  💊  Ecosprin                                🕐 8:00 AM ✓   │
│      Acetylsalicylic Acid 75mg               Morning         │
│      1 tablet daily · Prescribed by Dr. Krishnamurthy        │
│                                                              │
│  ✓  Taken at 8:14 AM                    [Undo]              │
└──────────────────────────────────────────────────────────────┘
```

- Card background: very light `secondary-container` (green tint) to indicate completion.
- Time chip shows ✓ overlay.
- Bottom row replaced with: "✓ Taken at [time]" (`bodyMedium`, `secondary` colour) + `TextButton` "Undo" (removes the log entry within a short window).
- Card slightly muted compared to unlogged cards.

### Reminder Card — Missed State

```
┌──────────────────────────────────────────────────────────────┐
│  💊  Metformin                               🕐 8:00 AM ✗   │
│      Metformin HCl 500mg                     Morning         │
│      1 tablet twice daily · Dr. Bose                         │
│                                                              │
│  ✗  Missed                                  [Log now]       │
└──────────────────────────────────────────────────────────────┘
```

- Card background: very light `error-container` (pale red) for missed doses.
- Bottom row: "✗ Missed" (`bodyMedium`, `error` colour) + `TextButton` "Log now" (to log a late dose).

### Upcoming Reminders

Below "Today" section, a "Today — Later" sub-section for reminders not yet due:

```
┌──────────────────────────────────────────────────────────────┐
│  💊  Metformin                               🕐 8:00 PM     │  ← greyed, future
│      Metformin HCl 500mg                     Evening         │
│      1 tablet twice daily · Dr. Bose                         │
│      [  Log early  ]                                         │
└──────────────────────────────────────────────────────────────┘
```

Future cards: slightly muted opacity (0.7). Only "Log early" option (in case patient takes it ahead of schedule).

---

## Section 2: Reminders Configuration List

Separate section below "Today" items. Header: "Your medicine reminders" (`titleMedium`).

Each configured medicine shown as a `ListItem`-style row:

```
  💊  Betaloc ZOK         8:00 AM · Daily         ● Active  [Edit ✏]
  💊  Ecosprin            8:00 AM · Daily         ● Active  [Edit ✏]
  💊  Metformin           8:00 AM, 8:00 PM · Daily ● Active  [Edit ✏]
  💊  Pregabalin          10:00 PM · Daily         ● Active  [Edit ✏]
  💊  Pan-D               7:30 AM · Daily          ● Active  [Edit ✏]
  💊  Shelcal             After dinner · Daily     ● Active  [Edit ✏]
  ────
  + Add reminder for another medicine
```

- Status dot: `secondary` green for active, `outline` grey for paused.
- "Edit ✏": `IconButton`, opens reminder configuration bottom sheet.
- "+ Add reminder for another medicine": `TextButton`, opens medicine picker then configuration sheet.

### Reminder Configuration Bottom Sheet

When "Edit ✏" or "Add" is tapped, a `ModalBottomSheet` opens:

```
  ─────────────────────────────────────────
  Configure reminder — Betaloc ZOK
  ─────────────────────────────────────────
  Medicine:        Betaloc ZOK
  Prescription:    Prescribed: 1 tablet daily (Dr. Krishnamurthy)

  Reminder time(s):  [8:00 AM]  [+ Add time]

  Frequency:       [Daily ▾]

  Start date:      [14 Jun 2025]  ← from prescription date

  End date:        [No end date ▾]

  Reminder enabled:  [Toggle: ON]

  ─ What does each dose log mean? ─
  Taken: you took the medicine
  Skipped: you chose not to take it
  Missed: you forgot or couldn't take it

  [Cancel]                  [Save reminder]
  ─────────────────────────────────────────
```

- Time picker: tapping time chip opens M3 `TimePicker` dialog.
- Frequency: `ExposedDropdownMenuBox` (Daily, Twice daily, Weekly, As needed).
- The prescription instruction is shown as reference but is not editable here.
- Note below start date: "Set from prescription date. You can change this." (`labelSmall`, `outline`).

---

## Section 3: Adherence Overview

Below the configuration list, a compact `ElevatedCard` — "This week's adherence":

```
┌──────────────────────────────────────────────────────────────┐
│  This week's overview                                        │
│  Mon  Tue  Wed  Thu  Fri  Sat  Sun                           │
│   ✓    ●    —    —    —    —    —   (today = Tue)            │
│                                                              │
│  Betaloc ZOK      ●●  ○  —  —  —  —  —                      │
│  Ecosprin         ●●  ○  —  —  —  —  —                      │
│  Metformin        ●○  ●○ —  —  —  —  —  (twice daily)       │
│                                                              │
│  ℹ  This is your recorded history — not a medical assessment │
└──────────────────────────────────────────────────────────────┘
```

Legend:
- ● green filled dot = taken
- ○ amber unfilled dot = skipped/missed
- — = not due (future or no reminder that day)

**Disclaimer at the bottom of the adherence card** (always visible):
"This is your recorded history — not a medical assessment. Adherence information is for your reference only." (`bodySmall`, `outline`)

No percentage scores, no "good adherence" or "poor adherence" labels — these are medical judgements the app does not make.

**Streak display:**
A small streak indicator: "🔥 3-day streak: Betaloc ZOK" — shown only as a motivational indicator, not a clinical metric. No penalties for missed doses.

---

## Section 4: Past Due Reminders

A collapsible section "Past due (yesterday)" at the bottom:

```
  ▸  Past due — Monday, 14 Jul 2025                    [Show]
```

When expanded, shows cards for yesterday's unlogged medicines with "Log now" option.

---

## Screen States

### State 1: No Reminders Set

Illustration: a gentle alarm clock with medicine capsule (friendly, line-art).
- Title: "No reminders yet" (`headlineMedium`).
- Body: "Set optional reminders based on your prescription instructions." (`bodyMedium`).
- `ExtendedFloatingActionButton`: `+  Set up reminders`.
- Disclaimer still shown.

### State 2: Active Reminders — Morning (Priya, 8 AM)

Today section shows 6 medicines with "not yet logged" cards (all morning reminders).
Configuration list below.
Adherence card below that.

### State 3: Partially Logged

Some cards in taken state (green), one in missed state (red), some upcoming (muted).

### State 4: All Logged for Today

Today section header updates: "Today — All done ✓" with a `secondary-container` (green) banner above the list. All cards in taken/skipped/missed state.

---

## Accessibility Specifications

- Reminder cards: "[Medicine name], [active ingredient] [strength]. Scheduled for [time]. [Status: not yet logged / Taken at [time] / Missed]. Buttons: Taken, Skipped, Missed."
- Taken state: "[Medicine name]. Taken at [time]. Tap Undo to remove this log."
- Missed state: "[Medicine name]. Marked as missed. Tap Log now to log a dose."
- Adherence dots: "Monday: taken. Tuesday: skipped. Wednesday: not logged."
- Disclaimer banner: "Note: These reminders are based on prescription instructions, not medical advice."
- Streak: "[N]-day streak for [medicine name]."
- Configuration sheet: modal, full announcement of all fields and their current values.

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Reminder list per medicine | FR-035 | AC-062: Reminders shown per medicine |
| Dose logging (Taken/Skipped/Missed) | FR-035 | AC-063: Three log states available |
| Distinction: prescribed vs actual | FR-036 | AC-064: Prescribed instruction shown separately from log |
| Adherence overview (no medical judgement) | FR-036 | AC-065: Adherence shown without clinical language |
| Non-dismissible disclaimer | FR-035 | AC-062: Disclaimer always visible |
| Reminder configuration bottom sheet | FR-035 | AC-062: Time and frequency configurable |
| Past due section | FR-035 | AC-063: Past due doses visible and loggable |
