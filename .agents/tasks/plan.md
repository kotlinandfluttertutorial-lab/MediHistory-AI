# Implementation Plan — Pre-Push Quality Gate

Generated from repository inspection. All facts below come from actually reading the
repo — not assumed.

## Repository facts confirmed during exploration

- Android Gradle wrapper: `android/gradlew.bat` (Windows), `android/gradlew` (Unix)
- Gradle working directory: `<repo-root>/android`
- Module: `:app` (confirmed in `android/settings.gradle.kts`)
- Confirmed Gradle tasks: `:app:compileDebugKotlin`, `:app:lintDebug`, `:app:testDebugUnitTest`
- NO ktlint plugin (plugins in `android/build.gradle.kts`: android-application, kotlin-android, hilt, ksp only)
- Backend: `backend/` directory exists but contains only `.gitkeep` — scaffolding stage
  `backend/requirements.txt` — does NOT exist yet
  `backend/app/main.py` — does NOT exist yet
  `backend/tests/` — does NOT exist yet
- CI workflows confirmed at `.github/workflows/`: `android-ci.yml`, `backend-ci.yml`, `pr-checks.yml`
- CI lint tools for backend: `ruff check .` AND `black --check .` (both required — NOT `ruff format`)
- CI type check: `mypy app/` (no `--ignore-missing-imports` in CI)
- CI test command: `pytest --cov=app --cov-report=xml --cov-fail-under=90`
- CI backend env vars: `DATABASE_URL=sqlite:///:memory:`, `JWT_SECRET=test-jwt-secret-for-ci-only-not-a-real-secret`, `TESTING=true`
- CI Android env vars: `BACKEND_BASE_URL=""`, `ENABLE_CLOUD_BACKUP="false"`, `ENABLE_INTERACTION_CHECK="false"`
- No existing `docs/deployment/` directory — must be created
- No existing `.githooks/` directory — must be created
- Secret-scan patterns from `.gitignore` and `pr-checks.yml`: confirmed in files section below
- `.env.example` contains the definitive list of sensitive key names

## CI gaps to document (do NOT modify CI workflows)

The following gaps exist in the CI workflows vs. what the local gate will check:

1. **No `compileDebugKotlin` step in CI** — CI runs `lintDebug` and `testDebugUnitTest` but never
   explicitly compiles. This means compilation errors that don't surface via lint can slip through.
   The local gate adds `:app:compileDebugKotlin` as the first Android check. Document as a CI gap.

2. **No dependency vulnerability scanning in CI** — No Dependabot, no OWASP dependency-check, no
   `pip-audit`. Document as a gap. The local gate runs `pip-audit` for Python only when the tool
   is available (SKIP otherwise, not FAIL). For Android, document as gap only; do not add
   vulnerability scanning that could produce false positives locally.

3. **Backend `mypy` uses no flags in CI** (`mypy app/`) — The local gate mirrors this exactly.
   No `--ignore-missing-imports` to avoid masking real errors.

4. **No secrets scan on Android build output or APK** in CI (only source). Document, do not add.

5. **pr-checks.yml uses `black --check .` (not `ruff format --check`)** — This is intentional.
   Local gate must match: run BOTH `ruff check .` AND `black --check .` separately, same as CI.

## Design decisions

- **Script architecture**: each script is self-contained (no shared include files). The pre-push
  hook is a thin OS-dispatcher shell script that detects the platform and calls the right script.
  Rationale: keeps hook installation simple (`git config core.hooksPath .githooks`) and avoids
  requiring developers to manually configure per-OS paths.

- **Backend skip logic**: when `backend/requirements.txt` does not exist, all backend checks emit
  SKIP with message "backend scaffolding not yet complete". This avoids failing developers who
  are working on Android-only changes against a stub backend.

- **PowerShell pipeline masking**: use `$LASTEXITCODE` after every external command rather than
  relying on `$?`. Never use `| Out-Null` on commands whose exit code matters. Capture output
  with `*>&1` where needed.

- **Bash subshell masking**: set `set -o pipefail` at the top. Run Gradle directly (not in a
  subshell). Capture exit codes with `$?` immediately after each command.

