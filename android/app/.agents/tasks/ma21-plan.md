# Implementation Plan — MA-21: Complete Material 3 Theme

## Investigation Summary

All seven theme files were read and `ThemeTest` was executed. Results:

| File | Status |
|---|---|
| `Color.kt` | **Gap** — 11 dark scheme constants missing (see item 1) |
| `Theme.kt` — `DarkColorScheme` | **Gap** — 11 `darkColorScheme()` slots unset (see item 2) |
| `ExtendedColors.kt` | ✅ Complete — `onWarning` field exists |
| `Theme.kt` — `LightColorScheme` | ✅ Complete — all M3 slots set |
| `Type.kt` | ✅ Complete — all 15 M3 styles present |
| `Shape.kt` | ✅ Complete |
| `ThemePreview.kt` | ✅ Complete |
| `ThemeTest.kt` | ✅ All 5 tests pass (no changes needed) |

**Verification command** (used to confirm tests pass):
```
"C:\Program Files\Java\jdk-17\bin\java.exe" -cp "C:\Users\admin\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\lib\gradle-launcher-8.7.jar" org.gradle.launcher.GradleMain testDebugUnitTest --tests com.medihistory.ai.ui.theme.ThemeTest
```
Run from: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`

---

## Ordered Implementation Steps

---

- [ ] 1. Add 11 missing dark scheme colour constants to `Color.kt`.

  Append the following block **immediately after** the `val MhDarkOutline` line (the current last line of the dark scheme section).  
  These are the missing raw colour values from the design spec.

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Color.kt`

  **Exact content to append** (after the existing `val MhDarkOutline = Color(0xFF899294)` line):

  ```kotlin
  val MhDarkTertiary             = Color(0xFFB8C4EE)
  val MhDarkOnTertiary           = Color(0xFF223160)
  val MhDarkTertiaryContainer    = Color(0xFF394778)
  val MhDarkOnTertiaryContainer  = Color(0xFFDAE2FF)
  val MhDarkErrorContainer       = Color(0xFF93000A)
  val MhDarkOnErrorContainer     = Color(0xFFFFDAD6)
  val MhDarkOutlineVariant       = Color(0xFF3F484A)
  val MhDarkInverseSurface       = Color(0xFFDEE3E5)
  val MhDarkInverseOnSurface     = Color(0xFF2B3133)
  val MhDarkInversePrimary       = Color(0xFF006874)
  val MhDarkScrim                = Color(0xFF000000)
  ```

  **Verify:** File compiles — run:
  ```
  "C:\Program Files\Java\jdk-17\bin\java.exe" -cp "C:\Users\admin\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\lib\gradle-launcher-8.7.jar" org.gradle.launcher.GradleMain compileDebugKotlin
  ```
  from `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`. Expect `BUILD SUCCESSFUL`.

---

- [ ] 2. Fill the 11 missing slots in `DarkColorScheme` in `Theme.kt`.

  The existing `DarkColorScheme` block ends at the `outline` slot. Add the missing tertiary, errorContainer, outlineVariant, inverseSurface, inverseOnSurface, inversePrimary, and scrim slots.

  **File:** `android/app/src/main/java/com/medihistory/ai/ui/theme/Theme.kt`

  **Find this exact block** (the current end of `DarkColorScheme`):

  ```kotlin
  internal val DarkColorScheme = darkColorScheme(
      primary                = MhDarkPrimary,
      onPrimary              = MhDarkOnPrimary,
      primaryContainer       = MhDarkPrimaryContainer,
      onPrimaryContainer     = MhDarkOnPrimaryContainer,
      secondary              = MhDarkSecondary,
      onSecondary            = MhDarkOnSecondary,
      secondaryContainer     = MhDarkSecondaryContainer,
      onSecondaryContainer   = MhDarkOnSecondaryContainer,
      error                  = MhDarkError,
      onError                = MhDarkOnError,
      background             = MhDarkBackground,
      onBackground           = MhDarkOnBackground,
      surface                = MhDarkSurface,
      onSurface              = MhDarkOnSurface,
      surfaceVariant         = MhDarkSurfaceVariant,
      onSurfaceVariant       = MhDarkOnSurfaceVariant,
      outline                = MhDarkOutline,
  )
  ```

  **Replace with:**

  ```kotlin
  internal val DarkColorScheme = darkColorScheme(
      primary                = MhDarkPrimary,
      onPrimary              = MhDarkOnPrimary,
      primaryContainer       = MhDarkPrimaryContainer,
      onPrimaryContainer     = MhDarkOnPrimaryContainer,
      secondary              = MhDarkSecondary,
      onSecondary            = MhDarkOnSecondary,
      secondaryContainer     = MhDarkSecondaryContainer,
      onSecondaryContainer   = MhDarkOnSecondaryContainer,
      tertiary               = MhDarkTertiary,
      onTertiary             = MhDarkOnTertiary,
      tertiaryContainer      = MhDarkTertiaryContainer,
      onTertiaryContainer    = MhDarkOnTertiaryContainer,
      error                  = MhDarkError,
      onError                = MhDarkOnError,
      errorContainer         = MhDarkErrorContainer,
      onErrorContainer       = MhDarkOnErrorContainer,
      background             = MhDarkBackground,
      onBackground           = MhDarkOnBackground,
      surface                = MhDarkSurface,
      onSurface              = MhDarkOnSurface,
      surfaceVariant         = MhDarkSurfaceVariant,
      onSurfaceVariant       = MhDarkOnSurfaceVariant,
      outline                = MhDarkOutline,
      outlineVariant         = MhDarkOutlineVariant,
      inverseSurface         = MhDarkInverseSurface,
      inverseOnSurface       = MhDarkInverseOnSurface,
      inversePrimary         = MhDarkInversePrimary,
      scrim                  = MhDarkScrim,
  )
  ```

  **Verify:** Run the full ThemeTest suite:
  ```
  "C:\Program Files\Java\jdk-17\bin\java.exe" -cp "C:\Users\admin\.gradle\wrapper\dists\gradle-8.7-bin\bhs2wmbdwecv87pi65oeuq5iu\gradle-8.7\lib\gradle-launcher-8.7.jar" org.gradle.launcher.GradleMain testDebugUnitTest --tests com.medihistory.ai.ui.theme.ThemeTest
  ```
  from `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`. Expect `5 tests completed, BUILD SUCCESSFUL`.

---

## No other files need changes

- `ExtendedColors.kt` — `onWarning` is already defined. `ThemeTest` references `c.onWarning` at line checking `LightExtendedColors` — this compiles fine today and will continue to after these changes.
- `ThemeTest.kt` — all 5 tests pass without modification. No new tests are required for the colour constant additions because the existing `should_have_no_unspecified_colors_in_light_scheme` and `should_have_correct_dark_primary_color` tests already exercise the theme plumbing.
- `Type.kt`, `Shape.kt`, `ThemePreview.kt` — no gaps found.

---

## Dependency order

Item 1 must complete before item 2 because `Theme.kt` references the constants defined in `Color.kt`. Both files compile in the same module, so the Kotlin compiler will catch a missing-reference error at item 2 if item 1 is skipped.
