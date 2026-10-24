# Figma Import and Handoff Guide — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Figma file:** https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis  
**Status:** Awaiting Figma file creation after Google Stitch export

---

## 1. Importing Google Stitch Output into Figma

### 1.1 Export from Google Stitch

Before importing, export each screen from Google Stitch using these settings:

| Setting | Value |
|---|---|
| Export format | PNG (for reference) + Figma plugin import where available |
| Resolution | 2× (for crisp display at 1× in Figma) |
| Frame naming | Use screen ID prefix (e.g. `SCR-01-DASHBOARD`) |

If Google Stitch supports direct Figma export, use that path. Otherwise import as reference images and rebuild components manually using the Stitch output as a visual guide.

### 1.2 Frame Sizes

All screens target a single canonical phone size. Design at 1× dp values; Figma auto-scales for export.

| Canon frame | Width | Height | Notes |
|---|---|---|---|
| Phone — Standard | 360 dp | 800 dp | Primary design target |
| Phone — Tall | 360 dp | 900 dp | Verify scroll behavior |
| Phone — Compact | 360 dp | 640 dp | Minimum supported height |

Set Figma frames to **360 × 800** for all primary screens. Use auto-layout so content reflows correctly at taller/shorter heights.

### 1.3 Naming Conventions

Every Figma frame must follow this naming scheme exactly:

```
[Screen ID] / [State]
```

Examples:
- `SCR-01-DASHBOARD / Populated`
- `SCR-01-DASHBOARD / Loading`
- `SCR-01-DASHBOARD / Empty`
- `SCR-03-OCR-REVIEW / Error`

State options (minimum required per screen): `Loading`, `Empty`, `Error`, `Populated`  
Additional optional states: `Skeleton`, `Offline`, `Partial` (for partially loaded data)

Do **not** use spaces around `/` — use ` / ` (space-slash-space) consistently so Figma's path-based component system works correctly.

---

## 2. Figma File Setup

### 2.1 Pages

The Figma file must be organized into exactly 18 pages in this order:

| Page # | Page Name | Contents |
|---|---|---|
| 1 | `00 — Cover & Legend` | Project cover, file legend, status key |
| 2 | `01 — Design Tokens` | Colors, typography, spacing, elevation, shape |
| 3 | `02 — Component Library` | Atoms, molecules, organisms |
| 4 | `SCR-01 Dashboard` | All states of F01 Dashboard |
| 5 | `SCR-02 Rx List` | Prescription list + doctor group view |
| 6 | `SCR-02 Rx Detail` | Prescription detail screen |
| 7 | `SCR-03 OCR Review` | OCR extraction review screen |
| 8 | `SCR-04 Med Explorer` | Medicine explorer list |
| 9 | `SCR-04 Med Detail` | Medicine detail screen |
| 10 | `SCR-05 Duplicate Review` | Duplicate alert review screen |
| 11 | `SCR-06 History` | Medical history summary screen |
| 12 | `SCR-07 New Doctor` | New Doctor Mode screen |
| 13 | `SCR-07 PDF Preview` | PDF preview before share |
| 14 | `SCR-08 Reminders` | Medication reminders screen |
| 15 | `SCR-09 Safety Center` | Safety and interaction check screen |
| 16 | `SCR-AUX Settings` | Settings, privacy, export, delete |
| 17 | `17 — Flows & Nav Map` | Navigation flow diagrams |
| 18 | `18 — Prototype Links` | Clickable prototype connections |

### 2.2 Component Library Setup

On Page 3 (`02 — Component Library`), organize components into three tiers:

**Atoms** (primitive building blocks):
- `Atom/ColorSwatch` — token reference swatches
- `Atom/TypeRamp` — typography specimens
- `Atom/Icon` — icon grid
- `Atom/Divider`
- `Atom/Badge`

**Molecules** (composed of atoms):
- `Molecule/ConfidenceBadge`
- `Molecule/OcrStatusChip`
- `Molecule/SourceLinkChip`
- `Molecule/DoseLogButton`
- `Molecule/ReminderRow`
- `Molecule/SafetyBanner`

**Organisms** (composed of molecules):
- `Organism/PrescriptionCard`
- `Organism/MedicineCard`
- `Organism/DuplicateAlertCard`
- `Organism/NavigationBar`
- `Organism/TopAppBar`
- `Organism/ConsentDialog`
- `Organism/LoadingSkeleton`
- `Organism/EmptyState`
- `Organism/ErrorState`

