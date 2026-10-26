# Pre-Push Quality Gate — Semantic Code Review

The change adds a cross-platform pre-push quality gate: `check-before-push.ps1` (Windows PowerShell), `check-before-push.sh` (Bash), `.githooks/pre-push` (OS-dispatching hook), `docs/deployment/pre-push-quality-gate.md` (documentation), and `docs/deployment/cicd.md` (CI/CD overview with gap analysis). The scripts run seven sections (0–6) covering git hygiene, Android build, Python backend, database tests, secret scanning, dependency vulnerability scanning, and CI config validation. The core contract — sections 0–6, PASS/FAIL/WARN/SKIP accounting, exit 1 on any mandatory FAIL — is met in both scripts.

Watch for: (1) **confirmed** — `JWT_SECRET` (no `_KEY` suffix) is used as the env-var name in both scripts and in `cicd.md`, but `.env.example` defines `JWT_SECRET_KEY`; the mismatch will silently skip JWT validation in every local pytest run. (2) **confirmed** — the local pytest invocation omits `--cov-fail-under=90` without documenting this gap in the scripts; a developer can push with coverage below 90% and only discover the failure in CI. (3) **possible** — the PowerShell `Invoke-External` helper splices argument arrays through `$Arguments[1..($Arguments.Length-1)]`, which returns `@(1,0)` rather than an empty array for a one-element input; no current call site triggers this but it is a latent trap. (4) **confirmed** — the Windows branch of `.githooks/pre-push` calls `exec powershell.exe` unconditionally, without first checking that `check-before-push.ps1` exists, while the Unix branch does check; the failure mode is safe (push blocked) but the error is a raw PowerShell complaint instead of the hook's formatted message.

**Verdict**: NEEDS_CHANGES

---

## High-level view

The Gradle commands are correct: `.\gradlew.bat` from `$ANDROID_DIR` in PowerShell and `./gradlew` in a `cd`-subshell in Bash; both use `:app:compileDebugKotlin`, `:app:lintDebug`, and `:app:testDebugUnitTest`; `gradlew.bat` (not `gradlew`) is the Windows path; ktlint is permanently SKIP with a clear reason. The backend formatting check uses `black --check .` in both scripts — not `ruff format`.

The `JWT_SECRET` / `JWT_SECRET_KEY` name divergence is a functional bug. Both scripts export `JWT_SECRET=test-jwt-secret-for-ci-only-not-a-real-secret`. The `cicd.md` "CI-safe values" table also lists `JWT_SECRET`. But `.env.example` names the variable `JWT_SECRET_KEY`, and `cicd.md` section 6 lists `JWT_SECRET_KEY` as the GitHub Actions secret the application reads. If the FastAPI app reads only `JWT_SECRET_KEY`, every local pytest run silently operates without a JWT secret.

Both scripts run `pytest tests/ -m "not db"` without `--cov-fail-under=90`, while CI runs `pytest --cov=app --cov-fail-under=90`. The scripts' comment on mypy says "no extra flags — matches CI", and the documentation says "Both scripts match CI exactly" in the context of `black --check`. These phrases create an expectation of pytest parity that does not hold. The Known Limitations section in the documentation does not list this divergence.

The PowerShell `Invoke-External` helper has a latent defect: `$Arguments[1..($Arguments.Length-1)]` with a one-element array produces `@(1, 0)` in PowerShell (range semantics), not an empty array. No current call passes a single argument, but the function is general-purpose.

All six required output files are present. The validation report records bash -n syntax checks (exit 0), PowerShell AST parse (0 errors), and a full execution run with result table. The report correctly identifies the pre-existing `gradle.properties` issue as a repository problem, not a script defect.

`cicd.md` correctly identifies all five documented CI gaps and includes concrete YAML remediation snippets for each.

---

<details>
<summary>Issues (4)</summary>

1. **JWT_SECRET vs JWT_SECRET_KEY name mismatch** — Both scripts and `cicd.md` export `JWT_SECRET`, but `.env.example` (the authoritative variable list) defines `JWT_SECRET_KEY`. If the FastAPI app reads `JWT_SECRET_KEY`, local pytest runs will silently lack a JWT secret. Align the variable name across both scripts, `cicd.md`, and verify against the actual app configuration.

2. **Local pytest missing --cov-fail-under=90** — CI runs `pytest --cov=app --cov-fail-under=90`; the local gate runs `pytest tests/ -m "not db"` with no coverage flag. A developer can reduce coverage below 90% and push successfully. Add either `--cov=app --cov-fail-under=90` to the local invocation, or add this divergence explicitly to the Known Limitations section and to the inline comment in both scripts.

