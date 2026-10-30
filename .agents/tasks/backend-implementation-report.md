# MediHistory AI — Backend Implementation Plan

> **Note:** This plan has been decomposed into FEAT artifacts under
> `.agents/tasks/backend-implementation-report/`. The workflow tail has been
> replaced with per-FEAT coder steps + a convergence loop. This document
> remains the human-readable reference.

---

## Current State

The backend at `backend/` has exactly:
- `app/main.py` — FastAPI app, `RateLimitMiddleware`, `auth` router only
- `app/routers/auth.py` — `POST /auth/login` (working)
- `app/core/security.py` — `get_current_patient` JWT dependency (reads `os.environ["JWT_SECRET_KEY"]` at import)
- `app/middleware/patient_auth.py` — `require_patient_owns_resource()` helper (raises HTTP 404)
- `app/services/auth_service.py` — stub AuthService, synthetic account only
- `app/schemas/auth.py` — `LoginRequest`, `TokenResponse`
- `app/middleware/rate_limit.py` — `RateLimitMiddleware`
- `tests/test_auth.py`, `tests/test_security.py` — existing tests (sync `TestClient`)
- No database, no models, no Alembic

---

## Design Decisions

### 1. Refresh token strategy: DB-stored (not JWT-only)

**Decision:** Store refresh tokens in a `refresh_tokens` table as a bcrypt hash of the token string.

**Rationale:** JWTs cannot be revoked before expiry. A health app requires real logout (POST /auth/logout must actually invalidate the session). DB storage adds one query on refresh/logout but makes revocation reliable. The refresh token itself is still a signed JWT (makes expiry self-contained) — the DB row is the revocation ledger.

### 2. SQLite via SQLAlchemy async (aiosqlite)

**Decision:** `sqlite+aiosqlite://` with `AsyncSession`.

**Rationale:** Matches the task spec, zero ops overhead for MVP, easy in-memory override for tests (`sqlite+aiosqlite:///:memory:`). Upgrade path to PostgreSQL is a `DATABASE_URL` change only.

### 3. All UUIDs stored as `String(36)` in SQLite

**Decision:** `str(uuid.uuid4())` in Python, stored as `VARCHAR(36)`.

**Rationale:** SQLite has no native UUID type. String(36) is universally readable and portable to Postgres (`uuid` type) via Alembic migration.

### 4. Cascade deletes via SQLAlchemy relationships

- `Prescription` → `PrescriptionMedicine`: `cascade='all, delete-orphan'`
- `PrescriptionMedicine` → `DuplicateAlert` (via `medicine_a_id`/`medicine_b_id`): FK `ON DELETE SET NULL` (alerts persist but lose the medicine reference — prevents orphan alert confusion)

**Rationale:** Deleting a prescription removes all its medicines automatically. Alerts become detached (SET NULL) rather than silently deleted, so the patient can still see they were alerted even if the source prescription is gone.

### 5. Active ingredient slug for medicine grouping

**Decision:** Add `active_ingredient_slug` column (lowercase, hyphenated) to `PrescriptionMedicine` and use it as the grouping key for `GET /patients/me/medicines`.

**Rationale:** SQLite lacks `GROUP BY` with aggregates over related tables in async SQLAlchemy without raw SQL. Python-side grouping after fetching all medicines for a patient is simpler and correct for MVP data sizes.

### 6. No prefix on `router.prefix` — apply `/api/v1` in `main.py`

**Decision:** All routers use `app.include_router(router, prefix='/api/v1')` in `main.py`.

**Rationale:** Central version prefix keeps router files clean and makes version migration a single-line change.

### 7. Error shape: `{"error": {"code": "...", "message": "...", "details": {}}}` via `raise_http_error()`

**Decision:** All HTTPException raises go through `app.core.errors.raise_http_error()`.

**Rationale:** Consistent error shape across all routers. The helper is the single place to change if the shape evolves.

### 8. Tests: AsyncClient + in-memory SQLite via conftest.py

**Decision:** `conftest.py` overrides `get_db` dependency with an in-memory async session. All test modules use `httpx.AsyncClient`. Switch existing `test_auth.py` and `test_security.py` to async gradually (they currently use sync `TestClient` — keep them as-is to avoid breaking changes; new tests are all async).

---

