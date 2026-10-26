# Implementation Plan — MA-21: Material 3 Theme

## Pre-flight findings (from code exploration)

### Current state of the four theme files

**Color.kt**
- Declares raw colour constants `MhPrimary`, `MhOnPrimary`, … (16 constants total).
- Has a minimal `data class MediHistoryColorScheme` with only 4 fields: `warning`, `onWarning`,
  `warningContainer`, `onWarningContainer`.
- Has `val LocalMediHistoryColorScheme = staticCompositionLocalOf { … }`.
- Missing: the full set of dark-mode raw constants, and the extended semantic tokens
  (status, OCR, confidence).

**Theme.kt**
- `LightColorScheme` is incomplete — only 12 of the ~20 M3 slots are set.
- `DarkColorScheme` incorrectly mirrors light by swapping container/non-container pairs rather
  than using a proper dark palette.
- `LightWarningScheme` / `DarkWarningScheme` are instances of `MediHistoryColorScheme` (old type).
- Accessor `MaterialTheme.mediHistoryColors` returns `MediHistoryColorScheme`.
- `CompositionLocalProvider` references `LocalMediHistoryColorScheme`.
- `dynamicColor` branch is present (Android 12+) — must be kept.

**Type.kt**
- Only 3 of the 15 M3 type styles are defined (`bodyLarge`, `titleLarge`, `labelSmall`).

**Shape.kt**
- Complete — 5 tokens already set.
- Variable is named `MediHistoryShapes`; the task asks to rename it to `Shapes`…
  DECISION: Keep `MediHistoryShapes` to avoid a breaking rename across current and future
  callers (Theme.kt already uses `MediHistoryShapes`). Add the usage comments instead.
  Rationale: `Shapes` is the M3 class name; renaming the val to `Shapes` would shadow the
  import `androidx.compose.material3.Shapes` in the same file and cause a compile error.
  The task note "rename variable to `Shapes`" is therefore not workable as stated.

**Test infrastructure**
- JUnit 4, `testDebugUnitTest` Gradle task.
- `buildFeatures { buildConfig = true }` already set — `BuildConfig` accessible in unit tests.
- No Compose instrumentation in the unit-test source set; `ThemeTest.kt` must be a plain JVM
  test (no `@Composable`, no Android runtime) — it tests colour hex values directly on the
  `Color` objects.

**Dependency versions**
- `compose-bom = "2024.06.00"`, `material3 = "1.2.1"`, Kotlin 1.9.24.
- No `ksp` or extra dependencies needed for theme work.

---

## Items

- [ ] 1. Extend `Color.kt` with all missing raw colour constants and replace `MediHistoryColorScheme`.

      Add the remaining light-palette constants not yet present:
      `MhOnSecondary`, `MhOnSecondaryContainer`, `MhTertiary`, `MhOnTertiary`,
      `MhTertiaryContainer`, `MhOnTertiaryContainer`, `MhOnError`, `MhOnErrorContainer`,
      `MhBackground`, `MhOnBackground`, `MhOnSurfaceVariant`, `MhOutlineVariant`,
      `MhInverseSurface`, `MhInverseOnSurface`, `MhInversePrimary`.

      Add the full dark-palette raw constants:
      `MhDarkPrimary`, `MhDarkOnPrimary`, `MhDarkPrimaryContainer`, `MhDarkOnPrimaryContainer`,
      `MhDarkSecondary`, `MhDarkOnSecondary`, `MhDarkSecondaryContainer`, `MhDarkOnSecondaryContainer`,
      `MhDarkError`, `MhDarkOnError`, `MhDarkBackground`, `MhDarkOnBackground`,
      `MhDarkSurface`, `MhDarkOnSurface`, `MhDarkSurfaceVariant`, `MhDarkOnSurfaceVariant`,
      `MhDarkOutline`.

      Add the extended-light-semantic constants:
      `MhOnWarning` (already partially there as a default), `MhOnWarningContainer`,
      `MhStatusCurrent`, `MhStatusPast`, `MhStatusUnknown`,
      `MhOcrReview`, `MhOcrFailed`, `MhConfidenceHigh`, `MhOnConfidenceHigh`.

      Add the extended-dark-semantic constants:
      `MhDarkWarning`, `MhDarkWarningContainer`, `MhDarkOnWarningContainer`,
      `MhDarkStatusCurrent`, `MhDarkStatusPast`, `MhDarkStatusUnknown`,
      `MhDarkOcrReview`, `MhDarkOcrFailed`, `MhDarkConfidenceHigh`, `MhDarkOnConfidenceHigh`.

      Replace the `data class MediHistoryColorScheme` declaration: rename it to
      `MediHistoryExtendedColors` and expand it with the full token set (15 fields):
      `warning`, `onWarning`, `warningContainer`, `onWarningContainer`,
      `statusCurrent`, `statusPast`, `statusUnknown`,
      `ocrReview`, `ocrFailed`, `confidenceHigh`, `onConfidenceHigh`.

      Rename `LocalMediHistoryColorScheme` → `LocalMediHistoryColors`, typed as
      `MediHistoryExtendedColors`.

      Hex values to use (exact, from task brief):
      Light: warning=#7D5700, warningContainer=#FFDEA8, onWarningContainer=#271900,
             statusCurrent=#006874, statusPast=#4A6267, statusUnknown=#6F797A,
             ocrReview=#7D5700, ocrFailed=#BA1A1A, confidenceHigh=#DAE2FF, onConfidenceHigh=#0E1B37.
      Dark:  warning=#FFBA3B, warningContainer=#5E4100, onWarningContainer=#FFDEA8,
             statusCurrent=#4BD3E5, statusPast=#B0CBD1, statusUnknown=#899294,
             ocrReview=#FFBA3B, ocrFailed=#FFB4AB, confidenceHigh=#394778, onConfidenceHigh=#DAE2FF.

      Files: `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt`
      Verify: The file compiles as part of step 4's Gradle build.

