# Requirements — Android UI

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F01, F02, F03, F04, F05, F06, F07
**Jira tickets:** MHA-009, MHA-010, MHA-011, MHA-012, MHA-013, MHA-014, MHA-015
**Functional requirements:** FR-003, FR-004
**Dependencies:** foundation spec (Room schema, Hilt wiring, authentication scaffold must exist)

---

## 1. Purpose

This spec covers all shared Android UI infrastructure that feature specs consume but do not own:

- **Material 3 theme** — colour scheme, typography scale, shape tokens, light/dark variants
- **Navigation graph** — Compose Navigation destinations, route definitions, argument types
- **Shared Compose component library** — reusable composables (StatusBadge, ConfidenceIndicator, DocumentThumbnail, EmptyState, LoadingOverlay, ErrorBanner, etc.)
- **Design tokens** — dimension, spacing, elevation, and colour constants mapped to the Material 3 theme
- **Accessibility baseline** — minimum touch targets, content descriptions, screen-reader ordering, contrast enforcement
- **Top app bar** — shared scaffold with back navigation, title, and action slots
- **Navigation map documentation** — screen inventory and transition diagram committed to `docs/design/`

All feature screens are built on top of this layer. Changes here affect every screen; review carefully before modifying.

---

## 2. Functional requirements

### FR-003 — Dashboard navigation entry points
The bottom navigation bar provides one-tap access to the four primary destinations: Prescriptions, Medicine Explorer, Duplicate Alerts, and Medical History. Navigation state is preserved on back stack in accordance with Jetpack Navigation conventions.

### FR-004 — Empty state guidance
A shared `EmptyState` composable renders a plain-language message and a primary call-to-action button. It accepts a title, body, optional illustration resource, and a labelled action. It must not show zero-value summary cards.

---

## 3. Tickets and acceptance criteria

### MHA-009 — Material 3 theme
Define `MediHistoryTheme` wrapping `MaterialTheme` with a custom colour scheme (light and dark), typography scale (Display through Label), and shape tokens. Theme is applied at the root composable.

Key ACs:
- Light and dark themes pass WCAG AA contrast ratio (4.5:1 for normal text, 3:1 for large text) verified by `AccessibilityChecks`.
- `MediHistoryTheme` is the single entry point — no `MaterialTheme` calls outside it.
- Theme snapshot tests exist for light and dark variants.

### MHA-010 — Navigation graph
Define all Compose Navigation destinations in `AppNavGraph.kt`. Each destination is a sealed object or data class in `Screen.kt`. Deep-link URIs are defined for externally navigable screens. Navigation arguments use type-safe `NavType`.

Key ACs:
- Every screen reachable from the bottom navigation bar has a defined route.
- Navigation from a nested back stack does not reset the bottom navigation selection.
- Argument serialisation/deserialisation is tested for all non-trivial argument types.

### MHA-011 — Shared Compose component library
Implement the following composables in `ui/components/`:
`StatusBadge`, `ConfidenceIndicator`, `DocumentThumbnail`, `EmptyState`, `LoadingOverlay`, `ErrorBanner`, `SourceReferenceChip`, `ConfirmationDialog`, `DismissibleChip`, `SectionHeader`.

Key ACs:
- Every composable has a Compose Preview.
- Every interactive composable meets the 48×48 dp minimum touch target.
- All components pass `AccessibilityChecks` in UI tests.
- Components do not import feature-layer code — zero upward dependencies.

### MHA-012 — Design tokens
Define spacing, dimension, elevation, and icon-size constants in `DesignTokens.kt`. All hardcoded dimension literals in `ui/` are replaced with token references.

Key ACs:
- No magic number dimension literals remain in `ui/` after this ticket.
- Token naming follows the pattern `Spacing.Medium`, `Elevation.Card`, `IconSize.Standard`.
- Tokens file has no runtime logic — constants only.

### MHA-013 — Accessibility baseline
Enforce: all `Image` composables have non-empty `contentDescription` or `contentDescription = null` with `Role` set to decorative; all `IconButton` composables have `contentDescription`; bottom navigation items announce their label and selected state to TalkBack; focus order follows reading order.

Key ACs:
- `AccessibilityChecks.enable()` is set in the base UI test class; no suppressed failures.
- All interactive elements have touch targets ≥ 48×48 dp (verified with `AccessibilityChecks`).
- TalkBack navigation test covers bottom navigation bar and top app bar actions.

### MHA-014 — Top app bar
Implement a shared `MediHistoryTopAppBar` composable accepting: title string, optional back navigation action, and a slot for trailing icon buttons. Uses `TopAppBarDefaults.pinnedScrollBehavior()` where applicable.

Key ACs:
- Back navigation icon is shown only when a back destination exists in the nav stack.
- Title is truncated with an ellipsis when it exceeds available width — does not wrap or overflow.
- Top app bar is keyboard focusable and announces correctly to TalkBack.

### MHA-015 — Navigation map documentation
Produce and commit `docs/design/navigation-map.md` containing: a table of all screens with their route, entry points, and exit points; a Mermaid flow diagram showing navigation transitions; and a note on deep-link URIs.

Key ACs:
- Navigation map is reviewed and signed off by the product owner before feature spec work starts.
- Mermaid diagram renders correctly in GitHub Markdown preview.
- Every route in `AppNavGraph.kt` appears in the navigation map.

---

## 4. Implementation order

1. **MHA-009** — Material 3 theme (required by all subsequent composables)
2. **MHA-010** — Navigation graph (required by all screen composables)
3. **MHA-011** — Shared Compose component library (depends on theme; required by feature screens)
4. **MHA-012** — Design tokens (applied across all components; complete before feature screens start)
5. **MHA-013** — Accessibility baseline (enforced before any feature screen is built)
6. **MHA-014** — Top app bar (consumed by every feature screen scaffold)
7. **MHA-015** — Navigation map documentation (documents the completed graph)

---

## 5. Out of scope

- Business logic, ViewModels, or data layer (feature specs).
- OCR pipeline or camera integration (prescription-management spec).
- Backend API calls (feature specs own their network layer).
- Any screen that displays real patient data — this spec builds the shell only.

---

## 6. Notes and constraints

- All composable previews must use synthetic data (fake names, fake medicine names).
- No feature-layer imports in `ui/components/` — components are pure UI primitives.
- `AccessibilityChecks` must be enabled in the base UI test class before the first feature PR lands.
- Dark mode support is required from the start; it is not a follow-up.
- Compose version, Material 3 version, and Navigation version are pinned in `libs.versions.toml` (foundation spec MHA-004).
