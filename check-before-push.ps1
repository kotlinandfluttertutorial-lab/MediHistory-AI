#Requires -Version 5.1
<#
.SYNOPSIS
    MediHistory AI -- Pre-Push Quality Gate (Windows PowerShell)

.DESCRIPTION
    Runs all mandatory quality checks before pushing code.
    Sections:
      0 -- Git hygiene
      1 -- Android (compile, lint, unit tests)
      2 -- Python backend (ruff, black, mypy, pytest)
      3 -- Database tests (optional -- skipped if DATABASE_URL not set)
      4 -- Secret / sensitive-file scan
      5 -- Dependency vulnerability scan (optional)
      6 -- CI config validation

    Exit 0 -- all mandatory checks passed
    Exit 1 -- one or more mandatory checks failed

.NOTES
    Run from the repository root.
    Never prints credential values -- only file:line references.
    Never modifies source files.
#>

$ErrorActionPreference = "Continue"
$SCRIPT_VERSION = "1.0.0"

# ---------------------------------------------------------------------------
# Counters
# ---------------------------------------------------------------------------
$script:PASS  = 0
$script:FAIL  = 0
$script:WARN  = 0
$script:SKIP  = 0
$script:FAILED_CHECKS = [System.Collections.Generic.List[string]]::new()

# ---------------------------------------------------------------------------
# Output helpers
# ---------------------------------------------------------------------------
function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host ("=" * 70) -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host ("=" * 70) -ForegroundColor Cyan
}

function Write-CheckResult {
    param(
        [string]$Name,
        [ValidateSet("PASS","FAIL","WARN","SKIP")]
        [string]$Status,
        [string]$Message = ""
    )
    switch ($Status) {
        "PASS" { $script:PASS++; $colour = "Green";   $tag = "[ PASS ]" }
        "FAIL" { $script:FAIL++; $colour = "Red";     $tag = "[ FAIL ]"; $script:FAILED_CHECKS.Add($Name) }
        "WARN" { $script:WARN++; $colour = "Yellow";  $tag = "[ WARN ]" }
        "SKIP" { $script:SKIP++; $colour = "DarkGray";$tag = "[ SKIP ]" }
    }
    $line = "$tag  $Name"
    if ($Message) { $line = $line + " -- " + $Message }
    Write-Host $line -ForegroundColor $colour
}

function Write-Info {
    param([string]$Message)
    Write-Host "         $Message" -ForegroundColor Gray
}

# ---------------------------------------------------------------------------
# Find repo root
# ---------------------------------------------------------------------------
function Find-RepoRoot {
    $dir = $PSScriptRoot
    if (-not $dir) { $dir = (Get-Location).Path }
    while ($dir) {
        if (Test-Path (Join-Path $dir ".git")) { return $dir }
        $parent = Split-Path $dir -Parent
        if ($parent -eq $dir) { return $null }
        $dir = $parent
    }
    return $null
}

# ---------------------------------------------------------------------------
# Run external command capturing output and exit code.
# Uses & operator directly -- never a pipeline -- to preserve $LASTEXITCODE.
# ---------------------------------------------------------------------------
function Invoke-External {
    param(
        [string[]]$Arguments,
        [switch]$CaptureOutput
    )
    if ($CaptureOutput) {
        try {
            $output = & $Arguments[0] $Arguments[1..($Arguments.Length-1)] 2>&1
            $exitCode = $LASTEXITCODE
        } catch {
            $output = $_.Exception.Message
            $exitCode = 1
        }
        return [PSCustomObject]@{ Output = $output; ExitCode = $exitCode }
    } else {
        try {
            & $Arguments[0] $Arguments[1..($Arguments.Length-1)]
            return $LASTEXITCODE
        } catch {
            return 1
        }
    }
}

# ---------------------------------------------------------------------------
# Convert a path with spaces to its 8.3 short name (Windows only).
# This works around the issue where JAVA_HOME or other paths with spaces
# cause batch scripts to fail because they expand unquoted.
# ---------------------------------------------------------------------------
function Get-ShortPath {
    param([string]$LongPath)
    if (-not $LongPath) { return $LongPath }
    $fso = New-Object -ComObject Scripting.FileSystemObject
    if (Test-Path $LongPath -PathType Container) {
        try { return $fso.GetFolder($LongPath).ShortPath } catch { return $LongPath }
    } elseif (Test-Path $LongPath -PathType Leaf) {
        try { return $fso.GetFile($LongPath).ShortPath } catch { return $LongPath }
    }
    return $LongPath
}

