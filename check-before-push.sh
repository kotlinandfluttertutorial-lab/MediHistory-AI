#!/usr/bin/env bash
# =============================================================================
# MediHistory AI -- Pre-Push Quality Gate (Bash)
# =============================================================================
#
# Runs all mandatory quality checks before pushing code.
# Sections:
#   0 -- Git hygiene
#   1 -- Android (compile, lint, unit tests)
#   2 -- Python backend (ruff, black, mypy, pytest)
#   3 -- Database tests (optional -- skipped if DATABASE_URL not set)
#   4 -- Secret / sensitive-file scan
#   5 -- Dependency vulnerability scan (optional)
#   6 -- CI config validation
#
# Exit 0 -- all mandatory checks passed
# Exit 1 -- one or more mandatory checks failed
#
# Never prints credential values -- only file:line references.
# Never modifies source files.
#
# Usage:
#   bash check-before-push.sh          # from repo root
#   ./check-before-push.sh             # after chmod +x
# =============================================================================

SCRIPT_VERSION="1.0.0"

# Do NOT use set -e -- we want to continue on individual check failures.
# Capture exit codes explicitly after every command.
set -o pipefail

# ---------------------------------------------------------------------------
# Counters and tracking
# ---------------------------------------------------------------------------
PASS=0
FAIL=0
WARN=0
SKIP=0
FAILED_CHECKS=()

# ---------------------------------------------------------------------------
# Color support
# ---------------------------------------------------------------------------
if command -v tput &>/dev/null && [ -t 1 ]; then
    COLOR_RESET="$(tput sgr0)"
    COLOR_GREEN="$(tput setaf 2)"
    COLOR_RED="$(tput setaf 1)"
    COLOR_YELLOW="$(tput setaf 3)"
    COLOR_CYAN="$(tput setaf 6)"
    COLOR_WHITE="$(tput bold)"
    COLOR_GRAY="$(tput setaf 8 2>/dev/null || tput setaf 7)"
else
    COLOR_RESET=""
    COLOR_GREEN=""
    COLOR_RED=""
    COLOR_YELLOW=""
    COLOR_CYAN=""
    COLOR_WHITE=""
    COLOR_GRAY=""
fi

# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------
print_section() {
    echo ""
    echo "${COLOR_CYAN}$(printf '=%.0s' {1..70})${COLOR_RESET}"
    echo "${COLOR_CYAN}  $1${COLOR_RESET}"
    echo "${COLOR_CYAN}$(printf '=%.0s' {1..70})${COLOR_RESET}"
}

print_info() {
    echo "${COLOR_GRAY}         $1${COLOR_RESET}"
}

check_result() {
    local name="$1"
    local status="$2"
    local message="${3:-}"
    local tag

    case "$status" in
        PASS) PASS=$((PASS + 1)); tag="${COLOR_GREEN}[ PASS ]${COLOR_RESET}" ;;
        FAIL) FAIL=$((FAIL + 1)); tag="${COLOR_RED}[ FAIL ]${COLOR_RESET}"; FAILED_CHECKS+=("$name") ;;
        WARN) WARN=$((WARN + 1)); tag="${COLOR_YELLOW}[ WARN ]${COLOR_RESET}" ;;
        SKIP) SKIP=$((SKIP + 1)); tag="${COLOR_GRAY}[ SKIP ]${COLOR_RESET}" ;;
    esac

    local line="$tag  $name"
    if [ -n "$message" ]; then
        line="$line -- $message"
    fi
    echo "$line"
}

# ---------------------------------------------------------------------------
# Find repo root
# ---------------------------------------------------------------------------
find_repo_root() {
    local dir
    dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    while [ "$dir" != "/" ]; do
        if [ -d "$dir/.git" ]; then
            echo "$dir"
            return 0
        fi
        dir="$(dirname "$dir")"
    done
    return 1
}

