# Implementation Plan — MA-15: CI Scaffold Hardening

## Pre-read Summary

### android-ci.yml (current state)
- Triggers: `pull_request` on `[main, develop]` + `push` on `develop` ✓
- Single job `android-lint-and-test` combining lint, unit test, security scan
- Uses `actions/checkout@v4`, `actions/setup-java@v4` (JDK 17 Temurin), `actions/cache@v4` for Gradle ✓
- Security scan uses an older grep pattern scoped only to `android/app/src/main/`
- `BACKEND_BASE_URL` env var uses `vars.BACKEND_BASE_URL || 'http://localhost:8000'` — must be changed to empty string
- No `ENABLE_CLOUD_BACKUP` or `ENABLE_INTERACTION_CHECK` env vars
- `continue-on-error` not set anywhere — GitHub default `false` applies ✓

### backend-ci.yml (current state)
- Triggers: `pull_request` on `[main, develop]` + `push` on `develop` ✓
- Single job `backend-lint-and-test` combining ruff, black, mypy, pytest, security scan
- Coverage threshold `--cov-fail-under=90` and artifact uploads for coverage and test results — must be preserved
- `DATABASE_URL: sqlite:///:memory:` ✓, `JWT_SECRET` set to test value
- Security scan scoped to `app/` inside `backend/` working-directory

### pr-checks.yml (current state)
- Triggers: `pull_request` on `[main]` ONLY — **missing `develop`** (must fix)
- Three jobs: `verify-no-secrets-committed`, `android-ci`, `backend-ci`, plus `pr-gate`
- `pr-gate` already lists `needs: [android-ci, backend-ci, verify-no-secrets-committed]` ✓
- Secrets grep is absent from `pr-checks.yml` entirely — the file checks (.env, google-services.json, local.properties, .keystore) are present ✓
- `android-ci` job has `BACKEND_BASE_URL` set to `vars.BACKEND_BASE_URL || 'http://localhost:8000'` — must change to `""`
- `backend-ci` job does not have `mypy` step (was in backend-ci.yml but not replicated here)

### .github/PULL_REQUEST_TEMPLATE.md
- Does not exist — must be created.

---

## Changes Required

---

- [ ] 1. **Rewrite `.github/workflows/android-ci.yml`** — split single job into three parallel jobs.

  **What to do:**

  Replace the single `android-lint-and-test` job with three jobs: `lint`, `unit-test`, and `security-scan`. Each job independently checks out the repo, sets up JDK 17 Temurin, and restores the Gradle cache. The jobs have no `needs` relationship — they run in parallel on every trigger.

  **Preserve verbatim:** the `on:` trigger block (pull_request branches main+develop, push on develop) and all `actions/` version pins (`checkout@v4`, `setup-java@v4`, `cache@v4`, `upload-artifact@v4`).

  **`lint` job:**
  - Steps: checkout → setup-java (JDK 17 Temurin, cache: gradle) → `chmod +x android/gradlew` → Gradle cache (`actions/cache@v4`, same key formula as current) → `./gradlew lintDebug` (working-directory: android) → upload lint artifact (always, path: `android/app/build/reports/lint-results-debug.html`, name: `lint-results`)
  - No env vars needed
  - No `continue-on-error` override

  **`unit-test` job:**
  - Steps: checkout → setup-java (JDK 17 Temurin, cache: gradle) → `chmod +x android/gradlew` → Gradle cache → `./gradlew testDebugUnitTest` (working-directory: android) → upload test results artifact (always, path: `android/app/build/reports/tests/testDebugUnitTest/`, name: `unit-test-results`)
  - Env vars on the `./gradlew testDebugUnitTest` step:
    ```yaml
    env:
      BACKEND_BASE_URL: ""
      ENABLE_CLOUD_BACKUP: "false"
      ENABLE_INTERACTION_CHECK: "false"
    ```
  - **Change from current:** `BACKEND_BASE_URL` was `${{vars.BACKEND_BASE_URL || 'http://localhost:8000'}}` — replace with `""` (empty string, no live URL in CI)
  - No `continue-on-error` override

  **`security-scan` job:**
  - Steps: checkout → grep for hardcoded secrets
  - Grep command must:
    - Search recursively under `android/app/src` (not just `src/main`)
    - Use pattern: `api_key|secret|password|ghp_|github_pat` (case-insensitive `-i` flag)
    - Include file types: `*.kt`, `*.java`, `*.xml`, `*.properties`
    - Exit 1 if any match found, print matching lines before exiting
    - Print "Security scan passed" and exit 0 if no match
  - **Change from current:** old pattern was `(api_key|apiKey|API_KEY|secret|password|token)\s*=\s*"[^"]{8,}"` — replace with simpler pattern matching the spec: `api_key|secret|password|ghp_|github_pat`
  - No setup-java or Gradle cache needed (grep only)
  - No `continue-on-error` override

  **Files:** `.github/workflows/android-ci.yml`

  **Verify:** Push to a `develop`-targeting PR branch and confirm three separate jobs appear in the GitHub Actions run. Locally, confirm YAML is valid with `python -c "import yaml; yaml.safe_load(open('.github/workflows/android-ci.yml'))"` (or any YAML linter).

