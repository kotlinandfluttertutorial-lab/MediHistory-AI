# Pre-Push Quality Gate

## 1. Purpose and Overview

The pre-push quality gate catches compilation errors, test failures, formatting
problems, accidentally committed secrets, and CI configuration issues **before**
code reaches GitHub. Running locally avoids broken builds and keeps the main
branch clean.

The gate consists of three files:

| File | Purpose |
|---|---|
| `check-before-push.ps1` | Windows PowerShell quality gate |
| `check-before-push.sh` | Bash quality gate (macOS, Linux, WSL) |
| `.githooks/pre-push` | Git hook — runs automatically on `git push` |

Both scripts run the same logical checks and produce the same output format.
They exit `0` when all mandatory checks pass and `1` when any mandatory check
fails.

---

## 2. Prerequisites

### All platforms
- Git ≥ 2.28
- JDK 17 (for Android Gradle tasks)
- Android SDK (set `ANDROID_SDK_ROOT` in your environment or `local.properties`)

### Windows
- PowerShell 5.1 or PowerShell 7
- Git for Windows (Git Bash for the `.sh` script)

### macOS / Linux / WSL
- Bash 4+
- `chmod +x check-before-push.sh` and `chmod +x .githooks/pre-push`

### Python backend (currently at scaffolding stage — all backend checks SKIP)
- Python 3.12
- Virtual environment at `backend/.venv/`
- All dependencies installed: `pip install -r backend/requirements.txt`
- Optional vulnerability scanners: `pip install pip-audit safety`

### Gradle
- `android/gradlew` (Unix) and `android/gradlew.bat` (Windows) are committed to
  the repository. No manual download is needed.
- Gradle daemon is disabled (`--no-daemon`) to avoid stale state during pre-push
  checks.

---

## 3. Installation

### Option A — Git hook (runs automatically on every `git push`)

Run once from the repository root:

```bash
# macOS / Linux / WSL / Git Bash
git config core.hooksPath .githooks
chmod +x .githooks/pre-push
chmod +x check-before-push.sh
```

```powershell
# Windows PowerShell (from repo root)
git config core.hooksPath .githooks
```

After installation, `git push` automatically invokes the quality gate. If any
mandatory check fails, the push is blocked and you must fix the issue.

### Option B — Manual run (no hook installed)

```powershell
# Windows PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File check-before-push.ps1
```

```bash
# Bash (macOS / Linux / WSL / Git Bash)
bash check-before-push.sh
```

### Option C — Verify hook is active

```bash
git config core.hooksPath
# Should output: .githooks
```

---

## 4. Running Manually

### Windows PowerShell (from repo root)

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File check-before-push.ps1
```

### Bash (from repo root)

```bash
bash check-before-push.sh
# or, after chmod +x:
./check-before-push.sh
```

---

## 5. Section Descriptions

### Section 0 — Git Hygiene

Checks the state of the repository before committing.

| Check | Status on issue |
|---|---|
| `git` in PATH | FAIL |
| Inside a git repository | FAIL |
| Uncommitted changes in working tree | WARN |
| Unresolved merge conflicts | FAIL |
| Commonly junk directories tracked (`node_modules/`, `.venv/`, `__pycache__/`, `build/`) | WARN |
| Sensitive files tracked (`.env`, `google-services.json`, `local.properties`, `*.keystore`, `*.jks`) | WARN |

Uncommitted changes produce a WARN, not a FAIL — the developer decides whether to
stash or commit before pushing.

### Section 1 — Android

Runs the Gradle quality tasks from `android/` using `gradlew.bat` (Windows) or
`gradlew` (Unix).

Required environment variables set automatically:
- `BACKEND_BASE_URL=""`
- `ENABLE_CLOUD_BACKUP="false"`
- `ENABLE_INTERACTION_CHECK="false"`

| Check | Gradle task | Status on issue |
|---|---|---|
| `gradlew.bat` / `gradlew` exists | — | FAIL |
| Kotlin compilation | `:app:compileDebugKotlin` | FAIL |
| Android Lint | `:app:lintDebug` | FAIL |
| ktlint formatting | n/a — not configured | SKIP (permanent) |
| Unit tests | `:app:testDebugUnitTest` | FAIL |

> **Note on ktlint:** The `ktlint` Gradle plugin is not configured in
> `android/build.gradle.kts`. The check is permanently SKIPped. To enable it,
> add the `org.jlleitschuh.gradle.ktlint` plugin to the project build file.

> **Note on compileDebugKotlin:** This task is **not** in the CI workflows.
> The local gate adds it as the first check to catch compile errors that lint
> alone might miss. See [CI gaps](#10-known-limitations) and
> [cicd.md](cicd.md).

### Section 2 — Python Backend

Runs Python quality checks from `backend/`. All checks are **SKIPped** when
`backend/requirements.txt` does not exist (scaffolding stage).

Python interpreter detection order:
1. `backend/.venv/Scripts/python.exe` (Windows) / `backend/.venv/bin/python` (Unix)
2. `py` (Windows system Python launcher)
3. `python` / `python3`

| Check | Command | Status on issue |
|---|---|---|
| Python detected | — | FAIL |
| Ruff lint | `python -m ruff check .` | FAIL |
| Black format | `python -m black --check .` | FAIL |
| Mypy type check | `python -m mypy app/` | FAIL |
| Unit tests (non-db) | `python -m pytest tests/ -m "not db"` | FAIL |

The CI uses `black --check .` — **not** `ruff format --check`. Both scripts match
CI exactly.

Environment variables set automatically for pytest:
- `DATABASE_URL=sqlite:///:memory:`
- `JWT_SECRET_KEY=test-jwt-secret-for-ci-only-not-a-real-secret`
- `TESTING=true`