- **Secret scan never prints values**: only prints `file:line` references. Uses `-l` (files only)
  for the initial pass; a second targeted pass prints line numbers but not content.

- **No auto-install**: the scripts never run `pip install`, `brew install`, or any package manager
  invocation. If a tool is missing, the check is SKIP with a message pointing to installation docs.

- **Check count tracking**: both scripts maintain `PASS`, `FAIL`, `WARN`, `SKIP` counters.
  Exit 0 only when `FAIL == 0`. Exit 1 when any FAIL > 0.

---

## File content outlines

### check-before-push.ps1

```
SECTION 0 — Header and helper functions
  - Script version, usage comment
  - $PASS, $FAIL, $WARN, $SKIP counter variables
  - Write-Check function: prints name + status + optional message, increments counter
  - Write-Section function: prints section heading with separator
  - Invoke-ExternalCommand function: runs command, captures exit code from $LASTEXITCODE,
    returns output and exit code. Never uses pipeline. Uses & operator.
  - Find-RepoRoot function: walks up from $PSScriptRoot looking for .git directory
  - Script-level $ErrorActionPreference = "Continue" (do not stop on non-terminating errors)

SECTION 0 — Git Status and Repository Hygiene
  - Validate git is available; SKIP whole section if not
  - Find and cd to repo root
  - Print current branch (git branch --show-current)
  - Print working tree status (git status --short)
  - WARN if staged changes exist (they should be committed before pushing)
  - WARN if unstaged changes exist to tracked files
  - FAIL if merge conflicts detected (grep for <<<<<<< in git status --short)
  - WARN for untracked files
  - INFO check for common junk: node_modules/, __pycache__/, .venv/ in working tree (WARN only)
  - WARN if android/local.properties is tracked (it is git-ignored but warn if somehow staged)

SECTION 1 — Secret Scan
  - Scan for sensitive file presence (not content):
    Patterns: .env, google-services.json, local.properties (at root), *.keystore, *.jks,
              *.pem, *.key, service-account*.json, secrets.yaml, secrets.json
    Use: git ls-files to check only tracked files (avoids false positives from .gitignore)
    FAIL if any sensitive file is tracked
  - Scan tracked source files for secret content patterns:
    File extensions: *.kt, *.java, *.xml, *.py, *.properties, *.yml, *.yaml, *.json
    Patterns (case-insensitive, exact key names from .env.example):
      KEYSTORE_PASSWORD, KEY_PASSWORD, JWT_SECRET_KEY, DB_PASSWORD,
      STORAGE_SECRET_KEY, ENCRYPTION_KEY, OCR_API_KEY, AI_EXTRACTION_API_KEY,
      AI_NORMALIZATION_API_KEY, AI_SUMMARIZATION_API_KEY, CLOUD_BACKUP_ENCRYPTION_KEY,
      MED_REFERENCE_API_KEY
    Also pattern-match: ghp_, github_pat, AKIA (AWS key prefix)
    Print only file:line reference — NEVER print the value
    FAIL if any match found
  - Medical data check (WARN only, not FAIL):
    Patterns: PATIENT_NAME, DOCTOR_NAME, prescription_text
    In *.kt, *.py source files (test fixtures allowed — only warn, do not fail)

SECTION 2 — Android Quality Gate
  - Detect android/gradlew.bat; FAIL (not SKIP) if missing (mandatory component)
  - cd to android/ directory
  - Set env vars: BACKEND_BASE_URL="", ENABLE_CLOUD_BACKUP="false", ENABLE_INTERACTION_CHECK="false"
  - Run: .\gradlew.bat :app:compileDebugKotlin --no-daemon
    PASS/FAIL based on $LASTEXITCODE
    Note in output: "compilation check not in CI — local gate adds this"
  - Run: .\gradlew.bat :app:lintDebug --no-daemon
    PASS/FAIL based on $LASTEXITCODE
  - SKIP ktlintCheck (no ktlint plugin configured — document why)
  - Run: .\gradlew.bat :app:testDebugUnitTest --no-daemon
    PASS/FAIL based on $LASTEXITCODE

SECTION 3 — Python / FastAPI Backend Quality Gate
  - Check for backend/requirements.txt
    If absent: SKIP all backend checks with message "backend not yet scaffolded"
    (This is the correct behaviour for this repo at scaffolding stage)
  - If requirements.txt exists:
    - Detect Python: check .venv\Scripts\python.exe -> py -> python in that order
    - FAIL if no Python found
    - Run: <python> -m ruff check .   (from backend/ directory)
      PASS/FAIL
    - Run: <python> -m black --check .  (from backend/ directory — matches CI exactly)
      PASS/FAIL
    - Run: <python> -m mypy app/   (from backend/ directory — no extra flags, matches CI)
      PASS/FAIL
    - Run: <python> -m pytest --cov=app --cov-report=term-missing --cov-fail-under=90
           with env: DATABASE_URL=sqlite:///:memory: JWT_SECRET=test-jwt-secret-for-ci-only-not-a-real-secret TESTING=true
      PASS/FAIL
    - pip-audit (optional, SKIP if not available — document as CI gap):
      Run: <python> -m pip_audit  (from backend/ directory)
      SKIP with message if pip-audit is not installed

SECTION 4 — Summary
  - Print table: PASS / FAIL / WARN / SKIP counts
  - List all FAILed checks by name
  - Exit 0 if $FAIL -eq 0, else Exit 1
```