## File Creation Order (dependency-first)

```
requirements.txt              [FEAT-001] — new deps needed before anything runs
app/core/config.py            [FEAT-001] — settings used by database.py and services
app/core/database.py          [FEAT-001] — Base + get_db used by all models + routers
app/core/errors.py            [FEAT-001] — raise_http_error used by all routers
app/models/__init__.py        [FEAT-001] — Alembic needs all models imported
app/models/patient.py         [FEAT-001]
app/models/refresh_token.py   [FEAT-001]
app/models/doctor.py          [FEAT-001]
app/models/prescription.py    [FEAT-001]
app/models/medicine.py        [FEAT-001]
app/models/duplicate_alert.py [FEAT-001]
app/models/history.py         [FEAT-001]
app/models/reminder.py        [FEAT-001]
migrations/                   [FEAT-001] — alembic init + env.py + initial migration

app/schemas/common.py         [FEAT-002]
app/schemas/auth.py (extend)  [FEAT-002]
app/schemas/patient.py        [FEAT-002]
app/schemas/doctor.py         [FEAT-002]
app/services/auth_service.py  [FEAT-002] — extend for register/refresh/logout
app/services/patient_service.py [FEAT-002]
app/services/doctor_service.py  [FEAT-002]
app/routers/auth.py (extend)  [FEAT-002]
app/routers/patients.py       [FEAT-002]
app/routers/doctors.py        [FEAT-002]
tests/conftest.py             [FEAT-002] — used by all test modules
tests/test_patients.py        [FEAT-002]
tests/test_doctors.py         [FEAT-002]

app/schemas/prescription.py   [FEAT-003]
app/schemas/medicine.py       [FEAT-003]
app/services/prescription_service.py [FEAT-003]
app/services/medicine_service.py     [FEAT-003]
app/routers/prescriptions.py  [FEAT-003]
app/routers/medicines.py      [FEAT-003]
tests/test_prescriptions.py   [FEAT-003]
tests/test_medicines.py       [FEAT-003]

app/schemas/duplicate_alert.py [FEAT-004]
app/schemas/history.py         [FEAT-004]
app/schemas/reminder.py        [FEAT-004]
app/services/duplicate_service.py [FEAT-004]
app/services/history_service.py   [FEAT-004]
app/services/reminder_service.py  [FEAT-004]
app/routers/duplicates.py     [FEAT-004]
app/routers/history.py        [FEAT-004]
app/routers/reminders.py      [FEAT-004]
app/routers/interactions.py   [FEAT-004]
app/routers/export.py         [FEAT-004]
tests/test_duplicates.py      [FEAT-004]
tests/test_history.py         [FEAT-004]
tests/test_reminders.py       [FEAT-004]

tests/test_auth.py (extend)   [FEAT-005] — register/refresh/logout tests
(full suite run)              [FEAT-005]
```

---

## SQLAlchemy Models Summary

### Patient
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | uuid4 default |
| email | String(254) unique | indexed |
| password_hash | String(256) | bcrypt |
| display_name | String(100) | |
| created_at | DateTime | UTC default |
| updated_at | DateTime | UTC default+onupdate |

### Doctor
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| name | String(200) | |
| specialty | String(100) nullable | |
| specialty_custom | String(200) nullable | |
| clinic_name | String(200) nullable | |
| contact_notes | Text nullable | |
| is_deleted | Boolean | soft-delete flag |
| created_at, updated_at | DateTime | |

Index on (patient_id, is_deleted).

### Prescription
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| doctor_id | String(36) FK→doctors nullable | SET NULL on doctor delete |
| prescription_date | Date nullable | |
| ocr_status | String(20) | PENDING/PROCESSING/REVIEWED |
| source_document_path | String(512) nullable | app-private path |
| patient_notes | Text nullable | |
| created_at, updated_at | DateTime | |

Relationship to PrescriptionMedicine: cascade='all, delete-orphan'.
Index on (patient_id, ocr_status).