- [ ] 2. Create `ExtendedColors.kt` — extract extension infrastructure out of `Color.kt`.

      Create a new file in the same package. Move (or re-declare, to avoid the file depending
      on what Color.kt declares at this step) into it:
      - The `MediHistoryExtendedColors` data class (same expanded definition from step 1).
      - `val LocalMediHistoryColors = staticCompositionLocalOf { … }` with a sensible light
        default (all fields populated with light-extended constants).
      - Two top-level `val` instances:
        `val lightMediHistoryExtendedColors = MediHistoryExtendedColors(…)` — all light values.
        `val darkMediHistoryExtendedColors  = MediHistoryExtendedColors(…)` — all dark values.

      DECISION: `MediHistoryExtendedColors` and `LocalMediHistoryColors` live in `ExtendedColors.kt`;
      `Color.kt` keeps only the raw `Color(0xFF…)` constants. This separation matches the
      task's intent and keeps each file single-purpose.

      Update `Color.kt` from step 1: remove the `data class MediHistoryColorScheme` block and
      `LocalMediHistoryColorScheme` val (they now live in `ExtendedColors.kt`).

      Files:
      - `android/app/src/main/java/com/medihistory/ai/ui/theme/ExtendedColors.kt` (new)
      - `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt` (remove old class/local)

      Verify: Compiles as part of step 4's Gradle build.

- [ ] 3. Update `Theme.kt` — complete LightColorScheme, add proper DarkColorScheme, wire extended colours.

      Replace the incomplete `LightColorScheme` with the full 20-slot M3 light scheme using every
      `MhXxx` constant now available after step 1.

      Exact mapping (light):
      ```
      primary, onPrimary, primaryContainer, onPrimaryContainer,
      secondary, onSecondary, secondaryContainer, onSecondaryContainer,
      tertiary, onTertiary, tertiaryContainer, onTertiaryContainer,
      error, onError, errorContainer, onErrorContainer,
      background, onBackground, surface, onSurface,
      surfaceVariant, onSurfaceVariant,
      outline, outlineVariant, inverseSurface, inverseOnSurface, inversePrimary
      ```

      Replace the placeholder `DarkColorScheme` with a proper dark scheme using `MhDarkXxx`
      constants.

      Replace `LightWarningScheme` / `DarkWarningScheme` (old type) with
      `lightMediHistoryExtendedColors` and `darkMediHistoryExtendedColors` from `ExtendedColors.kt`.

      Update `CompositionLocalProvider` to reference `LocalMediHistoryColors` (renamed).

      Update the `MaterialTheme.mediHistoryColors` extension property:
      - Rename property to `extendedColors`.
      - Return type changes from `MediHistoryColorScheme` to `MediHistoryExtendedColors`.
      - `LocalMediHistoryColorScheme.current` → `LocalMediHistoryColors.current`.

      Keep the `dynamicColor` branch exactly as it is — only the non-dynamic path changes.

      Files: `android/app/src/main/java/com/medihistory/ai/ui/theme/Theme.kt`
      Verify: Compiles as part of step 4's Gradle build. `MainActivity.kt` references only
      `MediHistoryTheme` so it is unaffected.

