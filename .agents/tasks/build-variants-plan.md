# Implementation Plan — MediHistory AI Build Variants and Environment Configuration

> **This task is decomposed into three FEAT files.**  
> See `.agents/tasks/build-variants-task/` for the full FEAT artifacts.  
> The existing implement-and-review loop should read the FEAT files; this plan file
> serves as a human-readable summary and fallback.

---

## Design decisions

**One flavor dimension, two flavors.** A single `environment` dimension with `local` and `cloud` flavors covers all four variants (localDebug, localRelease, cloudDebug, cloudRelease). No third flavor is needed; staging vs production is distinguished by separate deployment secrets, not by a separate APK variant.

**BuildConfig replaces BACKEND_BASE_URL.** The existing single `BACKEND_BASE_URL` field is removed and replaced by `API_BASE_URL` (per-flavor), `BUILD_VARIANT` (per-flavor String literal), and `IS_LOGGING_ENABLED` (Boolean, true in debug builds, false in release). The old field is not kept alongside the new ones — keeping both would cause confusion.

**IS_LOGGING_ENABLED override strategy.** The `cloud` flavor sets `IS_LOGGING_ENABLED=false`. The `debug` buildType overrides it to `true`. This means cloudDebug gets `true` (debug override wins) and cloudRelease gets `false` (no override). `local` flavor sets `true`; localRelease keeps `true` unless explicitly overridden — since local release is only used for verification builds, this is acceptable and simpler than a matrix.

**AppConfig is a plain `object`.** No Hilt injection for a static config singleton — BuildConfig constants are compile-time and don't benefit from injection. The object is testable by testing the logic (timeout selection, variant checks) separately from BuildConfig constants.

**Network security config in `main/`.** The `network_security_config.xml` allowing cleartext to `10.0.2.2` is placed in the main source set, not a flavor source set, because both debug flavors need it and the configuration is narrowly scoped (only `10.0.2.2` gets cleartext — all other domains use the default which requires HTTPS).

**Backend foundation is minimal.** The backend dir has only a `.gitkeep` — no Python files exist. The plan creates only what is required: a runnable FastAPI app skeleton with Pydantic Settings config, requirements.txt with pinned versions, env templates, and config unit tests. No routes beyond `/health`, no DB migrations, no auth middleware yet — those belong in future feature tickets.

**Docker Compose: separate dev and cloud files.** `docker-compose.yml` at root covers local dev (api + db). `docker-compose.cloud.yml` is a compose override for production. This follows the standard `docker-compose -f base -f override` pattern without duplicating service definitions.

---

## FEAT Summary

| FEAT | Scope | Depends on |
|------|-------|-----------|
| FEAT-001 | Android flavors, AppConfig, network security, string resources, AppConfigTest | — |
| FEAT-002 | Backend foundation, Pydantic Settings, requirements.txt, env templates, pytest | — |
| FEAT-003 | Docker, Compose, .gitignore, GitHub Actions CI updates, docs | FEAT-001 (task names), FEAT-002 (backend structure) |

FEAT-001 and FEAT-002 are independent and could run in any order. FEAT-003 must follow both.

---

## Ordered implementation items

### FEAT-001 — Android Build Variants

