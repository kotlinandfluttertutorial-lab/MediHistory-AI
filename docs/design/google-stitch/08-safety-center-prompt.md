# 08 — Safety Center Screen Prompt for Google Stitch

> **Pre-requisite:** Paste `00-master-stitch-prompt.md` first in this Google Stitch session, then paste this prompt.

---

## Screen Prompt

Design the **Safety Center** screen for MediHistory AI — a native Android application. This screen combines two sub-features: Medication Interaction Checks (F09) and Privacy and Security Controls (F10).

**Screen ID:** SCR-09-SAFETY-CENTER
**Features:** F09 — Medication Interaction Checks, F10 — Privacy and Security
**Linked requirements:** FR-037, FR-038, FR-039, FR-040
**Acceptance criteria:** AC-066, AC-067, AC-068, AC-069, AC-070, AC-071, AC-072, AC-073, AC-074, AC-075

---

## Screen Purpose

**Interaction checks:** Provide an optional, conservative tool for checking medicine interactions only when a licensed and configured reference source is available. When unavailable, say so clearly — never imply safety from the absence of a result.

**Privacy controls:** Give the patient clear, accessible controls over their data — export, deletion, cloud backup, consent history, and offline mode. These are not buried settings; they are first-class features.

---

## Device and Layout Context

- Phone portrait, reference width: **360dp**
- Accessed from: "More" tab → Safety Center.
- `MediumTopAppBar` — collapses on scroll.
- Two scrollable sections on one screen (tabbed layout within the screen using M3 `TabRow`).

---

## Top App Bar

- Title: "Safety Center" (`headlineMedium`)
- Leading: back arrow (returns to More/Settings screen)
- Trailing: overflow ⋮ (options: Help, About data handling)

---

## Tab Row

Two tabs below the top app bar (M3 `TabRow`):

```
  [  Interaction Check  |  Privacy & Security  ]
```

- "Interaction Check" tab selected by default.
- Each tab: `Tab` with `bodyLarge` label.
- Animated underline indicator.

---

## Tab 1: Interaction Check

### State A: Reference Source Not Configured

This is the default state for a fresh install or when no medicine reference has been configured.

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  [Illustration: disconnected plug / unavailable symbol]      │
│                                                              │
│  Interaction checking not available                          │  (headlineMedium)
│                                                              │
│  No medicine reference source is configured.                 │  (bodyMedium)
│  Interaction checks require a licensed, maintained medicine  │
│  reference source.                                           │
│                                                              │
│  [  Configure reference source  ]  (OutlinedButton)         │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

Below the card, a prominent disclaimer:

```
┌──────────────────────────────────────────────────────────────┐
│ ⚠  Important                                                 │
│    The absence of an interaction check result does NOT       │
│    mean a medicine combination is safe. Always consult       │
│    your doctor or pharmacist about your medicines.           │
└──────────────────────────────────────────────────────────────┘
```

`tertiary-container` (amber) background, ⚠ icon, `bodyMedium` text. **This disclaimer is always visible regardless of check state.**

### State B: Reference Source Configured — Idle

When a medicine reference source is configured, show the check interface:

```
  Reference source: [MedEx India v2.3]  ● Active  [Change]
  Last updated: 15 Jul 2025
```

`bodySmall`, source name in `AssistChip`.

**Medicine Selector:**

```
  Select medicines to check:

  Medicine 1:  [▾  Select medicine…  ]   (ExposedDropdownMenu)
  Medicine 2:  [▾  Select medicine…  ]   (ExposedDropdownMenu)
               [+ Add a third medicine]   (TextButton)

  [        Check interactions        ]   (FilledButton, disabled until ≥2 selected)
```

- Dropdown menus list Priya's 12 medicines.
- Up to 5 medicines can be added ("+  Add another" appears up to the limit).
- "Check interactions" button: `FilledButton`, disabled until at least 2 medicines selected.

**Pre-check disclaimer** (always shown below the button):

```
  ℹ  This check uses [source name] data. Results are         │
     informational only. Consult your doctor or pharmacist   │
     before making any changes to your medicines.            │
```

`secondary-container` background, ℹ icon, `bodySmall` text.

### State C: Checking (in progress)

