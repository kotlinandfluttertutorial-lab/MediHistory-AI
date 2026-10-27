"""
Security tests for MediHistory AI backend.

All tests use synthetic data only. No real patient identifiers or tokens.

Coverage targets:
- Cross-patient access returns HTTP 404 (never 403).
- Unauthenticated requests return HTTP 401.
- Rate limiter returns HTTP 429 after threshold is exceeded.
- require_patient_owns_resource helper raises 404 for mismatched patient IDs.
- require_patient_owns_resource passes silently for matching patient IDs.
"""

import os

import pytest
from fastapi import Depends
from fastapi.testclient import TestClient

# Set a synthetic JWT secret before importing anything that reads it at module level.
os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")

from app.core.security import get_current_patient
from app.main import app
from app.middleware.patient_auth import require_patient_owns_resource

# ---------------------------------------------------------------------------
# Synthetic test identifiers — never use real patient IDs or credentials.
# ---------------------------------------------------------------------------
SYNTHETIC_EMAIL = "test@synthetic.medihistory"
SYNTHETIC_PASSWORD = "SyntheticTest123!"
SYNTHETIC_PATIENT_ID_A = "synthetic-patient-001"
SYNTHETIC_PATIENT_ID_B = "synthetic-patient-002"


# ---------------------------------------------------------------------------
# Dummy protected routes registered once for the test suite.
# ---------------------------------------------------------------------------

@app.get("/test-patient-owned/{owner_id}", tags=["test-only"])
async def _dummy_patient_owned(
    owner_id: str,
    current_patient_id: str = Depends(get_current_patient),
) -> dict:
    """Returns the resource if the caller owns it; 404 otherwise."""
    require_patient_owns_resource(owner_id, current_patient_id)
    return {"owner_id": owner_id}


@pytest.fixture(scope="module")
def client() -> TestClient:
    return TestClient(app, raise_server_exceptions=True)


@pytest.fixture(scope="module")
def auth_token(client: TestClient) -> str:
    """Obtain a valid JWT for the synthetic patient."""
    response = client.post(
        "/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": SYNTHETIC_PASSWORD},
    )
    assert response.status_code == 200, f"Setup login failed: {response.text}"
    return response.json()["access_token"]


# ---------------------------------------------------------------------------
# test_unauthenticated_request_returns_401
# ---------------------------------------------------------------------------

def test_unauthenticated_request_returns_401(client: TestClient) -> None:
    """A request with no Authorization header must return HTTP 401."""
    response = client.get(
        f"/test-patient-owned/{SYNTHETIC_PATIENT_ID_A}"
    )
    assert response.status_code == 401, (
        f"Expected 401 for unauthenticated request; got {response.status_code}"
    )


# ---------------------------------------------------------------------------
# test_cross_patient_access_returns_404
# ---------------------------------------------------------------------------

def test_cross_patient_access_returns_404(
    client: TestClient, auth_token: str
) -> None:
    """
    Accessing a resource owned by a different patient must return HTTP 404,
    not 403, to avoid leaking whether the resource exists.

    The authenticated patient is SYNTHETIC_PATIENT_ID_A; the resource
    owner is SYNTHETIC_PATIENT_ID_B — a different patient.
    """
    response = client.get(
        f"/test-patient-owned/{SYNTHETIC_PATIENT_ID_B}",
        headers={"Authorization": f"Bearer {auth_token}"},
    )
    assert response.status_code == 404, (
        f"Cross-patient access must return 404 (not 403); got {response.status_code}"
    )


def test_cross_patient_access_does_not_return_403(
    client: TestClient, auth_token: str
) -> None:
    """Explicitly verify that 403 is never returned for cross-patient access."""
    response = client.get(
        f"/test-patient-owned/{SYNTHETIC_PATIENT_ID_B}",
        headers={"Authorization": f"Bearer {auth_token}"},
    )
    assert response.status_code != 403, (
        "403 must never be returned for cross-patient access — use 404 instead"
    )


# ---------------------------------------------------------------------------
# test_rate_limit_exceeded_returns_429
# ---------------------------------------------------------------------------

def test_rate_limit_exceeded_returns_429() -> None:
    """
    After max_requests within the window, the next request must return HTTP 429.

    A dedicated FastAPI app instance with a low limit (3 requests) is used so
    the test is fast and deterministic. The shared app singleton is not used
    here to avoid interference from other test modules that also hit it.
    """
    from fastapi import FastAPI
    from app.middleware.rate_limit import RateLimitMiddleware
    from app.routers import auth as auth_router

    # Isolated app with a very small limit for test speed.
    _test_app = FastAPI()
    _test_app.add_middleware(RateLimitMiddleware, max_requests=3, window_seconds=60)
    _test_app.include_router(auth_router.router)

    test_client = TestClient(_test_app, raise_server_exceptions=False)

    # First 3 requests must go through (401 or 200, not 429).
    for i in range(3):
        resp = test_client.post(
            "/auth/login",
            json={"email": SYNTHETIC_EMAIL, "password": "wrong-password"},
        )
        assert resp.status_code != 429, (
            f"Request {i + 1} was rate-limited before limit was reached"
        )

    # The 4th request must be rate-limited.
    resp = test_client.post(
        "/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": "wrong-password"},
    )
    assert resp.status_code == 429, (
        f"Expected 429 on request 4; got {resp.status_code}"
    )


# ---------------------------------------------------------------------------
# Unit tests for require_patient_owns_resource helper
# ---------------------------------------------------------------------------

def test_require_patient_owns_resource_raises_404_for_different_patient() -> None:
    """require_patient_owns_resource must raise HTTP 404 when IDs differ."""
    from fastapi import HTTPException

    with pytest.raises(HTTPException) as exc_info:
        require_patient_owns_resource(
            resource_patient_id=SYNTHETIC_PATIENT_ID_B,
            current_patient_id=SYNTHETIC_PATIENT_ID_A,
        )

    exc = exc_info.value
    assert exc.status_code == 404, (
        f"Expected status 404; got {exc.status_code}"
    )
    assert exc.detail == "Resource not found"


def test_require_patient_owns_resource_passes_for_same_patient() -> None:
    """require_patient_owns_resource must not raise when IDs match."""
    # Should complete without raising.
    require_patient_owns_resource(
        resource_patient_id=SYNTHETIC_PATIENT_ID_A,
        current_patient_id=SYNTHETIC_PATIENT_ID_A,
    )
