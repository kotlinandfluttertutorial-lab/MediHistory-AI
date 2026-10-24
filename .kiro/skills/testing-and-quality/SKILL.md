# Skill: Testing and Quality for MediHistory AI

## Purpose

Guide complete, safe test coverage for MediHistory AI. Covers synthetic data rules, required OCR uncertainty test cases, duplicate detection edge cases, accessibility test requirements, security test patterns, and CI gate requirements.

## When to Apply This Skill

- Writing any unit, integration, or UI test
- Adding synthetic test fixtures
- Setting up a new test class or module
- Configuring CI pipeline checks
- Reviewing test coverage against thresholds

## Steering Files to Read First

- `.kiro/steering/testing.md`
- `.kiro/steering/medical-safety.md`

---

## Step-by-Step Guidance

### 1. Use Synthetic Data Only — No Real Patient Data

Every test and demo uses entirely synthetic data. No real patient names, real medicine brand names, or real doctor names appear in any test file or fixture.

Approved synthetic examples:
- Patients: "Test Patient Alpha", "Synthetic User Beta"
- Doctors: "Dr. Synthetic", "Dr. Test Cardiologist"
- Medicines: "Synthamol 500mg", "Testocillin 250mg"

Fixtures live in `test/fixtures/` (backend) or `android/app/src/test/fixtures/` (Android). The minimum fixture set is: 3 synthetic patients, 5 doctors, 10 prescriptions, 20 medicines.

### 2. Test OCR Uncertainty Exhaustively

Every OCR confidence boundary and every review state transition must have a test. See the required test cases below.

### 3. Test Duplicate Detection Edge Cases Exhaustively

Every combination of ingredient overlap — including synonyms, combination products, and the unreviewed-exclusion rule — must have a test. See the required edge cases below.

### 4. Enable AccessibilityChecks in Every Compose UI Test

Call `AccessibilityChecks.enable()` in the base class of all Compose UI tests. This runs accessibility checks automatically on every render. Failures are treated as test failures.

### 5. Enforce Security Tests in CI

Security tests are not optional extras — they run on every PR:
- Grep for API key / secret patterns (fails if found).
- Grep for tokens in Logcat output (fails if found).
- Cross-patient access test: authenticated as Patient A, request Patient B's resource → assert 404.
- Unauthenticated request → assert 401.
- Verify app-private file is not accessible to other apps.

### 6. Follow Test Naming Conventions

- Kotlin: `given_[state]_when_[action]_then_[outcome]` or `should_[outcome]_when_[condition]`
- Python: `test_[outcome]_[condition]` or `test_[feature]_[scenario]`

### 7. Enforce Coverage Thresholds

| Layer | Threshold |
|---|---|
| Android data layer | ≥ 80% line coverage |
| `DuplicateDetector` | 100% branch coverage |
| `ValidationUtils` | 100% branch coverage |
| Backend API layer | ≥ 90% line coverage |

Coverage below threshold fails the CI pipeline.

---

## Required OCR Uncertainty Test Cases

All of these must exist:

| Test | Expected behaviour |
|---|---|
| Field confidence below threshold | Field flagged for review, shown to patient |
| Field confidence exactly at threshold | Field shown without flag |
| Field confidence above threshold | Field shown without flag |
| Patient edits field | `patientValue` updated; `rawValue` unchanged |
| Patient rejects field | `reviewState = REJECTED`; `rawValue` unchanged |
| OCR worker failure | `OcrStatus = ERROR`; plain-language error shown to patient |
| `PENDING_REVIEW → REVIEWED` | Valid transition accepted |
| `PENDING_REVIEW → REJECTED` | Valid transition accepted |
| `REVIEWED → PENDING_REVIEW` | Valid transition accepted |
| `REJECTED → PENDING_REVIEW` | Valid transition accepted |
| `REVIEWED → REJECTED` (invalid) | Transition rejected with error |

## Required Duplicate Detection Edge Cases

All of these must exist:

