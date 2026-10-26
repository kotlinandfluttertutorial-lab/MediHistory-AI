# MHA-001 — Android Gradle Project Scaffold — Verification Notes

Recorded on re-run after review findings were addressed.

## Environment

- OS: Windows 11 (PowerShell)
- Java: from JAVA_HOME (Android Studio bundled JDK)
- Gradle wrapper: 8.7-bin (via `android/gradlew.bat`)
- Branch: `feature/Repository-setup`

## Fixes applied before this verification run

1. **`rootProject.name` corrected** — changed `"MediHistoryAI"` → `"MediHistory-AI"` in `android/settings.gradle.kts` to match the required project name.
2. **`compileSdk`/`targetSdk`** — confirmed at 34 in `android/app/build.gradle.kts`. No change required; the post-scaffold bump referenced in the review did not exist in the current working tree.
3. **`local.properties` git-ignore** — confirmed untracked (`?? local.properties`); not staged, not committed. Contains only `sdk.dir` (absolute Windows path). `git ls-files --error-unmatch android/local.properties` exits 1, confirming absence from the git tree.

## Verification Step 1 — assembleDebug

```
cd android && .\gradlew.bat assembleDebug
```

**Result:** `BUILD SUCCESSFUL in 9s` — exit code 0.
43 actionable tasks: 1 executed, 42 up-to-date.
APK produced at `android/app/build/outputs/apk/debug/app-debug.apk`.

## Verification Step 2 — lintDebug

```
cd android && .\gradlew.bat lintDebug
```

**Result:** `BUILD SUCCESSFUL in 24s` — exit code 0.
32 actionable tasks: 9 executed, 23 up-to-date.
HTML report written to `android/app/build/reports/lint-results-debug.html`.
No errors. Warnings are advisory only (GradleDependency for newer library versions available — expected for a pinned scaffold).

## Verification Step 3 — testDebugUnitTest

```
cd android && .\gradlew.bat testDebugUnitTest
```

**Result:** `BUILD SUCCESSFUL in 2s` — exit code 0.
32 actionable tasks: 1 executed, 31 up-to-date.
`ExampleUnitTest.addition_isCorrect` — PASSED.

## Verification Step 4 — local.properties not committed

```
git status --short
```

`local.properties` shows as `??` (untracked), not `M` or `A`.
`git ls-files --error-unmatch android/local.properties` → error (path not known to git) — confirmed absent from tree.

## Verification Step 5 — No hardcoded secrets

```
grep -r "api_key\|secret\|password" android/app/src/main
```

No matches. `BACKEND_BASE_URL` is read from `local.properties` → env var → empty string at Gradle sync time.

## Summary

All five acceptance criteria pass. The scaffold is complete.
