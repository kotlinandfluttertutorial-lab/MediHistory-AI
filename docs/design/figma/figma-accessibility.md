# Figma Accessibility Specification — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Applies to:** All screens in `screen-inventory.md`

> **Important:** WCAG 2.1 AA compliance targets are defined here as design and development requirements. Full validation requires manual testing with assistive technologies (TalkBack, Switch Access, BrailleBack) and expert accessibility review. This document is not a compliance certification.

---

## 1. WCAG 2.1 AA Targets

MediHistory AI targets WCAG 2.1 Level AA throughout. The following criteria are most relevant to the app's medical data presentation.

| Criterion | Level | Requirement | How achieved |
|---|---|---|---|
| 1.1.1 Non-text Content | A | All non-decorative images have text alternatives | `contentDescription` on all images and icons |
| 1.3.1 Info and Relationships | A | Structure communicated via semantics, not appearance alone | `semantics { heading() }`, `Role.Button`, etc. |
| 1.3.2 Meaningful Sequence | A | Reading order matches visual order | Compose layout order = accessibility order |
| 1.3.3 Sensory Characteristics | A | Instructions don't rely on shape, color, or location alone | Labels accompany all colored states |
| 1.4.1 Use of Color | A | Color not the only visual means of conveying information | Icon + color + label on all state indicators |
| 1.4.3 Contrast (Minimum) | AA | Text ≥ 4.5:1, large text ≥ 3:1 | See Section 3 |
| 1.4.4 Resize Text | AA | Text resizable to 200% without loss of content | Tested with font scale 1.0 to 2.0 |
| 1.4.10 Reflow | AA | Content reflows at 320 CSS px (equivalent) | Scrollable single-column layout |
| 1.4.11 Non-text Contrast | AA | UI components ≥ 3:1 against adjacent colors | Border/icon contrast checked per component |
| 2.1.1 Keyboard | A | All functionality operable via directional input | Full D-pad / Switch Access support |
| 2.4.3 Focus Order | A | Focus order preserves meaning | See Section 5 per screen |
| 2.4.6 Headings and Labels | AA | Headings and labels describe purpose | `semantics { heading() }` on section titles |
| 2.5.3 Label in Name | A | Accessible name contains visible label | Button `contentDescription` includes visible text |
| 2.5.5 Target Size | AAA (targeted) | Touch targets ≥ 44 × 44 CSS px | 48 × 48 dp minimum (see Section 6) |
| 3.2.2 On Input | A | No unexpected context changes on focus | No auto-navigate on input field focus |
| 4.1.2 Name, Role, Value | A | All UI components have name, role, state | All interactive elements have `contentDescription` + `Role` |
| 4.1.3 Status Messages | AA | Status messages announced without focus | `LiveRegion` on error states, alerts, banners |

---

## 2. Android TalkBack Requirements Per Screen

### SCR-01-DASHBOARD

| Element | TalkBack announcement |
|---|---|
| Summary stat card (Prescriptions) | "Total prescriptions: [N]" |
| Summary stat card (Doctors) | "Doctors: [N]" |
| Summary stat card (Medicines) | "Total medicines: [N]" |
| Summary stat card (Alerts) | "Potential duplicate alerts: [N]" |
| Recent prescription card | "[Doctor name], [specialty], [date], [N] medicines, OCR status: [status]" |
| Duplicate alert banner | "Potential duplicate ingredient alert: [Medicine A] and [Medicine B] share the same active ingredient. Tap to review." |
| Add prescription FAB | "Add prescription" |
| Bottom navigation tab | "[Tab name], [selected/not selected], tab [N] of 5" |

### SCR-02-RX-LIST

| Element | TalkBack announcement |
|---|---|
| Prescription card | "[Doctor name], [specialty], [date], [N] medicines" |
| Doctor group header | "[Doctor name], [N] prescriptions" |
| Expand group button | "Expand [Doctor name] prescriptions" or "Collapse…" |
| FAB | "Add new prescription" |
| Filter chip | "[Filter name], [selected/not selected], filter" |