# ---------------------------------------------------------------------------
# Banner
# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "MediHistory AI -- Pre-Push Quality Gate v$SCRIPT_VERSION" -ForegroundColor White
Write-Host (Get-Date -Format "yyyy-MM-dd HH:mm:ss") -ForegroundColor Gray

# ---------------------------------------------------------------------------
# Locate repo root
# ---------------------------------------------------------------------------
$REPO_ROOT = Find-RepoRoot
if (-not $REPO_ROOT) {
    Write-Host "FATAL: Cannot find repo root (.git). Run from inside the repository." -ForegroundColor Red
    exit 1
}
Set-Location $REPO_ROOT
Write-Info "Repository root: $REPO_ROOT"

# ===========================================================================
# SECTION 0 -- Git hygiene
# ===========================================================================
Write-Section "Section 0 -- Git Hygiene"

$gitCheck = Invoke-External -Arguments @("git","--version") -CaptureOutput
if ($gitCheck.ExitCode -ne 0) {
    Write-CheckResult "git-available" "FAIL" "git not found in PATH"
} else {
    Write-CheckResult "git-available" "PASS"
}

$repoCheck = Invoke-External -Arguments @("git","rev-parse","--show-toplevel") -CaptureOutput
if ($repoCheck.ExitCode -ne 0) {
    Write-CheckResult "git-repo" "FAIL" "Not inside a git repository"
} else {
    Write-CheckResult "git-repo" "PASS"
}

$branchResult = Invoke-External -Arguments @("git","branch","--show-current") -CaptureOutput
$currentBranch = ($branchResult.Output | Out-String).Trim()
Write-Info "Current branch: $currentBranch"

$statusResult = Invoke-External -Arguments @("git","status","--short") -CaptureOutput
$statusLines = ($statusResult.Output | Out-String).Trim()
if ($statusLines) {
    Write-Info "Working tree changes:"
    foreach ($line in ($statusLines -split "`n")) {
        if ($line.Trim()) { Write-Info "  $($line.Trim())" }
    }
    Write-CheckResult "working-tree-clean" "WARN" "Uncommitted changes exist -- they will NOT be included in the push"
} else {
    Write-CheckResult "working-tree-clean" "PASS" "Working tree is clean"
}

$conflictResult = Invoke-External -Arguments @("git","diff","--name-only","--diff-filter=U") -CaptureOutput
$conflictFiles = ($conflictResult.Output | Out-String).Trim()
if ($conflictFiles) {
    Write-CheckResult "no-merge-conflicts" "FAIL" "Unresolved merge conflicts in: $conflictFiles"
} else {
    Write-CheckResult "no-merge-conflicts" "PASS"
}

# Check for junk directories that should not be tracked
$junkList = @(
    @{ P = "node_modules"; D = "node_modules/ directory" },
    @{ P = ".venv";        D = ".venv/ Python virtual environment" },
    @{ P = "__pycache__";  D = "__pycache__/ directory" },
    @{ P = "android/app/build"; D = "android/app/build/ Gradle output" }
)
foreach ($j in $junkList) {
    $r = Invoke-External -Arguments @("git","ls-files","--",$j.P) -CaptureOutput
    if (($r.Output | Out-String).Trim()) {
        $sn = $j.P -replace '[^a-zA-Z0-9]',''
        Write-CheckResult "junk-not-tracked-$sn" "WARN" "$($j.D) appears to be tracked"
    }
}

# Check for sensitive files in git index
$sensFiles = @(".env","google-services.json","local.properties","*.keystore","*.jks")
foreach ($pat in $sensFiles) {
    $r = Invoke-External -Arguments @("git","ls-files","--",$pat) -CaptureOutput
    $found = ($r.Output | Out-String).Trim()
    if ($found) {
        $sn = $pat -replace '[^a-zA-Z0-9]',''
        Write-CheckResult "sensitive-not-tracked-$sn" "WARN" "Sensitive file tracked: $found"
    }
}

# ===========================================================================
# SECTION 1 -- Android
# ===========================================================================
Write-Section "Section 1 -- Android"

$ANDROID_DIR = Join-Path $REPO_ROOT "android"
$GRADLEW_BAT = Join-Path $ANDROID_DIR "gradlew.bat"