### check-before-push.sh

```
SECTION 0 — Header and helper functions
  - Shebang: #!/usr/bin/env bash
  - set -euo pipefail NOT used at top level (we want to continue on failure)
    Instead: capture every exit code with $? immediately after command
  - PASS=0, FAIL=0, WARN=0, SKIP=0 counter variables
  - check_status() function: name, exit_code, message — prints PASS/FAIL/WARN/SKIP, increments counter
  - section() function: prints section heading
  - find_repo_root() function: walks up from script location looking for .git
  - REPO_ROOT set by find_repo_root
  - cd "$REPO_ROOT"

SECTION 0 — Git Status and Repository Hygiene
  - Validate git available
  - Print current branch
  - Print git status --short output
  - WARN if staged changes
  - WARN if unstaged changes to tracked files
  - FAIL if merge conflicts (<<<<<<< in git status)
  - WARN untracked files
  - WARN for node_modules/, __pycache__/, .venv/ presence

SECTION 1 — Secret Scan
  - Use git ls-files to scan only tracked files
  - Sensitive file patterns: same as PowerShell version
    FAIL if any found in tracked files
  - Content grep for secret key names (same list as PowerShell)
    Use grep -rn on tracked files; print file:line only
    FAIL if matches found
  - Medical data WARN (same as PowerShell)

SECTION 2 — Android Quality Gate
  - Detect android/gradlew; chmod +x if needed (no-op if already executable)
  - FAIL if gradlew missing
  - cd android/
  - Export env: BACKEND_BASE_URL="" ENABLE_CLOUD_BACKUP="false" ENABLE_INTERACTION_CHECK="false"
  - ./gradlew :app:compileDebugKotlin --no-daemon; exit code → PASS/FAIL
  - ./gradlew :app:lintDebug --no-daemon; exit code → PASS/FAIL
  - SKIP ktlintCheck with note
  - ./gradlew :app:testDebugUnitTest --no-daemon; exit code → PASS/FAIL
  - cd back to REPO_ROOT

SECTION 3 — Python / FastAPI Backend Quality Gate
  - Check backend/requirements.txt
    If absent: SKIP all with message
  - If present:
    - Detect Python: .venv/bin/python -> python3 -> python
    - FAIL if not found
    - Run ruff, black, mypy, pytest (same commands as PowerShell, with same env vars)
    - pip-audit SKIP if not available

SECTION 4 — Summary
  - Print counts table
  - List FAIL items
  - exit 0 if FAIL == 0, else exit 1
```

### .githooks/pre-push

```
#!/usr/bin/env bash
# Thin dispatcher — detects OS and delegates to the right quality-gate script.
# Install: git config core.hooksPath .githooks
#
# OS detection order:
#   1. $OSTYPE starts with "msys" or "cygwin" → Git Bash on Windows → run .sh
#   2. $OS == "Windows_NT" AND bash is available → run .sh (WSL or Git Bash)
#   3. PowerShell available AND running on Windows (uname -s starts with MINGW or no uname) → run .ps1
#   4. Default → run .sh
#
# The hook always runs from the repository root (Git sets cwd to repo root for hooks).
# Exits with the script's exit code — non-zero blocks the push.
```

