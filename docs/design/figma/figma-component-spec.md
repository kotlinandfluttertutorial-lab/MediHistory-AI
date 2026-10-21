# Figma Component Specification — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma file:** [FIGMA_URL_TBD]  
**Component library page:** Page 3 — `02 — Component Library`

---

## How to Read This Document

Each component entry contains:
- **Figma name** — path used in the Figma component library
- **Jetpack Compose mapping** — corresponding composable in the Android codebase
- **Variants / props** — all configurable dimensions
- **States** — UI states the component must support
- **Anatomy** — description of sub-elements (use as guide for Figma anatomy diagram)
- **Accessibility requirements** — TalkBack, contrast, and touch target requirements
- **Do / Do not** — usage guidance

---

## 1. NavigationBar

**Figma name:** `Organism/NavigationBar`  
**Compose mapping:** `MediHistoryBottomBar` (wraps `NavigationBar` from Material 3)

### Variants / Props

| Property | Values |
|---|---|
| `selected-tab` | `Dashboard`, `Prescriptions`, `Medicines`, `History`, `Safety` |
| `badge-count` | `none`, `number` (for alert count on Safety tab) |

### States

- Default (one tab selected, others unselected)
- With badge (Safety tab shows duplicate alert count)

### Anatomy

```
NavigationBar
  └─ NavigationBarItem × 5
       ├─ Icon (filled when selected, outlined when unselected)
       ├─ Label (type/label-medium)
       └─ BadgeBox (conditional, shows alert count)
```

### Accessibility

- Each tab has a content description: "[Tab name], [selected/not selected], [N of 5]"
- Badge reads as: "Safety, [N] alerts, tab 5 of 5"
- Touch target: minimum 48 × 48 dp per tab item
- Tab label must remain visible at all font sizes

### Compose Reference

```kotlin
NavigationBar {
    items.forEachIndexed { index, item ->
        NavigationBarItem(
            selected = selectedIndex == index,
            onClick = { onTabSelected(index) },
            icon = { Icon(item.icon, contentDescription = null) },
            label = { Text(item.label) }
        )
    }
}
```

---

## 2. TopAppBar

**Figma name:** `Organism/TopAppBar`  
**Compose mapping:** `MediHistoryTopBar` (wraps `TopAppBar` / `CenterAlignedTopAppBar` from Material 3)

### Variants / Props

| Property | Values |
|---|---|
| `variant` | `Default`, `WithBack`, `WithSearch`, `WithOverflow` |
| `title` | string |
| `subtitle` | string or `none` |
| `scroll-behavior` | `Pinned`, `EnterAlways`, `ExitUntilCollapsed` |

### States

- Default
- Search expanded (search field replaces title)
- Scrolled (with `EnterAlways`, bar shrinks on scroll)

### Anatomy

```
TopAppBar
  ├─ NavigationIcon (back arrow or menu icon, conditional)
  ├─ Title (type/title-large, centered or start-aligned)
  ├─ Subtitle (type/body-small, conditional)
  └─ Actions
       ├─ SearchIconButton (conditional)
       └─ OverflowMenuButton (conditional)
```

### Accessibility

- Navigation icon content description: "Navigate back" or "Open menu"
- Search icon: "Search"
- Overflow menu: "More options"

---

## 3. PrescriptionCard

**Figma name:** `Organism/PrescriptionCard`  
**Compose mapping:** `PrescriptionCard`

### Variants / Props

| Property | Values |
|---|---|
| `state` | `Default`, `Expanded`, `Loading` |
| `ocr-status` | `Extracted`, `Pending`, `Failed`, `Reviewing` |
| `has-duplicate-alert` | `true`, `false` |
| `confidence` | `High`, `Medium`, `Low` |

### States

- Default (collapsed, showing summary info)
- Expanded (shows medicine list preview)
- Loading (skeleton placeholder)

### Anatomy

```
PrescriptionCard (ElevatedCard, elevation/level1)
  ├─ ThumbnailImage (56 × 56 dp, rounded corner/small, conditional)
  ├─ Content
  │    ├─ DoctorName (type/title-medium)
  │    ├─ Specialty (type/body-small, color/on-surface-variant)
  │    ├─ Date (type/body-small)
  │    └─ MedicineCountLabel ("N medicines")
  ├─ OcrStatusChip (right-aligned)
  ├─ DuplicateAlertIcon (amber warning icon, conditional)
  └─ ExpandChevron (bottom center, conditional)
```

