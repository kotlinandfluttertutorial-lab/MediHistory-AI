# Pre-Push Quality Gate for MediHistory AI

The change adds a cross-platform pre-push quality gate: `check-before-push.ps1`
(Windows), `check-before-push.sh` (Bash), a `.githooks/pre-push` OS dispatcher,
documentation at `docs/deployment/pre-push-quality-gate.md`, and a CI/CD gap
analysis at `docs/deployment/cicd.md`. Both scripts run the same seven logical
sections (0–6) and exit `0` on all-mandatory-pass, `1` on any mandatory failure.
A second fix iteration addressed four prior-review findings including a critical
PowerShell splat bug and a variable name misalignment.

**Watch for:**
- `password=` and `password='` patterns are in the PowerShell `$contentPatterns`
  and the documentation but absent from the Bash `CONTENT_PATTERNS` array —
  confirmed cross-platform parity gap in secret scanning.
- The Bash Section 3 db-test block calls `"$PYTHON" -m pytest` without checking
  whether `$PYTHON` is set — likely null-dereference when `requirements.txt`
  exists but no Python interpreter was found.
- On WSL2 with Windows interop PATH enabled, `.githooks/pre-push` resolves
  `powershell.exe` and attempts to pass a Linux path to it — likely dispatch
  failure for those users.
- `service-account*.json` is scanned in Section 4 (FAIL) but not in Section 0
  (WARN) — possible silent arrival at a FAIL with no earlier warning.

**Verdict**: NEEDS_CHANGES

---

## High-level view

Both scripts cover all seven sections with correct PASS/FAIL/WARN/SKIP semantics.
The Gradle invocations are accurate: `.bat` on Windows, plain `./gradlew` on
Bash, both run from `android/` with `--no-daemon`, targeting `:app:compileDebugKotlin`,
`:app:lintDebug`, and `:app:testDebugUnitTest`. The ktlint check is permanently
SKIPped, matching the confirmed absence of the plugin. The backend formatter
check correctly uses `black --check` rather than `ruff format` on both platforms.

Secret scanning limits matches to source file extensions, excludes `.env.example`
and CI YAML from content matching, and prints only `file:line` references. The
`password=` pattern family is handled correctly in PowerShell but is absent from
the Bash `CONTENT_PATTERNS` array — a confirmed parity gap since the hook
dispatches by OS and the check is called out in the documentation.

The CI/CD documentation accurately identifies all six known gaps. The alignment
table in `cicd.md` and the CI mapping table in the documentation are consistent
with each other and with the actual workflow files.

The `.githooks/pre-push` dispatcher handles macOS/Linux/Git Bash correctly.
The WSL case is the weak point: WSL exports `$OS=Windows_NT`, setting
`IS_WINDOWS=1`, but WSL also exports `$OSTYPE=linux-gnu`. On WSL2 with Windows
interop PATH enabled (the default), `powershell.exe` resolves from
`/mnt/c/Windows/System32`, `cygpath -w` is unavailable, and PowerShell.exe
receives a Linux path it cannot open. The fallback that saves most WSL users is
incidental, not defensive.

The validation report records a full PowerShell run (exit code 0, 11 PASS, 0
FAIL, 2 WARN, 10 SKIP) and `bash -n` syntax checks on both shell files. The
`medical-data-scan` WARN is noted but the triggering file is not identified —
a reproducibility gap in the report.

---

<details>
<summary>Issues (5)</summary>

1. **Bash `password=` patterns absent** — `check-before-push.sh` `CONTENT_PATTERNS`
   does not include `password="` or `password='`, but the PowerShell script and
   documentation both list them. A hardcoded password in a `.kt` or `.py` file
   is caught on Windows and silently missed on Bash. Add `password=\"` and
   `password='` to the Bash array.

2. **Bash Section 3 null-`$PYTHON` dereference** — When `requirements.txt` exists
   but no Python interpreter was found in Section 2, `$PYTHON` is empty. Section 3
   calls `"$PYTHON" -m pytest tests/ -m "db"` unconditionally, producing a
   blank-command error instead of a clean SKIP. Guard with
   `if [ -z "$PYTHON" ]; then check_result "db-tests" "SKIP" "no Python found"; fi`.
   The PowerShell script has the same gap for `$null`.

3. **WSL dispatch failure in `.githooks/pre-push`** — WSL sets `$OS=Windows_NT`,
   triggering `IS_WINDOWS=1`. On WSL2 with Windows interop PATH enabled,
   `powershell.exe` resolves to the Windows binary, `cygpath -w` is unavailable,
   and PowerShell.exe receives a Linux path it cannot open. Fix by checking
   `uname -r` for the `microsoft` kernel string and forcing `IS_WINDOWS=0` for
   WSL before the PowerShell branch fires.