- [ ] 4. Update `Type.kt` — complete all 15 M3 typography styles.

      Expand `MediHistoryTypography` to cover all 15 M3 slots. Use `FontFamily.Default` throughout
      (Roboto or system default); add a comment noting custom font is deferred to the design
      system ticket as the existing comment already states.

      The 15 slots and their M3 spec values (sp / sp lineHeight / sp letterSpacing):
      - `displayLarge`:   57 / 64 / -0.25
      - `displayMedium`:  45 / 52 / 0
      - `displaySmall`:   36 / 44 / 0
      - `headlineLarge`:  32 / 40 / 0
      - `headlineMedium`: 28 / 36 / 0
      - `headlineSmall`:  24 / 32 / 0
      - `titleLarge`:     22 / 28 / 0   (already present — keep)
      - `titleMedium`:    16 / 24 / 0.15  FontWeight.Medium
      - `titleSmall`:     14 / 20 / 0.1   FontWeight.Medium
      - `bodyLarge`:      16 / 24 / 0.5   (already present — keep)
      - `bodyMedium`:     14 / 20 / 0.25
      - `bodySmall`:      12 / 16 / 0.4
      - `labelLarge`:     14 / 20 / 0.1   FontWeight.Medium
      - `labelMedium`:    12 / 16 / 0.5   FontWeight.Medium
      - `labelSmall`:     11 / 16 / 0.5   FontWeight.Medium (already present — keep)

      Add inline comments on specific styles:
      - `titleMedium`: "// Use for active ingredient names in medicine cards"
      - `labelMedium`: "// Use for OCR confidence score badges"
      - `bodySmall`:   "// Use for prescription dates and secondary metadata"

      Files: `android/app/src/main/java/com/medihistory/ai/ui/theme/Type.kt`
      Verify: Compiles as part of step 5's Gradle build.

- [ ] 5. Update `Shape.kt` — add usage comments.

      Keep `val MediHistoryShapes` unchanged (renaming to `Shapes` would shadow the M3 import and
      cause a compile error, as explained in the pre-flight findings).

      Add inline comments to each shape token documenting typical UI usage:
      - `extraSmall`: "// Chip, badge, tooltip"
      - `small`:      "// Button, text field, menu item"
      - `medium`:     "// Card, dialog, bottom sheet header"
      - `large`:      "// Bottom sheet, side sheet, navigation drawer"
      - `extraLarge`: "// Full-screen modal, large card"

      Files: `android/app/src/main/java/com/medihistory/ai/ui/theme/Shape.kt`
      Verify: Compiles as part of step 5's Gradle build.

- [ ] 6. Create `ThemePreview.kt` — light and dark previews.

      Create a new file in the same `ui/theme` package with two `@Preview` composables:

      ```kotlin
      @Preview(name = "Theme preview – Light", showBackground = true)
      @Composable
      fun ThemePreviewLight() { … }

      @Preview(name = "Theme preview – Dark", showBackground = true,
               uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
      @Composable
      fun ThemePreviewDark() { … }
      ```

      Each preview wraps `MediHistoryTheme(dynamicColor = false)` around a `Surface` containing
      a `Column` with:
      - A `Card` with `titleMedium` text showing the theme name and primary colour swatch.
      - A row of coloured `Box` composables sampling: `primary`, `secondary`, `error`,
        and `MaterialTheme.extendedColors.warning`.
      - A `Text` with `labelMedium` showing "OCR confidence: 92 %" (synthetic value).

      Add `@Suppress("PreviewAnnotationInFunctionWithParameters")` if the composable has
      no parameters; it does not, so no suppression is needed.

      No `@AndroidEntryPoint` or Hilt in this file — previews must be standalone.

      Files: `android/app/src/main/java/com/medihistory/ai/ui/theme/ThemePreview.kt` (new)
      Verify: Compiles as part of step 7's Gradle build (debug build includes the preview
      dependency `androidx.compose.ui:ui-tooling-preview`).