### Accessibility

- Card has role: `Role.Button` (if tappable)
- Content description: "[Doctor name], [specialty], [date], [N medicines], [OCR status]"
- Duplicate alert icon content description: "Potential duplicate ingredient detected"
- Loading skeleton: content description "Loading prescription"

### Do / Do not

- ✅ Show prescription date in relative format ("3 days ago") with absolute date in tooltip
- ❌ Never show full prescription text on the card — link to detail screen only

---

## 4. MedicineCard

**Figma name:** `Organism/MedicineCard`  
**Compose mapping:** `MedicineCard`

### Variants / Props

| Property | Values |
|---|---|
| `state` | `Default`, `WithDuplicateAlert`, `Loading` |
| `has-image` | `true`, `false` |
| `confidence` | `High`, `Medium`, `Low` |
| `status` | `Current`, `Past`, `Unknown` |

### States

- Default
- With duplicate alert (amber left border or icon)
- Loading skeleton

### Anatomy

```
MedicineCard (OutlinedCard)
  ├─ MedicineImage (40 × 40 dp, optional)
  ├─ Content
  │    ├─ MedicineName (type/title-medium)
  │    ├─ ActiveIngredient (type/body-small, italic)
  │    ├─ Strength (type/body-small)
  │    ├─ DoctorName (type/label-small)
  │    └─ PrescriptionDate (type/label-small)
  ├─ StatusChip (Current / Past / Unknown)
  ├─ ConfidenceBadge (right-aligned)
  └─ DuplicateAlertIcon (amber, conditional)
```

### Accessibility

- Content description: "[Medicine name], [active ingredient], [strength], prescribed by [doctor], [date], status: [current/past/unknown]"
- Duplicate alert: announced as "Potential duplicate ingredient alert"

---

## 5. DuplicateAlertCard

**Figma name:** `Organism/DuplicateAlertCard`  
**Compose mapping:** `DuplicateAlertCard`

### Variants / Props

| Property | Values |
|---|---|
| `review-state` | `Unreviewed`, `Reviewed`, `Dismissed` |
| `match-confidence` | `Exact`, `High`, `Moderate`, `Low` |
| `ingredient-count` | number (how many matching ingredients) |

### States

- Unreviewed (amber background tint, bold heading)
- Reviewed (neutral, checkmark indicator)
- Dismissed (muted, crossed-out styling)
- Expanded (shows full ingredient comparison table)

### Anatomy

```
DuplicateAlertCard (ElevatedCard)
  ├─ AlertHeader
  │    ├─ WarningIcon (amber, content desc: "Potential duplicate")
  │    ├─ AlertTitle (type/title-medium): "Potential duplicate ingredient"
  │    └─ ReviewStateBadge
  ├─ MedicineARow: Medicine A name, strength, doctor, date
  ├─ MedicineBRow: Medicine B name, strength, doctor, date
  ├─ ActiveIngredientRow: matched ingredient name
  ├─ SourceLinks: source prescription links for both
  ├─ ReviewDisclaimer (type/body-small): "Marking as reviewed is not medical clearance"
  └─ ActionRow
       ├─ "Mark as reviewed" button (TextButton)
       └─ "View details" button (FilledTonalButton)
```

### Accessibility

- Alert heading has `heading` semantic role
- Disclaimer text must meet 4.5:1 contrast ratio
- Review action button content description includes medicine names
- Do not use color alone to indicate reviewed vs unreviewed — also use icon

---

## 6. ConfidenceBadge

**Figma name:** `Molecule/ConfidenceBadge`  
**Compose mapping:** `ConfidenceBadge`

### Variants / Props

| Property | Values |
|---|---|
| `level` | `High`, `Medium`, `Low`, `Unreviewed` |
| `size` | `Small`, `Medium` |
| `show-label` | `true`, `false` |

### States

Single display state (no interactive states). Tooltip shown on long-press.

### Anatomy

```
ConfidenceBadge
  ├─ Icon (check_circle / warning / error / help)
  └─ Label (type/label-small, conditional): "High" / "Medium" / "Low" / "Unreviewed"
```

### Color Mapping

| Level | Icon | Background token | Foreground token |
|---|---|---|---|
| High | `check_circle` | `color/confidence-high-container` | `color/on-confidence-high-container` |
| Medium | `warning` | `color/warning-container` | `color/on-warning-container` |
| Low | `error` | `color/error-container` | `color/on-error-container` |
| Unreviewed | `help` | `color/surface-variant` | `color/on-surface-variant` |