### docs/deployment/pre-push-quality-gate.md

```
# Pre-Push Quality Gate

## Purpose
## Installation (3 methods: manual run, git hook, CI equivalent)
## Section reference
  - Section 0: Git hygiene
  - Section 1: Secret scan
  - Section 2: Android quality gate
  - Section 3: Backend quality gate
  - Section 4: Summary and exit codes
## Status codes: PASS / FAIL / WARN / SKIP
## Exit code contract
## How to fix common failures (lint, compile, test, secrets, backend)
## Known CI gaps (documented here, not auto-fixed)
  - compileDebugKotlin absent from CI
  - No dependency vulnerability scan in CI
## Running individual sections
## Repair commands (separate from the check-only gate)
## Updating the gate when new tools are added
```

### docs/deployment/cicd.md

```
# CI/CD Overview

## Pipeline architecture
  - android-ci.yml: triggers, jobs (lint, unit-test, security-scan)
  - backend-ci.yml: triggers, jobs (lint, test)
  - pr-checks.yml: triggers, job chain (secrets → android + backend → pr-gate)
## Local gate vs CI alignment table
  | Check                    | Local gate | android-ci | backend-ci | pr-checks |
  | compileDebugKotlin       | YES (S2)   | NO (gap)   | -          | NO (gap)  |
  | lintDebug                | YES (S2)   | YES        | -          | YES       |
  | testDebugUnitTest        | YES (S2)   | YES        | -          | YES       |
  | ruff check               | YES (S3)   | -          | YES        | YES       |
  | black --check            | YES (S3)   | -          | YES        | YES       |
  | mypy app/                | YES (S3)   | -          | YES        | NO (gap)  |
  | pytest --cov-fail-under  | YES (S3)   | -          | YES        | NO (gap)  |
  | Secret file scan         | YES (S1)   | YES        | -          | YES       |
  | Secret content scan      | YES (S1)   | YES        | -          | YES       |
  | pip-audit                | SKIP/warn  | NO (gap)   | -          | NO (gap)  |
## Known CI gaps (with rationale for not auto-fixing)
## Branch protection recommendations
## Environment variables required in CI (from .env.example)
## Adding a new CI check: process
```

### .agents/tasks/pre-push-gate-report.md

```
# Pre-Push Quality Gate — Implementation Report

## Summary
## Files created
## Verification results
  - PowerShell syntax check result
  - Bash syntax check result
  - Android compile/lint/test results (actual output)
  - Backend status (SKIP — not yet scaffolded, or actual results)
  - Secret scan results
## CI alignment assessment
## Known gaps documented
## Hook installation instructions verified
```

---

## Implementation order

The items below are ordered by dependency. Each item leaves the codebase in a buildable/
runnable state when complete.

---

- [ ] 1. Create `check-before-push.ps1`
      Full PowerShell implementation per the outline above. All four sections plus summary.
      Key implementation details:
      - `$ErrorActionPreference = "Continue"` at top
      - Helper functions: `Write-CheckResult`, `Write-Section`, `Invoke-Gradle`, `Find-RepoRoot`
      - Section 0: `git branch --show-current`, `git status --short`, conflict detection via
        `git diff --name-only --diff-filter=U` (unmerged files)
      - Section 1: `git ls-files` piped through pattern matching; content grep with
        `Select-String` on each tracked file — print only `file:line`, never the matched value
      - Section 2: `& ".\gradlew.bat" :app:compileDebugKotlin --no-daemon` then `$LASTEXITCODE`
        (repeat for lintDebug, testDebugUnitTest). Set `$env:BACKEND_BASE_URL`, etc. before running.
      - Section 3: `Test-Path backend\requirements.txt` → if false, all SKIP.
        Python detection: `Test-Path .venv\Scripts\python.exe` first.
        All tool invocations via `& $python -m ruff check .` pattern.
        `$env:DATABASE_URL`, `$env:JWT_SECRET`, `$env:TESTING` set before pytest.
      - Summary: `Write-Host` table, exit code.
      Files: `check-before-push.ps1`
      Verify: `powershell -NoProfile -Command "& { . .\check-before-push.ps1 -WhatIf 2>&1 }"` —
        actually, verify with `powershell -NoProfile -File check-before-push.ps1` (it will SKIP
        backend and run Gradle tasks; expect PASS/SKIP on a clean checkout). Also run
        `powershell -NoProfile -Command "Get-Content check-before-push.ps1 | Select-String 'syntax'"` —
        real verification is: `powershell -NoProfile -Command "[scriptblock]::Create((Get-Content -Raw .\check-before-push.ps1))" 2>&1`
        which parses the script without executing it.

