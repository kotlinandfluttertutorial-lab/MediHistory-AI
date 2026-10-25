# Dependency Catalogue — MediHistory AI Android

**Last updated:** 2026-10-25
**Managed via:** `android/gradle/libs.versions.toml` (Gradle version catalog)
**Policy:** All versions must be exact strings — no `+`, no `latest.release`, no range operators.

---

## Version Pins

| Dependency | Pinned version | Rationale |
|---|---|---|
| Android Gradle Plugin | `8.4.2` | Stable AGP compatible with Gradle 8.7 and Kotlin 1.9.x |
| Kotlin | `1.9.24` | LTS-era stable; compatible with Compose compiler 1.5.14 |
| Compose BOM | `2024.06.00` | June 2024 stable BOM; governs all androidx.compose.* versions |
| Compose Compiler | `1.5.14` | Paired exactly with Kotlin 1.9.24 |
| androidx.core:core-ktx | `1.13.1` | Latest stable at scaffold time |
| androidx.lifecycle | `2.8.2` | Latest stable; includes ViewModel + runtime-ktx |
| androidx.activity:activity-compose | `1.9.0` | Latest stable; required for Compose entry point |
| androidx.compose.material3 | `1.2.1` | Stable M3 release with full color role support |
| Hilt (Dagger) | `2.51.1` | Latest stable; required for constructor injection and WorkManager |
| androidx.hilt:hilt-navigation-compose | `1.2.0` | Stable bridge between Hilt and Compose Navigation |
| Room | `2.6.1` | Latest stable; supports KSP and Flow queries |
| Navigation Compose | `2.7.7` | Latest stable in 2.7.x line; safe back-stack management |
| Kotlinx Coroutines | `1.8.1` | Latest stable; aligned with Kotlin 1.9.x |
| CameraX | `1.3.3` | Latest stable 1.3.x; supports ImageCapture + Preview |
| WorkManager | `2.9.0` | Latest stable; supports Kotlin coroutine workers |
| KSP | `1.9.24-1.0.20` | Paired exactly with Kotlin 1.9.24 |
| JUnit 4 | `4.13.2` | Standard Android unit test runner |
| androidx.test.ext:junit | `1.1.5` | AndroidX JUnit extension for instrumented tests |
| Espresso | `3.5.1` | Standard UI test framework |

---

## Dependency Locking

Gradle dependency locking is enabled in `android/build.gradle.kts` via:

```kotlin
allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}
```

To regenerate lock files after adding or updating a dependency:

```bash
cd android
./gradlew dependencies --write-locks
```

Lock files are committed to version control. A CI check will fail if resolved dependencies drift from the lock file.

---

## Dependabot

Automated dependency update PRs are configured in `.github/dependabot.yml`:

- **Android Gradle** (`/android`) — weekly, Mondays, targeting `main`
- **GitHub Actions** (`/`) — weekly, Mondays, targeting `main`

All Dependabot PRs must pass CI before merging.

---

## Packages Not Yet Added

These packages are planned for later tickets and will be added when their implementation ticket is implemented:

| Package | Planned ticket | Reason not added yet |
|---|---|---|
| SQLCipher | MA-44 (encryption) | Requires key management decision first (see ADR-007) |
| Retrofit / OkHttp | MA-19 (auth scaffold) | Backend scaffold must be ready first |
| Coil (image loading) | MA-24 (prescription list) | Not needed until image display screens |
| ML Kit (on-device OCR) | MA-35 (OCR service) | Provider selection deferred per ADR-006 |

---

## Security Notes

- No dependency uses snapshot, SNAPSHOT, or release-candidate versions in production builds.
- All BOM-managed Compose dependencies intentionally omit a `version.ref` — the BOM governs their versions.
- `hilt-navigation-compose 1.2.0` is pinned inline (not via BOM) — this is intentional and documented.