### Accessibility

- Content description: "Extraction confidence: [High/Medium/Low/Unreviewed]"
- Must not use color alone — icon is required at all sizes
- Long-press tooltip explains what confidence means

---

## 7. OcrStatusChip

**Figma name:** `Molecule/OcrStatusChip`  
**Compose mapping:** `OcrStatusChip`

### Variants / Props

| Property | Values |
|---|---|
| `status` | `Extracted`, `Pending`, `Failed`, `Reviewing`, `PartiallyExtracted` |

### State Machine

```
Pending → Extracted
Pending → Failed
Pending → PartiallyExtracted
PartiallyExtracted → Reviewing
Reviewing → Extracted
```

### Anatomy

```
OcrStatusChip (AssistChip or SuggestionChip)
  ├─ LeadingIcon (status-specific icon)
  └─ Label (type/label-medium)
```

| Status | Icon | Label text | Color token |
|---|---|---|---|
| Extracted | `check_circle` | "Extracted" | `color/tertiary` |
| Pending | `hourglass_empty` | "Processing…" | `color/outline` |
| Failed | `error` | "Extraction failed" | `color/error` |
| Reviewing | `rate_review` | "Needs review" | `color/warning` |
| PartiallyExtracted | `warning` | "Partially extracted" | `color/warning` |

### Accessibility

- Content description: "OCR status: [status label]"
- Interactive chip (tappable to view OCR detail): "OCR status: [status]. Tap to review."

---

## 8. SourceLinkChip

**Figma name:** `Molecule/SourceLinkChip`  
**Compose mapping:** `SourceLinkChip`

### Variants / Props

| Property | Values |
|---|---|
| `source-type` | `Prescription`, `LabReport`, `PatientReported` |
| `state` | `Default`, `Pressed`, `Focused` |

### Anatomy

```
SourceLinkChip (AssistChip)
  ├─ LeadingIcon (description / biotech / person)
  ├─ Label: "[Document type] — [date]"
  └─ TrailingIcon: `open_in_new` (indicates navigation)
```

### Accessibility

- Content description: "Source: [document type] from [date]. Tap to open original document."
- The trailing icon does not need its own content description (it is decorative within the chip)

---

## 9. SafetyBanner

**Figma name:** `Molecule/SafetyBanner`  
**Compose mapping:** `SafetyBanner`

### Variants / Props

| Property | Values |
|---|---|
| `severity` | `Warning`, `Info`, `Error` |
| `dismissible` | `true`, `false` |
| `has-action` | `true`, `false` |

### Anatomy

```
SafetyBanner (Surface, full-width)
  ├─ LeadingIcon (warning / info / error)
  ├─ MessageText (type/body-medium)
  ├─ ActionButton (TextButton, conditional)
  └─ DismissButton (IconButton, conditional)
```

### Accessibility

- Role: `LiveRegion` — screen reader announces banner when it appears
- Content description: "[Severity]: [message text]"
- Dismiss button: "Dismiss [severity] message"

---

## 10. ConsentDialog

**Figma name:** `Organism/ConsentDialog`  
**Compose mapping:** `ConsentDialog`

### Variants / Props

| Property | Values |
|---|---|
| `consent-type` | `Share`, `Export`, `DeleteRecord`, `DeleteAllData`, `CloudBackupEnable` |
| `state` | `Idle`, `Confirming`, `Processing` |

### Anatomy

```
ConsentDialog (AlertDialog)
  ├─ Icon (share / download / delete, color/on-surface)
  ├─ Title (type/headline-small)
  ├─ SupportingText (type/body-medium, explains what will happen)
  ├─ SelectedRecordsSummary (conditional, for Share/Export)
  ├─ PrivacyNote (type/body-small, italic, color/on-surface-variant)
  ├─ DismissButton: "Cancel" (TextButton)
  └─ ConfirmButton: action-specific label (FilledButton or FilledTonalButton)
```

### Accessibility

- Dialog announces its title when opened
- Focus is trapped within the dialog
- Cancel button is always the first focusable element after title
- Destructive confirm button (delete) uses `color/error` tint
- Confirm button content description includes what will happen: "Share [N] records with [recipient]"

### Do / Do not

- ✅ List which records are included before confirming a share or export
- ❌ Never auto-dismiss the consent dialog
- ❌ Never default to "confirm" on destructive actions