# ---------------------------------------------------------------------------
# Banner
# ---------------------------------------------------------------------------
echo ""
echo "${COLOR_WHITE}MediHistory AI -- Pre-Push Quality Gate v${SCRIPT_VERSION}${COLOR_RESET}"
echo "${COLOR_GRAY}$(date '+%Y-%m-%d %H:%M:%S')${COLOR_RESET}"

# ---------------------------------------------------------------------------
# Locate repo root
# ---------------------------------------------------------------------------
REPO_ROOT="$(find_repo_root)"
if [ -z "$REPO_ROOT" ]; then
    echo "${COLOR_RED}FATAL: Cannot find repo root (.git). Run from inside the repository.${COLOR_RESET}"
    exit 1
fi
cd "$REPO_ROOT" || exit 1
print_info "Repository root: $REPO_ROOT"

# ===========================================================================
# SECTION 0 -- Git hygiene
# ===========================================================================
print_section "Section 0 -- Git Hygiene"

git --version &>/dev/null
GIT_EXIT=$?
if [ $GIT_EXIT -ne 0 ]; then
    check_result "git-available" "FAIL" "git not found in PATH"
else
    check_result "git-available" "PASS"
fi

git rev-parse --show-toplevel &>/dev/null
REPO_EXIT=$?
if [ $REPO_EXIT -ne 0 ]; then
    check_result "git-repo" "FAIL" "Not inside a git repository"
else
    check_result "git-repo" "PASS"
fi

CURRENT_BRANCH="$(git branch --show-current 2>/dev/null)"
print_info "Current branch: $CURRENT_BRANCH"

STATUS_OUTPUT="$(git status --short 2>/dev/null)"
if [ -n "$STATUS_OUTPUT" ]; then
    print_info "Working tree changes:"
    while IFS= read -r line; do
        print_info "  $line"
    done <<< "$STATUS_OUTPUT"
    check_result "working-tree-clean" "WARN" "Uncommitted changes exist -- they will NOT be included in the push"
else
    check_result "working-tree-clean" "PASS" "Working tree is clean"
fi

CONFLICT_FILES="$(git diff --name-only --diff-filter=U 2>/dev/null)"
if [ -n "$CONFLICT_FILES" ]; then
    check_result "no-merge-conflicts" "FAIL" "Unresolved merge conflicts in: $CONFLICT_FILES"
else
    check_result "no-merge-conflicts" "PASS"
fi

# Junk directories that should not be tracked
declare -A JUNK_DIRS
JUNK_DIRS["node_modules"]="node_modules/ directory"
JUNK_DIRS[".venv"]=".venv/ Python virtual environment"
JUNK_DIRS["__pycache__"]="__pycache__/ directory"
JUNK_DIRS["android/app/build"]="android/app/build/ Gradle output"

for pattern in "${!JUNK_DIRS[@]}"; do
    desc="${JUNK_DIRS[$pattern]}"
    if [ -n "$(git ls-files -- "$pattern" 2>/dev/null)" ]; then
        safe_name="$(echo "$pattern" | tr -dc 'a-zA-Z0-9')"
        check_result "junk-not-tracked-$safe_name" "WARN" "$desc appears to be tracked -- consider adding to .gitignore"
    fi
done

# Sensitive files (not .env.example -- that is the committed template)
SENSITIVE_PATTERNS=(".env" "google-services.json" "local.properties" "*.keystore" "*.jks" "service-account*.json")
for pat in "${SENSITIVE_PATTERNS[@]}"; do
    TRACKED="$(git ls-files -- "$pat" 2>/dev/null)"
    if [ -n "$TRACKED" ]; then
        safe_name="$(echo "$pat" | tr -dc 'a-zA-Z0-9')"
        check_result "sensitive-not-tracked-$safe_name" "WARN" "Sensitive file tracked: $TRACKED"
    fi
done

# ===========================================================================
# SECTION 1 -- Android
# ===========================================================================
print_section "Section 1 -- Android"

ANDROID_DIR="$REPO_ROOT/android"
GRADLEW="$ANDROID_DIR/gradlew"