if (-not (Test-Path $GRADLEW_BAT)) {
    Write-CheckResult "gradlew-bat-exists" "FAIL" "android/gradlew.bat not found"
} else {
    Write-CheckResult "gradlew-bat-exists" "PASS"

    # Fix JAVA_HOME path if it contains spaces (gradlew.bat uses it unquoted internally)
    $originalJavaHome = $env:JAVA_HOME
    if ($env:JAVA_HOME -and $env:JAVA_HOME -match " ") {
        $shortPath = Get-ShortPath $env:JAVA_HOME
        if ($shortPath -and -not ($shortPath -match " ")) {
            $env:JAVA_HOME = $shortPath
            Write-Info "JAVA_HOME adjusted to short path to avoid space issues: $env:JAVA_HOME"
        }
    }

    $env:BACKEND_BASE_URL         = ""
    $env:ENABLE_CLOUD_BACKUP      = "false"
    $env:ENABLE_INTERACTION_CHECK = "false"

    # 1.1 -- Compile
    Write-Info "Running :app:compileDebugKotlin (not in CI -- local gate adds this check)..."
    Set-Location $ANDROID_DIR
    & ".\gradlew.bat" ":app:compileDebugKotlin" "--no-daemon"
    $exitCode = $LASTEXITCODE
    Set-Location $REPO_ROOT
    if ($exitCode -eq 0) {
        Write-CheckResult "android-compile" "PASS" "(:app:compileDebugKotlin -- note: not in CI)"
    } else {
        Write-CheckResult "android-compile" "FAIL" ":app:compileDebugKotlin exited $exitCode -- fix Kotlin errors"
    }

    # 1.2 -- Lint
    Write-Info "Running :app:lintDebug..."
    Set-Location $ANDROID_DIR
    & ".\gradlew.bat" ":app:lintDebug" "--no-daemon"
    $exitCode = $LASTEXITCODE
    Set-Location $REPO_ROOT
    if ($exitCode -eq 0) {
        Write-CheckResult "android-lint" "PASS"
    } else {
        Write-CheckResult "android-lint" "FAIL" ":app:lintDebug exited $exitCode -- see android/app/build/reports/lint-results-debug.html"
    }

    # 1.3 -- ktlint (not configured)
    Write-CheckResult "android-ktlint" "SKIP" "ktlint not configured (no ktlint plugin in android/build.gradle.kts)"

    # 1.4 -- Unit tests
    Write-Info "Running :app:testDebugUnitTest..."
    Set-Location $ANDROID_DIR
    & ".\gradlew.bat" ":app:testDebugUnitTest" "--no-daemon"
    $exitCode = $LASTEXITCODE
    Set-Location $REPO_ROOT
    if ($exitCode -eq 0) {
        Write-CheckResult "android-unit-tests" "PASS"
    } else {
        Write-CheckResult "android-unit-tests" "FAIL" ":app:testDebugUnitTest exited $exitCode -- see android/app/build/reports/tests/testDebugUnitTest/"
    }

    # Restore original JAVA_HOME
    if ($originalJavaHome) { $env:JAVA_HOME = $originalJavaHome }
}

# ===========================================================================
# SECTION 2 -- Python backend
# ===========================================================================
Write-Section "Section 2 -- Python Backend"

$BACKEND_DIR       = Join-Path $REPO_ROOT "backend"
$REQUIREMENTS_FILE = Join-Path $BACKEND_DIR "requirements.txt"