- [ ] 2. Create `check-before-push.sh`
      Full Bash implementation mirroring the PowerShell script. Same sections, platform-appropriate commands.
      Key implementation details:
      - Shebang: `#!/usr/bin/env bash`
      - Do NOT use `set -e` at top level — capture exit codes manually
      - Counter variables: `PASS=0 FAIL=0 WARN=0 SKIP=0`
      - `check_result()` function: takes name, exit_code, message; updates counters; prints coloured status
      - Section 1 secret scan: `git ls-files -z | xargs -0 grep -lEi '<pattern>'` for file-level
        detection; then per-file grep for line references
      - Section 2: `chmod +x android/gradlew` (safe no-op if already executable)
        `cd android && ./gradlew :app:compileDebugKotlin --no-daemon` etc.
        Use subshell for cd: `(cd android && ./gradlew ...)` or push/pop dirs
      - Section 3: same skip logic as PowerShell
        Python detection: `[ -f .venv/bin/python ] && PYTHON=".venv/bin/python"` etc.
      - Final: `exit $( [ "$FAIL" -eq 0 ] && echo 0 || echo 1 )`
      Files: `check-before-push.sh`
      Verify: `bash -n check-before-push.sh` — must produce no output (syntax clean).
        Then actual run: `bash check-before-push.sh` from repo root.

- [ ] 3. Create `.githooks/pre-push`
      Thin dispatcher shell script. Must be executable (chmod +x in the file's git attributes
      or set explicitly). The implementer must `chmod +x .githooks/pre-push` before committing.
      Content:
      ```bash
      #!/usr/bin/env bash
      # Pre-push hook — dispatches to the appropriate quality gate script.
      # Install: git config core.hooksPath .githooks
      SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
      REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

      # Detect Windows Git Bash / MSYS / Cygwin
      if [[ "$OSTYPE" == "msys"* ]] || [[ "$OSTYPE" == "cygwin"* ]] || [[ "$OS" == "Windows_NT" ]]; then
        if command -v powershell.exe &>/dev/null; then
          exec powershell.exe -NoProfile -ExecutionPolicy Bypass \
            -File "$REPO_ROOT/check-before-push.ps1"
        fi
      fi
      # Default: Unix shell
      exec bash "$REPO_ROOT/check-before-push.sh"
      ```
      The hook must exit with the delegated script's exit code (exec replaces the process,
      so exit code propagates automatically).
      Files: `.githooks/pre-push`
      Verify: `bash -n .githooks/pre-push` must pass. Then: on a Windows Git Bash session,
        `git push --dry-run` should invoke check-before-push.ps1 (or .sh in WSL).

- [ ] 4. Create `docs/deployment/pre-push-quality-gate.md`
      Documentation per the outline above. Must include:
      - Exact installation commands (both git config and manual invocation)
      - Section-by-section reference with expected output format
      - Status code semantics (PASS/FAIL/WARN/SKIP)
      - Exit code contract (0 = all mandatory checks pass, 1 = at least one FAIL)
      - Common failure remedies:
        * Compile failure: read Gradle output; fix Kotlin errors
        * Lint failure: `./gradlew :app:lintDebug` and open `android/app/build/reports/lint-results-debug.html`
        * Test failure: `./gradlew :app:testDebugUnitTest` and open `android/app/build/reports/tests/testDebugUnitTest/`
        * Secret scan failure: remove the file/value and `git rm --cached <file>` if already tracked
        * Backend SKIP: expected until `backend/requirements.txt` exists
      - CI gaps table (identical to the one in cicd.md)
      - Note on ktlint: no ktlint plugin is configured; the check is permanently SKIPped until
        a ktlint Gradle plugin is added to `android/build.gradle.kts`
      Files: `docs/deployment/pre-push-quality-gate.md`
      Verify: file exists and is valid Markdown (no broken links to internal anchors).