if [ ! -f "$GRADLEW" ]; then
    check_result "gradlew-exists" "FAIL" "android/gradlew not found -- Android checkout may be incomplete"
else
    chmod +x "$GRADLEW"
    check_result "gradlew-exists" "PASS"

    export BACKEND_BASE_URL=""
    export ENABLE_CLOUD_BACKUP="false"
    export ENABLE_INTERACTION_CHECK="false"

    # 1.1 -- Compile
    print_info "Running :app:compileDebugKotlin (not in CI -- local gate adds this check)..."
    (cd "$ANDROID_DIR" && ./gradlew :app:compileDebugKotlin --no-daemon -PskipGoogleServices=true)
    ANDROID_COMPILE_EXIT=$?
    if [ $ANDROID_COMPILE_EXIT -eq 0 ]; then
        check_result "android-compile" "PASS" "(:app:compileDebugKotlin -- note: not in CI)"
    else
        check_result "android-compile" "FAIL" ":app:compileDebugKotlin exited $ANDROID_COMPILE_EXIT -- fix Kotlin errors"
    fi

    # 1.2 -- Lint
    print_info "Running :app:lintDebug..."
    (cd "$ANDROID_DIR" && ./gradlew :app:lintDebug --no-daemon -PskipGoogleServices=true)
    ANDROID_LINT_EXIT=$?
    if [ $ANDROID_LINT_EXIT -eq 0 ]; then
        check_result "android-lint" "PASS"
    else
        check_result "android-lint" "FAIL" ":app:lintDebug exited $ANDROID_LINT_EXIT -- see android/app/build/reports/lint-results-debug.html"
    fi

    # 1.3 -- ktlint (not configured)
    check_result "android-ktlint" "SKIP" "ktlint not configured (no ktlint plugin in android/build.gradle.kts)"

    # 1.4 -- Unit tests
    print_info "Running :app:testDebugUnitTest..."
    (cd "$ANDROID_DIR" && ./gradlew :app:testDebugUnitTest --no-daemon -PskipGoogleServices=true)
    ANDROID_TEST_EXIT=$?
    if [ $ANDROID_TEST_EXIT -eq 0 ]; then
        check_result "android-unit-tests" "PASS"
    else
        check_result "android-unit-tests" "FAIL" ":app:testDebugUnitTest exited $ANDROID_TEST_EXIT -- see android/app/build/reports/tests/testDebugUnitTest/"
    fi
fi

# ===========================================================================
# SECTION 2 -- Python backend
# ===========================================================================
print_section "Section 2 -- Python Backend"

BACKEND_DIR="$REPO_ROOT/backend"
REQUIREMENTS_FILE="$BACKEND_DIR/requirements.txt"

if [ ! -f "$REQUIREMENTS_FILE" ]; then
    check_result "backend-ruff"   "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    check_result "backend-black"  "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    check_result "backend-mypy"   "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    check_result "backend-pytest" "SKIP" "backend not yet scaffolded (requirements.txt absent)"
