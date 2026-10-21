# Design System — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Platform:** Android — Jetpack Compose / Material 3  
**Figma file:** [FIGMA_URL_TBD]

---

## 1. Material 3 Adoption Rationale

MediHistory AI uses **Material 3 (Material You)** as its design system foundation. The reasons:

- **Native Android feel:** Material 3 is the current design language for Android, making the app feel at home on the platform.
- **Dynamic Color:** Material 3's dynamic color system lets the app adapt to the user's wallpaper-based color scheme while the app still maintains its medical trust palette as the baseline.
- **Accessibility built-in:** Material 3 components are designed with accessibility in mind — correct touch targets, contrast, focus handling, and TalkBack support come from the library.
- **Compose component library:** The `material3` Compose library provides all foundation components (cards, buttons, chips, navigation bars, dialogs) so the team does not build primitives from scratch.
- **Theming flexibility:** Tokens (color scheme, typography, shapes) are structured and overridable, making it straightforward to apply the medical trust palette.

**What we do not change:**
- Component shapes follow the standard Material 3 shape scale (no custom rounded shapes beyond what Material 3 provides)
- Motion follows Material 3 motion specs (no custom animation frameworks)
- Typography uses Roboto (Android system font) — no custom typeface is introduced

---

## 2. Color Scheme Strategy

### 2.1 Medical Trust Palette

Medical applications must feel trustworthy, calm, and clear. The color strategy:

| Role | Choice | Reason |
|---|---|---|
| Primary | Teal/blue (`#006874`) | Healthcare associations, calm, trust |
| Secondary | Desaturated teal (`#4A6267`) | Supporting actions, complementary |
| Tertiary | Blue-grey (`#525E7D`) | High-confidence states, proven |
| Warning (amber) | `#7D5700` / container `#FFDEA8` | Low confidence, duplicate alerts — unambiguous caution color |
| Error (red) | `#BA1A1A` | Failures only — never for warnings |

**Amber is reserved exclusively for:**
- Low-confidence OCR extractions
- Duplicate ingredient alerts (unreviewed)
- Offline/sync warning banners
- OCR status "Needs review"

**Red (error) is used exclusively for:**
- Extraction failures
- Missed doses
- Destructive actions (delete)
- Error states

This strict semantic separation ensures patients immediately understand the severity of any alert.

### 2.2 Dark Mode Support

Dark mode is supported from launch. Material 3 dynamic color generates dark-theme counterparts automatically. The app must:

- Respect the system dark mode setting via `isSystemInDarkTheme()`
- Provide a manual dark/light toggle in Settings (user preference overrides system setting)
- Store the user preference in `DataStore`
- Test dark mode for all screens before "Ready for Dev"

Dark mode token values are defined in `figma-design-tokens.md` Section 1.4.

### 2.3 Dynamic Color (Material You)