### SCR-02-RX-DETAIL

| Element | TalkBack announcement |
|---|---|
| Prescription image thumbnail | "Prescription image from [doctor name] dated [date]. Tap to view full size." |
| Doctor name | Heading: "[Doctor name]" |
| Medicine list section heading | Heading: "Medicines in this prescription" |
| Medicine item | "[Medicine name], [active ingredient], [strength], [dosage instructions]" |
| Source document link | "Source document: original prescription image. Tap to open." |
| OcrStatusChip | "OCR status: [status]" |

### SCR-03-OCR-REVIEW

| Element | TalkBack announcement |
|---|---|
| Document image | "Original prescription image. Tap to zoom." |
| Extracted field (editable) | "[Field name] field: [extracted value]. Confidence: [High/Medium/Low]. Double-tap to edit." |
| Low-confidence field | "[Field name] field: [extracted value]. Confidence: Low — manual review required. Double-tap to edit." |
| Empty field | "[Field name] field: not extracted. Double-tap to enter manually." |
| ConfidenceBadge | "Extraction confidence: [level]" (long-press for full explanation) |
| "Mark all as reviewed" button | "Mark all fields as reviewed" |
| OCR progress | (Live region) "Extracting text from prescription…" then "Extraction complete" or "Extraction failed" |

### SCR-04-MED-EXPLORER

| Element | TalkBack announcement |
|---|---|
| Search field | "Search medicines. [N] results shown." |
| Filter chip | "[Filter label], [on/off], filter button" |
| Medicine card | "[Medicine name], [active ingredient], [strength], prescribed by [doctor], [date], status: [current/past/unknown]" |
| Duplicate alert icon on card | "Potential duplicate ingredient alert" (announced as part of card, or separately if iconButton) |
| Pagination / load more | "Load more medicines, [N] of [total] shown" |

### SCR-04-MED-DETAIL

| Element | TalkBack announcement |
|---|---|
| Medicine name | Heading: "[Medicine name]" |
| Active ingredient | "Active ingredient: [name]" |
| Strength | "Strength: [value]" |
| Prescribed by section | Heading: "Prescribed by" |
| Doctor row | "[Doctor name], [date]" |
| Source link | "Source: prescription from [date]. Tap to open." |
| Duplicate warning | "Warning: potential duplicate ingredient detected. Tap to see details." |

### SCR-05-DUPLICATE-REVIEW

| Element | TalkBack announcement |
|---|---|
| Alert card (unreviewed) | "Potential duplicate ingredient alert. [Medicine A] and [Medicine B] may share the same active ingredient [name]. Strength: [A strength] vs [B strength]. Not yet reviewed." |
| Alert card (reviewed) | "Duplicate alert reviewed: [Medicine A] and [Medicine B]. Reviewed on [date]." |
| Alert card (dismissed) | "Dismissed alert: [Medicine A] and [Medicine B]." |
| "Mark as reviewed" button | "Mark [Medicine A] and [Medicine B] duplicate alert as reviewed. This does not mean the combination is medically safe." |
| Disclaimer text | Announced inline with the card — not skipped |
| Expand button | "Show full ingredient comparison for [Medicine A] and [Medicine B]" |

### SCR-06-HISTORY

| Element | TalkBack announcement |
|---|---|
| Summary section heading | Heading: "[section title]" |
| Source citation chip | "Source: [document type] from [date]. Tap to view original." |
| Confidence note | "Note: this information was extracted by AI and may contain errors. Review original documents." |
| "AI generated" label | "This section was AI-generated from your records." |
| Generate button | "Generate medical history summary" |

### SCR-07-NEW-DOCTOR

| Element | TalkBack announcement |
|---|---|
| Record selection checkbox | "Include [prescription/medicine name] in summary, [checked/unchecked]" |
| Select all toggle | "Select all records, [checked/unchecked]" |
| Consent toggle | "I confirm I want to share this summary with a new doctor, [on/off]" |
| Generate PDF button | "Generate PDF summary for sharing" (disabled until consent checked) |

