"""
Authentication endpoint tests.

All tests use synthetic credentials only.
Security: no real patient data, no real tokens, no secret values in source.

The JWT_SECRET_KEY environment variable is set to a synthetic value inside
this module's fixture; it is never committed as a hardcoded secret elsewhere.
"""

import logging
import os

import pytest
from fastapi import Depends
from fastapi.testclient import TestClient

# Set a synthetic JWT secret before importing anything that reads it at module level.
os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")

from app.core.security import get_current_patient
from app.main import app

# ---------------------------------------------------------------------------
# Synthetic test data — never use real credentials
# ---------------------------------------------------------------------------
SYNTHETIC_EMAIL = "test@synthetic.medihistory"
SYNTHETIC_PASSWORD = "SyntheticTest123!"


# ---------------------------------------------------------------------------
# Dummy protected route — used only by test_protected_endpoint_without_token
# ---------------------------------------------------------------------------
@app.get("/test-protected", tags=["test-only"])
async def _dummy_protected(patient_id: str = Depends(get_current_patient)) -> dict:
    return {"patient_id": patient_id}


@pytest.fixture(scope="module")
def client() -> TestClient:
    return TestClient(app, raise_server_exceptions=True)


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


def test_login_valid_credentials_returns_200(client: TestClient) -> None:
    response = client.post(
        "/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": SYNTHETIC_PASSWORD},
    )
    assert response.status_code == 200, response.text


def test_login_invalid_credentials_returns_401(client: TestClient) -> None:
    response = client.post(
        "/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": "WrongPassword!"},
    )
    assert response.status_code == 401, response.text


def test_login_response_has_access_token(client: TestClient) -> None:
    response = client.post(
        "/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": SYNTHETIC_PASSWORD},
    )
    assert response.status_code == 200
    body = response.json()
    assert "access_token" in body, "Response must contain access_token field"
    assert isinstance(body["access_token"], str)
    assert len(body["access_token"]) > 0, "access_token must not be empty"
    assert body.get("token_type") == "bearer"


def test_protected_endpoint_without_token_returns_401(client: TestClient) -> None:
    response = client.get("/test-protected")
    assert (
        response.status_code == 401
    ), f"Unauthenticated request must return 401; got {response.status_code}"


def test_token_value_not_in_logs(
    client: TestClient, caplog: pytest.LogCaptureFixture
) -> None:
    """Verify that the JWT string never appears in application logs."""
    with caplog.at_level(logging.DEBUG):
        response = client.post(
            "/auth/login",
            json={"email": SYNTHETIC_EMAIL, "password": SYNTHETIC_PASSWORD},
        )
    assert response.status_code == 200
    token = response.json()["access_token"]

    # The JWT must not appear anywhere in the captured log output.
    for record in caplog.records:
        assert token not in record.getMessage(), (
            f"JWT token value found in log record from logger '{record.name}': "
            f"{record.getMessage()!r}"
        )
