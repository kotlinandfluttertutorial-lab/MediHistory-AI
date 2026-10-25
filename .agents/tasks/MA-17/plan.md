# Implementation Plan — MA-17: Environment Configuration / No Secrets in APK

## Pre-flight findings (from code exploration)

### `android/app/build.gradle.kts` — current state
The existing pattern reads `BACKEND_BASE_URL` from `local.properties` via the key
`backend.base.url`, then falls back to the env var `BACKEND_BASE_URL`, then to `""`.
The `buildConfigField` call emits the value as a quoted `String`.

**Gaps identified:**
1. `local.properties` lives inside `android/` (not the workspace root), but `.gitignore`
   at the workspace root only ignores `local.properties` at the root. The android-subdirectory
   `android/local.properties` is not explicitly listed in the root `.gitignore` — it relies on
   the implicit Gradle convention. The `.gitignore` must explicitly cover `android/local.properties`
   or have a recursive `**/local.properties` rule.
2. `ENABLE_CLOUD_BACKUP` and `ENABLE_INTERACTION_CHECK` feature-flag `Boolean` BuildConfig fields
   do not exist yet.
3. `.env.example` contains `CLOUD_BACKUP_ENABLED` (a backend env var) but does **not** contain the
   Android-specific BuildConfig names `BACKEND_BASE_URL`, `ENABLE_CLOUD_BACKUP`, or
   `ENABLE_INTERACTION_CHECK`.
4. No unit test verifies that `BuildConfig.BACKEND_BASE_URL` is non-null or that the feature flags
   default to `false`.
5. No `docs/architecture/environment-config.md` document exists.
6. No `local.properties.example` exists in `android/` — developers have no template.

---

## Items

- [ ] 1. Harden the `.gitignore` to explicitly ignore `android/local.properties`.
      The root `.gitignore` already ignores `local.properties` (bare rule), which Git resolves
      relative to the root — it does NOT cover `android/local.properties`. Add a recursive rule
      `**/local.properties` (or the explicit path) so the file is covered no matter where Gradle
      places it. Also confirm that `android/.gitignore` (if it exists) does not accidentally
      track the file.
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.gitignore`
      Verify: After the edit, run `git check-ignore -v android/local.properties` from the
      workspace root and confirm it outputs a matching rule.

- [ ] 2. Add `ENABLE_CLOUD_BACKUP` and `ENABLE_INTERACTION_CHECK` BuildConfig booleans to
      `android/app/build.gradle.kts`.
      Follow the identical pattern already used for `backendBaseUrl`:
      - Read from `local.properties` first (keys `feature.enable_cloud_backup` and
        `feature.enable_interaction_check`).
      - Fall back to env vars `ENABLE_CLOUD_BACKUP` and `ENABLE_INTERACTION_CHECK`.
      - Fall back to `"false"` (the string literal Gradle needs for boolean fields).
      Parse the resulting string with `toBoolean()` or pass the raw string directly to
      `buildConfigField("Boolean", ...)` — the safe approach is to emit `buildConfigField`
      with the string `"false"` or `"true"` (no extra quoting, since Boolean fields do not use
      surrounding quotes). Declare both `val` variables above `android {}` alongside
      `backendBaseUrl`.
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\build.gradle.kts`
      Verify: `cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android && gradlew assembleDebug`
      — build must succeed; inspect `app/build/generated/source/buildConfig/debug/com/medihistory/ai/BuildConfig.java`
      and confirm `ENABLE_CLOUD_BACKUP = false` and `ENABLE_INTERACTION_CHECK = false` are present.

- [ ] 3. Create `android/local.properties.example` as a developer template.
      This file is committed (it is an example, not a secret). It documents every key that
      `build.gradle.kts` reads from `local.properties`, with placeholder values and comments.
      Fields to include:
      ```
      # Copy this file to local.properties and fill in real values.
      # local.properties is git-ignored and must NEVER be committed.
      sdk.dir=/path/to/Android/Sdk
      backend.base.url=http://10.0.2.2:8000
      feature.enable_cloud_backup=false
      feature.enable_interaction_check=false
      ```
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\local.properties.example`
      Verify: File exists and is not listed in `.gitignore` (it should be committable).

- [ ] 4. Update `.env.example` at the workspace root to add the Android BuildConfig variable names.
      The file already has backend env vars. Add a new subsection under `# === Android / Build ===`
      (which already exists) for the three Android BuildConfig environment variables that CI uses:
      ```
      BACKEND_BASE_URL=
      ENABLE_CLOUD_BACKUP=false
      ENABLE_INTERACTION_CHECK=false
      ```
      Do not duplicate `CLOUD_BACKUP_ENABLED` — that is a separate backend var; just add the three
      Android-specific names with comments explaining they are read by Gradle. Remove or do not
      touch the existing backend `CLOUD_BACKUP_ENABLED=false` line.
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.env.example`
      Verify: Manually confirm the three names appear in the file. Then run
      `grep -E "BACKEND_BASE_URL|ENABLE_CLOUD_BACKUP|ENABLE_INTERACTION_CHECK"
      j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.env.example` — must return three lines.

- [ ] 5. Write `EnvironmentConfigTest.kt` in the existing unit-test package.
      Create the test file following the exact package, import, and style conventions of the
      existing `ExampleUnitTest.kt` (package `com.medihistory.ai`, JUnit 4, no Hilt, no Android
      instrumentation). The test class must cover:
      a. `BuildConfig.BACKEND_BASE_URL` is not null — use `assertNotNull`.
      b. `BuildConfig.ENABLE_CLOUD_BACKUP` is `false` — use `assertFalse`.
      c. `BuildConfig.ENABLE_INTERACTION_CHECK` is `false` — use `assertFalse`.
      Test method names follow the project convention:
      `should_notBeNull_when_backendBaseUrlIsBuilt`,
      `should_beFalse_when_cloudBackupFlagIsDefault`,
      `should_beFalse_when_interactionCheckFlagIsDefault`.
      Note: JVM unit tests cannot access `BuildConfig` fields at runtime in the standard test
      source set unless the project is configured with `unitTestVariant`. If `BuildConfig` is not
      available in the JVM test classpath, gate the test with a simple reflection-based accessor
      or a `try/catch` that marks the test as skipped — **do not delete the test**. A better
      approach (and the correct one for this project) is to confirm that `buildFeatures.buildConfig
      = true` is already set (it is, as read above), which makes `BuildConfig` available to local
      unit tests from the `testDebugUnitTest` task. Proceed with direct `BuildConfig` references.
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\test\java\com\medihistory\ai\EnvironmentConfigTest.kt`
      Verify: `cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android && gradlew testDebugUnitTest`
      — all tests must pass including the three new ones.