### SCR-07-PDF-PREVIEW

| Element | TalkBack announcement |
|---|---|
| PDF preview | "Preview of medical summary PDF. [N] pages. Contains: [summary of included content]." |
| Share button | "Share medical summary PDF" |
| Selected records summary | "This PDF includes [N] prescriptions from [date range]" |

### SCR-08-REMINDERS

| Element | TalkBack announcement |
|---|---|
| Reminder row | "[Medicine name] reminder at [time], [active/snoozed/disabled]" |
| Dose log button | "[Medicine name], dose [taken/skipped/missed/not recorded]. Double-tap to record." |
| Toggle switch | "Enable reminder for [medicine name], [on/off]" |
| Add reminder FAB | "Add medication reminder" |

### SCR-09-SAFETY-CENTER

| Element | TalkBack announcement |
|---|---|
| Source unavailable banner | "Interaction checking is not available. No reference source is configured." |
| Interaction result card | "Potential interaction between [Medicine A] and [Medicine B]. Tap to see details." |
| Warning banner | "Warning: review with your doctor before making any changes to your medication." |
| Disclaimer | Announced inline — not skipped |

---

## 3. Minimum Contrast Ratios

All contrast ratios are measured against the background the element appears on.

| Text style | Minimum ratio | Target ratio | Check against |
|---|---|---|---|
| Body text on surface | 4.5:1 | ≥ 7:1 | `color/surface` |
| Body text on surface-variant | 4.5:1 | ≥ 5:1 | `color/surface-variant` |
| Large text (≥ 18 sp regular or 14 sp bold) | 3:1 | ≥ 4.5:1 | `color/surface` |
| Disabled text | ≥ 3:1 | — | Parent background |
| Placeholder text | 4.5:1 | — | Input field background |
| White text on primary | 4.5:1 | — | `color/primary` |
| White text on error | 4.5:1 | — | `color/error` |
| Black text on warning-container | 4.5:1 | — | `color/warning-container` |
| Icon on surface | 3:1 (non-text) | ≥ 4.5:1 | `color/surface` |
| Focus indicator | 3:1 | — | Adjacent colors |

**Critical medical data contrast requirements:**
- Confidence badges: the badge text must meet 4.5:1 against badge background
- OCR status chips: chip label must meet 4.5:1 against chip fill color
- Duplicate alert card amber background: body text on `color/warning-container` must meet 4.5:1

**Verification tools:**  
Use the Figma Contrast plugin (Stark or similar) on every Figma frame before marking as "Ready for Dev". Document the result in the design review notes.

---

## 4. Content Descriptions for All Icons and Images

### Icon Content Description Rules

1. **Decorative icons** (icons that accompany a visible text label): set `contentDescription = null`
2. **Standalone action icons** (icon buttons without visible labels): must have `contentDescription`
3. **Status icons** (confidence badge, OCR chip icon): `contentDescription` is set on the parent component, not the icon itself

### Icon Content Description Table

| Icon | Context | Content description |
|---|---|---|
| `dashboard` | Bottom nav tab | null (label is visible) |
| `description` | Bottom nav tab | null |
| `medication` | Bottom nav tab | null |
| `history_edu` | Bottom nav tab | null |
| `health_and_safety` | Bottom nav tab | null |
| `add` | FAB | "Add prescription" (contextual) |
| `search` | TopAppBar action | "Search" |
| `more_vert` | TopAppBar overflow | "More options" |
| `arrow_back` | TopAppBar nav icon | "Navigate back" |
| `check_circle` | ConfidenceBadge (High) | null (parent component has description) |
| `warning` | ConfidenceBadge (Medium/Low) | null |
| `error` | ConfidenceBadge (Low) / ErrorState | null / "Error" (contextual) |
| `open_in_new` | SourceLinkChip trailing | null (chip has full description) |
| `delete` | Swipe action reveal | "Delete [item name]" |
| `close` | Dismiss banner / dialog | "Dismiss" |
| `expand_more` | Card expand chevron | "Expand [card name] details" |
| `expand_less` | Card collapse | "Collapse [card name] details" |
| `camera_alt` | Prescription capture | "Take photo of prescription" |
| `photo_library` | Image picker | "Choose from gallery" |
| `share` | Share action | "Share" |
| `download` | Export action | "Export" |
| `person` | Patient-reported source | null (chip has description) |

