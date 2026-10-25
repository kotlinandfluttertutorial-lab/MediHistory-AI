# Implementation Plan — MA-16: Dependency Management (Pinned Versions + Security Audit)

## Pre-work findings

### libs.versions.toml audit
Every entry in `android/gradle/libs.versions.toml` already uses exact version strings.
No `+`, no `latest.release`, no range operators were found.

Specific verifications:
| Entry | Value | Status |
|---|---|---|
| `agp` | `"8.4.2"` | ✓ |
| `kotlin` | `"1.9.24"` | ✓ |
| `compose-bom` | `"2024.06.00"` | ✓ |
| `compose-compiler` | `"1.5.14"` | ✓ |
| `core-ktx` | `"1.13.1"` | ✓ |
| `lifecycle` | `"2.8.2"` | ✓ |
| `activity-compose` | `"1.9.0"` | ✓ |
| `material3` | `"1.2.1"` | ✓ |
| `hilt` | `"2.51.1"` | ✓ |
| `room` | `"2.6.1"` | ✓ |
| `navigation` | `"2.7.7"` | ✓ |
| `coroutines` | `"1.8.1"` | ✓ |
| `camerax` | `"1.3.3"` | ✓ |
| `work` | `"2.9.0"` | ✓ |
| `junit` | `"4.13.2"` | ✓ |
| `junit-ext` | `"1.1.5"` | ✓ |
| `espresso` | `"3.5.1"` | ✓ |
| `ksp` plugin (inline) | `"1.9.24-1.0.20"` | ✓ |
| `androidx-hilt-navigation-compose` (inline) | `"1.2.0"` | ✓ |

**No version string changes are needed.**

Note on Compose BOM entries: `androidx-compose-ui`, `androidx-compose-ui-graphics`,
`androidx-compose-ui-tooling`, `androidx-compose-ui-tooling-preview`,
`androidx-compose-ui-test-manifest`, and `androidx-compose-ui-test-junit4` intentionally
have no explicit version — their version is governed by `compose-bom`. This is correct BOM
usage and is consistent with the `platform(libs.androidx.compose.bom)` call in
`app/build.gradle.kts`. `androidx-compose-material3` uses `version.ref = "material3"` which
overrides the BOM for that artifact; this is intentional pinning and is valid.

---

## Ordered implementation steps

- [ ] 1. **Verify libs.versions.toml is fully pinned (audit — no code change needed)**

  Grep the file for any `+`, `latest`, or range characters to confirm there are nothing
  unpinned before touching build files.

  Files: `android/gradle/libs.versions.toml` (read-only check)

  Verify:
  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android"
  Select-String -Path "gradle\libs.versions.toml" -Pattern '\+|latest\.release|>=|<='
  Pop-Location
  ```
  Expected outcome: zero matches. If any match is returned, fix that entry to an exact
  version string before proceeding.

---

- [ ] 2. **Add `dependencyLocking` to the root build file**

  Add the following block at the end of `android/build.gradle.kts`, after the `plugins {}`
  block. The file currently has no other content after the plugins block, so append directly.

  ```kotlin
  // Dependency locking — prevents unintentional version upgrades.
  // Regenerate lock files with: ./gradlew dependencies --write-locks
  dependencyLocking {
      lockAllConfigurations()
  }
  ```

  Files: `android/build.gradle.kts`

  Verify: File parses — run `.\gradlew.bat help` from `android/` and confirm it exits 0
  with no build-script errors.

  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android"
  .\gradlew.bat help
  Pop-Location
  ```

---

- [ ] 3. **Add `dependencyLocking` to the app build file**

  Add the same block to `android/app/build.gradle.kts`. Place it as the last top-level
  block, after the closing brace of the `dependencies {}` block.

  ```kotlin
  // Dependency locking — prevents unintentional version upgrades.
  // Regenerate lock files with: ./gradlew dependencies --write-locks
  dependencyLocking {
      lockAllConfigurations()
  }
  ```

  Files: `android/app/build.gradle.kts`

  Verify: Same `.\gradlew.bat help` check as step 2.

---