if (-not (Test-Path $REQUIREMENTS_FILE)) {
    Write-CheckResult "backend-ruff"   "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    Write-CheckResult "backend-black"  "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    Write-CheckResult "backend-mypy"   "SKIP" "backend not yet scaffolded (requirements.txt absent)"
    Write-CheckResult "backend-pytest" "SKIP" "backend not yet scaffolded (requirements.txt absent)"
} else {
    $PYTHON = $null
    $venvPython = Join-Path $BACKEND_DIR ".venv\Scripts\python.exe"
    if (Test-Path $venvPython) {
        $PYTHON = $venvPython
        Write-Info "Using venv Python: $PYTHON"
    } else {
        foreach ($candidate in @("py","python")) {
            $check = Invoke-External -Arguments @($candidate,"--version") -CaptureOutput
            if ($check.ExitCode -eq 0) { $PYTHON = $candidate; Write-Info "Using system Python: $PYTHON"; break }
        }
    }

    if (-not $PYTHON) {
        Write-CheckResult "backend-python-detected" "FAIL" "No Python interpreter found -- install Python 3.12"
    } else {
        Write-CheckResult "backend-python-detected" "PASS"

        function Test-PythonModule {
            param([string]$Module)
            $r = Invoke-External -Arguments @($script:PYTHON,"-c","import $Module") -CaptureOutput
            return $r.ExitCode -eq 0
        }

        # 2.1 -- ruff
        if (-not (Test-PythonModule "ruff")) {
            Write-CheckResult "backend-ruff" "FAIL" "ruff not installed -- run: pip install -r requirements.txt"
        } else {
            Write-Info "Running ruff check ..."
            Set-Location $BACKEND_DIR
            & $PYTHON "-m" "ruff" "check" "."
            $exitCode = $LASTEXITCODE
            Set-Location $REPO_ROOT
            if ($exitCode -eq 0) { Write-CheckResult "backend-ruff" "PASS" }
            else { Write-CheckResult "backend-ruff" "FAIL" "ruff check failed -- run: python -m ruff check . --fix" }
        }

        # 2.2 -- black (CI uses black --check, NOT ruff format)
        if (-not (Test-PythonModule "black")) {
            Write-CheckResult "backend-black" "FAIL" "black not installed -- run: pip install -r requirements.txt"
        } else {
            Write-Info "Running black --check ..."
            Set-Location $BACKEND_DIR
            & $PYTHON "-m" "black" "--check" "."
            $exitCode = $LASTEXITCODE
            Set-Location $REPO_ROOT
            if ($exitCode -eq 0) { Write-CheckResult "backend-black" "PASS" }
            else { Write-CheckResult "backend-black" "FAIL" "black format check failed -- repair: python -m black ." }
        }

        # 2.3 -- mypy (no extra flags -- matches CI)
        if (-not (Test-PythonModule "mypy")) {
            Write-CheckResult "backend-mypy" "FAIL" "mypy not installed -- run: pip install -r requirements.txt"
        } else {
            Write-Info "Running mypy app/ ..."
            Set-Location $BACKEND_DIR
            & $PYTHON "-m" "mypy" "app/"
            $exitCode = $LASTEXITCODE
            Set-Location $REPO_ROOT
            if ($exitCode -eq 0) { Write-CheckResult "backend-mypy" "PASS" }
            else { Write-CheckResult "backend-mypy" "FAIL" "mypy failed -- fix type errors in app/" }
        }

        # 2.4 -- pytest (non-db tests)
        if (-not (Test-PythonModule "pytest")) {
            Write-CheckResult "backend-pytest" "FAIL" "pytest not installed -- run: pip install -r requirements.txt"
        } else {
            Write-Info "Running pytest tests/ -m not db ..."
            $env:DATABASE_URL = "sqlite:///:memory:"
            $env:JWT_SECRET   = "test-jwt-secret-for-ci-only-not-a-real-secret"
            $env:TESTING      = "true"
            Set-Location $BACKEND_DIR
            & $PYTHON "-m" "pytest" "tests/" "-m" "not db"
            $exitCode = $LASTEXITCODE
            Set-Location $REPO_ROOT
            Remove-Item Env:\DATABASE_URL -ErrorAction SilentlyContinue
            Remove-Item Env:\JWT_SECRET   -ErrorAction SilentlyContinue
            Remove-Item Env:\TESTING      -ErrorAction SilentlyContinue
            if ($exitCode -eq 0) { Write-CheckResult "backend-pytest" "PASS" }
            else { Write-CheckResult "backend-pytest" "FAIL" "pytest exited $exitCode -- fix failing tests" }
        }
    }
}

# ===========================================================================
# SECTION 3 -- Database tests
# ===========================================================================
Write-Section "Section 3 -- Database Tests"

