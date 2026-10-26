# Implementation Plan — MA-22: Navigation Scaffold

## Context

- **Branch**: `feature/MA-22-navigation` based on `origin/develop`
- **Working directory for Gradle**: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`
- **Gradle wrapper**: `.\gradlew.bat`
- **Package root**: `com.medihistory.ai`
- **Source root**: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\main\java\com\medihistory\ai\`
- **Test root**: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\test\java\com\medihistory\ai\`

### Key findings from codebase exploration

- `MainActivity.kt` is minimal — a Scaffold with a centred `Text(app_name)`. No NavController yet.
- `ui/theme/` is the only existing `ui/` subdirectory. No `ui/navigation/`, `ui/screens/`, or `ui/components/` exist yet.
- `libs.versions.toml` uses BOM `2024.06.00` for Compose. All other Compose libs (material3, ui, etc.) omit `version` so the BOM manages them. `material-icons-extended` must follow the same pattern — add a library entry with no version, and reference the BOM-managed group `androidx.compose.material`.
- `navigation = "2.7.7"` is already in `libs.versions.toml` and `androidx-navigation-compose` is already in `build.gradle.kts`. No new navigation version needed.
- `testOptions { unitTests.isIncludeAndroidResources = true }` is set — Robolectric and `androidx-test-core` are already available for unit tests. Existing `ThemeTest.kt` is a plain JVM test (no Robolectric). `NavigationTest.kt` will also be a plain JVM test (pure logic, no Android resources required).
- The test naming convention in use: `should_[outcome]_when_[condition]` and backtick natural-language names.
- No `ui/screens/` placeholder composables exist. They must be created as part of this work.
- **Icons note**: `Icons.Outlined.Home`, `Icons.Outlined.Warning`, and `Icons.Filled.Warning` are in the core Material Icons set; `Icons.Outlined.Description`, `Icons.Outlined.Medication`, `Icons.Outlined.History`, `Icons.Filled.Description`, `Icons.Filled.Medication`, `Icons.Filled.History` are in the **extended** set. The `material-icons-extended` artifact must be added.

---

## Design decisions

1. **`Routes` as a plain `object`** — holds string constants. This keeps route values compile-time constants usable in `when` branches without overhead of a sealed class hierarchy at this layer.

2. **`NavDestination` as a sealed class** — each of the 5 bottom-nav tabs is a `data object` subclass carrying `route`, `selectedIcon`, `unselectedIcon`, `labelResId`, and `contentDescriptionResId`. The companion object exposes `bottomNavDestinations: List<NavDestination>` for iteration in the BottomBar.

3. **Icons strategy**: Use `Icons.Filled.*` for selected state and `Icons.Outlined.*` for unselected state, matching Material 3 guidance. All five icons (Home, Description, Medication, Warning, History) require `material-icons-extended`. Add the dependency managed by the existing BOM (no separate version pin).

4. **`MediHistoryNavHost`** — a `@Composable` function that takes a `NavHostController` and a `Modifier`. The 7 routes are: `dashboard`, `prescriptions`, `prescriptions/{id}` (detail), `medicines`, `alerts`, `alerts/{id}` (detail), `history`. The 5 bottom-bar destinations correspond to the first 5 (no `{id}` params). Detail routes get placeholder screens for now.

5. **`MediHistoryBottomBar`** — a `@Composable` function taking `navController: NavController` and `currentRoute: String?`. Uses `NavigationBar` + `NavigationBarItem`. Touch target size is not a concern here — `NavigationBarItem` uses Material 3's built-in 48dp minimum. Calls `navController.navigate(dest.route)` with `launchSingleTop = true` and `restoreState = true`, and pops back to `Routes.DASHBOARD` to maintain a single back stack entry per destination.

6. **`MainActivity`** — replaces the centred Text with `MediHistoryTheme { rememberNavController(); Scaffold(bottomBar = { MediHistoryBottomBar(...) }) { MediHistoryNavHost(...) } }`. The `currentRoute` for the BottomBar is derived from `navBackStackEntry.destination.route` via `navController.currentBackStackEntryAsState()`.

7. **`NavigationTest.kt`** — plain JVM test (no Robolectric, no Android resources). Tests: Routes constants have expected string values; `NavDestination.bottomNavDestinations` has exactly 5 entries; each destination has a non-blank route; no two destinations share a route; `Dashboard` is the first destination.

---

## Steps

- [ ] 1. Create the work branch `feature/MA-22-navigation` from `origin/develop`.
      Run `git -C "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI" checkout -b feature/MA-22-navigation origin/develop`
      Files: (no source files changed)
      Verify: `git -C "..." branch --show-current` prints `feature/MA-22-navigation`.

- [ ] 2. Add `material-icons-extended` to `libs.versions.toml` and `build.gradle.kts`.
      In `libs.versions.toml`, under `[libraries]` in the Compose BOM block, add:
      ```toml
      androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
      ```
      (No `version` field — BOM manages it, identical pattern to `androidx-compose-material3` and other BOM-managed entries.)
      In `build.gradle.kts`, under `dependencies`, add:
      ```kotlin
      implementation(libs.androidx.compose.material.icons.extended)
      ```
      Files:
      - `android/gradle/libs.versions.toml`
      - `android/app/build.gradle.kts`
      Verify: `.\gradlew.bat assembleDebug` completes without error.

- [ ] 3. Add nav string resources to `strings.xml`.
      Add 5 string entries to `android/app/src/main/res/values/strings.xml`:
      ```xml
      <string name="nav_dashboard">Dashboard</string>
      <string name="nav_prescriptions">Prescriptions</string>
      <string name="nav_medicines">Medicines</string>
      <string name="nav_alerts">Alerts</string>
      <string name="nav_history">History</string>
      ```
      Files: `android/app/src/main/res/values/strings.xml`
      Verify: `.\gradlew.bat lintDebug` — no missing resource errors.

- [ ] 4. Create `Routes.kt` with all 7 route constants.
      Package: `com.medihistory.ai.ui.navigation`
      Contents:
      ```kotlin
      object Routes {
          const val DASHBOARD     = "dashboard"
          const val PRESCRIPTIONS = "prescriptions"
          const val PRESCRIPTION_DETAIL = "prescriptions/{prescriptionId}"
          const val MEDICINES     = "medicines"
          const val ALERTS        = "alerts"
          const val ALERT_DETAIL  = "alerts/{alertId}"
          const val HISTORY       = "history"
      }
      ```
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/Routes.kt`
      Verify: `.\gradlew.bat compileDebugKotlin` — compiles without error.