---

- [ ] 2. **Rewrite `.github/workflows/backend-ci.yml`** — split single job into two jobs: `lint` and `test`.

  **What to do:**

  Replace the single `backend-lint-and-test` job with two jobs: `lint` and `test`. Both use Python 3.12 and `actions/setup-python@v5` with `cache: pip`. No `needs` relationship — they run in parallel.

  **Preserve verbatim:** the `on:` trigger block, all `actions/` version pins, the `--cov-fail-under=90` threshold, and all artifact upload steps (move them to the `test` job).

  **`lint` job:**
  - Steps: checkout → setup-python (3.12, cache: pip) → `pip install -r requirements.txt` (working-directory: backend) → `ruff check .` (working-directory: backend) → `black --check .` (working-directory: backend)
  - **Omit mypy from this job** — mypy is a type-checker, not a linter, and the spec only lists ruff + black for the lint job. Move mypy to the `test` job or drop it per the spec. **Decision: retain mypy in the `test` job** (it validates code before tests run and was present in the original; dropping it would reduce quality without justification).
  - No env vars needed
  - No `continue-on-error` override

  **`test` job:**
  - Steps: checkout → setup-python (3.12, cache: pip) → `pip install -r requirements.txt` (working-directory: backend) → `mypy app/` (working-directory: backend) → pytest with coverage (working-directory: backend) → upload coverage artifact (always) → upload test results artifact (always) → security scan
  - Pytest step env vars (preserve from current):
    ```yaml
    env:
      DATABASE_URL: sqlite:///./memory:
      JWT_SECRET: test-jwt-secret-for-ci-only-not-a-real-secret
      TESTING: "true"
    ```
  - **Note on DATABASE_URL:** The spec says `sqlite:///./memory:` — this differs from the current `sqlite:///:memory:`. Use `sqlite:///./memory:` as specified (file-path form). This is an intentional change per the task spec.
  - Coverage flags preserved verbatim: `--cov=app --cov-report=xml --cov-report=html --cov-fail-under=90`
  - Artifact uploads preserved verbatim (name: `backend-coverage`, path: `backend/htmlcov/`; name: `backend-test-results`, path: `backend/test-results.xml`)
  - Security scan step (moved from current single job, preserved verbatim):
    ```yaml
    - name: Security check — no hardcoded secrets
      run: |
        if grep -r --include="*.py" \
          -E '(SECRET_KEY|API_KEY|password|token)\s*=\s*"[^"]{8,}"' \
          app/; then
          echo "ERROR: Possible hardcoded secret found in Python source files"
          exit 1
        fi
        echo "Security check passed"
      working-directory: backend
    ```
  - No `continue-on-error` override

  **Files:** `.github/workflows/backend-ci.yml`

  **Verify:** YAML is valid. In a PR to develop or main, two separate jobs (`lint` and `test`) appear in the Actions run.

---

