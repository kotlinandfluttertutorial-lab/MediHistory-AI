"""
Medical history endpoint tests.

All tests use synthetic data only. No real patient names or medical data.
"""

import os

import pytest

os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient

from tests.conftest import register_and_login


async def test_generate_history_returns_202(async_client: AsyncClient) -> None:
    """POST /patients/me/history/generate returns 202 with summary_id and GENERATING status."""
    token = await register_and_login(async_client)
    response = await async_client.post(
        "/api/v1/patients/me/history/generate",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 202, response.text
    body = response.json()
    assert "summary_id" in body
    assert body["status"] == "GENERATING"


async def test_generate_history_returns_409_when_already_generating(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/history/generate returns 409 when a job is in progress."""
    token = await register_and_login(async_client)

    # First request starts a generation
    first = await async_client.post(
        "/api/v1/patients/me/history/generate",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert first.status_code == 202, first.text

    # Second request while first is still GENERATING must return 409
    second = await async_client.post(
        "/api/v1/patients/me/history/generate",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert second.status_code == 409, second.text


async def test_list_history_returns_empty_initially(async_client: AsyncClient) -> None:
    """GET /patients/me/history returns empty list for a new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/history",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["summaries"] == []
    assert body["total"] == 0


async def test_get_history_returns_404_for_missing_id(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/history/{id} returns 404 for a non-existent summary ID."""
    token = await register_and_login(async_client)
    fake_id = "00000000-0000-0000-0000-000000000000"
    response = await async_client.get(
        f"/api/v1/patients/me/history/{fake_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 404, response.text


async def test_list_history_returns_401_unauthenticated(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/history returns 401 without a token."""
    response = await async_client.get("/api/v1/patients/me/history")
    assert response.status_code == 401, response.text
