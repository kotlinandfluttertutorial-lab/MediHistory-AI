# Environment Configuration

## Purpose

MediHistory AI follows a strict no-secrets-in-APK rule. Android APK files are extractable and
inspectable by anyone who obtains a device or a copy of the APK. `BuildConfig` fields are compiled
into the generated `BuildConfig.java` class, which is readable by decompilation tools. Therefore:

- **Non-secret configuration** (backend base URL, boolean feature flags) may live in `BuildConfig`.
- **Real secrets** (API keys, JWT secrets, database credentials, encryption keys) must stay
  server-side as environment variables and must never appear in any source file, resource file,
  or compiled class in the APK.

`android/app/build.gradle.kts` reads configuration values in priority order:
1. `android/local.properties` — developer-local file, git-ignored, never committed.
2. CI environment variables — injected by the pipeline at build time.
3. Hard-coded defaults in `build.gradle.kts` (safe values only, e.g. `""` and `false`).

---

## BuildConfig Fields

| Field | Type | Purpose | Default | local.properties key | Env var |
|---|---|---|---|---|---|
| `BACKEND_BASE_URL` | `String` | Base URL for FastAPI backend. Empty string disables network calls. | `""` | `backend.base.url` | `BACKEND_BASE_URL` |
| `ENABLE_CLOUD_BACKUP` | `boolean` | Feature flag for opt-in cloud backup (F10). | `false` | `enable.cloud.backup` | `ENABLE_CLOUD_BACKUP` |
| `ENABLE_INTERACTION_CHECK` | `boolean` | Feature flag for drug interaction checking (F09, optional in MVP). | `false` | `enable.interaction.check` | `ENABLE_INTERACTION_CHECK` |

---

## Local Development Setup

1. Copy the template to create your local properties file:

   ```
   cp android/local.properties.example android/local.properties
   ```

2. Open `android/local.properties` and fill in the required values:

   ```properties
   # Path to your Android SDK installation
   sdk.dir=/path/to/Android/Sdk

   # URL of the local FastAPI dev server (Android emulator uses 10.0.2.2 for localhost)
   backend.base.url=http://10.0.2.2:8000

   # Optional: enable feature flags during local development
   enable.cloud.backup=false
   enable.interaction.check=false
   ```

3. **Do not commit `android/local.properties`.** It is listed in `.gitignore` and must never
   appear in a pull request or commit.

4. Sync your Gradle project. The `BuildConfig` class is regenerated on every build.

---

## CI / CD Setup

When `android/local.properties` is absent (as it always is in CI), Gradle reads the following
environment variables. Set them as repository secrets or workflow environment variables in your
GitHub Actions configuration:

| Env var | Required in CI | Notes |
|---|---|---|
| `BACKEND_BASE_URL` | Recommended | Set to the staging or production backend URL. Leave empty to disable network calls in builds that do not need them. |
| `ENABLE_CLOUD_BACKUP` | Optional | Set to `true` to enable the feature in CI builds. Defaults to `false`. |
| `ENABLE_INTERACTION_CHECK` | Optional | Set to `true` to enable the feature in CI builds. Defaults to `false`. |

Example GitHub Actions step:

```yaml
- name: Build debug APK
  env:
    BACKEND_BASE_URL: ${{ secrets.BACKEND_BASE_URL }}
    ENABLE_CLOUD_BACKUP: "false"
    ENABLE_INTERACTION_CHECK: "false"
  run: cd android && ./gradlew assembleDebug
```

---

## Security Rationale

### What is safe in BuildConfig

- Non-secret URLs (backend base URL with no embedded credentials)
- Boolean feature flags
- Non-secret string identifiers (e.g. analytics environment names)

None of these values are secrets. If a URL leaks, the backend still requires authentication.
If a feature flag leaks, no sensitive capability is exposed.

### What must never appear in BuildConfig or any APK resource

- API keys for third-party services (OCR provider, AI provider, medication reference)
- JWT secrets or signing keys
- Database credentials
- Encryption keys or key material
- Storage bucket credentials

APKs can be extracted from devices using `adb backup` or downloaded from app stores. Tools like
`jadx` or `apktool` decompile APK bytecode in seconds. Any value embedded in the APK — in source
code, resource files, `strings.xml`, or `BuildConfig` — must be treated as publicly readable.

Real secrets belong exclusively in server-side environment variables, managed secret stores
(e.g. AWS Secrets Manager, GitHub Actions secrets), or the Android Keystore for device-local
keys that are generated at runtime and never leave the device.

---

## Adding a New Config Field

Follow these steps every time a new environment-driven config value is needed:

1. **`android/app/build.gradle.kts`** — Add a `val` variable above the `android {}` block
   that reads `local.properties` → env var → safe default. Add a corresponding
   `buildConfigField(...)` call inside `defaultConfig`.

2. **`android/local.properties.example`** — Add the new `local.properties` key with a
   placeholder value and a comment explaining what it does.

3. **`.env.example`** — Add the new env var name under `# === Android / Build ===` with a
   comment explaining its purpose and default.

4. **This document** — Add a row to the BuildConfig Fields table above.

5. **`EnvironmentConfigTest.kt`** — Add a test case that verifies the new field's default value.

6. Confirm the new field does not contain a secret before committing. If it does, reconsider
   the design — secrets belong server-side.