### PrescriptionMedicine
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| prescription_id | String(36) FK→prescriptions CASCADE | |
| patient_id | String(36) FK→patients | denormalised for query speed |
| brand_name | String(200) nullable | |
| active_ingredient | String(200) nullable | |
| active_ingredient_slug | String(200) nullable | lowercase hyphenated |
| strength, formulation, dosage, frequency, route, duration | various nullable | |
| review_state | String(20) | PENDING_REVIEW/REVIEWED/REJECTED |
| medicine_status | String(20) | CURRENT/PAST/UNKNOWN |
| confidence_score | Float nullable | 0.0–1.0 |
| raw_value, source_reference | Text/String nullable | OCR provenance |
| created_at, updated_at | DateTime | |

Index on (patient_id, active_ingredient_slug, medicine_status).

### DuplicateAlert
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| medicine_a_id | String(36) FK→prescription_medicines nullable | SET NULL |
| medicine_b_id | String(36) FK→prescription_medicines nullable | SET NULL |
| matched_active_ingredient | String(200) nullable | |
| match_type | String(20) | EXACT/PARTIAL |
| match_confidence | Float | |
| formulation_notes | Text nullable | |
| reviewed_by_patient | Boolean | default False |
| reviewed_at | DateTime nullable | |
| created_at, updated_at | DateTime | |

Index on (patient_id, reviewed_by_patient).

### MedicalHistorySummary
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| version_number | Integer | auto-increment per patient |
| is_current | Boolean | default False |
| status | String(20) | GENERATING/READY/FAILED |
| summary_json | Text nullable | serialised SummaryContent |
| medicine_count, prescription_count | Integer | |
| job_id | String(36) nullable | async job reference |
| generated_at | DateTime nullable | |
| created_at | DateTime | |

Index on (patient_id, is_current).

### Reminder
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| prescription_medicine_id | String(36) FK→prescription_medicines nullable | SET NULL |
| reminder_times | Text | JSON-encoded list of 'HH:MM' |
| frequency_display | String(100) nullable | |
| is_active | Boolean | default True |
| created_at, updated_at | DateTime | |

Index on (patient_id, is_active).

### RefreshToken
| Column | Type | Notes |
|---|---|---|
| id | String(36) PK | |
| patient_id | String(36) FK→patients | indexed |
| token_hash | String(256) unique | bcrypt hash of the JWT string |
| expires_at | DateTime | UTC |
| revoked | Boolean | default False |
| created_at | DateTime | |

Index on (patient_id, revoked).

---

## Alembic Setup Strategy

1. Run `alembic init migrations` inside `backend/` to create `migrations/alembic.ini` and `migrations/env.py`.
2. Edit `alembic.ini`: set `script_location = migrations`, leave `sqlalchemy.url` as a placeholder — env.py overrides it.
3. Edit `migrations/env.py`:
   - Import `Base` from `app.core.database` and all models from `app.models` (so Alembic sees the metadata).
   - Read `DATABASE_URL` from `os.environ` with default.
   - For SQLite (async), use synchronous migration mode: create a sync engine from the URL by replacing `aiosqlite` with the sync SQLite driver for migration purposes, since Alembic's `run_migrations_online()` supports sync mode.
   - Alternatively, use `alembic.runtime.migration.MigrationContext` with a sync engine — this is the standard approach for SQLite + Alembic.
4. Run `alembic revision --autogenerate -m "initial schema"` to generate the initial migration file.
5. Run `alembic upgrade head` to apply — verify all 8 tables exist.

---

## Service Layer Pattern

All services receive `AsyncSession` via constructor injection:

```python
class PatientService:
    def __init__(self, db: AsyncSession) -> None:
        self.db = db
```

Routers get the service via `Depends`:

```python
async def get_patient_service(db: AsyncSession = Depends(get_db)) -> PatientService:
    return PatientService(db)

@router.get("/patients/me")
async def get_me(
    patient_id: str = Depends(get_current_patient),
    service: PatientService = Depends(get_patient_service),
):
    patient = await service.get_by_id(patient_id)
    if patient is None:
        raise_http_error(404, "NOT_FOUND", "Patient not found")
    return PatientResponse.model_validate(patient)
```

Services commit inside their methods (`await self.db.commit()` + `await self.db.refresh(obj)`). Routers do not call `db.commit()` directly.

---

## Router Error Propagation Pattern

All HTTPExceptions go through `raise_http_error()`:

```python
# app/core/errors.py
def raise_http_error(status_code: int, code: str, message: str, details: dict = {}) -> None:
    raise HTTPException(
        status_code=status_code,
        detail={"error": {"code": code, "message": message, "details": details}},
    )
```