These variables are unset after the tests complete. Their values are never
printed.

### Section 3 — Database Tests

Runs `pytest tests/ -m db` against a real database. These tests are
**optional locally** (SKIP if `DATABASE_URL` is not set) but run in CI.

| Condition | Result |
|---|---|
| `DATABASE_URL` not set | SKIP |
| `DATABASE_URL` set, tests pass | PASS |
| `DATABASE_URL` set, tests fail | WARN (not FAIL — local connectivity issues are common) |

### Section 4 — Secret / Sensitive-File Scan

Scans **tracked files only** (git index) for sensitive content. Never scans
untracked or ignored files.

Sensitive file patterns (FAIL if tracked):
- `.env`, `google-services.json`, `local.properties`
- `*.keystore`, `*.jks`, `service-account*.json`

Secret content patterns (FAIL if found in tracked source files):
- `KEYSTORE_PASSWORD=`, `JWT_SECRET_KEY=`, `OCR_API_KEY=`, `AI_EXTRACTION_API_KEY=`
- `AI_NORMALIZATION_API_KEY=`, `AI_SUMMARIZATION_API_KEY=`, `CLOUD_BACKUP_ENCRYPTION_KEY=`
- `MED_REFERENCE_API_KEY=`, `DB_PASSWORD=`, `STORAGE_SECRET_KEY=`, `ENCRYPTION_KEY=`
- `KEY_PASSWORD=`, `hardcoded_secret`, `password="`, `password='`
- `ghp_` (GitHub personal access token), `github_pat_`, `AKIA` (AWS access key prefix)

The scan prints only `file:line` references. **Secret values are never printed.**

Medical data patterns (WARN if found outside `test/fixtures/` directories):
- `PATIENT_NAME=`, `DOCTOR_NAME=`

### Section 5 — Dependency Vulnerability Scan

Optional. All checks WARN (not FAIL) because these tools are not yet in CI.

| Check | Condition |
|---|---|
| `pip-audit` Python scan | SKIP if `pip-audit` not installed |
| `safety` Python scan | SKIP if `safety` not installed |
| Android/Gradle scan | SKIP permanently (not configured — documented CI gap) |

To install the optional scanners:
```bash
pip install pip-audit safety
```

### Section 6 — CI Config Validation

Verifies that the required GitHub Actions workflow files exist.

Required files (FAIL if missing):
- `.github/workflows/android-ci.yml`
- `.github/workflows/backend-ci.yml`
- `.github/workflows/pr-checks.yml`

YAML syntax check (WARN if `yamllint` is installed and finds issues):
```bash
pip install yamllint
```

---

## 6. Exit Codes

