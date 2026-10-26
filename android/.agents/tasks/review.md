# Android Gradle Project Scaffold — MHA-001

The scaffold creates a complete Android project skeleton: Gradle wrapper at 8.7, version catalog, root and app-level build files, Hilt+Room+CameraX+WorkManager dependency declarations, a Material 3 theme with custom warning tokens, minimal Activity, and test stubs. All dependencies are pinned to exact versions. The `BACKEND_BASE_URL` secret is sourced from `local.properties` or a CI environment variable — never a literal. `assembleDebug`, `lintDebug`, and `testDebugUnitTest` outputs are present in the build directory, confirming the three required checks were run.

**Watch for:** `compileSdk`/`targetSdk` were bumped from 34→35 in a post-scaffold commit that ran before build evidence was captured (the lint report still references `targetSdk = 34`); the criteria require 34 but the current source says 35. `local.properties` exists on disk and is git-ignored, but the `git ls-files` invocation returned output suggesting it may be staged — this needs confirmation. The `rootProject.name` is `"MediHistoryAI"` (no hyphen) rather than the required `"MediHistory-AI"`.

**Verdict**: CHANGES_REQUESTED

## High-level view

The version catalog, wrapper properties, root build file, and app build file are well-formed and structurally complete. All declared versions are exact pinned strings with no `+` or range operators. The `BACKEND_BASE_URL` pattern — read from `local.properties` → env var → empty string — is the correct approach and no URL literal appears anywhere in source.

The `compileSdk` and `targetSdk` values were 34 at scaffold time but a subsequent ad-hoc commit (`a432e0b`, "Update Android compileSdk and targetSdk to 35") changed them before the current state was reviewed. The functional criteria require `compileSdk=34, targetSdk=34`. The current source has `compileSdk=35, targetSdk=35`. The lint report confirms it ran against `targetSdk=34`, meaning lint ran on the scaffold state, but the source no longer matches the tested state. This is a criterion mismatch that requires explicit sign-off or revert.

`settings.gradle.kts` sets `rootProject.name = "MediHistoryAI"` (camel-case, no hyphen). The review criteria require `"MediHistory-AI"`. This is a minor naming discrepancy but it affects how Gradle labels the project in IDE and CI output.

`local.properties` exists on disk, is listed in `.gitignore`, and `git cat-file` confirms it is not in the git tree at `HEAD`. It contains only `sdk.dir` — an absolute Windows path that must not be committed.

Build evidence is present on disk as build outputs but was not recorded in a commit message or a verification notes file. The three tasks (`assembleDebug`, `lintDebug`, `testDebugUnitTest`) can be inferred from the presence of `app-debug.apk`, `lint-results-debug.txt`, and the `testDebugUnitTest/` HTML report, but the criteria require the coder to have "recorded the result" — the commit messages contain no build output and no separate evidence file exists.

<details>
<summary>Issues (4)</summary>

1. **compileSdk/targetSdk mismatch** — Criteria require `compileSdk=34, targetSdk=34`. Commit `a432e0b` changed both to 35 after the scaffold and before evidence was captured. The current `app/build.gradle.kts` says 35. Either revert the bump or get the review criteria updated to accept 35 — the two cannot both be true at once.

2. **rootProject.name wrong** — `settings.gradle.kts` has `rootProject.name = "MediHistoryAI"` but the criteria require `"MediHistory-AI"`. Fix by changing the string literal in `settings.gradle.kts`.

3. **Build evidence not recorded in a commit or notes file** — The criteria require the coder to have run and *recorded* `assembleDebug`, `lintDebug`, and `testDebugUnitTest`. Build artifacts exist locally in `build/` but are git-ignored and the commit messages contain no output. Add a `verification-notes.md` (or similar) to `.agents/tasks/` capturing the pass/fail result of each of the three commands, committed alongside the scaffold.