- [ ] 5. Create placeholder screen composables for all 7 routes.
      Each screen is a minimal `@Composable` function that displays a centred `Text` label. Create one file per screen in `ui/screens/`:
      - `DashboardScreen.kt` — `DashboardScreen()`
      - `PrescriptionListScreen.kt` — `PrescriptionListScreen()`
      - `PrescriptionDetailScreen.kt` — `PrescriptionDetailScreen(prescriptionId: String)`
      - `MedicineExplorerScreen.kt` — `MedicineExplorerScreen()`
      - `AlertsScreen.kt` — `AlertsScreen()`
      - `AlertDetailScreen.kt` — `AlertDetailScreen(alertId: String)`
      - `HistoryScreen.kt` — `HistoryScreen()`
      Each file uses package `com.medihistory.ai.ui.screens`, imports `androidx.compose.foundation.layout.*`, `androidx.compose.material3.Text`, `androidx.compose.runtime.Composable`, and `androidx.compose.ui.Alignment` / `androidx.compose.ui.Modifier`.
      Files:
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/DashboardScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/PrescriptionListScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/PrescriptionDetailScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/MedicineExplorerScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/AlertsScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/AlertDetailScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/HistoryScreen.kt`
      Verify: `.\gradlew.bat compileDebugKotlin` — compiles without error.

- [ ] 6. Create `NavDestination.kt` — sealed class with 5 bottom-nav destinations.
      Package: `com.medihistory.ai.ui.navigation`
      Contents: sealed class `NavDestination` with fields `route: String`, `selectedIcon: ImageVector`, `unselectedIcon: ImageVector`, `labelResId: Int`, `contentDescriptionResId: Int`. Five `data object` subclasses:
      - `Dashboard` → route=`Routes.DASHBOARD`, selectedIcon=`Icons.Filled.Home`, unselectedIcon=`Icons.Outlined.Home`, labelResId=`R.string.nav_dashboard`, contentDescriptionResId=`R.string.nav_dashboard`
      - `Prescriptions` → `Icons.Filled.Description` / `Icons.Outlined.Description`, `R.string.nav_prescriptions`
      - `Medicines` → `Icons.Filled.Medication` / `Icons.Outlined.Medication`, `R.string.nav_medicines`
      - `Alerts` → `Icons.Filled.Warning` / `Icons.Outlined.Warning`, `R.string.nav_alerts`
      - `History` → `Icons.Filled.History` / `Icons.Outlined.History`, `R.string.nav_history`
      Companion object: `val bottomNavDestinations: List<NavDestination> = listOf(Dashboard, Prescriptions, Medicines, Alerts, History)`
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/NavDestination.kt`
      Verify: `.\gradlew.bat compileDebugKotlin` — compiles without error.