FastAPI's default exception handler returns the `detail` dict as-is, so the response body is:
```json
{"error": {"code": "...", "message": "...", "details": {}}}
```

---

## Test Infrastructure (conftest.py)

```python
# backend/tests/conftest.py
import pytest
import pytest_asyncio
from httpx import AsyncClient, ASGITransport
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

from app.core.database import Base, get_db
from app.main import app

TEST_DB_URL = "sqlite+aiosqlite:///:memory:"

@pytest_asyncio.fixture(scope="function")
async def async_client():
    engine = create_async_engine(TEST_DB_URL, echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    AsyncSessionLocal = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

    async def override_get_db():
        async with AsyncSessionLocal() as session:
            yield session

    app.dependency_overrides[get_db] = override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    app.dependency_overrides.clear()
    await engine.dispose()
```

`pytest.ini` (or `pyproject.toml [tool.pytest.ini_options]`):
```ini
[pytest]
asyncio_mode = auto
```

Helper in conftest for auth:
```python
async def register_and_login(client: AsyncClient, email: str = "testpatient@synthetic.medihistory") -> str:
    await client.post("/api/v1/auth/register", json={
        "email": email, "password": "SyntheticPass1!", "display_name": "Test Patient Alpha"
    })
    resp = await client.post("/api/v1/auth/login", json={"email": email, "password": "SyntheticPass1!"})
    return resp.json()["access_token"]
```

---

## Cross-Cutting Concerns

### Path traversal prevention in file upload
```python
import os
safe_filename = os.path.basename(filename.replace("\\", "/"))
# Never use user-supplied filename directly in a path join without basename
upload_path = os.path.join(upload_dir, patient_id, prescription_id, f"original_{safe_filename}")
```

### Cascade delete chain
- DELETE prescription → all `PrescriptionMedicine` rows deleted (ORM cascade)
- Deleted `PrescriptionMedicine` rows → `DuplicateAlert.medicine_a_id` / `medicine_b_id` set to NULL (FK SET NULL)
- Patient DELETE (future) → all prescriptions cascade → all medicines cascade → alerts cascade

### Doctor soft-delete
Deleting a doctor sets `is_deleted=True`. `GET /doctors` filters `is_deleted=False`. `Prescription.doctor_id` retains the UUID but the doctor detail endpoint returns 404. `doctor_name` in prescription responses is populated by joined doctor query; if doctor is soft-deleted, return doctor name from the row anyway (it's historical data) or set to `"[Deleted Doctor]"` — use the latter for clarity.

### Existing tests compatibility
`test_auth.py` and `test_security.py` use sync `TestClient` and import `app.main.app` directly. They will continue to work after changes because:
- The synthetic account in `AuthService.authenticate()` is preserved
- The `/auth/login` endpoint signature is unchanged (just adds `refresh_token` to the response — `TokenResponse` extends, not replaces)
- The `_dummy_protected` and `_dummy_patient_owned` test routes registered in those files still work

---

## Verification Commands

```powershell
# From backend/ directory with env vars set:
$env:JWT_SECRET_KEY = "synthetic-test-secret-for-ci"
$env:DATABASE_URL = "sqlite+aiosqlite:///./test.db"

# 1. Install dependencies
pip install -r requirements.txt

# 2. Run migrations
python -m alembic -c migrations/alembic.ini upgrade head

# 3. Run all tests
python -m pytest tests/ -v

# Expected: all tests pass, no errors
```

---

## FEAT Decomposition

| FEAT | What | Key outputs |
|---|---|---|
| FEAT-001 | Foundation: deps, config, DB, all 8 models, Alembic | requirements.txt, app/core/*, app/models/*, migrations/ |
| FEAT-002 | Auth extension + patient + doctor | extended auth_service, patients.py, doctors.py, conftest.py, test_patients.py, test_doctors.py |
| FEAT-003 | Prescriptions + medicines + file upload | prescriptions.py, medicines.py, test_prescriptions.py, test_medicines.py |
| FEAT-004 | Duplicates + history + reminders + interactions + export stubs | 5 routers + services + 3 test files |
| FEAT-005 | Full suite run + auth extension tests | test_auth.py extensions, full pytest pass |