$dbUrl = [System.Environment]::GetEnvironmentVariable("DATABASE_URL")
if (-not $dbUrl) {
    Write-CheckResult "db-tests" "SKIP" "DATABASE_URL not set -- db tests skipped (they run in CI)"
} else {
    Write-Info "DATABASE_URL is set (value not shown)"
    if (-not (Test-Path $REQUIREMENTS_FILE)) {
        Write-CheckResult "db-tests" "SKIP" "backend not yet scaffolded -- no tests to run"
    } else {
        Write-Info "Running pytest tests/ -m db (OPTIONAL -- WARN on failure)"
        $env:JWT_SECRET = "test-jwt-secret-for-ci-only-not-a-real-secret"
        $env:TESTING    = "true"
        Set-Location $BACKEND_DIR
        & $PYTHON "-m" "pytest" "tests/" "-m" "db"
        $exitCode = $LASTEXITCODE
        Set-Location $REPO_ROOT
        Remove-Item Env:\JWT_SECRET -ErrorAction SilentlyContinue
        Remove-Item Env:\TESTING    -ErrorAction SilentlyContinue
        if ($exitCode -eq 0) { Write-CheckResult "db-tests" "PASS" }
        else { Write-CheckResult "db-tests" "WARN" "db tests failed -- optional locally. Check DATABASE_URL connectivity." }
    }
}

# ===========================================================================
# SECTION 4 -- Secret / sensitive-file scan
# ===========================================================================
Write-Section "Section 4 -- Secret / Sensitive-File Scan"

$secretFailures = [System.Collections.Generic.List[string]]::new()

# 4.1 -- Check for sensitive files in git index
# Exclusions: .env.example is a committed template -- not a secret file
$sensitiveFilePatterns = @(".env","google-services.json","local.properties","*.keystore","*.jks","service-account*.json")
foreach ($pat in $sensitiveFilePatterns) {
    $r = Invoke-External -Arguments @("git","ls-files","--",$pat) -CaptureOutput
    $found = ($r.Output | Out-String).Trim()
    if ($found) {
        foreach ($f in ($found -split "`n")) {
            $fTrim = $f.Trim()
            # .env.example is intentionally tracked (template file, no real values)
            if ($fTrim -and $fTrim -ne ".env.example") {
                $secretFailures.Add("Sensitive file tracked: $fTrim")
            }
        }
    }
}

# 4.2 -- Scan tracked source files for secret content patterns
# Scope: Kotlin, Python, XML, properties -- NOT .env.example (template) or workflow YAML
# (workflows contain grep patterns that look like secrets but are not values)
# NEVER print the matched value -- only file:line references.
$contentPatterns = @(
    "KEYSTORE_PASSWORD=",
    "JWT_SECRET_KEY=",
    "OCR_API_KEY=",
    "AI_EXTRACTION_API_KEY=",
    "AI_NORMALIZATION_API_KEY=",
    "AI_SUMMARIZATION_API_KEY=",
    "CLOUD_BACKUP_ENCRYPTION_KEY=",
    "MED_REFERENCE_API_KEY=",
    "DB_PASSWORD=",
    "STORAGE_SECRET_KEY=",
    "ENCRYPTION_KEY=",
    "KEY_PASSWORD=",
    "hardcoded_secret"
)

# Source file extensions to scan (excludes .env.example, CI YAML, and *.example files)
$sourceExtensions = @("*.kt","*.java","*.xml","*.properties","*.py")

foreach ($pattern in $contentPatterns) {
    # Get tracked files matching the source extensions
    foreach ($ext in $sourceExtensions) {
        $r = Invoke-External -Arguments @("git","grep","--cached","-il","-e",$pattern,"--",$ext) -CaptureOutput
        $matched = ($r.Output | Out-String).Trim()
        if ($r.ExitCode -eq 0 -and $matched) {
            $lineResult = Invoke-External -Arguments @("git","grep","--cached","-in","-e",$pattern,"--",$ext) -CaptureOutput
            $lines = ($lineResult.Output | Out-String).Trim()
            foreach ($l in ($lines -split "`n")) {
                if ($l.Trim()) {
                    # Extract only file:line -- never the matched value
                    $parts = $l -split ":", 3
                    if ($parts.Count -ge 2) {
                        $ref = "$($parts[0]):$($parts[1])"
                        $secretFailures.Add("Secret pattern '$pattern' at $ref")
                    }
                }
            }
        }
    }
}