- [ ] 1. **`android/app/build.gradle.kts`** — Add flavor dimension and per-flavor BuildConfig fields.
      Remove the existing `backendBaseUrl`/`BACKEND_BASE_URL` block. Add `localApiBaseUrl` reading
      `local.api.base.url` from local.properties, falling back to Gradle project property
      `LOCAL_API_BASE_URL`, then to `"http://10.0.2.2:8000/api/v1/"`. Add `cloudApiBaseUrl` reading
      `cloud.api.base.url` from local.properties, falling back to `CLOUD_API_BASE_URL`, then to
      `"https://api.medihistory.example.com/api/v1/"`. Add signing config reading from
      `System.getenv("KEYSTORE_PATH/PASSWORD/KEY_ALIAS/KEY_PASSWORD")` — if KEYSTORE_PATH is set and
      the file exists, create a named `release` signingConfig; otherwise print a Gradle warning and
      leave release unsigned (CI will inject real values). Add `flavorDimensions("environment")` and
      `productFlavors { local { dimension="environment"; applicationIdSuffix=".local";
      versionNameSuffix="-local"; buildConfigField "String","API_BASE_URL",localApiBaseUrl;
      buildConfigField "String","BUILD_VARIANT","\"local\""; buildConfigField "Boolean",
      "IS_LOGGING_ENABLED","true" }; cloud { dimension="environment";
      buildConfigField "String","API_BASE_URL",cloudApiBaseUrl; buildConfigField
      "String","BUILD_VARIANT","\"cloud\""; buildConfigField "Boolean","IS_LOGGING_ENABLED","false" } }`.
      In `buildTypes.debug` add `buildConfigField("Boolean","IS_LOGGING_ENABLED","true")` so cloudDebug
      overrides the cloud flavor's false. In `buildTypes.release`, assign `signingConfig` if available.
      Keep all existing settings intact.
      Files: `android/app/build.gradle.kts`
      Verify: `cd android && ./gradlew assembleLocalDebug assembleCloudDebug` — both APKs build.

- [ ] 2. **`android/app/src/main/res/xml/network_security_config.xml`** — Create XML restricting
      cleartext HTTP to `10.0.2.2` only (emulator host alias). All other domains use default (HTTPS).
      Files: `android/app/src/main/res/xml/network_security_config.xml`
      Verify: File exists and is valid XML. Full build verify in step 1.

- [ ] 3. **`android/app/src/main/AndroidManifest.xml`** — Add
      `android:networkSecurityConfig="@xml/network_security_config"` to the `<application>` element.
      No other changes.
      Files: `android/app/src/main/AndroidManifest.xml`
      Verify: Included in the build verify from step 1.

- [ ] 4. **Flavor string resources** — Create `android/app/src/local/res/values/strings.xml` with
      `app_name = "MediHistory AI Dev"`. Create `android/app/src/cloud/res/values/strings.xml` with
      `app_name = "MediHistory AI"`. Remove `app_name` from
      `android/app/src/main/res/values/strings.xml` (replace with empty `<resources/>` stub since it
      was the only string).
      Files: `android/app/src/local/res/values/strings.xml`,
             `android/app/src/cloud/res/values/strings.xml`,
             `android/app/src/main/res/values/strings.xml`
      Verify: `cd android && ./gradlew assembleLocalDebug` — app label resolves from flavor source set.

- [ ] 5. **`android/app/src/main/java/com/medihistory/ai/config/AppConfig.kt`** — Create the runtime
      config singleton. Package `com.medihistory.ai.config`. Fields: `apiBaseUrl`, `buildVariant`,
      `isLoggingEnabled` all delegate to `BuildConfig`. Derived: `isLocal = BUILD_VARIANT == "local"`,
      `isCloud = BUILD_VARIANT == "cloud"`. Timeouts: `connectTimeoutMs = if (isLocal) 10_000L else
      30_000L`, `readTimeoutMs = if (isLocal) 30_000L else 60_000L`. `retryOnConnectionFailure =
      false`.
      Files: `android/app/src/main/java/com/medihistory/ai/config/AppConfig.kt`
      Verify: Compiles cleanly in `./gradlew assembleLocalDebug`.

- [ ] 6. **`android/local.properties.example`** — Create example file documenting all keys:
      `sdk.dir`, `local.api.base.url=http://10.0.2.2:8000/api/v1/`,
      `cloud.api.base.url=https://YOUR_STAGING_API/api/v1/`, and signing keys. Header comment that
      this file is safe to commit; `local.properties` is git-ignored.
      Files: `android/local.properties.example`
      Verify: File exists with correct content (no real values).