All components must be Figma **components** (not groups), with variants defined for each state.

### 2.3 Design Tokens Setup

On Page 2 (`01 — Design Tokens`), create Figma **local styles** that mirror the token names in `figma-design-tokens.md`:

- **Color styles** — one style per token (e.g. `color/primary`, `color/warning`)
- **Text styles** — one per typography token (e.g. `type/title-large`, `type/body-medium`)
- **Effect styles** — one per elevation level (e.g. `elevation/level2`)

> **Important:** Figma Variables (if available in your Figma plan) are preferred over Styles for color tokens, as they support dark/light mode switching via variable collections.

---

## 3. Handoff Workflow: Design → Android Developer

### 3.1 Developer Handoff Checklist

Before marking any screen ready for development, the designer must verify:

- [ ] All four minimum states are present: `Loading`, `Empty`, `Error`, `Populated`
- [ ] All components on the frame use published library components (no detached instances)
- [ ] All color fills use named color styles or variables (no raw hex values)
- [ ] All text uses named text styles (no local overrides)
- [ ] Spacing is applied via auto-layout, not manual positioning
- [ ] Content descriptions are annotated for all icons and images (use annotation kit or notes)
- [ ] Screen ID is correct in the frame name
- [ ] The corresponding row in `screen-inventory.md` is updated with the Figma page reference

### 3.2 What Developers Inspect in Figma

Developers use **Figma Dev Mode** (or the Inspect panel) to retrieve:

| Property | How to use in Compose |
|---|---|
| Color token name | Map to `MaterialTheme.colorScheme.*` |
| Text style name | Map to `MaterialTheme.typography.*` |
| Spacing values | Use `dp` values directly |
| Corner radius | Map to `MaterialTheme.shapes.*` |
| Elevation | Map to `Modifier.shadow()` or `Card(elevation=...)` |
| Icon | Confirm Material Symbol name or custom drawable |
| Component variant | Map to the corresponding Compose composable + parameters |

### 3.3 Android Compose Naming Conventions

Figma component names map to Compose composable names as follows:

| Figma name | Compose composable |
|---|---|
| `Organism/PrescriptionCard` | `PrescriptionCard` |
| `Organism/MedicineCard` | `MedicineCard` |
| `Organism/DuplicateAlertCard` | `DuplicateAlertCard` |
| `Molecule/ConfidenceBadge` | `ConfidenceBadge` |
| `Molecule/OcrStatusChip` | `OcrStatusChip` |
| `Molecule/SourceLinkChip` | `SourceLinkChip` |
| `Organism/NavigationBar` | `MediHistoryBottomBar` |
| `Organism/TopAppBar` | `MediHistoryTopBar` |
| `Organism/ConsentDialog` | `ConsentDialog` |
| `Organism/EmptyState` | `EmptyStateView` |
| `Organism/ErrorState` | `ErrorStateView` |
| `Organism/LoadingSkeleton` | `LoadingSkeletonView` |

---

## 4. Linking Figma Frames to Screen IDs

Maintain a mapping between Figma frame node IDs and screen IDs in this repo. Update this table whenever a Figma frame is added, renamed, or removed.

| Screen ID | Screen Name | Figma Page | Figma Frame (Populated state) | Figma Node ID |
|---|---|---|---|---|
| SCR-01-DASHBOARD | Patient Dashboard | `SCR-01 Dashboard` | `SCR-01-DASHBOARD / Populated` | [FIGMA_NODE_TBD] |
| SCR-02-RX-LIST | Prescription List | `SCR-02 Rx List` | `SCR-02-RX-LIST / Populated` | [FIGMA_NODE_TBD] |
| SCR-02-RX-DETAIL | Prescription Detail | `SCR-02 Rx Detail` | `SCR-02-RX-DETAIL / Populated` | [FIGMA_NODE_TBD] |
| SCR-02-DOCTOR-GROUP | Doctor Group View | `SCR-02 Rx List` | `SCR-02-DOCTOR-GROUP / Populated` | [FIGMA_NODE_TBD] |
| SCR-03-OCR-REVIEW | OCR Review | `SCR-03 OCR Review` | `SCR-03-OCR-REVIEW / Populated` | [FIGMA_NODE_TBD] |
| SCR-04-MED-EXPLORER | Medicine Explorer | `SCR-04 Med Explorer` | `SCR-04-MED-EXPLORER / Populated` | [FIGMA_NODE_TBD] |
| SCR-04-MED-DETAIL | Medicine Detail | `SCR-04 Med Detail` | `SCR-04-MED-DETAIL / Populated` | [FIGMA_NODE_TBD] |
| SCR-05-DUPLICATE-REVIEW | Duplicate Alert Review | `SCR-05 Duplicate Review` | `SCR-05-DUPLICATE-REVIEW / Populated` | [FIGMA_NODE_TBD] |
| SCR-06-HISTORY | Medical History | `SCR-06 History` | `SCR-06-HISTORY / Populated` | [FIGMA_NODE_TBD] |
| SCR-07-NEW-DOCTOR | New Doctor Mode | `SCR-07 New Doctor` | `SCR-07-NEW-DOCTOR / Populated` | [FIGMA_NODE_TBD] |
| SCR-07-PDF-PREVIEW | PDF Preview | `SCR-07 PDF Preview` | `SCR-07-PDF-PREVIEW / Populated` | [FIGMA_NODE_TBD] |
| SCR-08-REMINDERS | Reminders | `SCR-08 Reminders` | `SCR-08-REMINDERS / Populated` | [FIGMA_NODE_TBD] |
| SCR-09-SAFETY-CENTER | Safety Center | `SCR-09 Safety Center` | `SCR-09-SAFETY-CENTER / Populated` | [FIGMA_NODE_TBD] |

