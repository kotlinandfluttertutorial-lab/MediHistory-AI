"""
Reminders endpoint tests.

All tests use synthetic data only. No real patient names or medical data.
Synthetic medicine IDs are used in place of real prescription_medicine_id values
since the reminders service does not enforce a foreign key at the API level.
"""

import os
import uuid

import pytest

os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient

from tests.conftest import register_and_login

# Synthetic medicine ID — does not reference a real PrescriptionMedicine row
# because the FK is nullable (SET NULL on delete) and SQLite does not enforce
# FK constraints by default in test mode.
SYNTHETIC_MEDICINE_ID = str(uuid.uuid4())
SYNTHETIC_TIMES = ["08:00", "20:00"]


async def test_create_reminder_returns_201(async_client: AsyncClient) -> None:
    """POST /patients/me/reminders returns 201 with deserialised reminder_times."""
    token = await register_and_login(async_client)
    response = await async_client.post(
        "/api/v1/patients/me/reminders",
        json={
            "prescription_medicine_id": SYNTHETIC_MEDICINE_ID,
            "reminder_times": SYNTHETIC_TIMES,
            "frequency_display": "Twice daily",
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert "id" in body
    assert body["reminder_times"] == SYNTHETIC_TIMES
    assert body["is_active"] is True
    assert body["frequency_display"] == "Twice daily"


async def test_list_reminders_returns_empty_initially(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/reminders returns empty list for a new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/reminders",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["reminders"] == []
    assert body["total"] == 0


async def test_update_reminder_is_active_returns_200(
    async_client: AsyncClient,
) -> None:
    """PUT /patients/me/reminders/{id} returns 200 with updated is_active."""
    token = await register_and_login(async_client)

    # Create a reminder first
    create_resp = await async_client.post(
        "/api/v1/patients/me/reminders",
        json={
            "prescription_medicine_id": SYNTHETIC_MEDICINE_ID,
            "reminder_times": ["09:00"],
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201, create_resp.text
    reminder_id = create_resp.json()["id"]

    # Deactivate it
    response = await async_client.put(
        f"/api/v1/patients/me/reminders/{reminder_id}",
        json={"is_active": False},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["is_active"] is False


async def test_delete_reminder_returns_204(async_client: AsyncClient) -> None:
    """DELETE /patients/me/reminders/{id} returns 204."""
    token = await register_and_login(async_client)

    # Create a reminder first
    create_resp = await async_client.post(
        "/api/v1/patients/me/reminders",
        json={
            "prescription_medicine_id": SYNTHETIC_MEDICINE_ID,
            "reminder_times": ["10:00"],
        },
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201, create_resp.text
    reminder_id = create_resp.json()["id"]

    # Delete it
    response = await async_client.delete(
        f"/api/v1/patients/me/reminders/{reminder_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 204, response.text


async def test_delete_reminder_returns_404_for_other_patient(
    async_client: AsyncClient,
) -> None:
    """DELETE /patients/me/reminders/{id} returns 404 for another patient's reminder."""
    # Patient A creates a reminder
    token_a = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/reminders",
        json={
            "prescription_medicine_id": SYNTHETIC_MEDICINE_ID,
            "reminder_times": ["07:00"],
        },
        headers={"Authorization": f"Bearer {token_a}"},
    )
    assert create_resp.status_code == 201, create_resp.text
    reminder_id = create_resp.json()["id"]

    # Patient B registers and tries to delete patient A's reminder
    email_b = "testpatient_rem_b@synthetic.medihistory"
    await async_client.post(
        "/api/v1/auth/register",
        json={
            "email": email_b,
            "password": "SyntheticPass789!",
            "display_name": "Synthetic Reminder Beta",
        },
    )
    login_b = await async_client.post(
        "/api/v1/auth/login",
        json={"email": email_b, "password": "SyntheticPass789!"},
    )
    token_b = login_b.json()["access_token"]

    response = await async_client.delete(
        f"/api/v1/patients/me/reminders/{reminder_id}",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert response.status_code == 404, response.text


async def test_create_reminder_returns_401_unauthenticated(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/reminders returns 401 without a token."""
    response = await async_client.post(
        "/api/v1/patients/me/reminders",
        json={
            "prescription_medicine_id": SYNTHETIC_MEDICINE_ID,
            "reminder_times": ["08:00"],
        },
    )
    assert response.status_code == 401, response.text