- [ ] 7. **`android/app/src/test/java/com/medihistory/ai/config/AppConfigTest.kt`** — Unit tests for
      AppConfig logic using a test-local helper class that mirrors `AppConfig`'s derivation logic
      (avoids importing the compile-time `BuildConfig`). Tests:
      `given_localVariant_when_checkingIsLocal_then_returnsTrue`,
      `given_cloudVariant_when_checkingIsCloud_then_returnsTrue`,
      `given_localVariant_when_checkingConnectTimeout_then_returns10000`,
      `given_cloudVariant_when_checkingConnectTimeout_then_returns30000`,
      `given_localVariant_when_checkingReadTimeout_then_returns30000`,
      `given_cloudVariant_when_checkingReadTimeout_then_returns60000`,
      `given_anyVariant_when_checkingRetryOnConnectionFailure_then_returnsFalse`. All use synthetic
      values.
      Files: `android/app/src/test/java/com/medihistory/ai/config/AppConfigTest.kt`
      Verify: `cd android && ./gradlew testLocalDebugUnitTest testCloudDebugUnitTest` — all tests pass.

---

### FEAT-002 — Backend Foundation

- [ ] 8. **`backend/requirements.txt`** — Pinned exact versions:
      `fastapi==0.111.0`, `uvicorn[standard]==0.30.1`, `pydantic==2.7.4`,
      `pydantic-settings==2.3.4`, `sqlalchemy==2.0.31`, `alembic==1.13.2`,
      `psycopg2-binary==2.9.9`, `python-jose[cryptography]==3.3.0`,
      `passlib[bcrypt]==1.7.4`, `python-multipart==0.0.9`, `ruff==0.5.0`,
      `black==24.4.2`, `mypy==1.10.1`, `pytest==8.2.2`, `pytest-cov==5.0.0`,
      `httpx==0.27.0`.
      Files: `backend/requirements.txt`
      Verify: `cd backend && pip install -r requirements.txt` — no resolution errors.

- [ ] 9. **`backend/app/__init__.py`** and **`backend/app/core/__init__.py`** — Empty module markers.
      Files: `backend/app/__init__.py`, `backend/app/core/__init__.py`
      Verify: Python can import the modules.

- [ ] 10. **`backend/app/core/config.py`** — Pydantic-settings `Settings` class. Fields: `env`
       (default `"local"`), `database_url` (default `"sqlite:///:memory:"`), `jwt_secret` (required,
       no default), `jwt_algorithm` (default `"HS256"`), `access_token_expire_minutes` (default 30),
       `testing` (default False), `log_level` (default `"INFO"`), `docs_enabled: bool | None`
       (default None). Properties: `is_local`, `is_cloud`, `effective_docs_enabled` (None → auto from
       env). `model_config = SettingsConfigDict(env_file=".env.local", env_file_encoding="utf-8",
       extra="ignore")`. `@lru_cache` decorated `get_settings()` factory.
       Files: `backend/app/core/config.py`
       Verify: Included in pytest run from step 12.

- [ ] 11. **`backend/app/main.py`** — Minimal FastAPI app. Reads `settings = get_settings()`.
       Conditionally exposes `/docs` and `/redoc` via `effective_docs_enabled`. Single `GET /health`
       endpoint returning `{"status": "ok", "env": settings.env}`. Never logs secrets.
       Files: `backend/app/main.py`
       Verify: `cd backend && uvicorn app.main:app --reload` starts without errors (manual check).
       Full CI verify in step 12.

- [ ] 12. **`backend/tests/__init__.py`** and **`backend/tests/test_config.py`** — Eight pytest tests
       covering: default env is local, `is_local`/`is_cloud` toggle, `effective_docs_enabled` auto
       behavior, `docs_enabled` explicit override, `testing` default, `jwt_secret` required
       (ValidationError). Use `Settings(jwt_secret="test-only-synthetic-secret")` pattern — never
       real secrets. Follow naming: `test_[outcome]_[condition]`.
       Files: `backend/tests/__init__.py`, `backend/tests/test_config.py`
       Verify: `cd backend && pytest tests/test_config.py -v` — all 8 tests pass.

- [ ] 13. **`backend/pytest.ini`** and **`backend/mypy.ini`** — pytest config with
       `testpaths = tests`, `addopts = --tb=short`. mypy config with `python_version = 3.12`,
       `strict = true`, `ignore_missing_imports = true`.
       Files: `backend/pytest.ini`, `backend/mypy.ini`
       Verify: `cd backend && mypy app/` — zero errors. `cd backend && ruff check app/` — zero errors.