```
  ⏳  Checking interactions…
  [────────────────────────────]  (indeterminate LinearProgressIndicator)
  Betaloc ZOK + Ecosprin
```

`bodyMedium` text, progress bar, medicine names listed.

### State D: Result — Interactions Found

```
┌──────────────────────────────────────────────────────────────┐
│  Interaction check result                                    │
│  Betaloc ZOK (Metoprolol) + Ecosprin (Aspirin)               │
│  Checked: 15 Jul 2025, 10:32 AM                              │
│  Reference: MedEx India v2.3                                 │
│                                                              │
│  ⚠  Interaction information found                           │
│  ─────────────────────────────────────────────              │
│  • Metoprolol + Aspirin: [summary from reference source]    │
│    Source: MedEx India                    [📎 View source]  │
│                                                              │
│  ─────────────────────────────────────────────              │
│  ⚠  Review with your doctor or pharmacist.                  │
│     MediHistory AI does not assess clinical significance.    │
│                                                              │
│  [  Check different medicines  ]  (OutlinedButton)           │
└──────────────────────────────────────────────────────────────┘
```

- Result card: `ElevatedCard`.
- ⚠ banner above result text: `tertiary-container` (amber).
- Result text: `bodyMedium`, from the reference source (not AI-generated).
- Source attribution: always shown — reference source name + `AssistChip` link to source.
- Bottom disclaimer always present.
- "View source": opens web view or external link to reference source.

### State E: Result — No Interactions Found in Reference

```
┌──────────────────────────────────────────────────────────────┐
│  Interaction check result                                    │
│  Betaloc ZOK + Shelcal                                       │
│  Checked: 15 Jul 2025, 10:35 AM                              │
│  Reference: MedEx India v2.3                                 │
│                                                              │
│  ℹ  No interactions found in the reference database          │
│  ─────────────────────────────────────────────              │
│  No interaction data was found for this combination in       │
│  the configured reference source.                            │
│                                                              │
│  ⚠  This does NOT mean the combination is safe.             │
│     Reference databases may not be complete. Consult        │
│     your doctor or pharmacist.                               │
│                                                              │
│  [  Check different medicines  ]  (OutlinedButton)           │
└──────────────────────────────────────────────────────────────┘
```

Critical: the "not safe" disclaimer is shown prominently even when no interactions are found. This is non-negotiable.

### State F: Check Failed / Reference Unavailable

```
┌──────────────────────────────────────────────────────────────┐
│  ✗  Interaction check could not be completed                 │
│     The reference source is currently unavailable.           │
│                                                              │
│     [  Retry  ]   (OutlinedButton)                           │
│                                                              │
│  ⚠  The absence of a check result does not mean             │
│     the combination is safe.                                 │
└──────────────────────────────────────────────────────────────┘
```

---

## Tab 2: Privacy and Security

### Sub-section 1: Data and Export

`ElevatedCard` — "Your data":

```
  📤  Export my medical records
      Download all your data as a structured file (JSON/PDF)
      [Export data]  (OutlinedButton)

  ─────────────────────────────────────────────
  🗑  Delete my account and all data
      Permanently removes your account and all records.
      This cannot be undone.
      [Delete account]  (OutlinedButton, error colour border)
```

- "Export data": triggers an export flow (format selection bottom sheet: JSON, PDF summary, PDF prescriptions).
- "Delete account": `OutlinedButton` with `error` colour border — tap opens a multi-step confirmation dialog (see below).

### Deletion Confirmation Dialog (multi-step)

Step 1:

```
  Delete your account?
  ─────────────────────────────────────────────
  This will permanently delete:
  • All prescriptions and extracted data
  • Your medicine list and history
  • All reminders and logs
  • Your account and login credentials

  Cloud backup: If you have cloud backup enabled, cloud
  copies will also be deleted. This may take up to 30 days.

  [Cancel]   [Continue →]
```

Step 2 (confirmation):

```
  Are you sure?
  ─────────────────────────────────────────────
  Type "DELETE" to confirm permanent deletion.

  [Text field: type DELETE here]

  [Cancel]   [Delete permanently]  (FilledButton, error colour)
```

"Delete permanently" only enabled when "DELETE" is typed correctly. `error` colour button.

### Sub-section 2: Cloud Backup

`ElevatedCard` — "Cloud backup":

