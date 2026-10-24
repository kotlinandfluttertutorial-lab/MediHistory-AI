# MediHistory AI — Testing Steering

## Test Strategy

### Unit Tests
- Test all ViewModel state transitions (initial, loading, success, error)
- Test all Repository implementations with in-memory Room databases
- Test all UseCase business logic with fake repositories
- Test all ValidationUtils functions with boundary values
- Test DuplicateDetector with synthetic medicine fixtures
- Test OCR confidence scoring and review state transitions
- Target: ≥80% line coverage for the data layer; 100% branch coverage for DuplicateDetector and ValidationUtils

### Integration Tests
- Test complete data flows: create prescription → OCR → review → generate alert
- Test Room DAOs with real in-memory databases
- Test WorkManager workers with TestWorkerBuilder
- Test FastAPI endpoints with TestClient and in-memory SQLite
- Target: ≥90% line coverage for the backend API layer

### UI Tests (Compose/Espresso)
- Test critical user flows: add prescription, OCR review, mark duplicate, generate history
- Use HiltAndroidRule for dependency injection in UI tests
- Enable AccessibilityChecks in all Compose UI tests
- Use mock CameraX output for camera tests (no real camera required)

### Security Tests
- Verify no tokens or document content in Logcat
- Verify cross-patient access returns 404
- Verify unauthenticated requests return 401
- Verify app-private storage files are not accessible to other apps
- Verify no hardcoded secrets in source files (grep in CI)

## Synthetic Data Only
All tests and demos must use synthetic data:
- Fake patient names (e.g. "Test Patient Alpha", "Synthetic User Beta")
- Fake doctor names (e.g. "Dr. Synthetic", "Dr. Test Cardiologist")
- Fake medicine names that do not correspond to real brands (e.g. "Synthamol 500mg", "Testocillin 250mg")
- No real Indian medicine brand names or real patient names in any test fixture
- Synthetic fixtures live in `test/fixtures/` or `android/app/src/test/fixtures/`

## Test Naming Conventions
- Kotlin tests: `given_[state]_when_[action]_then_[outcome]` or `should_[outcome]_when_[condition]`
- Python tests: `test_[outcome]_[condition]` or `test_[feature]_[scenario]`

## CI Gate
- Every PR to main must pass: lint, unit tests, and integration tests
- Coverage below threshold fails the pipeline
- Security grep (no hardcoded secrets) runs on every PR
- UI/emulator tests run on every PR to main (may be parallelised)

## OCR Uncertainty Tests
Must test: field with confidence below threshold, field with confidence exactly at threshold, field with confidence above threshold, all review state transitions, edit that stores patientValue without changing rawValue, reject that does not delete the field.

## Duplicate Detection Edge Cases
Must test: same ingredient different brands, different ingredients similar names, combination product vs single ingredient (partial match), combination product vs same combination product (full match), paracetamol/acetaminophen synonym, unreviewed records excluded, determinism (same input → same output).

## Offline / Sync Conflict Tests
Must test: create/edit patient and doctor in airplane mode, network-dependent feature shows offline indicator, no crash when toggling airplane mode during save.

## Accessibility Tests
Must test: all interactive elements have ≥48×48 dp touch target, all StatusBadge instances pass AccessibilityChecks, all image composables have contentDescription or are marked decorative, TalkBack announces all bottom navigation items correctly.