| Code | Meaning |
|---|---|
| `0` | All mandatory checks passed (WARN and SKIP are allowed) |
| `1` | One or more mandatory checks failed |

WARN and SKIP never cause a non-zero exit. Only FAIL does.

---

## 7. How to Skip (Not Recommended)

To bypass the hook for an emergency push:

```bash
git push --no-verify
```

This skips all pre-push hooks. Use only in genuine emergencies. The CI pipeline
will still run all checks on the pushed code.

---

## 8. Repair Commands

The quality gate never modifies source files. If a check fails, use the
appropriate repair command separately.

| Failing check | Repair command |
|---|---|
| `backend-black` | `cd backend && python -m black .` |
| `backend-ruff` | `cd backend && python -m ruff check . --fix` |
| Android compile | Fix Kotlin errors shown in Gradle output |
| Android lint | Open `android/app/build/reports/lint-results-debug.html` |
| Android tests | Open `android/app/build/reports/tests/testDebugUnitTest/index.html` |
| Secret scan | Remove the file/value, then `git rm --cached <file>` if already staged |

---

## 9. What Each SKIP Means and How to Resolve It

| SKIP reason | How to resolve |
|---|---|
| `backend not yet scaffolded (requirements.txt absent)` | Create `backend/requirements.txt` and scaffold the FastAPI app |
| `ktlint is not configured` | Add `org.jlleitschuh.gradle.ktlint` plugin to `android/build.gradle.kts` |
| `DATABASE_URL not set` | Set `DATABASE_URL` in your shell before running, or use `.env` (never commit) |
| `pip-audit not installed` | `pip install pip-audit` |
| `safety not installed` | `pip install safety` |
| `yamllint not installed` | `pip install yamllint` |
| `No Android/Gradle vulnerability scanning configured` | Add OWASP dependency-check Gradle plugin (see `cicd.md`) |

---

## 10. Known Limitations

1. **ktlint not configured** — The `ktlint` Gradle plugin is absent from this
   project. The `android-ktlint` check is permanently SKIPped until the plugin
   is added.

2. **Backend not yet scaffolded** — `backend/requirements.txt` does not exist.
   All backend checks (Sections 2 and 3) SKIP automatically and become active
   once the file is created.

3. **No `compileDebugKotlin` in CI** — The CI workflows run `lintDebug` and
   `testDebugUnitTest` but not `compileDebugKotlin`. The local gate adds this
   check. See `cicd.md` for the recommended CI fix.

4. **No dependency vulnerability scanning in CI** — `pip-audit` and OWASP
   Gradle dependency-check are not in any CI workflow. The local gate runs them
   optionally (SKIP if not installed, WARN if installed). See `cicd.md`.

5. **pip-audit and safety not in CI** — These are optional local checks only.
   Vulnerability findings produce WARN, not FAIL.

6. **Local pytest omits `--cov-fail-under=90`** — The local gate runs `pytest tests/ -m "not db"` without a coverage threshold flag, to keep local runs fast. CI runs `pytest --cov=app --cov-fail-under=90`. A developer can push with backend coverage below 90% and the CI coverage gate will catch it on the first push. To check coverage locally before pushing, run: `python -m pytest --cov=app --cov-fail-under=90 tests/ -m "not db"` from the `backend/` directory.

---

## 11. CI Mapping Table

| Local check | Section | CI job | CI workflow |
|---|---|---|---|
| `android-compile` (`:app:compileDebugKotlin`) | 1 | **none (CI gap)** | — |
| `android-lint` (`:app:lintDebug`) | 1 | `lint` / `android-ci` | `android-ci.yml`, `pr-checks.yml` |
| `android-unit-tests` (`:app:testDebugUnitTest`) | 1 | `unit-test` / `android-ci` | `android-ci.yml`, `pr-checks.yml` |
| `android-ktlint` | 1 | **not configured** | — |
| `backend-ruff` | 2 | `lint` (ruff check) | `backend-ci.yml`, `pr-checks.yml` |
| `backend-black` | 2 | `lint` (black --check) | `backend-ci.yml`, `pr-checks.yml` |
| `backend-mypy` | 2 | `test` (mypy app/) | `backend-ci.yml` only |
| `backend-pytest` | 2 | `test` (pytest --cov) | `backend-ci.yml`, `pr-checks.yml` |
| `db-tests` | 3 | `test` (DATABASE_URL set) | `backend-ci.yml` |
| `secret-scan` | 4 | `verify-no-secrets-committed` | `pr-checks.yml`, `android-ci.yml` |
| `pip-audit` | 5 | **none (CI gap)** | — |
| `safety` | 5 | **none (CI gap)** | — |
| `android-vuln-scan` | 5 | **none (CI gap)** | — |
| CI workflow files exist | 6 | — | — |