# Additional token patterns (scan all tracked files -- these are always secrets if present)
$tokenPatterns = @("ghp_","github_pat_","AKIA[0-9A-Z]{16}")
foreach ($pattern in $tokenPatterns) {
    # Scan source files only (exclude CI YAML to avoid matching grep pattern strings)
    foreach ($ext in $sourceExtensions) {
        $r = Invoke-External -Arguments @("git","grep","--cached","-iEl","-e",$pattern,"--",$ext) -CaptureOutput
        $matched = ($r.Output | Out-String).Trim()
        if ($r.ExitCode -eq 0 -and $matched) {
            $lineResult = Invoke-External -Arguments @("git","grep","--cached","-iEn","-e",$pattern,"--",$ext) -CaptureOutput
            $lines = ($lineResult.Output | Out-String).Trim()
            foreach ($l in ($lines -split "`n")) {
                if ($l.Trim()) {
                    $parts = $l -split ":", 3
                    if ($parts.Count -ge 2) {
                        $ref = "$($parts[0]):$($parts[1])"
                        $secretFailures.Add("Token pattern '$pattern' at $ref")
                    }
                }
            }
        }
    }
}

# password= patterns in source (NOT in .env.example or YAML)
$pwdPatterns = @('password="',"password='","password=`$")
foreach ($pattern in $pwdPatterns) {
    foreach ($ext in @("*.kt","*.java","*.py")) {
        $r = Invoke-External -Arguments @("git","grep","--cached","-il","-e",$pattern,"--",$ext) -CaptureOutput
        $matched = ($r.Output | Out-String).Trim()
        if ($r.ExitCode -eq 0 -and $matched) {
            $lineResult = Invoke-External -Arguments @("git","grep","--cached","-in","-e",$pattern,"--",$ext) -CaptureOutput
            $lines = ($lineResult.Output | Out-String).Trim()
            foreach ($l in ($lines -split "`n")) {
                if ($l.Trim()) {
                    $parts = $l -split ":", 3
                    if ($parts.Count -ge 2) {
                        $ref = "$($parts[0]):$($parts[1])"
                        $secretFailures.Add("Password pattern '$pattern' at $ref")
                    }
                }
            }
        }
    }
}

if ($secretFailures.Count -gt 0) {
    Write-CheckResult "secret-scan" "FAIL" "Found $($secretFailures.Count) potential secret(s):"
    foreach ($f in $secretFailures) { Write-Info "  $f" }
} else {
    Write-CheckResult "secret-scan" "PASS" "No secrets or sensitive files found in tracked source files"
}

# 4.3 -- Medical data patterns (WARN only -- allowed in test fixtures)
$medicalPatterns = @("PATIENT_NAME=","DOCTOR_NAME=")
$medicalWarnings = [System.Collections.Generic.List[string]]::new()
foreach ($pattern in $medicalPatterns) {
    $r = Invoke-External -Arguments @("git","grep","--cached","-il","-e",$pattern) -CaptureOutput
    $matched = ($r.Output | Out-String).Trim()
    if ($r.ExitCode -eq 0 -and $matched) {
        foreach ($f in ($matched -split "`n")) {
            $fTrim = $f.Trim()
            if ($fTrim -and -not ($fTrim -match "test[/\\]fixtures" -or $fTrim -match "src[/\\]test[/\\]fixtures")) {
                $medicalWarnings.Add("Medical data pattern '$pattern' in non-fixture file: $fTrim")
            }
        }
    }
}
if ($medicalWarnings.Count -gt 0) {
    Write-CheckResult "medical-data-scan" "WARN" "Medical data patterns found outside test fixtures:"
    foreach ($w in $medicalWarnings) { Write-Info "  $w" }
} else {
    Write-CheckResult "medical-data-scan" "PASS"
}

# ===========================================================================
# SECTION 5 -- Dependency vulnerability scan
# ===========================================================================
Write-Section "Section 5 -- Dependency Vulnerability Scan"

if (-not (Test-Path $REQUIREMENTS_FILE)) {
    Write-CheckResult "pip-audit" "SKIP" "backend not yet scaffolded"
    Write-CheckResult "safety"    "SKIP" "backend not yet scaffolded"
} else {
    $pipAuditCheck = Invoke-External -Arguments @($PYTHON,"-m","pip_audit","--version") -CaptureOutput
    if ($pipAuditCheck.ExitCode -ne 0) {
        Write-CheckResult "pip-audit" "SKIP" "pip-audit not installed -- consider: pip install pip-audit"
    } else {
        Write-Info "Running pip-audit ..."
        Set-Location $BACKEND_DIR
        & $PYTHON "-m" "pip_audit"
        $exitCode = $LASTEXITCODE
        Set-Location $REPO_ROOT
        if ($exitCode -eq 0) { Write-CheckResult "pip-audit" "PASS" }
        else { Write-CheckResult "pip-audit" "WARN" "pip-audit found vulnerabilities -- review before production (not a CI gate yet)" }
    }

    $safetyCheck = Invoke-External -Arguments @($PYTHON,"-m","safety","--version") -CaptureOutput
    if ($safetyCheck.ExitCode -ne 0) {
        Write-CheckResult "safety" "SKIP" "safety not installed -- consider: pip install safety"
    } else {
        Write-Info "Running safety check ..."
        Set-Location $BACKEND_DIR
        & $PYTHON "-m" "safety" "check"
        $exitCode = $LASTEXITCODE
        Set-Location $REPO_ROOT
        if ($exitCode -eq 0) { Write-CheckResult "safety" "PASS" }
        else { Write-CheckResult "safety" "WARN" "safety found vulnerabilities -- review before production (not a CI gate yet)" }
    }
}