> **Process note:** After creating a frame in Figma, right-click → Copy link to frame, and paste the node ID portion of the URL into this table. The node ID appears as `?node-id=XXXX-YYYY` in the Figma share URL.

---

## 5. Version Control of Figma Files

Figma is not a git repository. Use this process to create a versioned paper trail alongside the code repository.

### 5.1 Manual Export Cadence

Export Figma snapshots at these milestones:

| Milestone | Export action |
|---|---|
| After each sprint design review | Export all screen frames as PNG 2×, save to `docs/design/figma/exports/YYYY-MM-DD/` |
| Before each development sprint starts | Export component library page as PNG + PDF |
| When a design token changes | Export design token page |
| At release milestone | Export complete file as `.fig` backup (if Figma plan supports) |

### 5.2 Export Directory Structure

```
docs/design/figma/exports/
  YYYY-MM-DD/
    cover.png
    tokens.png
    components.png
    SCR-01-DASHBOARD-populated.png
    SCR-01-DASHBOARD-loading.png
    SCR-01-DASHBOARD-empty.png
    SCR-01-DASHBOARD-error.png
    ... (all screens, all states)
```

### 5.3 Figma Version History

Figma has built-in named versions (File → Save to version history). Use this naming scheme for named versions:

```
v{sprint}.{iteration} — {brief description}
```

Examples:
- `v1.0 — Initial component library`
- `v1.1 — Dashboard + Rx screens`
- `v2.0 — OCR review and duplicate alert screens`

---

## 6. Keeping Figma in Sync with Spec Changes

### 6.1 Change Flow

When a spec document changes, follow this process:

```
Spec change in docs/ (PRD / acceptance criteria)
  → Update affected screen spec in figma-component-spec.md or figma-interaction-spec.md
  → Designer updates Figma frame
  → Designer exports updated PNG to docs/design/figma/exports/
  → screen-inventory.md Status column updated
  → Developer re-inspects affected Compose screen
  → Git commit includes: spec file + exported PNG + inventory update
```

### 6.2 Traceability Links

Every Figma frame must trace back to at least one acceptance criterion. The `screen-inventory.md` file is the source of truth for these links. If a Figma frame is added without a matching AC ID, it must be treated as experimental and not used for development work.

### 6.3 Design Review Gate

No Figma screen transitions to `Status: Ready for Dev` without:

1. Product owner sign-off on populated state
2. Accessibility review (contrast, touch targets, focus order)
3. `screen-inventory.md` row updated
4. Export PNG committed to `docs/design/figma/exports/`

---

## Related Documents

- [`figma-page-structure.md`](figma-page-structure.md) — Detailed page layout specification
- [`figma-component-spec.md`](figma-component-spec.md) — Component anatomy and variants
- [`figma-design-tokens.md`](figma-design-tokens.md) — Token definitions
- [`figma-interaction-spec.md`](figma-interaction-spec.md) — Interaction and motion spec
- [`figma-accessibility.md`](figma-accessibility.md) — Accessibility requirements
- [`../screen-inventory.md`](../screen-inventory.md) — Screen inventory and status
- [`../navigation-map.md`](../navigation-map.md) — Navigation map