3. **PowerShell Invoke-External one-element array splicing** — `$Arguments[1..($Arguments.Length-1)]` with `$Arguments.Length -eq 1` evaluates to `@(1, 0)` rather than an empty array. Fix: `$rest = if ($Arguments.Length -gt 1) { $Arguments[1..($Arguments.Length-1)] } else { @() }` then `& $Arguments[0] @rest`.

4. **pre-push hook missing existence check for PS1 on Windows path** — The Unix branch guards with `if [ ! -f "$SH_PATH" ]`; the Windows branch calls `exec powershell.exe ... "$PS1_PATH"` unconditionally. If `check-before-push.ps1` is absent, the push is blocked but the error is a raw PowerShell complaint. Add a symmetric file-existence check before the `exec` on the Windows path.

</details>

<details>
<summary>Details</summary>

### JWT_SECRET vs JWT_SECRET_KEY

Both scripts export:

```bash
JWT_SECRET="test-jwt-secret-for-ci-only-not-a-real-secret"
```

The `cicd.md` "CI-safe values" table also lists `JWT_SECRET`. But `.env.example` defines `JWT_SECRET_KEY`, and the same document's GitHub Actions secrets table (section 6) lists `JWT_SECRET_KEY` as the variable the app and CI consume. Two different names are in flight simultaneously. If the FastAPI app reads only `JWT_SECRET_KEY`, the local pytest run silently has no JWT secret; auth middleware that doesn't fail-hard on a missing secret will accept unauthenticated requests, producing false-green local test results. This is a **confirmed** bug visible from a two-line diff between the scripts and `.env.example`.

### Local pytest vs CI coverage threshold

CI `backend-ci.yml` runs:
```
pytest --cov=app --cov-report=xml --cov-fail-under=90
```

Both scripts run:
```
python -m pytest tests/ -m "not db"
```

The testing steering file requires ≥90% line coverage for the data layer. The local gate opts out of coverage enforcement entirely. The documentation states "The CI uses `black --check .` — not `ruff format`. Both scripts match CI exactly." A developer reading this phrase will reasonably expect pytest to also match CI. The mismatch is not listed in Known Limitations. A PR that reduces backend coverage from 91% to 80% will pass the local gate and FAIL only in CI.

### PowerShell Invoke-External array splicing

```powershell
$output = & $Arguments[0] $Arguments[1..($Arguments.Length-1)] 2>&1
```

PowerShell evaluates `1..0` as `@(1, 0)`. So when `$Arguments` is `@("git")` (one element), this becomes `& "git" 1 0`, not `& "git"`. All current call sites pass at least two elements, but the function is documented and structured as a general-purpose wrapper. The corrected form:

```powershell
$rest = if ($Arguments.Length -gt 1) { $Arguments[1..($Arguments.Length-1)] } else { @() }
$output = & $Arguments[0] @rest 2>&1
```

### Windows hook missing existence check

The Unix path in `.githooks/pre-push`:
```bash
if [ ! -f "$SH_PATH" ]; then
    echo "ERROR: check-before-push.sh not found at $SH_PATH" >&2
    exit 1
fi
exec bash "$SH_PATH"
```

The Windows path:
```bash
PS1_PATH="$(cygpath -w "$REPO_ROOT/check-before-push.ps1" 2>/dev/null || echo "$REPO_ROOT/check-before-push.ps1")"
exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$PS1_PATH"
```

If `check-before-push.ps1` is absent, `powershell.exe` will report a file-not-found error and exit non-zero. The push is blocked, which is safe. The UX is worse than the Unix path. Adding a parallel check before `exec` closes the parity gap.

</details>

---

<details>
<summary>File map</summary>

| File | What changed |
|---|---|
| `check-before-push.sh` | New — Bash quality gate, sections 0–6 |
| `check-before-push.ps1` | New — PowerShell quality gate, sections 0–6 |
| `.githooks/pre-push` | New — OS-dispatching pre-push hook |
| `docs/deployment/pre-push-quality-gate.md` | New — 15-section documentation |
| `docs/deployment/cicd.md` | New — CI/CD overview and 5-gap analysis |
| `.agents/tasks/pre-push-gate-report.md` | New — validation report with bash -n, PS parse, full run results |

Full diff: `git diff main -- check-before-push.sh check-before-push.ps1 .githooks/pre-push docs/deployment/ .agents/tasks/pre-push-gate-report.md`

</details>