| Test | Expected behaviour |
|---|---|
| Same ingredient, different brand names | One `DuplicatePair` produced |
| Different ingredients, similar brand names | No `DuplicatePair` produced |
| paracetamol vs acetaminophen (synonym) | Treated as same ingredient → one `DuplicatePair` |
| Combination A+B vs single ingredient A | Partial match; reduced confidence; `combinationNote` populated |
| Combination A+B vs combination A+B | Full match; standard confidence |
| Unreviewed (`PENDING_REVIEW`) record | Excluded from detection input |
| Determinism check | Same input list → same output on repeated calls |
| Self-match (same prescription) | No `DuplicatePair` produced |

## Accessibility Test Requirements

All of these must be covered:

- `AccessibilityChecks.enable()` called in the Compose UI test base class.
- All interactive elements have a minimum 48×48 dp touch target.
- `StatusBadge`: meaning conveyed by text, not colour alone; passes `AccessibilityChecks`.
- All image composables: non-empty `contentDescription`, or explicitly marked as decorative (`contentDescription = null` with `Role` semantics).
- TalkBack announces all bottom navigation items correctly (label + selected state).

## Security Test Patterns

```
// Cross-patient access — must return 404
authenticateAsPatientA()
response = GET /prescriptions/{patientB_prescription_id}
assert response.status == 404

// Unauthenticated request — must return 401
response = GET /prescriptions/{id}   // no Authorization header
assert response.status == 401

// No token in Logcat
launchApp()
logLines = captureLogcat()
assert logLines.none { it.contains(sessionToken) }

// No hardcoded secrets in source (CI grep)
grep -r "AIza\|sk-\|Bearer \|password\s*=" android/ backend/ → must produce zero matches
```

---

## Patterns to Follow

- All test fixtures use synthetic names: "Test Patient Alpha", "Dr. Synthetic", "Synthamol 500mg".
- No real patient names, real medicine brand names, or real doctor names in any test file.
- Fixtures live in `test/fixtures/` or `android/app/src/test/fixtures/`.
- Minimum fixture set: 3 synthetic patients, 5 doctors, 10 prescriptions, 20 medicines.
- `AccessibilityChecks.enable()` in every Compose UI test base class.
- Use `HiltAndroidRule` for dependency injection in UI tests.
- Use `TestWorkerBuilder` for WorkManager worker tests.
- Use `FakeOcrService` and `FakeHistoryGenerationService` — no real network calls in tests.
- Use in-memory Room database and in-memory SQLite for integration tests.
- Security grep runs on every PR in CI.

## Patterns to Avoid