- [ ] 4. **Generate Gradle lock files**

  Run the lock-file generation command from the `android/` directory. This writes one
  `.lockfile` per resolved Gradle configuration under `android/` and `android/app/`.

  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android"
  .\gradlew.bat dependencies --write-locks
  Pop-Location
  ```

  Expected output paths created (one per resolved configuration):
  - `android/gradle.lockfile` — root-project configurations
  - `android/app/gradle.lockfile` — app-module configurations

  If the command exits non-zero, check for network issues (Maven/Google repository
  reachability). The lock-file generation requires network access on first run.

  Verify files were created:
  ```powershell
  Test-Path "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\gradle.lockfile"
  Test-Path "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\gradle.lockfile"
  ```
  Both must return `True`.

---

- [ ] 5. **Confirm assembleDebug still passes after locking**

  Run a full debug build to verify the lock files are consistent with the declared
  dependencies and no resolution errors were introduced.

  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android"
  .\gradlew.bat assembleDebug
  Pop-Location
  ```

  Expected: `BUILD SUCCESSFUL` with the APK produced at
  `android/app/build/outputs/apk/debug/app-debug.apk`.

  If the build fails with a `LOCK_FILE_MISMATCH` error, it means a transitive dependency
  resolved to a version not captured in the lock file. Fix by re-running
  `.\gradlew.bat dependencies --write-locks` and committing the updated lock files.

---

- [ ] 6. **Create `.github/dependabot.yml`**

  Create the file at
  `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.github\dependabot.yml`.

  The file must configure two ecosystems:
  - `gradle` — targeting the `android/` directory, weekly schedule, targeting `main`
  - `github-actions` — targeting the `.github/workflows/` directory, weekly schedule,
    targeting `main`

  Content:
  ```yaml
  # Dependabot configuration
  # Keeps Gradle and GitHub Actions dependencies up to date.
  # PRs are created weekly; the team reviews and merges manually after verifying
  # that pinned versions in libs.versions.toml are updated and lock files are regenerated.
  version: 2

  updates:
    - package-ecosystem: "gradle"
      directory: "/android"
      schedule:
        interval: "weekly"
        day: "monday"
        time: "06:00"
        timezone: "Asia/Kolkata"
      target-branch: "main"
      labels:
        - "dependencies"
        - "android"
      open-pull-requests-limit: 5
      # Dependabot PRs for Gradle will suggest version bumps only.
      # After merging, manually update libs.versions.toml and regenerate lock files.

    - package-ecosystem: "github-actions"
      directory: "/"
      schedule:
        interval: "weekly"
        day: "monday"
        time: "06:00"
        timezone: "Asia/Kolkata"
      target-branch: "main"
      labels:
        - "dependencies"
        - "ci"
      open-pull-requests-limit: 5
  ```

  Files: `.github/dependabot.yml` (new file)

  Verify: YAML parses correctly.
  ```powershell
  # PowerShell does not include a YAML parser natively; verify the file exists and
  # has non-zero size as a minimum check.
  $f = Get-Item "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.github\dependabot.yml"
  Write-Host "Size: $($f.Length) bytes"
  ```
  Expected: `Size: <non-zero> bytes`.
  Full validation happens when GitHub reads the file on the next PR to main.

---

