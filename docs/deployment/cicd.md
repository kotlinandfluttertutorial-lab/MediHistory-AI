# CI/CD Overview — MediHistory AI

## 1. Overview

MediHistory AI uses GitHub Actions for all CI/CD automation. There are three
workflow files, each with a distinct role:

| Workflow file | Trigger | Purpose |
|---|---|---|
| `.github/workflows/android-ci.yml` | PR to `main`/`develop`; push to `develop` | Android lint, unit tests, security scan |
| `.github/workflows/backend-ci.yml` | PR to `main`/`develop`; push to `develop` | Backend lint (ruff + black), type check (mypy), tests (pytest + coverage) |
| `.github/workflows/pr-checks.yml` | PR to `main`/`develop` | Full PR gate: secrets check → android + backend → merge gate |

### Deployment target

The backend service is deployed to **Google Cloud Run**. Deployment configuration
and Cloud Run service definitions are maintained in `docs/architecture/` and the
Cloud Run service YAML (to be added before production release).

---

## 2. Workflow Details

### `android-ci.yml`

**Triggers:** `pull_request` to `main`/`develop`; `push` to `develop`

| Job | Steps |
|---|---|
| `lint` | Checkout → JDK 17 → Gradle cache → `./gradlew lintDebug` → upload lint report |
| `unit-test` | Checkout → JDK 17 → Gradle cache → `./gradlew testDebugUnitTest` → enable KVM → API 34 emulator → `./gradlew connectedDebugAndroidTest` → upload both test reports |
| `security-scan` | Checkout → grep source files for hardcoded secrets |

Environment variables set for `unit-test` job:
- `BACKEND_BASE_URL=""`
- `ENABLE_CLOUD_BACKUP="false"`
- `ENABLE_INTERACTION_CHECK="false"`

The instrumented tests use Android Keystore and therefore run on an API 34
emulator rather than Robolectric. The workflow enables KVM and disables emulator
window, audio, boot animation, and device animations to reduce startup time.

### `backend-ci.yml`

**Triggers:** `pull_request` to `main`/`develop`; `push` to `develop`

| Job | Steps |
|---|---|
| `lint` | Checkout → Python 3.12 → `pip install -r requirements.txt` → `ruff check .` → `black --check .` |
| `test` | Checkout → Python 3.12 → `pip install -r requirements.txt` → `mypy app/` → `pytest --cov=app --cov-report=xml --cov-fail-under=90` |

Environment variables set for `test` job:
- `DATABASE_URL=sqlite:///:memory:`
- `JWT_SECRET_KEY=test-jwt-secret-for-ci-only-not-a-real-secret`
- `TESTING=true`

Coverage threshold: 90% (fails the build if not met).

### `pr-checks.yml`

**Triggers:** `pull_request` to `main`/`develop`

| Job | Depends on | Steps |
|---|---|---|
| `verify-no-secrets-committed` | — | Check for `.env`, `google-services.json`, `local.properties`, `*.keystore`, hardcoded secrets in source |
| `android-ci` | `verify-no-secrets-committed` | `./gradlew lintDebug` → `./gradlew ktlintCheck` → `./gradlew testDebugUnitTest` (Google Services plugin is applied only when the untracked `google-services.json` is present; Firebase-dependent runtime features require it) |
| `backend-ci` | `verify-no-secrets-committed` | `ruff check .` → `black --check .` → `mypy app/` → `pytest --cov-fail-under=90` (uses `sqlite+aiosqlite` and `JWT_SECRET_KEY` for tests) |
| `pr-gate` | `android-ci`, `backend-ci`, `verify-no-secrets-committed` | Aggregate pass/fail — must pass for merge |

The `pr-gate` job is the recommended required status check for branch protection
on `main`.

---

## 3. Local Gate vs CI Alignment Table