- Real patient names, real medicine brand names, or real doctor names in any fixture or test.
- Real network or cloud API calls in any test.
- Skipping `AccessibilityChecks.enable()` in Compose UI tests.
- Testing duplicate detection with real medicine brand names.
- Writing security tests that only run manually (they must be in CI).
- Coverage thresholds as aspirational targets — they are CI gates.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```kotlin
// ── Synthetic fixture constants ────────────────────────────────────────────────
object SyntheticFixtures {
    val PATIENT_ALPHA  = Patient(id = "test-patient-alpha",  name = "Test Patient Alpha")
    val PATIENT_BETA   = Patient(id = "test-patient-beta",   name = "Synthetic User Beta")
    val DOCTOR_SYNTH   = Doctor(id = "test-doctor-synth",    name = "Dr. Synthetic")

    fun prescription(patient: Patient = PATIENT_ALPHA) = Prescription(
        id = "test-rx-001",
        patientId = patient.id,
        doctorId = DOCTOR_SYNTH.id
    )

    fun medicine(name: String = "Synthamol 500mg") = PrescriptionMedicine(
        id = "test-med-001",
        name = name   // always synthetic — never a real brand name
    )
}


// ── OCR uncertainty tests ──────────────────────────────────────────────────────
class OcrConfidenceTest {

    @Test
    fun `should flag field when confidence is below threshold`() {
        val field = ExtractedField(FieldType.DOSE, rawValue = "1 tablet", confidence = 0.49f)
        val result = OcrConfidenceEvaluator.evaluate(field, threshold = 0.50f)
        assertThat(result.isFlagged).isTrue()
    }

    @Test
    fun `should not modify rawValue when patient edits field`() {
        val field = ExtractedField(FieldType.DOSE, rawValue = "1 tablet", confidence = 0.92f)
        val updated = field.applyPatientEdit(patientValue = "2 tablets")
        assertThat(updated.rawValue).isEqualTo("1 tablet")    // rawValue unchanged
        assertThat(updated.patientValue).isEqualTo("2 tablets")
    }

    @Test
    fun `should reject invalid review state transition`() {
        val valid = ValidationUtils.validateReviewTransition(
            from = ReviewState.REVIEWED, to = ReviewState.REJECTED
        )
        assertThat(valid).isFalse()   // REVIEWED → REJECTED is not a valid transition
    }
}


// ── Duplicate detection edge case tests ───────────────────────────────────────
class DuplicateDetectorTest {

    @Test
    fun `should detect paracetamol and acetaminophen as same ingredient`() {
        val med1 = reviewedMedicine("Synthamol 500mg", normalisedIngredients = listOf("acetaminophen"))
        val med2 = reviewedMedicine("Parafake 500mg",  normalisedIngredients = listOf("acetaminophen"))
        val pairs = DuplicateDetector.detect(listOf(med1, med2))
        assertThat(pairs).hasSize(1)
    }

    @Test
    fun `should not produce pair for different ingredients with similar brand names`() {
        val med1 = reviewedMedicine("Synthamol 500mg",  normalisedIngredients = listOf("acetaminophen"))
        val med2 = reviewedMedicine("Synthamol-X 500mg", normalisedIngredients = listOf("testocillin"))
        val pairs = DuplicateDetector.detect(listOf(med1, med2))
        assertThat(pairs).isEmpty()
    }

    @Test
    fun `should produce same output on repeated calls with same input`() {
        val medicines = listOf(
            reviewedMedicine("Synthamol 500mg",  normalisedIngredients = listOf("acetaminophen")),
            reviewedMedicine("Parafake 250mg",   normalisedIngredients = listOf("acetaminophen")),
        )
        val first  = DuplicateDetector.detect(medicines)
        val second = DuplicateDetector.detect(medicines)
        assertThat(first).isEqualTo(second)   // determinism
    }

    @Test
    fun `should exclude unreviewed records from detection`() {
        val reviewed   = reviewedMedicine("Synthamol 500mg", normalisedIngredients = listOf("acetaminophen"))
        val unreviewed = pendingMedicine("Parafake 250mg",   normalisedIngredients = listOf("acetaminophen"))
        // Only pass REVIEWED records to the detector
        val pairs = DuplicateDetector.detect(listOf(reviewed))   // unreviewed excluded from input
        assertThat(pairs).isEmpty()
    }
}


// ── Accessibility test base class ─────────────────────────────────────────────
@HiltAndroidTest
abstract class BaseComposeUiTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true)
    }
}
```

```python
# ── Backend cross-patient security test ───────────────────────────────────────
def test_patient_cannot_access_other_patients_prescription(client, patient_a_headers, patient_b_prescription):
    response = client.get(
        f"/prescriptions/{patient_b_prescription.id}",
        headers=patient_a_headers,
    )
    assert response.status_code == 404  # 404, never 403


def test_unauthenticated_request_returns_401(client, patient_b_prescription):
    response = client.get(f"/prescriptions/{patient_b_prescription.id}")
    assert response.status_code == 401
```

---

## CI Gate Requirements

Every PR to `main` must pass all of the following:

| Check | Threshold / Requirement |
|---|---|
| Android lint | Zero errors |
| Android unit tests | All pass |
| Android integration tests | All pass |
| Android UI/emulator tests | All pass (may be parallelised) |
| Backend unit tests | All pass |
| Backend integration tests | All pass |
| Android data layer coverage | ≥ 80% line coverage |
| `DuplicateDetector` coverage | 100% branch coverage |
| `ValidationUtils` coverage | 100% branch coverage |
| Backend API layer coverage | ≥ 90% line coverage |
| Security grep (no hardcoded secrets) | Zero matches |

Coverage below threshold and security grep matches both fail the pipeline.