### Prescription Image Content Descriptions

Original prescription images: `"Prescription from [doctor name] dated [date]. Tap to view full size."`  
If doctor name is not yet extracted: `"Prescription image. Tap to view full size."`  
Thumbnail in PrescriptionCard: `"Prescription thumbnail from [doctor name]"` or null if the card already announces this information.

---

## 5. Focus Order Per Screen

Focus order must follow a logical reading sequence (top → bottom, start → end in LTR layouts). Compose respects layout order for accessibility traversal by default; deviations must be explicitly defined.

### SCR-01-DASHBOARD Focus Order

1. Top app bar title ("Dashboard")
2. Summary stats (left to right: Prescriptions → Doctors → Medicines → Alerts)
3. Duplicate alert banner (if present) + "Review" action
4. "Recent prescriptions" section heading
5. Prescription cards (top to bottom)
6. "View all" link
7. Bottom navigation bar (Dashboard tab focused)

### SCR-03-OCR-REVIEW Focus Order

1. Top app bar back button + title
2. Document image ("Original prescription image")
3. "Extraction status" section heading
4. Extracted fields (top to bottom per field group):
   - Field label + extracted value + confidence badge
5. Low-confidence fields flagged for review (no special order change — keep top-to-bottom)
6. "Mark all as reviewed" button
7. "Confirm and save" button

### SCR-05-DUPLICATE-REVIEW Focus Order

1. Top app bar
2. Screen summary ("N potential duplicate alerts")
3. Alert cards (top to bottom):
   - Alert card header (announced as heading)
   - Medicine A row
   - Medicine B row
   - Active ingredient row
   - Disclaimer text
   - "Mark as reviewed" button
   - "View details" button
4. Bottom navigation bar

### SCR-07-NEW-DOCTOR Focus Order

1. Top app bar back + title
2. "Select records to include" heading
3. "Select all" toggle
4. Record selection list (each checkbox in order)
5. "Consent" section heading
6. Consent toggle
7. "Generate PDF" button (after consent given)

---

## 6. Touch Target Sizes

All interactive elements must meet a minimum tap target of **48 × 48 dp**.

When a visual element is smaller than 48 dp, add invisible padding to bring the tap area to 48 dp without changing the visual appearance.

| Element | Visual size | Touch target |
|---|---|---|
| Icon button (TopAppBar) | 24 × 24 dp icon | 48 × 48 dp (Compose default) |
| ConfidenceBadge (small) | 20 × 20 dp | 48 × 48 dp via padding |
| Toggle switch | 36 × 20 dp | 48 × 48 dp via padding |
| Navigation bar item | Full tab width × 48 dp | 48 dp height (Compose default) |
| DoseLogButton (compact) | 32 × 32 dp | 48 × 48 dp via padding |
| Checkbox | 20 × 20 dp | 48 × 48 dp via padding |
| SourceLinkChip | Variable × 32 dp | 48 dp height via padding |
| Swipe-to-dismiss area | Full card width | Full card height |

---

## 7. Color Not Sole Indicator Policy

No state, status, or alert may be communicated by color alone. Every colored state must also use at least one of: icon, label, text, shape change, or border change.