- [ ] 7. **Create `docs/architecture/dependencies.md`**

  Create the dependency catalogue at
  `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\docs\architecture\dependencies.md`.

  The file must list every Gradle dependency from `libs.versions.toml` with its pinned
  version and a one-line rationale. Use the exact versions from the TOML at the time of
  writing (reproduced below). If a dependency is updated between planning and
  implementation, use the version actually in `libs.versions.toml` at implementation time.

  Content to write:

  ```markdown
  # Android Dependency Catalogue

  > **Last updated:** <!-- date of commit -->
  > All versions are exact pins. Managed via `android/gradle/libs.versions.toml`.
  > Lock files: `android/gradle.lockfile`, `android/app/gradle.lockfile`.
  > To upgrade a dependency: update the version in `libs.versions.toml`, then run
  > `./gradlew dependencies --write-locks` and commit the updated lock files.

  ## Build plugins

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `android-application` | `com.android.application` | `8.4.2` | Official AGP release matching compileSdk 35 support. |
  | `kotlin-android` | `org.jetbrains.kotlin.android` | `1.9.24` | Kotlin release compatible with Compose compiler 1.5.14 and KSP 1.9.24-1.0.20. |
  | `hilt` | `com.google.dagger.hilt.android` (plugin) | `2.51.1` | Paired with the Hilt runtime to ensure codegen compatibility. |
  | `ksp` | `com.google.devtools.ksp` | `1.9.24-1.0.20` | KSP release that matches Kotlin 1.9.24; required for Hilt and Room annotation processing. |

  ## AndroidX Core

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-core-ktx` | `androidx.core:core-ktx` | `1.13.1` | Latest stable KTX extensions for Android platform APIs; required baseline for all modules. |

  ## Lifecycle

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-lifecycle-runtime-ktx` | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.2` | Provides `lifecycleScope` and `repeatOnLifecycle`; latest stable 2.8.x. |
  | `androidx-lifecycle-viewmodel` | `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.8.2` | ViewModel with coroutine scope support; same series as runtime for consistency. |

  ## Activity

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-activity-compose` | `androidx.activity:activity-compose` | `1.9.0` | Compose entry-point (`setContent`); latest stable compatible with Compose BOM 2024.06.00. |

  ## Compose

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-compose-bom` | `androidx.compose:compose-bom` | `2024.06.00` | Bill of Materials that aligns all Compose artifact versions; pinned to a tested BOM release. |
  | `compose-compiler` | _(compiler extension, not a library)_ | `1.5.14` | Compose compiler version compatible with Kotlin 1.9.24. |
  | `androidx-compose-ui` | `androidx.compose.ui:ui` | _via BOM_ | Core Compose UI runtime; version governed by BOM. |
  | `androidx-compose-ui-graphics` | `androidx.compose.ui:ui-graphics` | _via BOM_ | Canvas/paint APIs; version governed by BOM. |
  | `androidx-compose-ui-tooling` | `androidx.compose.ui:ui-tooling` | _via BOM_ | Layout inspector and preview rendering (debug only); version governed by BOM. |
  | `androidx-compose-ui-tooling-preview` | `androidx.compose.ui:ui-tooling-preview` | _via BOM_ | `@Preview` annotation support; version governed by BOM. |
  | `androidx-compose-ui-test-manifest` | `androidx.compose.ui:ui-test-manifest` | _via BOM_ | Test manifest for Compose UI tests (debug only); version governed by BOM. |
  | `androidx-compose-ui-test-junit4` | `androidx.compose.ui:ui-test-junit4` | _via BOM_ | JUnit 4 integration for Compose UI tests; version governed by BOM. |
  | `androidx-compose-material3` | `androidx.compose.material3:material3` | `1.2.1` | Material Design 3 components; explicitly pinned outside BOM to lock the exact API surface. |

  ## Navigation

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-navigation-compose` | `androidx.navigation:navigation-compose` | `2.7.7` | Compose navigation host; latest 2.7.x stable, compatible with current Compose BOM. |

  ## Dependency Injection

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `hilt-android` | `com.google.dagger:hilt-android` | `2.51.1` | Hilt DI runtime; selected per tech steering as the mandatory DI framework. |
  | `hilt-android-compiler` | `com.google.dagger:hilt-android-compiler` | `2.51.1` | Hilt KSP codegen; must match runtime version exactly. |
  | `androidx-hilt-navigation-compose` | `androidx.hilt:hilt-navigation-compose` | `1.2.0` | `hiltViewModel()` helper for Navigation Compose; latest stable, explicitly pinned. |

  ## Local Database

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-room-runtime` | `androidx.room:room-runtime` | `2.6.1` | Room persistence library; selected per tech steering for local DB. |
  | `androidx-room-ktx` | `androidx.room:room-ktx` | `2.6.1` | Coroutine/Flow extensions for Room; same version as runtime. |
  | `androidx-room-compiler` | `androidx.room:room-compiler` | `2.6.1` | KSP-based Room codegen; must match runtime version exactly. |

  ## Async

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `kotlinx-coroutines-android` | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` | Android dispatcher and coroutine runtime; latest stable 1.8.x. |

  ## Camera

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-camera-core` | `androidx.camera:camera-core` | `1.3.3` | CameraX core APIs; selected per tech steering for camera capture. |
  | `androidx-camera-camera2` | `androidx.camera:camera-camera2` | `1.3.3` | Camera2 implementation backend for CameraX; same version as core. |
  | `androidx-camera-lifecycle` | `androidx.camera:camera-lifecycle` | `1.3.3` | Lifecycle-aware camera binding; same version as core. |
  | `androidx-camera-view` | `androidx.camera:camera-view` | `1.3.3` | `PreviewView` composable integration; same version as core. |

  ## Background Work

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `androidx-work-runtime-ktx` | `androidx.work:work-runtime-ktx` | `2.9.0` | WorkManager with coroutine support; selected per tech steering for background OCR tasks. |

  ## Testing

  | Alias | Artifact | Pinned version | Rationale |
  |---|---|---|---|
  | `junit` | `junit:junit` | `4.13.2` | JUnit 4 for local unit tests; industry-standard, pinned to latest 4.x security-patched release. |
  | `androidx-junit` | `androidx.test.ext:junit` | `1.1.5` | AndroidX JUnit runner extensions for instrumented tests. |
  | `androidx-espresso-core` | `androidx.test.espresso:espresso-core` | `3.5.1` | UI interaction testing for instrumented tests. |

  ---

  ## Dependency locking

  Lock files (`gradle.lockfile`) are committed alongside source code and verified in CI.
  They prevent Gradle from silently resolving a newer transitive dependency version.

  To update a dependency:
  1. Change the version in `android/gradle/libs.versions.toml`.
  2. Run `./gradlew dependencies --write-locks` from `android/`.
  3. Commit the updated `libs.versions.toml` and both `gradle.lockfile` files together.
  4. The CI pipeline will verify the lock files on every PR to main.

  ## Dependabot

  `.github/dependabot.yml` is configured to open weekly PRs for:
  - Gradle dependency upgrades (targeting the `android/` directory)
  - GitHub Actions version bumps

  Dependabot PRs for Gradle suggest new versions only; the engineer must update
  `libs.versions.toml` and regenerate lock files manually before merging.
  ```

  Files: `docs/architecture/dependencies.md` (new file)

  Verify: File exists and is non-empty.
  ```powershell
  $f = Get-Item "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\docs\architecture\dependencies.md"
  Write-Host "Lines: $((Get-Content $f).Count)"
  ```
  Expected: line count > 50.

---

- [ ] 8. **Create the feature branch and commit all changes**

  All changed/new files must be committed on branch `feature/MA-16-dependency-management`.
  Do **not** push to `main` or `develop` directly.

  Files to stage (in order of logical grouping):
  ```
  android/build.gradle.kts
  android/app/build.gradle.kts
  android/gradle.lockfile
  android/app/gradle.lockfile
  .github/dependabot.yml
  docs/architecture/dependencies.md
  ```

  Commands:
  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI"

  # Create and switch to the feature branch
  git checkout -b feature/MA-16-dependency-management

  # Stage specific files (never git add -A to avoid committing local.properties)
  git add android/build.gradle.kts
  git add android/app/build.gradle.kts
  git add android/gradle.lockfile
  git add android/app/gradle.lockfile
  git add .github/dependabot.yml
  git add docs/architecture/dependencies.md

  # Verify staged files before committing
  git status

  # Commit
  git commit -m "chore(deps): pin all Gradle versions, add dependency locking, and configure Dependabot

  - Verified all libs.versions.toml entries use exact version strings (no + or ranges)
  - Added dependencyLocking { lockAllConfigurations() } to root and app build.gradle.kts
  - Generated gradle.lockfile for root and app module
  - Added .github/dependabot.yml for weekly Gradle and GitHub Actions updates (targeting main)
  - Added docs/architecture/dependencies.md with full dependency catalogue

  Closes MA-16"

  Pop-Location
  ```

  Verify:
  ```powershell
  Push-Location "j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI"
  git log --oneline -3
  git branch --show-current
  Pop-Location
  ```
  Expected: current branch is `feature/MA-16-dependency-management` and the commit
  message appears at the top of the log.