| Check | Local gate section | `android-ci.yml` | `backend-ci.yml` | `pr-checks.yml` |
|---|---|---|---|---|
| `:app:compileDebugKotlin` | Section 1 — MANDATORY | **absent (CI gap)** | — | **absent (CI gap)** |
| `:app:lintDebug` | Section 1 — MANDATORY | `lint` job | — | `android-ci` job |
| `:app:testDebugUnitTest` | Section 1 — MANDATORY | `unit-test` job | — | `android-ci` job |
| `:app:connectedDebugAndroidTest` | Not run locally | `unit-test` job (API 34 emulator) | — | — |
| `ktlintCheck` | Section 1 — MANDATORY | `lint` job | — | `android-ci` job |
| `ruff check .` | Section 2 — MANDATORY | — | `lint` job | `backend-ci` job |
| `black --check .` | Section 2 — MANDATORY | — | `lint` job | `backend-ci` job |
| `mypy app/` | Section 2 — MANDATORY | — | `test` job | **absent (CI gap)** |
| `pytest -m "not db"` | Section 2 — MANDATORY | — | `test` job | `backend-ci` job |
| `pytest -m db` | Section 3 — OPTIONAL/WARN | — | `test` job | `backend-ci` job |
| Secret file scan | Section 4 — MANDATORY | `security-scan` job | — | `verify-no-secrets-committed` job |
| Secret content scan | Section 4 — MANDATORY | `security-scan` job | — | `verify-no-secrets-committed` job |
| `pip-audit` | Section 5 — SKIP/WARN | **absent (CI gap)** | **absent (CI gap)** | **absent (CI gap)** |
| `safety check` | Section 5 — SKIP/WARN | **absent (CI gap)** | **absent (CI gap)** | **absent (CI gap)** |
| Android vuln scan | Section 5 — SKIP | **absent (CI gap)** | — | **absent (CI gap)** |
| CI workflow files exist | Section 6 — MANDATORY | — | — | — |
| `yamllint` | Section 6 — SKIP/WARN | **absent (CI gap)** | **absent (CI gap)** | **absent (CI gap)** |

---

## 4. Known CI Gaps

### Gap 1 — No `compileDebugKotlin` in CI

**Affected workflows:** `android-ci.yml`, `pr-checks.yml`

The CI runs `lintDebug` and `testDebugUnitTest` but never explicitly compiles the
Kotlin sources. Certain compile errors (e.g. in generated code or annotation
processors) may not surface via lint alone.

**Local gate behaviour:** adds `:app:compileDebugKotlin` as the first Android
check.

**Recommendation:** Add an explicit compile step to `android-ci.yml` before
production release:
```yaml
- name: Compile Kotlin sources
  run: ./gradlew :app:compileDebugKotlin
  working-directory: android
```

### Gap 2 — No dependency vulnerability scanning in CI

**Affected workflows:** all

Neither `pip-audit`, `safety`, nor the OWASP Gradle dependency-check plugin are
configured in any CI workflow.

**Local gate behaviour:** runs `pip-audit` and `safety` when installed (SKIP if
absent, WARN on findings — not FAIL).

**Recommendation before production:**
- Enable Dependabot security alerts on the GitHub repository settings.
- Add `pip-audit` to `backend-ci.yml`:
  ```yaml
  - name: Audit Python dependencies
    run: pip-audit -r requirements.txt
    working-directory: backend
  ```
- Consider adding the OWASP dependency-check Gradle plugin to `android/build.gradle.kts`.

### Gap 3 — `mypy` absent from `pr-checks.yml` backend job

**Affected workflows:** `pr-checks.yml`

The `pr-checks.yml` `backend-ci` job runs `ruff`, `black`, and `pytest` but
does **not** run `mypy`. Type errors can merge through the PR gate even though
`backend-ci.yml` (the standalone workflow) does run `mypy`.

**Recommendation:** Add `mypy app/` to the `backend-ci` job in `pr-checks.yml`
to ensure type errors block PRs.

### Gap 4 — No `yamllint` in CI

YAML syntax of workflow files is not validated in CI.

**Recommendation:** Add `yamllint` to `pr-checks.yml` as a lightweight check.

### Gap 5 — Local pytest omits `--cov-fail-under=90`

**Affected:** local gate Section 2; CI `backend-ci.yml` `test` job; `pr-checks.yml` `backend-ci` job.

The local gate runs `pytest tests/ -m "not db"` without a coverage threshold, to keep
local runs fast and avoid requiring `pytest-cov` to be installed separately. CI runs
`pytest --cov=app --cov-report=xml --cov-fail-under=90`.