- [ ] 7. Create `ThemeTest.kt` — unit tests for colour token correctness.

      Create in the existing JVM unit-test source set:
      `android/app/src/test/java/com/medihistory/ai/ui/theme/ThemeTest.kt`

      Package: `com.medihistory.ai.ui.theme`
      Imports: `org.junit.Assert.*`, `org.junit.Test`, `androidx.compose.ui.graphics.Color`

      Test class `ThemeTest` with four tests:

      ```kotlin
      @Test
      fun `should have correct primary in LightColorScheme`() {
          // LightColorScheme.primary = #006874 = 0xFF006874
          assertEquals(Color(0xFF006874), MhPrimary)
      }

      @Test
      fun `should have correct primary in DarkColorScheme`() {
          // DarkColorScheme.primary = #4BD3E5 = 0xFF4BD3E5
          assertEquals(Color(0xFF4BD3E5), MhDarkPrimary)
      }

      @Test
      fun `should have extended warning colour distinct from error`() {
          // lightMediHistoryExtendedColors.warning must not equal light error
          val extendedColors = lightMediHistoryExtendedColors
          assertNotEquals(MhError, extendedColors.warning)
      }

      @Test
      fun `should have no Color_Unspecified in extended light colours`() {
          val c = lightMediHistoryExtendedColors
          listOf(
              c.warning, c.onWarning, c.warningContainer, c.onWarningContainer,
              c.statusCurrent, c.statusPast, c.statusUnknown,
              c.ocrReview, c.ocrFailed, c.confidenceHigh, c.onConfidenceHigh,
          ).forEachIndexed { i, color ->
              assertNotEquals("Field $i must not be Color.Unspecified",
                  Color.Unspecified, color)
          }
      }
      ```

      Note: `ThemeTest` is a plain JVM test — it references top-level vals from `Color.kt` and
      `ExtendedColors.kt` directly. No `@Composable`, no Android runtime, no Hilt.
      `Color(0xFF006874)` is a pure Kotlin/JVM call and works without Android.

      Files: `android/app/src/test/java/com/medihistory/ai/ui/theme/ThemeTest.kt` (new)
      Verify: `cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android && gradlew testDebugUnitTest`
      All four new tests pass alongside the existing `ExampleUnitTest` and `EnvironmentConfigTest`.

- [ ] 8. Run full build and test verification.

      From `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`:

      a. `gradlew assembleDebug`
         — Must succeed with 0 errors.
         — Spot-check `app/build/generated/source/buildConfig/debug/…/BuildConfig.java` still
           has the expected fields (unchanged from MA-17).

      b. `gradlew testDebugUnitTest`
         — All tests must pass: `ExampleUnitTest`, `EnvironmentConfigTest`, `ThemeTest` (4 cases).

      c. Security grep — run from the workspace root:
         ```
         grep -rE "(api_key|secret|password|token)" android\app\src\main
         ```
         Must return no hits inside the theme files or any other main source file.

      Files: (no file changes — verification only)
      Verify: All three commands exit cleanly as described.

---

## Dependency order

- Items 1 and 2 must be done together (step 1 adds constants; step 2 creates `ExtendedColors.kt`
  and removes the old class from `Color.kt`). Do item 1 first, then item 2 immediately after,
  before attempting to compile.
- Item 3 depends on items 1 and 2 (references new constants and `ExtendedColors.kt` symbols).
- Items 4 and 5 are independent of items 1–3 and of each other; they can be done at any point
  before the final build.
- Item 6 (`ThemePreview.kt`) depends on item 3 (uses `MaterialTheme.extendedColors`).
- Item 7 (`ThemeTest.kt`) depends on items 1 and 2 (references `MhPrimary`, `MhDarkPrimary`,
  `lightMediHistoryExtendedColors`).
- Item 8 is last — full verification.

## Key constraints and decisions

1. **No rename of `MediHistoryShapes`** — renaming to `Shapes` would shadow `androidx.compose.material3.Shapes`
   in the same file. Usage comments satisfy the intent of the task note without a breaking rename.

2. **`ThemeTest` is a plain JVM unit test** — Compose colours are pure Kotlin/JVM objects; no
   Android instrumentation or Robolectric is needed. The existing `testDebugUnitTest` task
   is sufficient.

3. **`ExtendedColors.kt` is a new file** — splitting the data class + CompositionLocal + instances
   out of `Color.kt` satisfies the task's requirement for a dedicated `ExtendedColors.kt` and
   keeps `Color.kt` as a pure palette constants file.

4. **`dynamicColor` branch preserved verbatim** — only the static light/dark paths are updated.

5. **`assertNotEquals` for colour tests** — `Color(0xFFRRGGBB)` constructs a 64-bit packed int;
   equality is value-based, so JUnit assertEquals/assertNotEquals is correct.

6. **No new Gradle dependencies** — all required classes (`staticCompositionLocalOf`, `Color`,
   `Typography`, etc.) are already on the compile classpath via the existing BOM.
