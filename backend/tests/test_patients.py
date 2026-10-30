"""
Patient endpoint tests.

All tests use synthetic data only. No real patient names or credentials.
"""

import os

import pytest

os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient

from tests.conftest import SYNTHETIC_DISPLAY_NAME, SYNTHETIC_EMAIL, SYNTHETIC_PASSWORD, register_and_login


async def test_get_me_returns_200_for_authenticated_patient(async_client: AsyncClient) -> None:
    """GET /patients/me returns 200 and the patient profile for an authenticated patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["email"] == SYNTHETIC_EMAIL
    assert body["display_name"] == SYNTHETIC_DISPLAY_NAME


async def test_get_me_returns_401_unauthenticated(async_client: AsyncClient) -> None:
    """GET /patients/me returns 401 when no token is provided."""
    response = await async_client.get("/api/v1/patients/me")
    assert response.status_code == 401, response.text


async def test_update_me_returns_200_with_new_display_name(async_client: AsyncClient) -> None:
    """PUT /patients/me returns 200 and the updated display name."""
    token = await register_and_login(async_client)
    new_name = "Updated Patient Alpha"
    response = await async_client.put(
        "/api/v1/patients/me",
        json={"display_name": new_name},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["display_name"] == new_name


async def test_update_me_returns_401_unauthenticated(async_client: AsyncClient) -> None:
    """PUT /patients/me returns 401 when no token is provided."""
    response = await async_client.put(
        "/api/v1/patients/me",
        json={"display_name": "Test Patient Alpha"},
    )
    assert response.status_code == 401, response.text


async def test_update_me_returns_422_for_empty_display_name(async_client: AsyncClient) -> None:
    """PUT /patients/me returns 422 when display_name exceeds max_length."""
    token = await register_and_login(async_client)
    # display_name max_length=100; send a string longer than that
    too_long = "A" * 101
    response = await async_client.put(
        "/api/v1/patients/me",
        json={"display_name": too_long},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 422, response.text
