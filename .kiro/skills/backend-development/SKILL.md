# Skill: Backend Development for MediHistory AI

## Purpose

Guide consistent, secure backend development for MediHistory AI. Covers FastAPI, Pydantic typed schemas, SQLAlchemy ORM, Alembic migrations, JWT authentication middleware, patient-scoped authorisation, rate limiting, and pytest.

## When to Apply This Skill

- Adding or modifying any FastAPI route or endpoint
- Defining request/response schemas
- Writing or updating database models or queries
- Adding authentication or authorisation logic
- Creating or modifying Alembic migrations
- Writing backend tests

## Steering Files to Read First

- `.kiro/steering/tech.md`
- `.kiro/steering/medical-safety.md`

---

## Step-by-Step Guidance

### 1. Define Typed Schemas First

Every endpoint has a dedicated Pydantic `BaseModel` for its request body and its response body. Never use raw `dict` or untyped `Any` in endpoint signatures.

### 2. Apply Authentication on Every Protected Endpoint

Every endpoint except `/auth/login` and `/health` uses the `get_current_patient` dependency. There are no opt-in or conditional auth checks; protection is structural.

### 3. Scope Every Query to the Current Patient

Every database query that touches patient data includes a `patient_id == current_patient.id` filter. This is the primary authorisation control. Cross-patient access returns HTTP 404, never 403 — returning 403 leaks that the resource exists.

### 4. Apply Rate Limiting via Middleware

Rate limiting is applied at the middleware level to all routes, both public and authenticated. It is not applied per-endpoint as an afterthought.

### 5. Use SQLAlchemy ORM for All Queries

No raw SQL strings. All queries use SQLAlchemy ORM constructs. This ensures query composition, parameterisation, and type safety.

### 6. All Schema Changes Through Alembic

Every database schema change — adding a column, changing a type, dropping a table — is represented by an Alembic migration. No manual SQL migrations or ad-hoc `CREATE TABLE` calls.

### 7. Secrets via Environment Variables Only

Database connection strings, JWT secrets, OCR API keys, and all other secrets are read from environment variables at runtime. They are never hardcoded, never in committed config files, and never embedded in the APK.

### 8. Mock External Services in All Tests

OcrService and HistoryGenerationService are injected as dependencies. In tests, inject fakes/mocks. No real network calls or cloud API calls in the test suite. Use an in-memory SQLite database for test sessions.

---

## Patterns to Follow

- Every endpoint has a typed request schema and a typed response schema (Pydantic `BaseModel`).
- Every protected endpoint uses the `get_current_patient` dependency.
- Patient-scoped queries always filter by `patient_id == current_patient.id`.
- Cross-patient access returns HTTP 404 (not 403).
- Rate limiting applied via middleware to all routes.
- All schema changes go through an Alembic migration.
- Tests use `TestClient` with an in-memory SQLite test database.
- Mock `OcrService` and `HistoryGenerationService` in all tests.
- All endpoints documented with OpenAPI annotations (`summary`, `description`, `response_model`).
- Service accounts and DB roles have minimum required permissions (least privilege).

## Patterns to Avoid

- Returning 403 for cross-patient access (leaks resource existence — always use 404).
- Raw SQL strings in queries (use SQLAlchemy ORM).
- Hardcoded database connection strings (use environment variables).
- Logging request bodies, tokens, or prescription/medicine content.
- Secrets in source code, config files, or committed `.env` files.
- Using `Any` or untyped `dict` in request/response schemas.
- Per-endpoint rate limiting instead of middleware-level rate limiting.
- Real network or cloud API calls in any test.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```python
# ── Pydantic schemas ──────────────────────────────────────────────────────────
class PrescriptionResponse(BaseModel):
    id: str
    patient_id: str
    issued_date: date | None
    doctor_name: str | None
    ocr_status: OcrStatus
    source_document_ref: str    # reference only, never the content itself

    class Config:
        from_attributes = True


class PrescriptionCreateRequest(BaseModel):
    issued_date: date | None = None
    doctor_name: str | None = None
    notes: str | None = None


# ── Auth dependency ───────────────────────────────────────────────────────────
async def get_current_patient(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db)
) -> Patient:
    # validate JWT, look up patient — raises 401 if invalid
    ...


# ── Patient-scoped endpoint ───────────────────────────────────────────────────
@router.get(
    "/prescriptions/{prescription_id}",
    response_model=PrescriptionResponse,
    summary="Get a single prescription",
)
async def get_prescription(
    prescription_id: str,
    current_patient: Patient = Depends(get_current_patient),
    db: Session = Depends(get_db),
):
    prescription = (
        db.query(Prescription)
        .filter(
            Prescription.id == prescription_id,
            Prescription.patient_id == current_patient.id,  # patient-scoped
        )
        .first()
    )
    if not prescription:
        raise HTTPException(status_code=404)   # 404, never 403
    return prescription


# ── Rate limiting (middleware, not per-endpoint) ──────────────────────────────
app.add_middleware(RateLimitMiddleware, requests_per_minute=60)


# ── No-logging rule in endpoints ─────────────────────────────────────────────
# CORRECT — log anonymised metadata only
logger.info("prescription.fetched", extra={"prescription_id": prescription_id, "status": 200})

# WRONG — never log content
# logger.info(f"prescription content: {prescription.raw_text}")  # ← never do this


# ── Test pattern ──────────────────────────────────────────────────────────────
@pytest.fixture
def client(db_session):
    # Override get_db and OcrService with test doubles
    app.dependency_overrides[get_db] = lambda: db_session
    app.dependency_overrides[get_ocr_service] = lambda: FakeOcrService()
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


def test_cannot_access_other_patient_prescription(client, auth_headers_patient_a, prescription_patient_b):
    # Patient A attempts to read Patient B's prescription → must be 404
    response = client.get(
        f"/prescriptions/{prescription_patient_b.id}",
        headers=auth_headers_patient_a,
    )
    assert response.status_code == 404


# ── Alembic migration (always, never manual SQL) ──────────────────────────────
# $ alembic revision --autogenerate -m "add ocr_status to prescriptions"
# Review the generated file, then:
# $ alembic upgrade head
```

---

## Key Constraints (from Steering)

- Medical records are highly sensitive: never log content, names, or tokens.
- Patient-scoped authorisation is required on every object — it is not optional.
- Cross-patient access must return 404, not 403.
- All secrets are server-side environment variables; nothing sensitive goes in the APK or source tree.
- Use synthetic data in all tests; never use real patient data.
- Obtain qualified legal/security review of applicable India requirements before launch.