---

## Dependency order summary

```
Step 1 (audit)
  → Step 2 (root build.gradle.kts)
  → Step 3 (app build.gradle.kts)
    → Step 4 (generate lock files — needs both build files updated)
      → Step 5 (assembleDebug — needs lock files to exist)
Steps 6, 7 (dependabot.yml, dependencies.md) — independent of Steps 2-5, can be done at any point
  → Step 8 (commit — needs all prior steps complete)
```

## Notes and assumptions

1. **Network required for step 4.** Gradle must resolve all configurations from Google and
   Maven Central to write lock files. If running in an air-gapped environment, pre-populate
   the Gradle cache first.

2. **No `local.properties` in commit.** `local.properties` is git-ignored and must never be
   staged. The `backendBaseUrl` will be empty string during the lock-file generation run,
   which is fine — it only affects `BuildConfig`, not dependency resolution.

3. **Compose BOM entries without explicit versions.** The six BOM-governed Compose entries
   (`ui`, `ui-graphics`, `ui-tooling`, `ui-tooling-preview`, `ui-test-manifest`,
   `ui-test-junit4`) have no `version` in the TOML. This is correct BOM usage; Gradle
   resolves their versions through the BOM constraint, which is itself pinned to
   `2024.06.00`. The lock files will capture the actual resolved versions.

4. **`material3` version override.** `androidx-compose-material3` has `version.ref =
   "material3"` which pins it to `1.2.1` independently of the BOM. This is intentional and
   is not a violation of the pinning policy.

5. **Dependabot and lock files.** When Dependabot opens a Gradle PR, it cannot automatically
   regenerate Gradle lock files. The PR will fail CI until an engineer manually updates
   `libs.versions.toml` and runs `--write-locks`. This is expected behavior and is
   documented in the dependency catalogue.
