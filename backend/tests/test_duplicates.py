"""
Duplicate alert endpoint tests.

All tests use synthetic data only. No real patient names or medical data.
"""

import os


os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

import uuid
from datetime import UTC, datetime

from httpx import AsyncClient

from app.models.duplicate_alert import DuplicateAlert
from tests.conftest import register_and_login


async def test_list_duplicates_returns_empty_initially(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/duplicates returns empty list for a new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/duplicates",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["alerts"] == []
    assert body["total"] == 0
    assert body["unreviewed_count"] == 0


async def test_get_duplicate_returns_404_for_other_patient(
    async_client: AsyncClient, async_db
) -> None:
    """GET /patients/me/duplicates/{id} returns 404 for another patient's alert."""
    # Register patient A
    token_a = await register_and_login(async_client)

    # Register patient B
    email_b = "testpatient_dup_b@synthetic.medihistory"
    await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": email_b,
            "password": "SyntheticPass456!",
            "display_name": "Synthetic User Beta",
        },
    )
    login_b = await async_client.post(
        "/api/v1/auth/login",
        json={"email": email_b, "password": "SyntheticPass456!"},
    )
    token_b = login_b.json()["access_token"]

    # Get patient A's ID from the profile endpoint
    profile_a = await async_client.get(
        "/api/v1/patients/me",
        headers={"Authorization": f"Bearer {token_a}"},
    )
    patient_a_id = profile_a.json()["id"]

    # Insert a DuplicateAlert directly for patient A
    alert = DuplicateAlert(
        id=str(uuid.uuid4()),
        patient_id=patient_a_id,
        matched_active_ingredient="synth-ingredient",
        match_type="EXACT",
        match_confidence=1.0,
    )
    async_db.add(alert)
    await async_db.commit()

    # Patient B tries to access patient A's alert — must get 404
    response = await async_client.get(
        f"/api/v1/patients/me/duplicates/{alert.id}",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert response.status_code == 404, response.text


async def test_mark_reviewed_returns_200(async_client: AsyncClient, async_db) -> None:
    """PUT /patients/me/duplicates/{id}/reviewed returns 200."""
    token = await register_and_login(async_client)
    profile = await async_client.get(
        "/api/v1/patients/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    patient_id = profile.json()["id"]

    # Insert a DuplicateAlert directly
    alert = DuplicateAlert(
        id=str(uuid.uuid4()),
        patient_id=patient_id,
        matched_active_ingredient="synthamol-ingredient",
        match_type="EXACT",
        match_confidence=1.0,
        reviewed_by_patient=False,
    )
    async_db.add(alert)
    await async_db.commit()

    response = await async_client.put(
        f"/api/v1/patients/me/duplicates/{alert.id}/reviewed",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["reviewed_by_patient"] is True
    assert body["reviewed_at"] is not None


async def test_mark_reviewed_returns_409_when_already_reviewed(
    async_client: AsyncClient, async_db
) -> None:
    """PUT /patients/me/duplicates/{id}/reviewed returns 409 if already reviewed."""
    token = await register_and_login(async_client)
    profile = await async_client.get(
        "/api/v1/patients/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    patient_id = profile.json()["id"]

    # Insert an already-reviewed alert
    alert = DuplicateAlert(
        id=str(uuid.uuid4()),
        patient_id=patient_id,
        matched_active_ingredient="testocillin-ingredient",
        match_type="EXACT",
        match_confidence=1.0,
        reviewed_by_patient=True,
        reviewed_at=datetime.now(tz=UTC),
    )
    async_db.add(alert)
    await async_db.commit()

    response = await async_client.put(
        f"/api/v1/patients/me/duplicates/{alert.id}/reviewed",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 409, response.text


async def test_list_duplicates_returns_401_unauthenticated(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/duplicates returns 401 without a token."""
    response = await async_client.get("/api/v1/patients/me/duplicates")
    assert response.status_code == 401, response.text
