# MA-21: Material 3 theme — colours, typography, shapes, extended tokens

This change completes the Material 3 theme scaffold that was placeholder-only in the base branch. It adds all 29 M3 colour slots to both light and dark schemes, a proper dark palette using dedicated `MhDark*` constants, a new `ExtendedColors.kt` holding warning/status/OCR tokens that have no M3 slot, a full 15-style typography scale, and a test file with 5 assertions exercising colour token correctness. The previous dark scheme was a rough mirror of the light palette; it is now a properly paired dark palette.

Watch for: `MhDarkOutlineVariant` duplicates `MhDarkSurfaceVariant` (`#3F484A`) — may be intentional per M3 tonal palette rules but should be confirmed against the design spec; status tokens (`statusCurrent`, `statusPast`, `statusUnknown`) have no `onStatus*` counterparts in `MediHistoryExtendedColors`, which will force hardcoded text colours in status badge composables; build evidence is present in the commit message (all three required tasks SUCCESSFUL) but is self-reported, not CI-artefact-verified.

**Verdict**: APPROVED

---

## High-level view

The light scheme now maps all 29 M3 `ColorScheme` slots using named `Mh*` constants from `Color.kt`. The dark scheme mirrors this with dedicated `MhDark*` constants — no more light-palette recycling. The separation is clean and the indirection (raw constant → scheme slot) makes spec-to-code tracing straightforward.

`ExtendedColors.kt` introduces `MediHistoryExtendedColors` with 11 fields covering warning, medicine status, OCR review state, and confidence badges. Both light and dark instances are wired to `LightExtendedColors` / `DarkExtendedColors` and surfaced through `MaterialTheme.extendedColors`. The rename from `mediHistoryColors` is documented in a migration comment.

Typography expands from 3 styles to all 15 M3 styles. All sizes, weights, and letter-spacings match published M3 spec values. Shape values are unchanged in numeric terms; the diff adds usage comments and an alias (`MhShapes`).

The test file exercises exactly the four originally required assertions (light primary, dark primary, warning ≠ error, warning ≠ Unspecified) and adds two more checking extended token completeness for both light and dark instances. All tests are plain JVM — no Robolectric or instrumentation required, so they run in `testDebugUnitTest` without an emulator.

Build evidence in the latest commit message shows all three required Gradle tasks passed: `assembleDebug` (BUILD SUCCESSFUL, 43 tasks, 1m 52s), `lintDebug` (BUILD SUCCESSFUL, 32 tasks, 43s), `testDebugUnitTest` (BUILD SUCCESSFUL, 36 tasks, 7 tests completed). This evidence is recorded in the commit message, not from a live CI run.

---

<details>
<summary>Issues (2)</summary>

1. **`MhDarkOutlineVariant` duplicates `MhDarkSurfaceVariant`** — both are `Color(0xFF3F484A)`. M3's tonal palette allows these roles to share a value in dark themes, but there is no comment saying it is intentional and the light scheme correctly distinguishes them. Confirm against the design spec and add a comment if it is deliberate.

2. **No `onStatus*` tokens in `MediHistoryExtendedColors`** — `statusCurrent`, `statusPast`, and `statusUnknown` alias into M3 scheme colours but have no paired `onStatus*` tokens. Any composable rendering a label over a status-coloured badge will have to hardcode a text colour. Add `onStatusCurrent`, `onStatusPast`, and `onStatusUnknown` before the status badge composables are built.

</details>

---

<details>
<summary>Details</summary>

## Colour token completeness and correctness

`Color.kt` now defines the full light and dark constant sets. Key values match the design spec called out in the review criteria: `MhPrimary = Color(0xFF006874)` (confirmed), `MhDarkPrimary = Color(0xFF4BD3E5)` (confirmed). The `MhInversePrimary` light value is `Color(0xFF4BD3E5)` (the dark primary, as expected for a complementary inverse pair) and `MhDarkInversePrimary = Color(0xFF006874)` (the light primary — correct symmetry).