- [ ] 7. Create `MediHistoryNavHost.kt`.
      Package: `com.medihistory.ai.ui.navigation`
      A `@Composable` function `MediHistoryNavHost(navController: NavHostController, modifier: Modifier = Modifier)` that creates a `NavHost` with `startDestination = Routes.DASHBOARD` and composable routes for all 7 routes. Detail routes extract the `{prescriptionId}` and `{alertId}` arguments via `backStackEntry.arguments?.getString(...)`. Imports: `androidx.navigation.compose.NavHost`, `androidx.navigation.compose.composable`, `androidx.navigation.compose.NavHostController`.
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`
      Verify: `.\gradlew.bat compileDebugKotlin` — compiles without error.

- [ ] 8. Create `MediHistoryBottomBar.kt`.
      Package: `com.medihistory.ai.ui.navigation`
      A `@Composable` function `MediHistoryBottomBar(navController: NavController, currentRoute: String?)` that renders a `NavigationBar` containing one `NavigationBarItem` per entry in `NavDestination.bottomNavDestinations`. Each item:
      - `selected = currentRoute == dest.route`
      - `onClick`: `navController.navigate(dest.route) { launchSingleTop = true; restoreState = true; popUpTo(Routes.DASHBOARD) { saveState = true } }`
      - `icon`: `Icon(if selected dest.selectedIcon else dest.unselectedIcon, contentDescription = stringResource(dest.contentDescriptionResId))`
      - `label`: `Text(stringResource(dest.labelResId))`
      Imports: `androidx.navigation.NavController`, `androidx.compose.material3.NavigationBar`, `androidx.compose.material3.NavigationBarItem`, `androidx.compose.material3.Icon`, `androidx.compose.material3.Text`, `androidx.compose.ui.res.stringResource`.
      Files: `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryBottomBar.kt`
      Verify: `.\gradlew.bat compileDebugKotlin` — compiles without error.

- [ ] 9. Update `MainActivity.kt` to wire NavController, NavHost, and BottomBar into the Scaffold.
      Replace the existing Scaffold body:
      - Add `val navController = rememberNavController()`
      - Add `val navBackStackEntry by navController.currentBackStackEntryAsState()`
      - Add `val currentRoute = navBackStackEntry?.destination?.route`
      - Change `Scaffold` to: `bottomBar = { MediHistoryBottomBar(navController, currentRoute) }`, and replace the inner Box+Text with `MediHistoryNavHost(navController = navController, modifier = Modifier.padding(innerPadding))`.
      Remove the now-unused imports (`Box`, `Alignment`, `stringResource`, `R` if no longer used).
      Files: `android/app/src/main/java/com/medihistory/ai/MainActivity.kt`
      Verify: `.\gradlew.bat assembleDebug` — builds APK without error.

- [ ] 10. Write unit tests in `NavigationTest.kt`.
      Package: `com.medihistory.ai.ui.navigation`
      Location: `android/app/src/test/java/com/medihistory/ai/ui/navigation/NavigationTest.kt`
      Plain JVM test class (no Robolectric, no `@RunWith`). Tests following the project's naming convention:
      - `should_have_7_route_constants_defined` — asserts all 7 `Routes.*` constants are non-blank strings
      - `should_have_unique_route_constants` — asserts `setOf(Routes.DASHBOARD, Routes.PRESCRIPTIONS, ...)` has 7 distinct values
      - `should_have_exactly_5_bottom_nav_destinations` — asserts `NavDestination.bottomNavDestinations.size == 5`
      - `should_have_unique_routes_in_bottom_nav_destinations` — asserts no two destinations share a route
      - `should_have_dashboard_as_first_bottom_nav_destination` — asserts `NavDestination.bottomNavDestinations.first() is NavDestination.Dashboard`
      - `should_have_dashboard_route_matching_routes_constant` — asserts `NavDestination.Dashboard.route == Routes.DASHBOARD`
      - `should_have_all_bottom_nav_destinations_with_non_blank_routes` — iterates and asserts each route is non-blank
      Files: `android/app/src/test/java/com/medihistory/ai/ui/navigation/NavigationTest.kt`
      Verify: `.\gradlew.bat testDebugUnitTest` — all new tests pass.

- [ ] 11. Run full verification: assembleDebug, lintDebug, testDebugUnitTest.
      From `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android`:
      ```
      .\gradlew.bat assembleDebug
      .\gradlew.bat lintDebug
      .\gradlew.bat testDebugUnitTest
      ```
      All three must succeed with no errors. Fix any issues before proceeding.

- [ ] 12. Commit all changes to the work branch.
      Stage and commit all new and modified files:
      - `android/gradle/libs.versions.toml`
      - `android/app/build.gradle.kts`
      - `android/app/src/main/res/values/strings.xml`
      - `android/app/src/main/java/com/medihistory/ai/ui/navigation/Routes.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/navigation/NavDestination.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryNavHost.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/navigation/MediHistoryBottomBar.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/DashboardScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/PrescriptionListScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/PrescriptionDetailScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/MedicineExplorerScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/AlertsScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/AlertDetailScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/ui/screens/HistoryScreen.kt`
      - `android/app/src/main/java/com/medihistory/ai/MainActivity.kt`
      - `android/app/src/test/java/com/medihistory/ai/ui/navigation/NavigationTest.kt`
      Commit message: `feat(navigation): add navigation scaffold for MA-22`
      Do NOT push. Do NOT use `--no-verify`.

---

## Notes and assumptions

- `Icons.Filled.Home` and `Icons.Outlined.Home` are in the core `material-icons-core` artifact (included via `androidx-compose-material3`), but to keep things consistent and avoid icon availability surprises, all 5 nav icons are sourced from `material-icons-extended`. The extended dependency covers the full set.
- `Icons.Filled.Warning` / `Icons.Outlined.Warning` are also in extended; the entry point from core only guarantees a limited subset.
- The `contentDescriptionResId` per destination reuses the same label string resource for now. These can be split into separate strings (e.g. `cd_nav_dashboard`) in a future accessibility pass when TalkBack testing reveals a need for distinct values.
- Placeholder screens intentionally have no ViewModel or DI — they are stubs that will be replaced by feature tickets F01–F07.
- The `lintDebug` step may produce warnings about unused resources or placeholder composable signatures. These are not errors and do not block the build, but genuine errors must be fixed.