4. **Section 0 / Section 4 sensitive-file pattern mismatch** — Section 0 checks
   five sensitive-file patterns (`.env`, `google-services.json`, `local.properties`,
   `*.keystore`, `*.jks`). Section 4 adds `service-account*.json` and FAILs
   rather than WARNs. A newly tracked `service-account-prod.json` produces no
   Section 0 warning before the later Section 4 FAIL. Add `service-account*.json`
   to the Section 0 pattern list.

5. **Validation report omits the `medical-data-scan` WARN trigger file** — The
   report records a WARN but does not name the file that matched `PATIENT_NAME=`
   or `DOCTOR_NAME=`. Update the report to include the path so the result is
   reproducible and distinguishable from a real fixture violation.

</details>

<details>
<summary>Details</summary>

### Bash `password=` pattern gap in Section 4

The PowerShell `$contentPatterns` array explicitly includes `password="` and
`password='`. The Bash `CONTENT_PATTERNS` array contains thirteen entries
(`KEYSTORE_PASSWORD=` through `hardcoded_secret`) but not `password="` or
`password='`. The documentation's Section 4 bullet list includes both.
A hardcoded password assignment in a Kotlin or Python source file passes the
Bash scan and fails the PowerShell scan. Because the hook dispatches based on
OS, a Windows developer catches what a Linux developer (or CI runner) misses.

```
# Bash CONTENT_PATTERNS (confirmed missing entries):
"password=\""
"password='"
```

### Bash Section 3 null-`$PYTHON` scope issue

`$PYTHON` is assigned inside the `else` branch of the Section 2
`[ ! -f "$REQUIREMENTS_FILE" ]` guard. In Section 3, when `DATABASE_URL` is set
and `requirements.txt` exists, the script calls `"$PYTHON" -m pytest tests/ -m "db"`
without checking whether `$PYTHON` was populated. If Section 2 found no Python
interpreter, `$PYTHON` is empty; in Bash, `"" -m pytest` passes an empty string
as the executable, which produces a confusing "command not found" or blank-command
error rather than a SKIP.

The PowerShell script has the same logical gap: `& $null "-m" "pytest"` with
`$null` raises a terminating error under `$ErrorActionPreference = "Continue"`,
which the script will swallow without incrementing the FAIL counter — effectively
a silent non-result.

### WSL dispatch failure in `.githooks/pre-push`

```bash
if [[ "${OSTYPE:-}" == "msys"* ]] || \
   [[ "${OSTYPE:-}" == "cygwin"* ]] || \
   [[ "${OSTYPE:-}" == "mingw"* ]] || \
   [[ "${OS:-}" == "Windows_NT" ]]; then
    IS_WINDOWS=1
fi
```

WSL2 sets `$OS=Windows_NT` (matching the last condition) and `$OSTYPE=linux-gnu`
(not matching any explicit condition). `IS_WINDOWS=1` is set. The hook then
checks `command -v powershell.exe`. With Windows interop PATH enabled — the
default in WSL2 since `/etc/wsl.conf` ships with `appendWindowsPath=true` — the
check finds `powershell.exe` at `/mnt/c/Windows/System32/...`. `cygpath -w` is
not available in WSL, so `PS1_PATH` retains the Linux path. PowerShell.exe
receives `-File /home/user/.../check-before-push.ps1` and fails to open the file.

The fix is explicit WSL detection before the Windows branch:
```bash
if [[ "$(uname -r 2>/dev/null)" == *microsoft* ]] || \
   [[ "$(uname -r 2>/dev/null)" == *Microsoft* ]]; then
    IS_WINDOWS=0
fi
```
This covers both WSL1 and WSL2 kernel strings before the `$OS=Windows_NT` check
can set `IS_WINDOWS=1`.

</details>

---

<details>
<summary>File map</summary>

| File | What changed |
|---|---|
| `check-before-push.ps1` | New: full Windows PowerShell quality gate, 7 sections |
| `check-before-push.sh` | New: full Bash quality gate, 7 sections |
| `.githooks/pre-push` | New: OS-dispatching Git pre-push hook |
| `docs/deployment/pre-push-quality-gate.md` | New: 15-section user-facing documentation |
| `docs/deployment/cicd.md` | New: CI/CD overview with 6 identified gaps and alignment table |
| `.agents/tasks/pre-push-gate-report.md` | New: validation report with syntax checks and full run output |

Full diff source: `j:\Android\AndroidStudioProjects\Kiro\MediHistory-AI`

</details>