else
    PYTHON=""
    VENV_PYTHON="$BACKEND_DIR/.venv/bin/python"
    if [ -f "$VENV_PYTHON" ]; then
        PYTHON="$VENV_PYTHON"
        print_info "Using venv Python: $PYTHON"
    else
        for candidate in python3 python; do
            if command -v "$candidate" &>/dev/null; then
                PYTHON="$candidate"
                print_info "Using system Python: $PYTHON ($("$PYTHON" --version 2>&1))"
                break
            fi
        done
    fi

    if [ -z "$PYTHON" ]; then
        check_result "backend-python-detected" "FAIL" "No Python interpreter found -- install Python 3.12"
    else
        check_result "backend-python-detected" "PASS"

        python_module_available() {
            "$PYTHON" -c "import $1" &>/dev/null 2>&1
        }

        # 2.1 -- ruff
        if ! python_module_available ruff; then
            check_result "backend-ruff" "FAIL" "ruff not installed -- run: pip install -r requirements.txt"
        else
            print_info "Running ruff check ..."
            (cd "$BACKEND_DIR" && "$PYTHON" -m ruff check .)
            RUFF_EXIT=$?
            if [ $RUFF_EXIT -eq 0 ]; then
                check_result "backend-ruff" "PASS"
            else
                check_result "backend-ruff" "FAIL" "ruff check failed -- run: python -m ruff check . --fix"
            fi
        fi

        # 2.2 -- black (CI uses black --check, NOT ruff format)
        if ! python_module_available black; then
            check_result "backend-black" "FAIL" "black not installed -- run: pip install -r requirements.txt"
        else
            print_info "Running black --check ..."
            (cd "$BACKEND_DIR" && "$PYTHON" -m black --check .)
            BLACK_EXIT=$?
            if [ $BLACK_EXIT -eq 0 ]; then
                check_result "backend-black" "PASS"
            else
                check_result "backend-black" "FAIL" "black format check failed -- repair: python -m black ."
            fi
        fi

        # 2.3 -- mypy (no extra flags -- matches CI)
        if ! python_module_available mypy; then
            check_result "backend-mypy" "FAIL" "mypy not installed -- run: pip install -r requirements.txt"
        else
            print_info "Running mypy app/ ..."
            (cd "$BACKEND_DIR" && "$PYTHON" -m mypy app/)
            MYPY_EXIT=$?
            if [ $MYPY_EXIT -eq 0 ]; then
                check_result "backend-mypy" "PASS"
            else
                check_result "backend-mypy" "FAIL" "mypy failed -- fix type errors in app/"
            fi
        fi

        # 2.4 -- pytest (non-db tests)
        if ! python_module_available pytest; then
            check_result "backend-pytest" "FAIL" "pytest not installed -- run: pip install -r requirements.txt"
        else
            print_info "Running pytest tests/ -m not db ..."
            export DATABASE_URL="sqlite:///:memory:"
            export JWT_SECRET_KEY="test-jwt-secret-for-ci-only-not-a-real-secret"
            export TESTING="true"
            # NOTE: CI runs pytest --cov=app --cov-fail-under=90; the local gate omits
            # --cov-fail-under to keep the run fast (no coverage report generated).
            # This is a documented divergence: a developer can push with coverage below 90%
            # and the CI coverage gate will catch it. See docs/deployment/cicd.md Known CI Gaps.
            (cd "$BACKEND_DIR" && "$PYTHON" -m pytest tests/ -m "not db")
            PYTEST_EXIT=$?
            unset DATABASE_URL JWT_SECRET_KEY TESTING
            if [ $PYTEST_EXIT -eq 0 ]; then
                check_result "backend-pytest" "PASS"
            else
                check_result "backend-pytest" "FAIL" "pytest exited $PYTEST_EXIT -- fix failing tests"
            fi
        fi
    fi
fi

# ===========================================================================
# SECTION 3 -- Database tests
# ===========================================================================
print_section "Section 3 -- Database Tests"

if [ -z "${DATABASE_URL:-}" ]; then
    check_result "db-tests" "SKIP" "DATABASE_URL not set -- db tests skipped (they run in CI)"
else
    print_info "DATABASE_URL is set (value not shown)"
    if [ ! -f "$REQUIREMENTS_FILE" ]; then
        check_result "db-tests" "SKIP" "backend not yet scaffolded -- no tests to run"
    elif [ -z "$PYTHON" ]; then
        check_result "db-tests" "SKIP" "no Python interpreter found -- db tests skipped"
    else
        print_info "Running pytest tests/ -m db (OPTIONAL -- WARN on failure)"
        export JWT_SECRET_KEY="test-jwt-secret-for-ci-only-not-a-real-secret"
        export TESTING="true"
        (cd "$BACKEND_DIR" && "$PYTHON" -m pytest tests/ -m "db")
        DB_TEST_EXIT=$?
        unset JWT_SECRET_KEY TESTING
        if [ $DB_TEST_EXIT -eq 0 ]; then
            check_result "db-tests" "PASS"
        else
            check_result "db-tests" "WARN" "db tests failed -- optional locally. Check DATABASE_URL connectivity."
        fi
    fi