4. **local.properties git-ignore confirmation** — `local.properties` is correctly excluded from the git tree at HEAD, but it exists on disk. Confirm with `git status --short` that it shows as untracked (not staged). The `sdk.dir` line in `local.properties` is an absolute Windows path — acceptable for a local dev file that is git-ignored, but the file must never be committed.

</details>

<details>
<summary>Details</summary>

### compileSdk/targetSdk drift from criteria

The scaffold commit (`c045232`) set `compileSdk = 34` and `targetSdk = 34` as required. A separate commit (`a432e0b`, "Update Android compileSdk and targetSdk to 35") changed both values before the branch was reviewed. The lint report in `build/reports/lint-results-debug.txt` explicitly references `targetSdk = 34` in its `OldTargetApi` warning, confirming lint ran against the 34 state. The current `app/build.gradle.kts` on disk has `compileSdk = 35` and `targetSdk = 35`. The review criteria specify 34 in both positions. These cannot both be satisfied simultaneously in the current state. This is a confirmed blocking discrepancy.

### rootProject.name format

`settings.gradle.kts` line: `rootProject.name = "MediHistoryAI"`. The criteria require `"MediHistory-AI"`. Gradle project names are cosmetic for most purposes but affect the generated `.idea/` project name, CI log labels, and any artifact names that derive from the project name (e.g. AAB file names if the project name is used in `archivesName`). The fix is a one-character change to the string literal. Confirmed by reading the file directly.

### Build evidence gap

`app/build/outputs/apk/debug/app-debug.apk` exists (confirmed, 11 MB). `build/reports/lint-results-debug.txt` exists with `0 errors, 81 warnings`. `build/reports/tests/testDebugUnitTest/index.html` exists showing 1 test, 0 failures, generated by Gradle 8.7. All three tools were run. The gap is that none of this was captured in a committed artifact. Build output directories are covered by `.gitignore` (`build/`), so these files are ephemeral. A machine reinstalling from source would have no record that these checks passed before submission.

### Lint warnings

The 81 warnings are all advisory. The `OldTargetApi` warning (`targetSdk = 34`) was present when lint ran but disappears at `targetSdk = 35` — which is now on disk but introduces the criterion mismatch described above. The `GradleDependency` warnings flag newer library versions available; these are expected for a scaffold pinned to specified versions and are not blocking.

</details>

## File map

<details>
<summary>Changed files</summary>

| File | What changed |
|---|---|
| `android/settings.gradle.kts` | Project name, plugin repos, `:app` include — Kotlin DSL |
| `android/build.gradle.kts` | Root build file, four plugins declared with `apply false` |
| `android/gradle/libs.versions.toml` | Full version catalog — 18 versions, all exact pinned |
| `android/gradle/wrapper/gradle-wrapper.properties` | Gradle 8.7-bin distribution URL |
| `android/app/build.gradle.kts` | App module: applicationId, SDK levels, Compose, BuildConfig, full dependency block |
| `android/app/src/main/AndroidManifest.xml` | Package, application class, single exported MainActivity |
| `android/app/src/main/java/…/MainActivity.kt` | Minimal Compose activity with `MediHistoryTheme` |
| `android/app/src/main/java/…/MediHistoryApplication.kt` | `@HiltAndroidApp` Application subclass |
| `android/app/src/main/java/…/ui/theme/Color.kt` | All design token colors including custom warning extension |
| `android/app/src/main/java/…/ui/theme/Theme.kt` | `MediHistoryTheme` composable, light/dark schemes, `CompositionLocal` |
| `android/app/src/main/java/…/ui/theme/Shape.kt` | Material 3 shape scale |
| `android/app/src/main/java/…/ui/theme/Type.kt` | Typography scale stub |
| `android/app/src/test/…/ExampleUnitTest.kt` | Scaffold unit test with synthetic data |
| `android/app/src/androidTest/…/ExampleInstrumentedTest.kt` | Scaffold instrumented test |
| `android/gradle.properties` | Gradle JVM args, AndroidX flags, non-transitive R |

Full diff: `git diff c045232..HEAD -- android/`

</details>