```
  ☁  Cloud backup

  [Toggle: OFF]  Cloud backup is off

  When enabled, your records are encrypted and backed up
  to cloud storage. You can restore from backup if you
  reinstall the app.

  ─ Backup status ─────────────────────────────────────

  Status:      Not configured (backup is off)

  ─ Important ─────────────────────────────────────────
  ℹ  Enabling cloud backup stores your health records
     outside your device. Review our privacy policy
     before enabling.
     [View privacy policy →]  (TextButton)
```

**When cloud backup is ON:**

```
  [Toggle: ON]  Cloud backup is on

  Status:         ✓ Last backup: 15 Jul 2025, 9:00 AM
  Storage used:   12.4 MB of 1 GB
  [Backup now]  [Restore from backup]  (OutlinedButtons)

  ⚠  Deletion note: If you delete your account, cloud
     backups are scheduled for deletion and may remain
     for up to 30 days.
```

Toggle is a `Switch` M3 component. Turning ON shows a confirmation dialog explaining what gets backed up before enabling.

### Sub-section 3: Consent and Sharing History

`ElevatedCard` — "Sharing history":

```
  📋  Sharing history

  • 14 Jul 2025 — Medical summary shared via [app name]
    4 conditions · 12 medicines · 3 prescriptions
    [View details]

  • No other sharing events recorded.
```

`bodyMedium` entries. "View details" `TextButton` expands a detail bottom sheet with the exact content that was shared.

A note: "This list shows summaries you shared from MediHistory AI. It does not track what the recipient did with the file." (`bodySmall`, `outline`).

### Sub-section 4: Offline Mode Indicator

`OutlinedCard` — "Offline access":

```
  📱  Offline mode

  Status:  ✓ Your records are available offline
           Last synced: 15 Jul 2025, 9:00 AM

  All your prescriptions and medicine data are stored
  locally on your device and accessible without
  an internet connection.
```

If not synced recently:
```
  Status: ⚠  Sync required — last synced 5 days ago
  [Sync now]  (OutlinedButton)
```

---

## Screen States

### Interaction Check Tab States

Already specified above as State A–F.

### Privacy Tab States

- Default (backup off, no sharing history): as shown above.
- Cloud backup on and synced: green status, last backup time, storage used.
- Cloud backup on and failed: `error-container` banner: "Last backup failed — [date]. Try syncing manually."
- Deletion in progress: loading state after "Delete permanently" confirmed.

---

## Accessibility Specifications

- Tab row: "Tab 1 of 2: Interaction Check. Tab 2 of 2: Privacy and Security."
- Medicine dropdowns: "[label]: Select medicine, [current selection or none]. Double tap to open list."
- "Check interactions" button (disabled): "Check interactions. Disabled. Select at least 2 medicines."
- Result disclaimer: always read aloud — "Warning: This does not mean the combination is safe."
- "No interactions found" disclaimer: read after result — "This does not confirm the combination is safe. Consult your doctor."
- Cloud backup toggle: "Cloud backup, currently [on/off]. Double tap to toggle."
- Export button: "Export all medical records as a downloadable file."
- Delete button: "Delete account. This action is permanent and cannot be undone."
- Deletion type field: "Type DELETE to confirm. Currently: [current input]."

---

## Feature and Requirement Traceability

| Element | Requirement | Acceptance Criteria |
|---|---|---|
| Interaction check unavailable state | FR-037 | AC-066: Unavailable message shown when no source configured |
| Interaction check result with source | FR-037 | AC-067: Source name and link shown on result |
| "No result ≠ safe" disclaimer | FR-037 | AC-068: Disclaimer shown on all result states |
| Check failed / unavailable message | FR-037 | AC-069: Clear failure message, no safety implied |
| Data export flow | FR-038 | AC-070: Export triggers format selection and download |
| Account deletion with confirmation | FR-038 | AC-071: Multi-step deletion with typed confirmation |
| Cloud backup toggle with status | FR-039 | AC-072: Backup status visible, opt-in, failure shown |
| Cloud backup retention disclaimer | FR-039 | AC-073: 30-day retention note shown on deletion |
| Consent/sharing history list | FR-040 | AC-074: Sharing events listed with date and content summary |
| Offline mode indicator | FR-040 | AC-075: Offline status visible with last sync time |