---

## 11. LoadingSkeleton

**Figma name:** `Organism/LoadingSkeleton`  
**Compose mapping:** `LoadingSkeletonView` (uses shimmer effect)

### Variants / Props

| Property | Values |
|---|---|
| `type` | `CardSkeleton`, `ListSkeleton`, `DetailSkeleton` |
| `item-count` | 1–5 (for list skeleton) |

### Anatomy

Each skeleton variant mirrors the shape of the real component it replaces, using `color/surface-variant` fill with shimmer animation.

### Accessibility

- Content description on the skeleton container: "Loading [screen name]"
- Individual skeleton items should not have separate content descriptions

---

## 12. EmptyState

**Figma name:** `Organism/EmptyState`  
**Compose mapping:** `EmptyStateView`

### Variants / Props

| Property | Values |
|---|---|
| `context` | `NoPrescriptions`, `NoMedicines`, `NoAlerts`, `NoReminders`, `NoHistory`, `SearchNoResults` |
| `has-action` | `true`, `false` |

### Anatomy

```
EmptyState (Column, centered)
  ├─ Illustration (SVG or vector drawable, 120 × 120 dp)
  ├─ Title (type/title-large)
  ├─ SupportingText (type/body-medium, color/on-surface-variant)
  └─ ActionButton (FilledButton, conditional)
```

### Accessibility

- Illustration: content description matching the empty context (or `null` if purely decorative when title is self-explanatory)
- Title has `heading` semantic role

---

## 13. ErrorState

**Figma name:** `Organism/ErrorState`  
**Compose mapping:** `ErrorStateView`

### Variants / Props

| Property | Values |
|---|---|
| `error-type` | `NetworkError`, `OcrFailed`, `SyncFailed`, `GenericError` |
| `has-retry` | `true`, `false` |

### Anatomy

```
ErrorState (Column, centered)
  ├─ ErrorIcon (error_outline, color/error, 48 × 48 dp)
  ├─ Title (type/title-large, color/error)
  ├─ SupportingText (type/body-medium)
  └─ RetryButton (OutlinedButton, conditional): "Try again"
```

### Accessibility

- Container has `LiveRegion` so the error is announced when it replaces content
- Retry button: "Retry [action name]"

---

## 14. DoseLogButton

**Figma name:** `Molecule/DoseLogButton`  
**Compose mapping:** `DoseLogButton`

### Variants / Props

| Property | Values |
|---|---|
| `state` | `Unrecorded`, `Taken`, `Skipped`, `Missed` |
| `size` | `Default`, `Compact` |

### Anatomy

```
DoseLogButton (FilledTonalButton or IconButton depending on size)
  ├─ Icon (radio_button_unchecked / check_circle / skip_next / cancel)
  └─ Label (type/label-medium, conditional in compact size)
```

| State | Icon | Color token |
|---|---|---|
| Unrecorded | `radio_button_unchecked` | `color/outline` |
| Taken | `check_circle` | `color/tertiary` |
| Skipped | `skip_next` | `color/secondary` |
| Missed | `cancel` | `color/error` |

### Accessibility

- Content description: "[Medicine name], dose [state]: [Taken/Skipped/Missed/Not recorded]. Tap to record."
- State change is announced by screen reader

---

## 15. ReminderRow

**Figma name:** `Molecule/ReminderRow`  
**Compose mapping:** `ReminderRow`

### Variants / Props

| Property | Values |
|---|---|
| `state` | `Active`, `Snoozed`, `Disabled` |
| `has-dose-log` | `true`, `false` |

### Anatomy

```
ReminderRow (ListItem)
  ├─ TimeLabel (type/title-medium): "08:00"
  ├─ MedicineName (type/body-medium)
  ├─ DoseInstructions (type/body-small, color/on-surface-variant)
  ├─ DoseLogButton (compact, conditional)
  └─ ToggleSwitch (enable/disable reminder)
```

### Accessibility

- Row content description: "[Medicine name] reminder at [time], [active/snoozed/disabled]"
- Toggle switch: "Enable reminder for [medicine name]"
- DoseLogButton follows its own content description rules

---

## Component Update Process

When a component spec changes:

1. Update this document (the entry for that component)
2. Update the Figma component in Page 3 — Component Library
3. Publish the Figma library update
4. Notify developers of the change via PR description
5. Update the Compose composable to match
6. Bump the version comment in this file

All components are versioned together with this document. The document version must be incremented on any change.