fi

# ===========================================================================
# SECTION 4 -- Secret / sensitive-file scan
# ===========================================================================
print_section "Section 4 -- Secret / Sensitive-File Scan"

SECRET_FAILURES=()

# 4.1 -- Check for sensitive files in git index
# .env.example is intentionally tracked (template file, no real values -- excluded)
SENSITIVE_FILE_PATTERNS=(".env" "google-services.json" "local.properties" "*.keystore" "*.jks" "service-account*.json")
for pat in "${SENSITIVE_FILE_PATTERNS[@]}"; do
    FOUND="$(git ls-files -- "$pat" 2>/dev/null)"
    if [ -n "$FOUND" ]; then
        while IFS= read -r f; do
            # .env.example is the committed template -- not a secret file
            if [ -n "$f" ] && [ "$f" != ".env.example" ]; then
                SECRET_FAILURES+=("Sensitive file tracked: $f")
            fi
        done <<< "$FOUND"
    fi
done

# 4.2 -- Scan tracked SOURCE files for secret content patterns
# Scope: Kotlin, Java, XML, properties, Python source files only.
# Excludes: .env.example (template), CI YAML (contains grep patterns, not secrets),
#           *.example files.
# NEVER print the matched value -- only file:line references.

SOURCE_EXTENSIONS=("*.kt" "*.java" "*.xml" "*.properties" "*.py")

CONTENT_PATTERNS=(
    "KEYSTORE_PASSWORD="
    "JWT_SECRET_KEY="
    "OCR_API_KEY="
    "AI_EXTRACTION_API_KEY="
    "AI_NORMALIZATION_API_KEY="
    "AI_SUMMARIZATION_API_KEY="
    "CLOUD_BACKUP_ENCRYPTION_KEY="
    "MED_REFERENCE_API_KEY="
    "DB_PASSWORD="
    "STORAGE_SECRET_KEY="
    "ENCRYPTION_KEY="
    "KEY_PASSWORD="
    "hardcoded_secret"
    "password=\""
    "password='"
)

for pattern in "${CONTENT_PATTERNS[@]}"; do
    for ext in "${SOURCE_EXTENSIONS[@]}"; do
        MATCHED_FILES="$(git grep --cached -il -e "$pattern" -- "$ext" 2>/dev/null)"
        if [ -n "$MATCHED_FILES" ]; then
            while IFS= read -r matched_file; do
                [ -z "$matched_file" ] && continue
                # Extract file:line only -- not the matched value
                LINE_REFS="$(git grep --cached -in -e "$pattern" -- "$matched_file" 2>/dev/null | cut -d: -f1-2)"
                while IFS= read -r ref; do
                    [ -n "$ref" ] && SECRET_FAILURES+=("Secret pattern '$pattern' at $ref")
                done <<< "$LINE_REFS"
            done <<< "$MATCHED_FILES"
        fi
    done
done

# Token patterns: scan source files only (not CI YAML -- it contains grep patterns)
TOKEN_PATTERNS=("ghp_" "github_pat_")
for pattern in "${TOKEN_PATTERNS[@]}"; do
    for ext in "${SOURCE_EXTENSIONS[@]}"; do
        MATCHED_FILES="$(git grep --cached -il -e "$pattern" -- "$ext" 2>/dev/null)"
        if [ -n "$MATCHED_FILES" ]; then
            while IFS= read -r matched_file; do
                [ -z "$matched_file" ] && continue
                LINE_REFS="$(git grep --cached -in -e "$pattern" -- "$matched_file" 2>/dev/null | cut -d: -f1-2)"
                while IFS= read -r ref; do
                    [ -n "$ref" ] && SECRET_FAILURES+=("Token pattern '$pattern' at $ref")
                done <<< "$LINE_REFS"
            done <<< "$MATCHED_FILES"
        fi
    done