- [ ] 5. Create `docs/deployment/cicd.md`
      Documentation per the outline above. Must include:
      - Overview of all three workflow files with their triggers and job structure
      - Local gate vs CI alignment table (as shown in the outline)
      - Documented CI gaps with rationale:
        1. No `compileDebugKotlin` in CI — "compilation errors surfaced only via lint; gap accepted for
           now; recommend adding as an explicit CI step before production release"
        2. No dependency vulnerability scan — "pip-audit and OWASP not configured; recommend Dependabot
           alerts enabled on GitHub and pip-audit added to backend-ci before production"
        3. `mypy` absent from `pr-checks.yml` backend job — "mypy runs in backend-ci.yml but not
           in the pr-checks consolidation job; not blocking but should be aligned"
        4. pytest + coverage threshold absent from `pr-checks.yml` backend job — same pattern
      - Branch protection recommendation: require `pr-gate` status check on `main`
      - Environment variables table (from `.env.example`) split into CI-safe (empty/false) and
        secret (must be in GitHub Actions secrets, never in YAML)
      Files: `docs/deployment/cicd.md`
      Verify: file exists; cross-reference the workflow YAML job names to confirm accuracy.

- [ ] 6. Run the quality gate and write `.agents/tasks/pre-push-gate-report.md`
      This is the verification step for the whole feature.
      Steps:
      a. Run `powershell -NoProfile -Command "[scriptblock]::Create((Get-Content -Raw .\check-before-push.ps1))"` — confirms PS1 parses cleanly.
      b. Run `bash -n check-before-push.sh` — confirms bash syntax clean.
      c. Run `bash -n .githooks/pre-push` — confirms hook syntax clean.
      d. Run `powershell -NoProfile -File check-before-push.ps1` from repo root.
         Capture actual output and exit code.
      e. Run `bash check-before-push.sh` from repo root (if bash available via Git Bash or WSL).
         Capture actual output and exit code.
      f. Summarise results in `.agents/tasks/pre-push-gate-report.md` including:
         - Actual PASS/FAIL/WARN/SKIP counts from each run
         - Any unexpected failures and their resolution
         - Backend section status (expected: SKIP — requirements.txt not yet present)
         - Android section status (expected: PASS for compile/lint/test if Gradle tasks succeed)
         - Secret scan status (expected: PASS on a clean checkout)
         - Confirmation that exit code is 0 on a clean run
      Files: `.agents/tasks/pre-push-gate-report.md`
      Verify: report file exists; exit codes match expected values.

---

## Validation commands summary

| Script | Syntax check command | Run command |
|--------|---------------------|-------------|
| `check-before-push.ps1` | `powershell -NoProfile -Command "[scriptblock]::Create((Get-Content -Raw .\check-before-push.ps1))"` | `powershell -NoProfile -File check-before-push.ps1` |
| `check-before-push.sh` | `bash -n check-before-push.sh` | `bash check-before-push.sh` |
| `.githooks/pre-push` | `bash -n .githooks/pre-push` | invoked by `git push` when hook is registered |

Expected results on a clean checkout with backend not yet scaffolded:
- Secret scan: PASS (no tracked secrets)
- Android compile: PASS (build artifacts already present in `android/app/build/`)
- Android lint: PASS (lint reports already in `android/app/build/reports/`)
- Android unit tests: PASS (test results already in `android/app/build/test-results/`)
- ktlint check: SKIP (no plugin configured)
- Backend (all checks): SKIP (requirements.txt absent)
- Overall exit code: 0

---

## Loop stop contract

The review loop gates on:
`j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI\.agents\tasks\review.json`

The reviewer writes this file with `{"verdict": "APPROVED"}` or `{"verdict": "CHANGES_REQUESTED"}`.