- [ ] 14. **`backend/.env.local.example`** and **`backend/.env.cloud.example`** — Env templates.
       `.env.local.example`: `ENV=local`, `DATABASE_URL=postgresql://medihistory:changeme@localhost:5432/medihistory_local`,
       `JWT_SECRET=replace-with-a-long-random-secret-for-local-dev-only`, `LOG_LEVEL=DEBUG`,
       `DOCS_ENABLED=true`. `.env.cloud.example`: `ENV=cloud`, all values are `REPLACE_WITH_*`
       placeholders, `LOG_LEVEL=WARNING`, `DOCS_ENABLED=false`, `ACCESS_TOKEN_EXPIRE_MINUTES=15`.
       Files: `backend/.env.local.example`, `backend/.env.cloud.example`
       Verify: Files exist, no real secrets — both contain only placeholder/example values.

---

### FEAT-003 — Infrastructure and CI

_Depends on FEAT-001 (Gradle task names) and FEAT-002 (backend structure)._

- [ ] 15. **`.gitignore`** — Add under the existing `# Environment variables and secrets` section:
       `backend/.env.local`, `backend/.env.cloud`, `backend/.env`.
       Files: `.gitignore`
       Verify: `git check-ignore -v backend/.env.local` — outputs the .gitignore rule.

- [ ] 16. **`backend/Dockerfile`** — Single-stage, `python:3.12-slim` base. Non-root user `appuser`.
       `WORKDIR /app`. Copy `requirements.txt` first, `pip install --no-cache-dir`. Copy `app/`.
       `EXPOSE 8000`. `USER appuser`. `CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0",
       "--port", "8000"]`. Comment: pin digest before production. No secrets baked in.
       Files: `backend/Dockerfile`
       Verify: `docker build ./backend -t medihistory-api-test` builds without errors (if Docker
       available). Otherwise, verify file structure is correct.

- [ ] 17. **`docker-compose.yml`** (repo root) — `db` service: `postgres:16-alpine`, env via
       `${DB_PASSWORD:-changeme_local_only}`, healthcheck. `api` service: builds from `./backend`,
       `env_file: ./backend/.env.local`, `DATABASE_URL` overriding to `@db:5432/medihistory_local`,
       `depends_on` db with `condition: service_healthy`. Named volume `postgres_data`. Header comment
       that it requires `backend/.env.local` and is not for production.
       Files: `docker-compose.yml`
       Verify: `docker-compose config` validates the file (if Docker available). Otherwise confirm
       YAML is well-formed.

- [ ] 18. **`docker-compose.cloud.yml`** (repo root) — Override file for production. Removes db
       service (external). `api` service: `env_file: ./backend/.env.cloud`, `restart: always`,
       removes `ports` mapping (use reverse proxy), adds `deploy.resources.limits: memory: 512m`.
       Header comment explaining usage with `-f` flag.
       Files: `docker-compose.cloud.yml`
       Verify: File exists and is valid YAML.

- [ ] 19. **`.github/workflows/android-ci.yml`** — Update for multi-flavor:
       (a) Lint step: `lintDebug` → `lintLocalDebug`.
       (b) Unit test step: `testDebugUnitTest` → `testLocalDebugUnitTest testCloudDebugUnitTest`
       with `-PLOCAL_API_BASE_URL=$LOCAL_API_BASE_URL -PCLOUD_API_BASE_URL=$CLOUD_API_BASE_URL`
       appended to the gradlew command.
       (c) Update env vars: replace `BACKEND_BASE_URL` with `LOCAL_API_BASE_URL` and
       `CLOUD_API_BASE_URL` (from repository vars with safe fallbacks).
       (d) Update artifact paths: `lint-results-debug.html` → `lint-results-localDebug.html`,
       `testDebugUnitTest` → `testLocalDebugUnitTest`.
       Files: `.github/workflows/android-ci.yml`
       Verify: YAML is valid. Full CI verification on PR.