- [ ] 6. Create `docs/architecture/environment-config.md`.
      This document belongs alongside the other architecture docs already present in
      `docs/architecture/` (`architecture.md`, `security-threat-model.md`, etc.).
      Required sections:
      **1. Purpose** — explain the no-secrets-in-APK rule and why `BuildConfig` is the approved
      mechanism for environment-specific values.
      **2. Complete BuildConfig field list** — table with columns: Field Name | Type | Purpose |
      Default | local.properties key | Env var name. Rows:
      - `BACKEND_BASE_URL` | `String` | Base URL for FastAPI backend | `""` | `backend.base.url` | `BACKEND_BASE_URL`
      - `ENABLE_CLOUD_BACKUP` | `Boolean` | Feature flag: cloud backup (opt-in) | `false` | `feature.enable_cloud_backup` | `ENABLE_CLOUD_BACKUP`
      - `ENABLE_INTERACTION_CHECK` | `Boolean` | Feature flag: interaction checking (F09) | `false` | `feature.enable_interaction_check` | `ENABLE_INTERACTION_CHECK`
      **3. Local development setup** — step-by-step: copy `android/local.properties.example` to
      `android/local.properties`, fill in `sdk.dir` and `backend.base.url`, do not commit the file.
      **4. CI environment variable setup** — how to configure GitHub Actions secrets/env vars:
      set `BACKEND_BASE_URL`, `ENABLE_CLOUD_BACKUP`, `ENABLE_INTERACTION_CHECK` as repository or
      workflow env vars; the Gradle build reads them automatically when `local.properties` is absent.
      **5. Security rationale** — why secrets must never appear in `BuildConfig` (APK is
      extractable; `BuildConfig` fields are visible in decompiled bytecode), what types of values
      ARE acceptable in `BuildConfig` (non-secret URLs, boolean flags), and what must stay
      server-side (API keys, JWT secrets, DB credentials).
      **6. Adding a new field** — checklist for implementers: add val in `build.gradle.kts`,
      add row to `.env.example`, add row to `local.properties.example`, add row to this document,
      add unit test case.
      Files: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\docs\architecture\environment-config.md`
      Verify: File exists and is readable. Spot-check that the table contains all three BuildConfig
      fields.

- [ ] 7. Run the full verification suite.
      Execute all four verification commands from the task spec and confirm each passes:
      a. `cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android && gradlew assembleDebug`
         — build succeeds with no errors.
      b. `grep -r "http://" j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\main`
         — must return no output (no hardcoded HTTP URLs in main source).
      c. `grep -rE "(api_key|secret|password|token)" j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android\app\src\main`
         — must return no output (no hardcoded secrets in main source).
      d. `cd j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\android && gradlew testDebugUnitTest`
         — all unit tests pass including `EnvironmentConfigTest`.
      Record the output of each command. If any command returns unexpected matches, trace them to
      their source and fix before marking this item done.
      Files: (no file changes — verification only)
      Verify: All four commands exit cleanly as described above.

---

## Dependency order

Items 1 and 3 are independent and can be done in any order.
Item 2 (Gradle changes) must complete before item 5 (tests reference new BuildConfig fields).
Item 4 (`.env.example`) is independent of all other items.
Item 6 (docs) is independent and can be done in any order.
Item 7 (full verification) must be last — it depends on items 1–6 all being complete.

## Notes and assumptions

- The `buildFeatures { buildConfig = true }` flag is already set in `build.gradle.kts` — no
  change needed to enable `BuildConfig` generation.
- `Boolean` BuildConfig fields are declared as `buildConfigField("Boolean", "NAME", "false")` —
  the value is a raw Kotlin/Java literal, not a quoted string, so no extra quoting is needed.
- The `local.properties` file in `android/` only contains `sdk.dir` today — the implementer
  does not need to modify it (it is git-ignored). The example file created in item 3 is what
  gets committed.
- No new Gradle dependencies are required for any of these changes.
- `docs/architecture/environment-config.md` does not exist yet — it will be a new file.