---

## 12. Synthetic Data Policy

All test fixtures must use synthetic data:
- Fake patient names: `"Test Patient Alpha"`, `"Synthetic User Beta"`
- Fake doctor names: `"Dr. Synthetic"`, `"Dr. Test Cardiologist"`
- Fake medicine names: `"Synthamol 500mg"`, `"Testocillin 250mg"`

No real Indian medicine brand names or real patient names in any test fixture.
Synthetic fixtures live in `test/fixtures/` or `android/app/src/test/fixtures/`.

---

## 13. Troubleshooting Common Failures

### `android-compile` FAIL
```
:app:compileDebugKotlin exited 1
```
Open the Gradle output above the FAIL line. Fix Kotlin compile errors. Then re-run.

### `android-lint` FAIL
```
:app:lintDebug exited 1
```
Open `android/app/build/reports/lint-results-debug.html` in a browser.
The report lists every lint violation with file, line, and suggested fix.

### `android-unit-tests` FAIL
```
:app:testDebugUnitTest exited 1
```
Open `android/app/build/reports/tests/testDebugUnitTest/index.html` for the
full test report.

### `backend-black` FAIL
```
black format check failed
```
Run the repair command (this modifies source files — review changes before committing):
```bash
cd backend && python -m black .
```

### `backend-ruff` FAIL
```
ruff check found lint errors
```
Run the repair command (auto-fixes safe issues):
```bash
cd backend && python -m ruff check . --fix
```
Review remaining errors manually.

### `secret-scan` FAIL
```
Secret pattern 'JWT_SECRET_KEY=' at path/to/file:42
```
1. Open `path/to/file` at line 42.
2. Remove the hardcoded value.
3. Move the value to `.env` (which is git-ignored) or a secrets manager.
4. If the file is already tracked: `git rm --cached path/to/file`.
5. If the secret was pushed previously, rotate it immediately.

### `ci-workflow-...` FAIL
```
.github/workflows/android-ci.yml is MISSING
```
The CI workflow files must be committed. Check `git ls-files .github/workflows/`.

### `db-tests` WARN (when DATABASE_URL is set)
```
db tests failed — optional locally but run in CI
```
This is a WARN, not a FAIL. Check database connectivity. The db tests will also
run in CI where the database is available.

---

## 14. Platform Notes

### Windows
- The scripts use `.\gradlew.bat` (Windows) via the `check-before-push.ps1` script.
- The `.githooks/pre-push` hook auto-detects Git Bash / MSYS / Cygwin and runs
  PowerShell when available, otherwise falls back to `check-before-push.sh`.
- PowerShell execution policy: if restricted, run with
  `-ExecutionPolicy Bypass` as shown in the installation instructions above.

### macOS / Linux / WSL
- The scripts use `./gradlew` (Unix).
- `chmod +x` is required on first checkout for both `check-before-push.sh` and
  `.githooks/pre-push`. Git does not always preserve executable bits on Windows
  checkouts.
- WSL (Windows Subsystem for Linux) uses the Bash path even though the host OS
  is Windows, because Bash is the shell executing the hook.

---

## 15. Security Notes

- The secret scan **never** prints secret values — only `file:line` references.
- The scripts **never** modify source files. If a formatting check fails, a
  separate repair command must be run deliberately.
- Credentials used in test environment variables (e.g.
  `JWT_SECRET_KEY=test-jwt-secret-for-ci-only-not-a-real-secret`) are synthetic
  test values. They are never used in production. They are set as environment
  variables for the duration of the pytest run and immediately unset.
- The script never calls `pip install`, `brew install`, or any package manager.
  If a tool is missing, the check is SKIPped with installation instructions.