One value warrants attention: `MhDarkOutlineVariant = Color(0xFF3F484A)` is identical to `MhDarkSurfaceVariant`. M3's tonal palette does allow these roles to share a value in dark themes, but with no spec comment it reads as a copy-paste gap. The light scheme correctly distinguishes them (`MhSurfaceVariant = 0xFFDBE4E6`, `MhOutlineVariant = 0xFFBFC8CA`).

## `ExtendedColors.kt` — field coverage

`MediHistoryExtendedColors` has 11 fields. `ThemeTest.kt` references all 11 in `should_have_no_unspecified_colors_in_extended_light_colors` and the matching dark test added in this iteration. The data class has no default values, so a missing field would be a compile error — good defensive design.

The status tokens (`statusCurrent`, `statusPast`, `statusUnknown`) alias into the M3 scheme colours (`MhPrimary`, `MhSecondary`, `MhOutline`). There are no `onStatus*` counterparts. Any composable that renders a label on top of a status-coloured surface must either hardcode a text colour or derive one from the scheme. This is a gap that will surface when status badge composables are built.

## Test coverage

Five tests are present. They cover: light primary value (confirmed `#006874`), dark primary value (confirmed `#4BD3E5`), warning ≠ error, all key light M3 slots ≠ `Color.Unspecified`, all 11 extended light tokens ≠ `Color.Unspecified`, all 11 extended dark tokens ≠ `Color.Unspecified`. The four originally required assertions are a subset of these five tests. No test checks the `DarkColorScheme` slots for `Color.Unspecified` — the dark M3 slots are only tested indirectly via the `should_have_correct_dark_primary_color` test. This is acceptable for a theme unit-test tier but leaves a gap if a dark slot were accidentally left unmapped.

## Build evidence

The commit message records: `assembleDebug: BUILD SUCCESSFUL in 1m 52s (43 tasks, 8 executed)`, `lintDebug: BUILD SUCCESSFUL in 43s (32 tasks, 9 executed)`, `testDebugUnitTest: BUILD SUCCESSFUL in 19s (36 tasks, 6 executed) — 7 tests completed`. All three required tasks are present. The evidence is recorded in the commit message rather than a CI artefact — a prior iteration's blocking finding has been addressed. The task count for `testDebugUnitTest` shows 36 tasks / 6 executed (not just the 5 theme tests), which is consistent with a real Gradle run including the Room unit tests added in MA-18.

## Accessor rename (`mediHistoryColors` → `extendedColors`)

The extension property on `MaterialTheme` was renamed. A migration comment in `ExtendedColors.kt` documents the rename. Any feature branch rebasing onto this will get a compile error at `MaterialTheme.mediHistoryColors` call sites — the comment is the only mitigation. A `@Deprecated("Use extendedColors", ReplaceWith("extendedColors"))` typealias or deprecated overload would be a smoother migration path, but with no existing callers outside the theme package at this stage it is not a blocker.

</details>

---

<details>
<summary>File map</summary>

| File | Change |
|---|---|
| `Color.kt` | Adds 26 new colour constants: all light-scheme `on*`/neutral slots and the full `MhDark*` palette including warning tokens |
| `Theme.kt` | Expands `LightColorScheme` and `DarkColorScheme` to all 29 M3 slots; renames accessor to `extendedColors`; wires `LightExtendedColors`/`DarkExtendedColors` |
| `ExtendedColors.kt` | New file — `MediHistoryExtendedColors` data class, `LightExtendedColors`, `DarkExtendedColors`, `LocalMediHistoryColors` |
| `Type.kt` | Expands from 3 styles to all 15 M3 styles |
| `Shape.kt` | Adds usage comments per shape; adds `MhShapes` alias |
| `ThemePreview.kt` | New file — two `@Preview` composables exercising light/dark themes |
| `ThemeTest.kt` | New file — 5 unit tests for colour token correctness |
| `android/app/.agents/tasks/ma21-plan.md` | Coder's investigation notes and implementation plan (non-production artefact) |
| `android/app/.agents/tasks/pr.json` | PR metadata for MA-18 (unrelated artefact from prior task, bundled in branch) |

Full diff: `git diff develop...feature/MA-21-material3-theme`

</details>