**Effect:** A developer can reduce backend coverage below 90% and the local gate will
not catch it. The CI coverage gate will catch it on the first push.

**Workaround:** Run `python -m pytest --cov=app --cov-fail-under=90 tests/ -m "not db"`
manually before pushing if you have changed backend coverage-sensitive code.

---

## 5. Cloud Run Deployment Overview

The backend FastAPI service is deployed to **Google Cloud Run**.

Planned deployment pipeline (to be implemented before production release):

1. PR merged to `main` → `backend-ci.yml` passes.
2. A `deploy.yml` workflow (not yet created) triggers on push to `main`.
3. Steps: Docker build → push to Artifact Registry → `gcloud run deploy`.
4. Environment variables are injected from GitHub Actions secrets (never in YAML).
5. Alembic migrations run as a Cloud Run Job before the new revision takes traffic.

**Current state:** Cloud Run deployment workflow not yet implemented. This is a
pre-production gap. See `docs/architecture/` for the target architecture.

---

## 6. Environment Variables Required in CI/CD

All secrets must be stored in **GitHub Actions secrets** (repository settings →
Secrets and variables → Actions). They must never appear in YAML workflow files
or source code.

### Android build (set directly in workflow YAML — not secrets)

| Variable | Value in CI | Notes |
|---|---|---|
| `BACKEND_BASE_URL` | `""` (empty) | No backend during Android-only CI |
| `ENABLE_CLOUD_BACKUP` | `"false"` | Feature disabled in CI |
| `ENABLE_INTERACTION_CHECK` | `"false"` | Feature disabled in CI |

### Backend — required GitHub Actions secrets

| Secret name | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL connection string for integration tests |
| `JWT_SECRET_KEY` | JWT signing secret for integration tests |
| `OCR_API_KEY` | OCR provider API key (integration tests only) |
| `AI_EXTRACTION_API_KEY` | AI extraction provider key |
| `AI_NORMALIZATION_API_KEY` | AI normalization provider key |
| `AI_SUMMARIZATION_API_KEY` | AI summarisation provider key |
| `CLOUD_BACKUP_ENCRYPTION_KEY` | Cloud backup encryption key |
| `MED_REFERENCE_API_KEY` | Medication reference API key |
| `KEYSTORE_PASSWORD` | Android release keystore password (release builds only) |
| `KEY_ALIAS` | Android release key alias |
| `KEY_PASSWORD` | Android release key password |
| `GCP_SERVICE_ACCOUNT_KEY` | Google Cloud service account for Cloud Run deployment |

### Backend — CI-safe values (set inline in workflow YAML)

| Variable | Value | Notes |
|---|---|---|
| `DATABASE_URL` | `sqlite:///:memory:` | In-memory SQLite for unit tests |
| `JWT_SECRET_KEY` | `test-jwt-secret-for-ci-only-not-a-real-secret` | Synthetic test value only |
| `TESTING` | `true` | Enables test mode in the FastAPI app |

The full list of environment variable names (without values) is in `.env.example`.

---

## 7. Branch Protection Recommendations

Configure the following on the `main` branch (GitHub → Settings → Branches →
Branch protection rules):

- **Require a pull request before merging** — at least 1 approving review
- **Require status checks to pass before merging**
  - Required checks: `pr-gate` (the `pr-checks.yml` aggregate job)
- **Require branches to be up to date before merging**
- **Dismiss stale pull request approvals when new commits are pushed**
- **Do not allow bypassing the above settings**

For `develop`:
- Require status checks: `android-ci / lint`, `android-ci / unit-test`, `backend-ci / lint`, `backend-ci / test`

---

## 8. PR Gate Configuration

The `pr-gate` job in `pr-checks.yml` acts as the single required status check.
It fails if any of the following jobs fail:

- `verify-no-secrets-committed`
- `android-ci` (lint + unit tests)
- `backend-ci` (ruff + black + pytest with coverage)

To make the PR gate the only required check in branch protection:
1. Set required status check to: `All checks must pass to merge` (the `pr-gate` job name)
2. This allows GitHub to show a single green tick for the entire PR pipeline.

Once `mypy` is added to `pr-checks.yml` (see Gap 3) and the compile step is
added (see Gap 1), the PR gate will be comprehensive.
