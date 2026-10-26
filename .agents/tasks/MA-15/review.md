# CI Scaffold Hardening (MA-15)

Three GitHub Actions workflows and a PR template are introduced to gate every PR to `main` and `develop`. The android workflow covers lint, unit tests, and a secret scan; the backend workflow covers ruff/black lint and pytest with 90% coverage enforcement; a PR gate workflow re-runs the critical jobs behind a `verify-no-secrets-committed` barrier. The PR template enforces a five-section checklist on every submission.

Watch for: **The secrets grep in `pr-checks.yml` scans `*.yml` files across the entire repo — including itself and `backend-ci.yml`, both of which contain the string `secret` in a hardcoded `JWT_SECRET` value. Every PR will fail the `verify-no-secrets-committed` job on day one (confirmed). The hardcoded `JWT_SECRET` value in the committed YAML files also violates the no-secrets-in-committed-files rule (confirmed).**

**Verdict**: NEEDS_CHANGES

---

## High-level view

The overall workflow topology is correct: three files gate three distinct concerns, the pr-gate job correctly declares `needs` on all three upstream jobs, and all Gradle and Python commands use the right working directories. The env-var isolation for Android unit tests is in place in both `android-ci.yml` and the inline job in `pr-checks.yml`.

The fatal flaw is in `pr-checks.yml`'s `verify-no-secrets-committed` step. The grep includes `--include="*.yml"`, and the pattern `secret` (case-sensitive, no word boundary) matches the literal string `test-jwt-secret-for-ci-only-not-a-real-secret` embedded directly in `backend-ci.yml` and in `pr-checks.yml` itself. The scan will report a match and exit 1 on every PR, making the gate permanently broken before any application code is written.

The root cause of the false positive is that `JWT_SECRET` is hardcoded inline rather than pulled from a GitHub Actions secret or environment variable. This is also a standalone violation of the requirement that no secrets appear in committed files — even a dummy test value trains the codebase to accept inline credential patterns, and the value ends up in git history permanently.

The Android-side security scan in `android-ci.yml` is scoped to `android/app/src` and does not include `*.yml`, so it does not have the same self-defeating problem. The scope difference between the two grep calls is intentional and correct for the Android scan but the repo-wide scan in `pr-checks.yml` needed the YAML exclusion.

---

<details>
<summary>Issues (2)</summary>

1. **Self-defeating secrets grep** — `pr-checks.yml` `verify-no-secrets-committed` includes `--include="*.yml"` in its grep scope. The pattern `secret` matches `test-jwt-secret-for-ci-only-not-a-real-secret` in both `backend-ci.yml` and `pr-checks.yml`, causing the step to exit 1 on every PR. Fix: either add `--exclude-dir=".github"` to the grep, add a per-file exclusion for known CI-only values, or remove `*.yml`/`*.yaml` from the include list. *(confirmed)*

2. **Hardcoded JWT_SECRET in committed YAML** — `backend-ci.yml` (test job) and `pr-checks.yml` (backend-ci inline job) both contain `JWT_SECRET: test-jwt-secret-for-ci-only-not-a-real-secret` as a plain inline env var. Even though the value is not a real production secret, committing any literal bearing the name `JWT_SECRET` embeds it in git history and violates the no-secrets-in-committed-files rule. Fix: move the value to a GitHub Actions secret (`${{ secrets.CI_JWT_SECRET }}`) or generate it at job runtime with `openssl rand`. *(confirmed)*

</details>

---

<details>
<summary>Details</summary>

### Self-defeating repo-wide secrets grep

The `verify-no-secrets-committed` job in `pr-checks.yml` runs:

```bash
grep -r --exclude-dir=".git" \
  -E 'api_key|secret|password|ghp_|github_pat' \
  --include="*.kt" --include="*.java" --include="*.xml" \
  --include="*.py" --include="*.properties" --include="*.yml" \
  --include="*.yaml" --include="*.json" \
  .
```

The pattern `secret` is case-sensitive and unanchored. The value `test-jwt-secret-for-ci-only-not-a-real-secret` in `backend-ci.yml` contains the substring `secret` three times. The value also appears in the `pr-checks.yml` file itself. Since `*.yml` is in the include list and the search root is `.`, both files are scanned, both match, and the step exits 1. The CI gate will never pass.

The fix requires one of: excluding the `.github/` directory from the grep scope (`--exclude-dir=.github`), removing `*.yml` and `*.yaml` from the include list (YAML workflow files aren't application source), or replacing the inline literal with a reference to a GitHub Actions secret so there is nothing to match.

### Hardcoded JWT_SECRET in committed workflow files

`backend-ci.yml` (test job) and `pr-checks.yml` (backend-ci inline job) both contain:

```yaml
env:
  JWT_SECRET: test-jwt-secret-for-ci-only-not-a-real-secret
```

The intent is clear — this is a CI-only dummy value — but committing it violates the project rule that no secrets appear in any committed file. The value is now in git history and cannot be expunged without a force-push. More practically, it is the direct cause of the grep false positive above. The correct pattern is:

```yaml
env:
  JWT_SECRET: ${{ secrets.CI_JWT_SECRET }}
```

with `CI_JWT_SECRET` set to any dummy value in the repository's Actions secrets, or generate it inline:

```yaml
- name: Generate ephemeral JWT secret
  run: echo "JWT_SECRET=$(openssl rand -hex 32)" >> $GITHUB_ENV
```

Either approach removes the literal from source and unblocks the secrets grep.

### Requirement conformance — confirmed passing

Requirements 1–10, 12, and 13 are fully satisfied:

- `android-ci.yml` triggers on PR to both `main` and `develop`, has three separate jobs (`lint`, `unit-test`, `security-scan`), all Gradle commands use `working-directory: android`, the `unit-test` job carries the three required env vars, all applicable jobs use `actions/checkout@v4` + `actions/setup-java@v4` with JDK 17 Temurin, and `actions/cache@v4` covers `~/.gradle/caches` and `~/.gradle/wrapper`.
- The Android security-scan greps `android/app/src` with the exact required pattern `api_key|secret|password|ghp_|github_pat`.
- `backend-ci.yml` has separate `lint` and `test` jobs both using `working-directory: backend` and Python 3.12.
- `pr-checks.yml` triggers on both `develop` and `main`.
- `pr-gate` declares `needs: [android-ci, backend-ci, verify-no-secrets-committed]`.
- `PULL_REQUEST_TEMPLATE.md` contains all five required sections: Jira Ticket(s), Summary of Changes, Test Results, Security Checklist, Definition of Done.

The two blocking findings above are the only gaps between the current state and a fully passing scaffold.

</details>

---

<details>
<summary>File map</summary>

| File | What changed |
|------|-------------|
| `.github/workflows/android-ci.yml` | New: three-job Android CI (lint, unit-test, security-scan) |
| `.github/workflows/backend-ci.yml` | New: two-job backend CI (lint, test) with 90% coverage gate |
| `.github/workflows/pr-checks.yml` | New: pr-gate workflow with secrets verification and combined android/backend jobs |
| `.github/PULL_REQUEST_TEMPLATE.md` | New: five-section PR checklist template |

Full diff: `git diff main -- .github/`

</details>