When running on Android 12+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`), the app applies dynamic color from the system (`dynamicLightColorScheme()` / `dynamicDarkColorScheme()`). The medical trust palette is used as fallback on Android 11 and below.

**Limitation:** Dynamic color may shift the amber warning tokens. The extended color tokens for `warning`, `warning-container`, and the semantic confidence tokens are not overridden by dynamic color — they are always sourced from `MediHistoryExtendedColors`.

---

## 3. Typography Choices for Medical Data Readability

### 3.1 Font Family

**Roboto** (Android system default). No custom typeface is loaded.

**Why Roboto:**
- Zero APK size increase
- Fully tested for accessibility and legibility on Android
- Familiar to Android users, reducing cognitive load in a medical context
- Available in all required weights (Regular 400, Medium 500)

### 3.2 Medical Data Typography Guidelines

Medical data often involves: long medicine names, dosage numbers, active ingredient names (often Latin), and numerical values.

| Data type | Style | Notes |
|---|---|---|
| Medicine name (primary) | `title-medium` | Clear, prominent |
| Active ingredient | `body-small`, italic style | Visually separated from brand name |
| Dosage / strength | `body-medium` | Standard legibility |
| Numerical values in tables | `body-medium` with `fontFeatureSettings = "tnum"` | Tabular figures for alignment |
| Prescription dates in lists | `label-small` with `fontFeatureSettings = "tnum"` | Alignment in list rows |
| Confidence percentages | `label-medium` with `tnum` | Consistent digit width |
| AI-generated content label | `label-small`, uppercase, letter-spaced | Clear visual separation from factual data |
| Doctor-ready summary body | `body-large` | Larger for readability in the history screen |

### 3.3 Dynamic Font Size Support

The app must support Android's font size accessibility setting (font scale 0.85× to 2.0×). All text must be defined in `sp` (scalable pixels) — never `dp` for text.

Test the following screens at font scale 2.0 before "Ready for Dev":
- SCR-03-OCR-REVIEW (many fields that may overflow)
- SCR-05-DUPLICATE-REVIEW (ingredient comparison table)
- SCR-06-HISTORY (long text summary)

---

## 4. Component Library Overview

The component library is organized in three tiers. Full specifications are in `figma/figma-component-spec.md`.

### 4.1 Atoms

Primitive visual building blocks. No business logic.

| Atom | Description |
|---|---|
| Color swatch | Token reference display |
| Type ramp specimen | Typography style display |
| Icon | Individual Material Symbol usage |
| Divider | Horizontal content separator |
| Badge | Notification count dot |

### 4.2 Molecules

Composed from atoms. Carry specific meaning in the MediHistory context.

| Molecule | Purpose |
|---|---|
| `ConfidenceBadge` | Shows OCR extraction confidence level (High/Medium/Low/Unreviewed) |
| `OcrStatusChip` | Shows OCR processing status with state machine transitions |
| `SourceLinkChip` | Links extracted data back to the originating document |
| `DoseLogButton` | Records patient-reported dose (Taken/Skipped/Missed) |
| `ReminderRow` | Displays a single medication reminder with toggle |
| `SafetyBanner` | Full-width informational, warning, or error banner |

### 4.3 Organisms

Composed from molecules. Represent complete UI units.

| Organism | Purpose |
|---|---|
| `PrescriptionCard` | Summary card for a single prescription |
| `MedicineCard` | Summary card for a single medicine |
| `DuplicateAlertCard` | Full duplicate ingredient alert with comparison detail |
| `NavigationBar` | Bottom navigation bar with 5 tabs |
| `TopAppBar` | Screen top bar with contextual actions |
| `ConsentDialog` | Explicit consent collection for sharing, export, or deletion |
| `LoadingSkeleton` | Placeholder UI while data loads |
| `EmptyState` | Contextual empty state for each screen |
| `ErrorState` | Error display with retry action |

---

## 5. Iconography Guidelines

### 5.1 Icon Set

Use **Material Symbols** (the variable-font icon set, available in Compose via `material-icons-extended` or the newer `material-symbols` library).

**Preferred style:** Outlined (default). Use Filled only for selected/active states (e.g. bottom nav selected tab icon).

**Why Material Symbols:**
- Consistent with Material 3 visual language
- Available as variable fonts — weight and fill can be animated
- No custom icon set to maintain

### 5.2 Medical Context Icons

These Material Symbols icons are used for medical-specific UI elements:

| Context | Icon name | Usage |
|---|---|---|
| Prescription / document | `description` | Prescription cards, nav tab |
| Medicine / pill | `medication` | Medicine cards, nav tab |
| Doctor | `stethoscope` | Doctor information sections |
| Duplicate alert | `warning` | Duplicate alert indicator |
| Confidence high | `check_circle` | High confidence badge |
| Confidence medium/low | `warning` | Medium/low confidence badge |
| OCR processing | `document_scanner` | OCR in-progress state |
| History | `history_edu` | History tab, history sections |
| Safety | `health_and_safety` | Safety tab |
| Reminder | `alarm` | Reminder rows |
| Dose taken | `check_circle` | Dose log — taken |
| Dose skipped | `skip_next` | Dose log — skipped |
| Dose missed | `cancel` | Dose log — missed |
| Source link | `open_in_new` | Trailing icon on source chips |
| Export | `download` | Export actions |
| Share | `share` | Share actions |
| Delete | `delete` | Delete actions |
| Camera | `camera_alt` | Capture prescription |
| Gallery | `photo_library` | Pick from gallery |

### 5.3 Custom Icons

No custom icons in the MVP. If a required icon is not available in Material Symbols, raise it as an open decision before adding a custom drawable.

### 5.4 Icon Sizes

| Context | Icon size | Touch target |
|---|---|---|
| In-text icons (inline with body text) | 16 dp | No separate touch target |
| Chip leading icon | 18 dp | Part of chip touch target |
| Card status icon | 20 dp | No separate touch target |
| List item leading icon | 24 dp | No separate touch target |
| TopAppBar action icon button | 24 dp icon | 48 × 48 dp button |
| FAB icon | 24 dp | FAB touch target |
| Bottom nav icon | 24 dp | Full tab width × 48 dp |

---

## 6. Animation and Motion Principles

Full motion specifications are in `figma/figma-interaction-spec.md`. This section covers design principles.

### 6.1 Motion Principles

- **Purposeful:** Every animation communicates something (state change, spatial relationship, progress). No decorative motion.
- **Appropriate speed:** Medical apps demand calm. Use the slower end of Material 3's speed range. No fast, energetic animations.
- **Non-distracting:** Animations should not draw attention away from medical data.
- **Interruptible:** All animations can be interrupted by user input without leaving the UI in a broken state.

### 6.2 Reduced Motion

The app respects the Android **Animator Duration Scale** accessibility setting:
- When set to 0 (animations off), all transitions are instant
- When set to 0.5–2.0, animations play at the proportional speed
- Use `animationDurationScale` from `Settings.Global` or observe it at runtime

In Compose, check `LocalReduceMotion` if using Compose 1.7+ or implement a manual check on app startup.

### 6.3 Motion Categories

| Category | Examples | Duration range |
|---|---|---|
| Micro-interactions | Confidence badge tooltip, chip state change | 100–200 ms |
| Component transitions | Card expand/collapse, OcrStatusChip update | 200–300 ms |
| Screen transitions | Navigate forward/back | 250–350 ms |
| Shared element transitions | Card → Detail | 300–400 ms |
| Loading/progress | Skeleton shimmer, circular progress | Continuous (looping) |

---

## 7. Dark Mode Support

Requirements:
- All components must be tested in dark mode before "Ready for Dev"
- The `ConsentDialog`, `SafetyBanner`, and `DuplicateAlertCard` amber colors must maintain 4.5:1 contrast in dark mode
- Prescription image thumbnails should use a dark surface container as background in dark mode
- Skeleton shimmer animation color must adapt to dark mode surface colors
- No hardcoded hex values in Compose — all colors via `MaterialTheme.colorScheme.*` or `LocalMediHistoryColors.current`

---

## 8. Density and Layout Adaptability

### 8.1 Target Layout

The app is designed for **compact** width (≤ 600 dp), the standard phone form factor. No tablet or large-screen layout is required for MVP.

Future (post-MVP): Medium width (600–840 dp) support with two-pane layouts for prescription detail and OCR review.

### 8.2 Minimum Supported Screen

| Dimension | Minimum |
|---|---|
| Width | 360 dp |
| Height | 640 dp |

### 8.3 Safe Areas and System Bar Handling

- Use `WindowCompat.setDecorFitsSystemWindows(window, false)` to draw edge-to-edge
- Apply `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` to the bottom navigation bar
- Apply `Modifier.statusBarsPadding()` to the top app bar
- Never let medical data be obscured by system bars

### 8.4 Keyboard Handling

Screens with text input (OCR review field editing) must:
- Use `Modifier.imePadding()` to push content above the soft keyboard
- Ensure the currently focused input field is scrolled into view
- Not resize the entire screen on keyboard show — use `adjustPan` window soft input mode

---

## Related Documents

- [`figma/figma-design-tokens.md`](figma/figma-design-tokens.md) — Complete token definitions
- [`figma/figma-component-spec.md`](figma/figma-component-spec.md) — Component anatomy and variants
- [`figma/figma-accessibility.md`](figma/figma-accessibility.md) — Accessibility requirements
- [`figma/figma-interaction-spec.md`](figma/figma-interaction-spec.md) — Motion and interaction specs