done

# AWS key prefix -- scan all tracked files (always a real secret if present in source)
AWS_MATCHED="$(git grep --cached -iEl -e 'AKIA[0-9A-Z]{16}' -- "*.kt" "*.java" "*.py" "*.properties" 2>/dev/null)"
if [ -n "$AWS_MATCHED" ]; then
    while IFS= read -r matched_file; do
        [ -z "$matched_file" ] && continue
        LINE_REFS="$(git grep --cached -iEn -e 'AKIA[0-9A-Z]{16}' -- "$matched_file" 2>/dev/null | cut -d: -f1-2)"
        while IFS= read -r ref; do
            [ -n "$ref" ] && SECRET_FAILURES+=("AWS key prefix at $ref")
        done <<< "$LINE_REFS"
    done <<< "$AWS_MATCHED"
fi

if [ ${#SECRET_FAILURES[@]} -gt 0 ]; then
    check_result "secret-scan" "FAIL" "Found ${#SECRET_FAILURES[@]} potential secret(s):"
    for f in "${SECRET_FAILURES[@]}"; do
        print_info "  $f"
    done
else
    check_result "secret-scan" "PASS" "No secrets or sensitive files found in tracked source files"
fi

# 4.3 -- Medical data patterns (WARN only -- allowed in test fixtures)
MEDICAL_WARNINGS=()
MEDICAL_PATTERNS=("PATIENT_NAME=" "DOCTOR_NAME=")
for pattern in "${MEDICAL_PATTERNS[@]}"; do
    FOUND_FILES="$(git grep --cached -il -e "$pattern" -- 2>/dev/null)"
    if [ -n "$FOUND_FILES" ]; then
        while IFS= read -r f; do
            [ -z "$f" ] && continue
            if ! echo "$f" | grep -qE "test/fixtures|src/test/fixtures"; then
                MEDICAL_WARNINGS+=("Medical data pattern '$pattern' in non-fixture file: $f")
            fi
        done <<< "$FOUND_FILES"
    fi
done

if [ ${#MEDICAL_WARNINGS[@]} -gt 0 ]; then
    check_result "medical-data-scan" "WARN" "Medical data patterns found outside test fixtures:"
    for w in "${MEDICAL_WARNINGS[@]}"; do
        print_info "  $w"
    done
else
    check_result "medical-data-scan" "PASS"
fi

# ===========================================================================
# SECTION 5 -- Dependency vulnerability scan
# ===========================================================================
print_section "Section 5 -- Dependency Vulnerability Scan"

if [ ! -f "$REQUIREMENTS_FILE" ]; then
    check_result "pip-audit" "SKIP" "backend not yet scaffolded"
    check_result "safety"    "SKIP" "backend not yet scaffolded"
else
    if ! "$PYTHON" -m pip_audit --version &>/dev/null 2>&1; then
        check_result "pip-audit" "SKIP" "pip-audit not installed -- consider: pip install pip-audit"
    else
        print_info "Running pip-audit ..."
        (cd "$BACKEND_DIR" && "$PYTHON" -m pip_audit)
        PIP_AUDIT_EXIT=$?
        if [ $PIP_AUDIT_EXIT -eq 0 ]; then
            check_result "pip-audit" "PASS"
        else
            check_result "pip-audit" "WARN" "pip-audit found vulnerabilities -- review before production (not a CI gate yet)"
        fi
    fi

    if ! "$PYTHON" -m safety --version &>/dev/null 2>&1; then
        check_result "safety" "SKIP" "safety not installed -- consider: pip install safety"
    else
        print_info "Running safety check ..."
        (cd "$BACKEND_DIR" && "$PYTHON" -m safety check)
        SAFETY_EXIT=$?
        if [ $SAFETY_EXIT -eq 0 ]; then
            check_result "safety" "PASS"
        else
            check_result "safety" "WARN" "safety found vulnerabilities -- review before production (not a CI gate yet)"
        fi
    fi
fi

check_result "android-vuln-scan" "SKIP" "No Android/Gradle vulnerability scanning configured yet (CI gap -- see docs/deployment/cicd.md)"

# ===========================================================================
# SECTION 6 -- CI config validation
# ===========================================================================
print_section "Section 6 -- CI Config Validation"

REQUIRED_WORKFLOWS=(
    ".github/workflows/android-ci.yml"
    ".github/workflows/backend-ci.yml"
    ".github/workflows/pr-checks.yml"
)

for wf in "${REQUIRED_WORKFLOWS[@]}"; do
    wf_path="$REPO_ROOT/$wf"
    safe_name="$(echo "$wf" | tr -dc 'a-zA-Z0-9-')"
    if [ -f "$wf_path" ]; then
        check_result "ci-workflow-$safe_name" "PASS" "$wf exists"
    else
        check_result "ci-workflow-$safe_name" "FAIL" "$wf is MISSING -- CI will not run"
    fi
done

if ! command -v yamllint &>/dev/null; then
    check_result "yamllint" "SKIP" "yamllint not installed -- consider: pip install yamllint"
else
    for wf in "${REQUIRED_WORKFLOWS[@]}"; do
        wf_path="$REPO_ROOT/$wf"
        safe_name="$(echo "$wf" | tr -dc 'a-zA-Z0-9-')"
        if [ -f "$wf_path" ]; then
            print_info "Running yamllint on $wf ..."
            yamllint "$wf_path"
            YAMLLINT_EXIT=$?
            if [ $YAMLLINT_EXIT -eq 0 ]; then
                check_result "yamllint-$safe_name" "PASS"
            else
                check_result "yamllint-$safe_name" "WARN" "yamllint warnings in $wf (not a blocking failure)"
            fi
        fi
    done
fi

# ===========================================================================
# SUMMARY
# ===========================================================================
echo ""
echo "${COLOR_WHITE}$(printf '=%.0s' {1..70})${COLOR_RESET}"
echo "${COLOR_WHITE}  SUMMARY${COLOR_RESET}"
echo "${COLOR_WHITE}$(printf '=%.0s' {1..70})${COLOR_RESET}"
echo ""
printf "  ${COLOR_GREEN}PASSED  : %4d${COLOR_RESET}\n" "$PASS"
if [ "$FAIL" -gt 0 ]; then
    printf "  ${COLOR_RED}FAILED  : %4d${COLOR_RESET}\n" "$FAIL"
else
    printf "  FAILED  : %4d\n" "$FAIL"
fi
if [ "$WARN" -gt 0 ]; then
    printf "  ${COLOR_YELLOW}WARNED  : %4d${COLOR_RESET}\n" "$WARN"
else
    printf "  WARNED  : %4d\n" "$WARN"
fi
printf "  ${COLOR_GRAY}SKIPPED : %4d${COLOR_RESET}\n" "$SKIP"
echo ""

if [ ${#FAILED_CHECKS[@]} -gt 0 ]; then
    echo "  ${COLOR_RED}Failed checks:${COLOR_RESET}"
    for fc in "${FAILED_CHECKS[@]}"; do
        echo "    ${COLOR_RED}- $fc${COLOR_RESET}"
    done
    echo ""
fi

if [ "$FAIL" -gt 0 ]; then
    echo "  ${COLOR_RED}RESULT: QUALITY GATE FAILED -- fix the issues above before pushing.${COLOR_RESET}"
    echo "  ${COLOR_YELLOW}To skip this hook (not recommended): git push --no-verify${COLOR_RESET}"
    echo ""
    exit 1
else
    echo "  ${COLOR_GREEN}RESULT: QUALITY GATE PASSED -- safe to push.${COLOR_RESET}"
    echo ""
    exit 0
fi