| State | Color | Icon | Label/Text |
|---|---|---|---|
| High confidence | Green tint | `check_circle` | "High" |
| Medium confidence | Amber tint | `warning` | "Medium" |
| Low confidence | Red tint | `error` | "Low" |
| OCR extracted | Teal chip | `check_circle` | "Extracted" |
| OCR failed | Red chip | `error` | "Extraction failed" |
| OCR needs review | Amber chip | `rate_review` | "Needs review" |
| Duplicate alert (unreviewed) | Amber card tint | `warning` | "Potential duplicate" |
| Duplicate alert (reviewed) | Neutral | `check_circle` | "Reviewed" |
| Duplicate alert (dismissed) | Muted | `cancel` | "Dismissed" |
| Dose taken | Green button | `check_circle` | "Taken" |
| Dose skipped | Blue button | `skip_next` | "Skipped" |
| Dose missed | Red button | `cancel` | "Missed" |
| Error state | Red icon | `error_outline` | Error title text |
| Offline banner | Amber banner | `wifi_off` | "You're offline" |

---

## 8. Semantic Headings in Compose

Section headings in Compose must use the `heading` semantic role so screen readers treat them as navigation landmarks.

```kotlin
Text(
    text = "Medicines in this prescription",
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier.semantics { heading() }
)
```

**Screens with required heading semantics:**

| Screen | Headings |
|---|---|
| SCR-02-RX-DETAIL | "Doctor information", "Medicines in this prescription", "Source document" |
| SCR-03-OCR-REVIEW | "Extracted information", "Doctor and clinic", "Medicines", "Fields needing review" |
| SCR-04-MED-DETAIL | Medicine name, "Prescribed by", "Dosage instructions", "Source prescriptions" |
| SCR-05-DUPLICATE-REVIEW | Each `DuplicateAlertCard` alert title |
| SCR-06-HISTORY | Each section heading (Conditions, Medicines, Allergies, etc.) |
| SCR-07-NEW-DOCTOR | "Select records", "Review content", "Consent" |
| SCR-09-SAFETY-CENTER | "Interaction check results" |

---

## 9. Screen Reader Announcement Patterns for Confidence Scores and Duplicate Alerts

### Confidence Score Announcements

Confidence scores must be announced in plain language, not as raw percentages.

| Backend confidence | Announced as |
|---|---|
| 0.85 – 1.00 | "Extraction confidence: High" |
| 0.60 – 0.84 | "Extraction confidence: Medium — please review" |
| 0.00 – 0.59 | "Extraction confidence: Low — manual review required" |
| Not available | "Extraction confidence: Not assessed" |

**Never announce** a raw percentage as the primary accessibility label (e.g. "87% confidence"). Raw percentages may appear visually if the design calls for it, but the accessible name must use the level label.

### Duplicate Alert Announcements

When a `DuplicateAlertCard` becomes visible (enters the screen or is loaded):

**Live region announcement (on appear):**  
`"Potential duplicate ingredient alert: [Medicine A] and [Medicine B] may share [Active Ingredient]. Tap to review."`

**When alert count badge updates on Safety tab:**  
`"[N] potential duplicate ingredient alerts"`

**After marking as reviewed:**  
`"Duplicate alert marked as reviewed. This does not mean the combination is medically safe."`

**After dismissing:**  
`"Alert dismissed. Tap Undo to restore."`

### OCR Status Change Announcements

All OCR status changes use a `LiveRegion` (`Modifier.semantics { liveRegion = LiveRegionMode.Polite }`):

| Status transition | Announced |
|---|---|
| → Pending | "Processing prescription…" |
| → PartiallyExtracted | "Prescription partially extracted. Review required for some fields." |
| → Extracted | "Prescription extracted successfully." |
| → Failed | "Extraction failed. Tap to retry." |
| → Reviewing | "[N] fields need review." |

---

## Related Documents

- [`figma-design-tokens.md`](figma-design-tokens.md) — Contrast-compliant token values
- [`figma-component-spec.md`](figma-component-spec.md) — Per-component accessibility requirements
- [`figma-interaction-spec.md`](figma-interaction-spec.md) — Focus order and gesture accessibility
- [`../screen-inventory.md`](../screen-inventory.md) — Screen list