- [ ] 3. **Update `.github/workflows/pr-checks.yml`** — fix trigger, expand secrets grep, update env vars.

  **What to do:**

  Three targeted changes:

  **3a. Fix trigger — add `develop` to branches:**
  ```yaml
  # BEFORE
  on:
    pull_request:
      branches: [ main ]

  # AFTER
  on:
    pull_request:
      branches: [ main, develop ]
  ```

  **3b. Expand secrets grep — add a new step to `verify-no-secrets-committed` job:**

  The current file has no grep-based secrets scan — only file-existence checks. Add a new step after the existing four file checks:
  ```yaml
  - name: Check for hardcoded secrets in source files
    run: |
      if grep -r --exclude-dir=".git" \
        -E 'api_key|secret|password|ghp_|github_pat' \
        --include="*.kt" --include="*.java" --include="*.xml" \
        --include="*.py" --include="*.properties" --include="*.yml" \
        --include="*.yaml" --include="*.json" \
        .; then
        echo "ERROR: Possible hardcoded secret found. Remove before merging."
        exit 1
      fi
      echo "Secrets grep passed — no matches found"
  ```
  Note: search the entire repo (`.`) excluding `.git/`. This is broader than `android/app/src/main/` (previous android-ci scope) and covers all relevant file types.

  **3c. Fix `BACKEND_BASE_URL` env var in the inline `android-ci` job:**
  ```yaml
  # BEFORE
  env:
    BACKEND_BASE_URL: ${{vars.BACKEND_BASE_URL || 'http://localhost:8000'}}

  # AFTER
  env:
    BACKEND_BASE_URL: ""
    ENABLE_CLOUD_BACKUP: "false"
    ENABLE_INTERACTION_CHECK: "false"
  ```

  **Preserve verbatim:** all four file-existence check steps, the `pr-gate` job's `needs` list and failure-check logic, the `backend-ci` inline job's pytest command and env vars (keep `DATABASE_URL: sqlite:///:memory:` in pr-checks.yml — the `sqlite:///./memory:` change applies only to backend-ci.yml).

  **Files:** `.github/workflows/pr-checks.yml`

  **Verify:** YAML is valid. A PR to `develop` triggers the workflow (not just PRs to `main`). All five file checks in `verify-no-secrets-committed` still pass on a clean repo. The `pr-gate` job still depends on all three upstream jobs.

---

- [ ] 4. **Create `.github/PULL_REQUEST_TEMPLATE.md`** — new file.

  **What to do:**

  Create the file at `.github/PULL_REQUEST_TEMPLATE.md` with exactly the following content. GitHub auto-populates PR bodies with this template when a contributor opens a new PR.

  ```markdown
  ## Jira Ticket(s)
  <!-- Link to MA-NNN -->

  ## Summary of Changes
  <!-- What was changed and why -->

  ## Test Results
  <!-- Paste build/lint/test output or link to CI run -->

  ## Security Checklist
  - [ ] No secrets or API keys in source files
  - [ ] No hardcoded backend URLs (use BuildConfig)
  - [ ] No .env, local.properties, or .keystore files committed
  - [ ] No real patient data or real medicine names in test fixtures

  ## Definition of Done
  - [ ] All acceptance criteria pass
  - [ ] Tests exist for all new behaviour and pass in CI
  - [ ] Privacy impact reviewed
  - [ ] Documentation updated
  - [ ] No secrets committed
  ```

  **Files:** `.github/PULL_REQUEST_TEMPLATE.md` (new)

  **Verify:** Open a new PR on GitHub and confirm the PR body is pre-populated with the template. The file has no YAML front-matter — GitHub detects it automatically from its location.

---

## Dependency Order

Items 1, 2, and 4 are independent of each other and can be implemented in any order. Item 3 has no code dependency on 1 or 2, but logically should be done after 1 and 2 are confirmed correct so the inline jobs in pr-checks.yml can be aligned.

Recommended order: 1 → 2 → 3 → 4.

## Content That Must Be Preserved Verbatim

| File | Content to preserve |
|---|---|
| android-ci.yml | `on:` trigger block; all `actions/` version pins; artifact upload paths and names |
| backend-ci.yml | `on:` trigger block; `--cov-fail-under=90`; artifact upload names and paths; `JWT_SECRET` test value format |
| pr-checks.yml | All four file-existence check steps and their exact error messages; `pr-gate` `needs` list; `pr-gate` result-check logic |
| pr-checks.yml | `backend-ci` inline job's `DATABASE_URL: sqlite:///:memory:` (keep as-is; the `sqlite:///./memory:` value is only for the standalone backend-ci.yml) |

## Assumptions

1. `sqlite:///./memory:` in the spec for backend-ci.yml is intentional and differs from the current `sqlite:///:memory:`. Applied as specified only in backend-ci.yml; the pr-checks.yml inline job retains the existing value to avoid divergence risk without a backend test environment to validate against.
2. mypy retained in the `test` job of backend-ci.yml — the spec assigns ruff+black to `lint` but does not explicitly drop mypy. Retaining it is the conservative choice.
3. The secrets grep in pr-checks.yml excludes `.git/` but includes `.yml`/`.yaml` and `.json` files — these are the most likely vectors for accidentally committed tokens alongside Kotlin/Python source files. If `.yml` triggers false positives (e.g. the CI files themselves referencing env var names), the `--include` list can be narrowed to exclude `*.yml`.