- [ ] 20. **`.github/workflows/backend-ci.yml`** — Add PostgreSQL service container:
       `services.postgres: image: postgres:16-alpine`, env `POSTGRES_DB=medihistory_test`,
       `POSTGRES_USER=medihistory`, `POSTGRES_PASSWORD=test-password-ci-only`, ports `5432:5432`,
       health-check options. Update pytest env: `DATABASE_URL: postgresql://medihistory:test-password-ci-only@localhost:5432/medihistory_test`,
       `ENV: local`. Keep `JWT_SECRET` and `TESTING` as they are.
       Files: `.github/workflows/backend-ci.yml`
       Verify: YAML is valid.

- [ ] 21. **`.github/workflows/pr-checks.yml`** — (a) Add step: check for committed
       `backend/.env.local` or `backend/.env.cloud` files. (b) Add step: scan Android source for
       hardcoded production domain patterns. (c) Update inline android-ci job to use
       `lintLocalDebug` and `testLocalDebugUnitTest testCloudDebugUnitTest`. (d) Update inline
       backend-ci job to include `ENV=local`.
       Files: `.github/workflows/pr-checks.yml`
       Verify: YAML is valid.

- [ ] 22. **Deployment documentation** — Three files:
       `docs/deployment/build-variants.md`: flavor table, BuildConfig field descriptions, AppConfig
       accessor table with timeout defaults, Gradle task name reference, how to switch variants in
       Android Studio.
       `docs/deployment/local-setup.md`: new developer onboarding — SDK setup, local.properties
       keys, Docker setup, running localDebug.
       `docs/deployment/cicd.md`: workflow descriptions, required GitHub secrets/vars
       (`LOCAL_API_BASE_URL`, `CLOUD_API_BASE_URL`, `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`,
       `KEY_ALIAS`, `KEY_PASSWORD`), PR gate requirements.
       Files: `docs/deployment/build-variants.md`, `docs/deployment/local-setup.md`,
              `docs/deployment/cicd.md`
       Verify: Files exist and contain accurate content.

- [ ] 23. **`j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.agents\tasks\build-variants-done.md`** —
       Final report. Summary of all files created/modified, verification results, any deviations from
       the plan, notes for future tickets (e.g. Retrofit/OkHttp wiring to AppConfig when networking is
       added, SQLCipher evaluation, DB migration setup).
       Files: `.agents/tasks/build-variants-done.md`
       Verify: File exists.

---

## Files created/modified summary

| File | Action |
|------|--------|
| `android/app/build.gradle.kts` | Modify — add flavors, replace BACKEND_BASE_URL, add signing |
| `android/app/src/main/res/xml/network_security_config.xml` | Create |
| `android/app/src/main/AndroidManifest.xml` | Modify — add networkSecurityConfig |
| `android/app/src/main/res/values/strings.xml` | Modify — remove app_name |
| `android/app/src/local/res/values/strings.xml` | Create |
| `android/app/src/cloud/res/values/strings.xml` | Create |
| `android/app/src/main/java/com/medihistory/ai/config/AppConfig.kt` | Create |
| `android/app/src/test/java/com/medihistory/ai/config/AppConfigTest.kt` | Create |
| `android/local.properties.example` | Create |
| `backend/requirements.txt` | Create |
| `backend/app/__init__.py` | Create |
| `backend/app/core/__init__.py` | Create |
| `backend/app/core/config.py` | Create |
| `backend/app/main.py` | Create |
| `backend/tests/__init__.py` | Create |
| `backend/tests/test_config.py` | Create |
| `backend/pytest.ini` | Create |
| `backend/mypy.ini` | Create |
| `backend/.env.local.example` | Create |
| `backend/.env.cloud.example` | Create |
| `backend/Dockerfile` | Create |
| `docker-compose.yml` | Create |
| `docker-compose.cloud.yml` | Create |
| `.gitignore` | Modify — add backend env entries |
| `.github/workflows/android-ci.yml` | Modify — multi-flavor tasks |
| `.github/workflows/backend-ci.yml` | Modify — add PostgreSQL service |
| `.github/workflows/pr-checks.yml` | Modify — secret scan + flavor tasks |
| `docs/deployment/build-variants.md` | Create |
| `docs/deployment/local-setup.md` | Create |
| `docs/deployment/cicd.md` | Create |
| `.agents/tasks/build-variants-done.md` | Create (final report) |