Write-CheckResult "android-vuln-scan" "SKIP" "No Android/Gradle vulnerability scanning configured yet (CI gap -- see docs/deployment/cicd.md)"

# ===========================================================================
# SECTION 6 -- CI config validation
# ===========================================================================
Write-Section "Section 6 -- CI Config Validation"

$requiredWorkflows = @(
    ".github\workflows\android-ci.yml",
    ".github\workflows\backend-ci.yml",
    ".github\workflows\pr-checks.yml"
)

foreach ($wf in $requiredWorkflows) {
    $wfPath = Join-Path $REPO_ROOT $wf
    $safeName = $wf -replace '[^a-zA-Z0-9]','-'
    if (Test-Path $wfPath) {
        Write-CheckResult "ci-workflow-$safeName" "PASS" "$wf exists"
    } else {
        Write-CheckResult "ci-workflow-$safeName" "FAIL" "$wf is MISSING -- CI will not run"
    }
}

if (-not (Get-Command "yamllint" -ErrorAction SilentlyContinue)) {
    Write-CheckResult "yamllint" "SKIP" "yamllint not installed -- consider: pip install yamllint"
} else {
    foreach ($wf in $requiredWorkflows) {
        $wfPath = Join-Path $REPO_ROOT $wf
        $safeName = $wf -replace '[^a-zA-Z0-9]','-'
        if (Test-Path $wfPath) {
            Write-Info "Running yamllint on $wf ..."
            $r = Invoke-External -Arguments @("yamllint",$wfPath) -CaptureOutput
            if ($r.ExitCode -eq 0) {
                Write-CheckResult "yamllint-$safeName" "PASS"
            } else {
                Write-CheckResult "yamllint-$safeName" "WARN" "yamllint warnings in $wf (not a blocking failure)"
                Write-Info ($r.Output | Out-String).Trim()
            }
        }
    }
}

# ===========================================================================
# SUMMARY
# ===========================================================================
Write-Host ""
Write-Host ("=" * 70) -ForegroundColor White
Write-Host "  SUMMARY" -ForegroundColor White
Write-Host ("=" * 70) -ForegroundColor White
Write-Host ""
Write-Host ("  PASSED  : {0,4}" -f $script:PASS) -ForegroundColor Green
$fc = if ($script:FAIL -gt 0) { "Red" } else { "Green" }
Write-Host ("  FAILED  : {0,4}" -f $script:FAIL) -ForegroundColor $fc
$wc = if ($script:WARN -gt 0) { "Yellow" } else { "White" }
Write-Host ("  WARNED  : {0,4}" -f $script:WARN) -ForegroundColor $wc
Write-Host ("  SKIPPED : {0,4}" -f $script:SKIP) -ForegroundColor DarkGray
Write-Host ""

if ($script:FAILED_CHECKS.Count -gt 0) {
    Write-Host "  Failed checks:" -ForegroundColor Red
    foreach ($fc2 in $script:FAILED_CHECKS) {
        Write-Host "    - $fc2" -ForegroundColor Red
    }
    Write-Host ""
}

if ($script:FAIL -gt 0) {
    Write-Host "  RESULT: QUALITY GATE FAILED -- fix the issues above before pushing." -ForegroundColor Red
    Write-Host "  To skip this hook (not recommended): git push --no-verify" -ForegroundColor Yellow
    Write-Host ""
    exit 1
} else {
    Write-Host "  RESULT: QUALITY GATE PASSED -- safe to push." -ForegroundColor Green
    Write-Host ""
    exit 0
}