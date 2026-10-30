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
from httpx import AsyncClient

# Set a synthetic JWT secret before importing anything that reads it at module level.
os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")

from app.core.security import get_current_patient
from app.main import app

# ---------------------------------------------------------------------------
# Synthetic test data — never use real credentials
# ---------------------------------------------------------------------------
SYNTHETIC_EMAIL = "test@synthetic.medihistory"
SYNTHETIC_PASSWORD = "SyntheticTest123!"

# New-registration synthetic data (distinct from login synthetic account)
NEW_PATIENT_EMAIL = "new-patient@synthetic.medihistory"
NEW_PATIENT_PASSWORD = "SyntheticPass1!"
NEW_PATIENT_DISPLAY_NAME = "Test Patient Alpha"


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
# Existing sync tests (TestClient-based)
# ---------------------------------------------------------------------------


def test_login_valid_credentials_returns_200(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": SYNTHETIC_PASSWORD},
    )
    assert response.status_code == 200, response.text


def test_login_invalid_credentials_returns_401(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/login",
        json={"email": SYNTHETIC_EMAIL, "password": "WrongPassword!"},
    )
    assert response.status_code == 401, response.text


def test_login_response_has_access_token(client: TestClient) -> None:
    response = client.post(
        "/api/v1/auth/login",
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
            "/api/v1/auth/login",
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


# ---------------------------------------------------------------------------
# New async tests — register / refresh / logout endpoints
# ---------------------------------------------------------------------------


async def test_register_returns_201_with_valid_data(
    async_client: AsyncClient,
) -> None:
    """POST /auth/register with valid data returns 201 and the new patient's info."""
    response = await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": NEW_PATIENT_EMAIL,
            "password": NEW_PATIENT_PASSWORD,
            "display_name": NEW_PATIENT_DISPLAY_NAME,
        },
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert "patient_id" in body
    assert body["email"] == NEW_PATIENT_EMAIL
    assert body["display_name"] == NEW_PATIENT_DISPLAY_NAME
    assert "created_at" in body


async def test_register_returns_409_for_duplicate_email(
    async_client: AsyncClient,
) -> None:
    """POST /auth/register with an already-registered email returns 409."""
    payload = {
        "email": NEW_PATIENT_EMAIL,
        "password": NEW_PATIENT_PASSWORD,
        "display_name": NEW_PATIENT_DISPLAY_NAME,
    }
    # First registration — must succeed
    first = await async_client.post("/api/v1/auth/register", json=payload)
    assert first.status_code == 201, first.text

    # Second registration with the same email — must return 409
    second = await async_client.post("/api/v1/auth/register", json=payload)
    assert second.status_code == 409, second.text


async def test_register_returns_422_for_short_password(
    async_client: AsyncClient,
) -> None:
    """POST /auth/register with a password shorter than 8 characters returns 422."""
    response = await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": "shortpass@synthetic.medihistory",
            "password": "short",
            "display_name": "Test Patient Short",
        },
    )
    assert response.status_code == 422, response.text


async def test_login_returns_refresh_token_in_response(
    async_client: AsyncClient,
) -> None:
    """POST /auth/login for a registered patient returns both access and refresh tokens."""
    # Register first
    await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": NEW_PATIENT_EMAIL,
            "password": NEW_PATIENT_PASSWORD,
            "display_name": NEW_PATIENT_DISPLAY_NAME,
        },
    )
    # Login
    response = await async_client.post(
        "/api/v1/auth/login",
        json={"email": NEW_PATIENT_EMAIL, "password": NEW_PATIENT_PASSWORD},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert "access_token" in body
    assert "refresh_token" in body
    assert isinstance(body["refresh_token"], str)
    assert len(body["refresh_token"]) > 0, "refresh_token must not be empty"


async def test_refresh_returns_new_access_token(
    async_client: AsyncClient,
) -> None:
    """POST /auth/refresh with a valid refresh token returns a new access token."""
    # Register and login to obtain a refresh token
    await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": NEW_PATIENT_EMAIL,
            "password": NEW_PATIENT_PASSWORD,
            "display_name": NEW_PATIENT_DISPLAY_NAME,
        },
    )
    login = await async_client.post(
        "/api/v1/auth/login",
        json={"email": NEW_PATIENT_EMAIL, "password": NEW_PATIENT_PASSWORD},
    )
    assert login.status_code == 200, login.text
    refresh_token = login.json()["refresh_token"]
    # Exchange refresh token for a new access token
    response = await async_client.post(
        "/api/v1/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert "access_token" in body
    assert isinstance(body["access_token"], str)
    assert len(body["access_token"]) > 0
    # New token should be a valid JWT (different iat/exp from the original)
    assert body["access_token"] != refresh_token


async def test_logout_returns_204(
    async_client: AsyncClient,
) -> None:
    """POST /auth/logout with a valid refresh token returns 204 No Content."""
    # Register and login to obtain a refresh token
    await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": NEW_PATIENT_EMAIL,
            "password": NEW_PATIENT_PASSWORD,
            "display_name": NEW_PATIENT_DISPLAY_NAME,
        },
    )
    login = await async_client.post(
        "/api/v1/auth/login",
        json={"email": NEW_PATIENT_EMAIL, "password": NEW_PATIENT_PASSWORD},
    )
    assert login.status_code == 200, login.text
    refresh_token = login.json()["refresh_token"]

    # Logout — must return 204 with no body
    response = await async_client.post(
        "/api/v1/auth/logout",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 204, response.text
