# Figma Design Tokens — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma file:** https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis  
**Figma page:** Page 2 — `01 — Design Tokens`

---

## Overview

These tokens are the single source of truth for all visual decisions. Every color, size, and shape used in the UI must trace to a token defined here.

**Implementation layers:**
1. These definitions → Figma local styles / variables
2. Figma → Android `MaterialTheme` in Compose (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`)
3. Compose `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*`

---

## 1. Color Tokens

### 1.1 Medical Trust Palette Rationale

MediHistory AI uses a **teal/blue primary** to evoke medical trust, clarity, and calm. Amber is reserved exclusively for low-confidence and warning states. Red/error is reserved for failures and destructive actions. Green is reserved for confirmed/high-confidence states only.

Color does not carry meaning alone. Every colored state also uses an icon and/or label.

### 1.2 Light Theme Color Tokens

| Token name | Figma style name | Hex (light) | Usage |
|---|---|---|---|
| `primary` | `color/primary` | `#006874` | Primary actions, FAB, key interactive elements |
| `on-primary` | `color/on-primary` | `#FFFFFF` | Text/icons on primary color |
| `primary-container` | `color/primary-container` | `#97F0FF` | Containers, selected chips |
| `on-primary-container` | `color/on-primary-container` | `#001F24` | Content inside primary-container |
| `secondary` | `color/secondary` | `#4A6267` | Secondary actions, doctor badge |
| `on-secondary` | `color/on-secondary` | `#FFFFFF` | Text/icons on secondary |
| `secondary-container` | `color/secondary-container` | `#CCE8ED` | Prescription card accents |
| `on-secondary-container` | `color/on-secondary-container` | `#051F23` | Content inside secondary-container |
| `tertiary` | `color/tertiary` | `#525E7D` | High-confidence badge, taken dose |
| `on-tertiary` | `color/on-tertiary` | `#FFFFFF` | Text/icons on tertiary |
| `tertiary-container` | `color/tertiary-container` | `#DAE2FF` | High-confidence container |
| `on-tertiary-container` | `color/on-tertiary-container` | `#0E1B37` | Content inside tertiary-container |
| `error` | `color/error` | `#BA1A1A` | Error states, missed dose, failed OCR |
| `on-error` | `color/on-error` | `#FFFFFF` | Text/icons on error |
| `error-container` | `color/error-container` | `#FFDAD6` | Error state backgrounds |
| `on-error-container` | `color/on-error-container` | `#410002` | Content inside error-container |
| `warning` | `color/warning` | `#7D5700` | Low-confidence, duplicate alerts, pending review |
| `warning-container` | `color/warning-container` | `#FFDEA8` | Warning state backgrounds |
| `on-warning-container` | `color/on-warning-container` | `#271900` | Content inside warning-container |
| `surface` | `color/surface` | `#F5FAFB` | Screen backgrounds |
| `on-surface` | `color/on-surface` | `#171D1E` | Primary text on surface |
| `surface-variant` | `color/surface-variant` | `#DBE4E6` | Card backgrounds, input fills |
| `on-surface-variant` | `color/on-surface-variant` | `#3F484A` | Secondary text, icons on surface-variant |
| `background` | `color/background` | `#F5FAFB` | Page background (same as surface) |
| `on-background` | `color/on-background` | `#171D1E` | Text on background |
| `outline` | `color/outline` | `#6F797A` | Borders, dividers, unrecorded dose |
| `outline-variant` | `color/outline-variant` | `#BFC8CA` | Subtle dividers |
| `scrim` | `color/scrim` | `#000000` | Modal scrim overlay (at 32% opacity) |
| `inverse-surface` | `color/inverse-surface` | `#2B3133` | Snackbar background |
| `inverse-on-surface` | `color/inverse-on-surface` | `#ECF2F3` | Text on snackbar |
| `inverse-primary` | `color/inverse-primary` | `#4BD3E5` | Links on dark surfaces |

### 1.3 Custom Semantic Tokens

These tokens are not part of the Material 3 baseline but are needed for MediHistory AI-specific states:

| Token name | Figma style name | Light hex | Usage |
|---|---|---|---|
| `confidence-high-container` | `color/confidence-high-container` | `#DAE2FF` | High-confidence badge background |
| `on-confidence-high-container` | `color/on-confidence-high-container` | `#0E1B37` | High-confidence badge text |
| `status-current` | `color/status-current` | `#006874` | "Current" medicine status chip |
| `status-past` | `color/status-past` | `#4A6267` | "Past" medicine status chip |
| `status-unknown` | `color/status-unknown` | `#6F797A` | "Unknown" medicine status chip |
| `source-link` | `color/source-link` | `#006874` | Source link chip tint |
| `ocr-success` | `color/ocr-success` | `#006874` | Extracted status chip |
| `ocr-pending` | `color/ocr-pending` | `#6F797A` | Pending status chip |
| `ocr-failed` | `color/ocr-failed` | `#BA1A1A` | Failed status chip |
| `ocr-review` | `color/ocr-review` | `#7D5700` | Needs review status chip |

### 1.4 Dark Theme Color Tokens

Dark theme uses Material 3 dynamic color. All tokens above have a dark-mode counterpart generated by the Material Theme Builder. Store both light and dark variants as Figma variable collections.

| Token name | Hex (dark) |
|---|---|
| `primary` | `#4BD3E5` |
| `on-primary` | `#00363D` |
| `primary-container` | `#004F58` |
| `on-primary-container` | `#97F0FF` |
| `secondary` | `#B0CBD1` |
| `on-secondary` | `#1B3439` |
| `secondary-container` | `#324B50` |
| `on-secondary-container` | `#CCE8ED` |
| `error` | `#FFB4AB` |
| `on-error` | `#690005` |
| `warning` | `#FFBA3B` |
| `warning-container` | `#5E4100` |
| `surface` | `#0E1415` |
| `on-surface` | `#DEE3E5` |
| `surface-variant` | `#3F484A` |
| `on-surface-variant` | `#BFC8CA` |
| `background` | `#0E1415` |
| `on-background` | `#DEE3E5` |
| `outline` | `#899294` |

### 1.5 Material 3 Dynamic Color Mapping

| Token | Android Compose reference |
|---|---|
| `primary` | `MaterialTheme.colorScheme.primary` |
| `on-primary` | `MaterialTheme.colorScheme.onPrimary` |
| `primary-container` | `MaterialTheme.colorScheme.primaryContainer` |
| `on-primary-container` | `MaterialTheme.colorScheme.onPrimaryContainer` |
| `secondary` | `MaterialTheme.colorScheme.secondary` |
| `on-secondary` | `MaterialTheme.colorScheme.onSecondary` |
| `surface` | `MaterialTheme.colorScheme.surface` |
| `on-surface` | `MaterialTheme.colorScheme.onSurface` |
| `surface-variant` | `MaterialTheme.colorScheme.surfaceVariant` |
| `on-surface-variant` | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `error` | `MaterialTheme.colorScheme.error` |
| `error-container` | `MaterialTheme.colorScheme.errorContainer` |
| `outline` | `MaterialTheme.colorScheme.outline` |
| `background` | `MaterialTheme.colorScheme.background` |
| `scrim` | `MaterialTheme.colorScheme.scrim` |

Custom semantic tokens are defined in `MediHistoryExtendedColors` and accessed via `LocalMediHistoryColors.current`.

---

## 2. Typography Tokens

Font family: **Roboto** (Android system default).  
Scale follows Material 3 type scale.

### 2.1 Type Scale Table

| Token name | Figma style | Size (sp) | Weight | Line height (sp) | Usage |
|---|---|---|---|---|---|
| `display-large` | `type/display-large` | 57 | Regular (400) | 64 | Not used in MVP |
| `display-medium` | `type/display-medium` | 45 | Regular (400) | 52 | Not used in MVP |
| `display-small` | `type/display-small` | 36 | Regular (400) | 44 | Not used in MVP |
| `headline-large` | `type/headline-large` | 32 | Regular (400) | 40 | Section headers on history screen |
| `headline-medium` | `type/headline-medium` | 28 | Regular (400) | 36 | Duplicate alert card title |
| `headline-small` | `type/headline-small` | 24 | Regular (400) | 32 | Dialog titles, screen section headings |
| `title-large` | `type/title-large` | 22 | Regular (400) | 28 | TopAppBar title, medicine name on detail |
| `title-medium` | `type/title-medium` | 16 | Medium (500) | 24 | Card titles (doctor name, medicine name) |
| `title-small` | `type/title-small` | 14 | Medium (500) | 20 | Sub-card titles |
| `body-large` | `type/body-large` | 16 | Regular (400) | 24 | Main body text on detail screens |
| `body-medium` | `type/body-medium` | 14 | Regular (400) | 20 | Card body text, supporting text |
| `body-small` | `type/body-small` | 12 | Regular (400) | 16 | Supplementary info, dates, specialty |
| `label-large` | `type/label-large` | 14 | Medium (500) | 20 | Prominent button labels |
| `label-medium` | `type/label-medium` | 12 | Medium (500) | 16 | Chip labels, tab labels, badge labels |
| `label-small` | `type/label-small` | 11 | Medium (500) | 16 | Doctor name on medicine card, footnotes |

### 2.2 Android Compose Reference

| Token | Compose reference |
|---|---|
| `headline-large` | `MaterialTheme.typography.headlineLarge` |
| `headline-medium` | `MaterialTheme.typography.headlineMedium` |
| `headline-small` | `MaterialTheme.typography.headlineSmall` |
| `title-large` | `MaterialTheme.typography.titleLarge` |
| `title-medium` | `MaterialTheme.typography.titleMedium` |
| `title-small` | `MaterialTheme.typography.titleSmall` |
| `body-large` | `MaterialTheme.typography.bodyLarge` |
| `body-medium` | `MaterialTheme.typography.bodyMedium` |
| `body-small` | `MaterialTheme.typography.bodySmall` |
| `label-large` | `MaterialTheme.typography.labelLarge` |
| `label-medium` | `MaterialTheme.typography.labelMedium` |
| `label-small` | `MaterialTheme.typography.labelSmall` |

### 2.3 Typography Notes for Medical Data

- **Active ingredient names** are rendered in `body-small`, italic style to visually distinguish from brand names
- **Confidence scores** (e.g. "78%") use `label-medium`, tabular figures (`fontFeatureSettings = "tnum"`)
- **Prescription dates** use `body-small` with `fontFeatureSettings = "tnum"` for alignment in lists
- Never use font sizes below 11 sp in the UI

---

## 3. Spacing Scale

All spacing is in **density-independent pixels (dp)**. Use multiples of 4 dp only.

| Token name | Value (dp) | Compose reference | Typical usage |
|---|---|---|---|
| `spacing-xxs` | 4 | `4.dp` | Icon padding, tight inner padding |
| `spacing-xs` | 8 | `8.dp` | Chip internal padding, list item leading icon gap |
| `spacing-sm` | 12 | `12.dp` | Card inner vertical padding |
| `spacing-md` | 16 | `16.dp` | Standard horizontal page margin, list item padding |
| `spacing-lg` | 20 | `20.dp` | Section top/bottom padding |
| `spacing-xl` | 24 | `24.dp` | Section spacing, dialog padding |
| `spacing-2xl` | 32 | `32.dp` | Large section gap, empty state vertical padding |
| `spacing-3xl` | 40 | `40.dp` | Above fold primary action areas |
| `spacing-4xl` | 48 | `48.dp` | Min touch target size (also a spacing-related constraint) |
| `spacing-5xl` | 64 | `64.dp` | Hero spacing, illustration gap |

**Standard page margins:** 16 dp left/right (matches Material 3 recommended margins for compact screens).  
**List item horizontal padding:** 16 dp.  
**Card internal padding:** 12 dp vertical, 16 dp horizontal.

---

## 4. Elevation Tokens

Material 3 uses tonal surface overlay percentages to communicate elevation in light theme. Shadows are used in dark theme.

| Token name | Level | Tonal overlay % | Shadow dp | Compose reference |
|---|---|---|---|---|
| `elevation/level0` | 0 | 0% | 0 | `CardDefaults.cardElevation(defaultElevation = 0.dp)` |
| `elevation/level1` | 1 | 5% | 1 | `CardDefaults.cardElevation(defaultElevation = 1.dp)` |
| `elevation/level2` | 2 | 8% | 3 | `CardDefaults.cardElevation(defaultElevation = 3.dp)` |
| `elevation/level3` | 3 | 11% | 6 | `CardDefaults.cardElevation(defaultElevation = 6.dp)` |
| `elevation/level4` | 4 | 12% | 8 | `CardDefaults.cardElevation(defaultElevation = 8.dp)` |
| `elevation/level5` | 5 | 14% | 12 | `CardDefaults.cardElevation(defaultElevation = 12.dp)` |

**Usage guidance:**
- Level 0: Outlined cards (MedicineCard default)
- Level 1: Elevated cards (PrescriptionCard, DuplicateAlertCard)
- Level 2: Top app bar when scrolled, navigation drawer
- Level 3: FAB resting state
- Level 4: FAB pressed state, modals
- Level 5: Navigation drawer header, dialogs

---

## 5. Shape Tokens

Material 3 shape scale. Corners are rounded using `RoundedCornerShape`.

| Token name | Figma style | Corner radius (dp) | Compose reference | Usage |
|---|---|---|---|---|
| `shape/extra-small` | `shape/extra-small` | 4 | `MaterialTheme.shapes.extraSmall` | Small chips, tooltips |
| `shape/small` | `shape/small` | 8 | `MaterialTheme.shapes.small` | Chips, text fields |
| `shape/medium` | `shape/medium` | 12 | `MaterialTheme.shapes.medium` | Cards (PrescriptionCard, MedicineCard) |
| `shape/large` | `shape/large` | 16 | `MaterialTheme.shapes.large` | Dialogs, bottom sheets |
| `shape/extra-large` | `shape/extra-large` | 28 | `MaterialTheme.shapes.extraLarge` | FAB |
| `shape/full` | `shape/full` | 50% | `CircleShape` | Icon buttons, badges, avatar |

### Shape Usage Map

| Component | Shape token |
|---|---|
| PrescriptionCard | `shape/medium` |
| MedicineCard | `shape/medium` |
| DuplicateAlertCard | `shape/medium` |
| ConfidenceBadge | `shape/full` |
| OcrStatusChip | `shape/small` |
| SourceLinkChip | `shape/small` |
| SafetyBanner | `shape/extra-small` |
| ConsentDialog | `shape/large` |
| FAB | `shape/extra-large` |
| Snackbar | `shape/extra-small` |
| Bottom navigation | `shape/full` (indicator) |

---

## 6. Touch Target and Density Constraints

| Constraint | Value | Notes |
|---|---|---|
| Min touch target | 48 × 48 dp | All interactive elements including small icon buttons |
| Recommended touch target | 48 × 48 dp or larger | Use padding to increase tap area without changing visual size |
| Min spacing between touch targets | 8 dp | Prevent mis-taps on adjacent interactive elements |
| Standard page horizontal margin | 16 dp | Match on all screens |
| Max content width (compact) | 328 dp (360 − 2 × 16) | Content inside page margins |

---

## 7. Motion and Animation Tokens

| Token | Value | Notes |
|---|---|---|
| `motion/duration-short` | 100 ms | Icon state change, fade |
| `motion/duration-medium` | 300 ms | Card expand/collapse, screen enter |
| `motion/duration-long` | 500 ms | Full-screen transitions |
| `motion/easing-standard` | `EmphasizedEasing` (cubic) | Standard screen transitions |
| `motion/easing-decelerate` | `EmphasizedDecelerateEasing` | Enter transitions |
| `motion/easing-accelerate` | `EmphasizedAccelerateEasing` | Exit transitions |

**Reduced motion:** When `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` is set or `Settings.Global.ANIMATOR_DURATION_SCALE = 0`, all animations should be disabled. Use `LocalReduceMotion` if available or check `animationDurationScale` at runtime.

---

## 8. Token File Locations in Codebase

| Token category | Android file |
|---|---|
| Color tokens | `app/src/main/java/com/medihistory/ui/theme/Color.kt` |
| Typography tokens | `app/src/main/java/com/medihistory/ui/theme/Type.kt` |
| Shape tokens | `app/src/main/java/com/medihistory/ui/theme/Shape.kt` |
| Custom semantic colors | `app/src/main/java/com/medihistory/ui/theme/ExtendedColors.kt` |
| Theme assembly | `app/src/main/java/com/medihistory/ui/theme/Theme.kt` |

---

## Related Documents

- [`figma-component-spec.md`](figma-component-spec.md)
- [`figma-accessibility.md`](figma-accessibility.md)
- [`design-system.md`](../design-system.md)
