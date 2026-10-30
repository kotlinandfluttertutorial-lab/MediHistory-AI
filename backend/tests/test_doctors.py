"""
Doctor endpoint tests.

All tests use synthetic data only. No real doctor names or patient data.
"""

import os


os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient

from tests.conftest import register_and_login

SYNTHETIC_DOCTOR_NAME = "Dr. Synthetic"
SYNTHETIC_SPECIALTY = "Cardiology"

# A second synthetic patient for cross-patient 404 tests
SYNTHETIC_EMAIL_B = "testpatient_b@synthetic.medihistory"
SYNTHETIC_PASSWORD_B = "SyntheticPass456!"
SYNTHETIC_DISPLAY_NAME_B = "Synthetic User Beta"


async def _register_second_patient(client: AsyncClient) -> str:
    """Register a second synthetic patient and return its access token."""
    reg = await client.post(
        "/api/v1/auth/register",
        json={
            "email": SYNTHETIC_EMAIL_B,
            "password": SYNTHETIC_PASSWORD_B,
            "display_name": SYNTHETIC_DISPLAY_NAME_B,
        },
    )
    assert reg.status_code == 201, f"Second patient registration failed: {reg.text}"
    login = await client.post(
        "/api/v1/auth/login",
        json={"email": SYNTHETIC_EMAIL_B, "password": SYNTHETIC_PASSWORD_B},
    )
    assert login.status_code == 200, f"Second patient login failed: {login.text}"
    return login.json()["access_token"]


async def test_list_doctors_returns_empty_list_initially(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/doctors returns an empty list for a new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/doctors",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["doctors"] == []
    assert body["total"] == 0


async def test_create_doctor_returns_201(async_client: AsyncClient) -> None:
    """POST /patients/me/doctors creates a doctor and returns 201."""
    token = await register_and_login(async_client)
    response = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME, "specialty": SYNTHETIC_SPECIALTY},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert body["name"] == SYNTHETIC_DOCTOR_NAME
    assert body["specialty"] == SYNTHETIC_SPECIALTY
    assert "id" in body


async def test_create_doctor_returns_401_unauthenticated(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/doctors returns 401 when no token is provided."""
    response = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME},
    )
    assert response.status_code == 401, response.text


async def test_get_doctor_returns_404_for_other_patient(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/doctors/{id} returns 404 when the doctor belongs to another patient."""
    # Patient A creates a doctor
    token_a = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME, "specialty": SYNTHETIC_SPECIALTY},
        headers={"Authorization": f"Bearer {token_a}"},
    )
    assert create_resp.status_code == 201, create_resp.text
    doctor_id = create_resp.json()["id"]

    # Patient B tries to access patient A's doctor
    token_b = await _register_second_patient(async_client)
    response = await async_client.get(
        f"/api/v1/patients/me/doctors/{doctor_id}",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert response.status_code == 404, response.text


async def test_update_doctor_returns_200(async_client: AsyncClient) -> None:
    """PUT /patients/me/doctors/{id} returns 200 with the updated doctor."""
    token = await register_and_login(async_client)
    # Create
    create_resp = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME, "specialty": SYNTHETIC_SPECIALTY},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    doctor_id = create_resp.json()["id"]

    # Update
    updated_name = "Dr. Synthetic Updated"
    response = await async_client.put(
        f"/api/v1/patients/me/doctors/{doctor_id}",
        json={"name": updated_name},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["name"] == updated_name


async def test_delete_doctor_returns_204(async_client: AsyncClient) -> None:
    """DELETE /patients/me/doctors/{id} returns 204."""
    token = await register_and_login(async_client)
    # Create
    create_resp = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME, "specialty": SYNTHETIC_SPECIALTY},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    doctor_id = create_resp.json()["id"]

    # Delete
    response = await async_client.delete(
        f"/api/v1/patients/me/doctors/{doctor_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 204, response.text


async def test_deleted_doctor_not_in_list(async_client: AsyncClient) -> None:
    """A deleted doctor does not appear in GET /patients/me/doctors."""
    token = await register_and_login(async_client)
    # Create
    create_resp = await async_client.post(
        "/api/v1/patients/me/doctors",
        json={"name": SYNTHETIC_DOCTOR_NAME, "specialty": SYNTHETIC_SPECIALTY},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    doctor_id = create_resp.json()["id"]

    # Delete
    del_resp = await async_client.delete(
        f"/api/v1/patients/me/doctors/{doctor_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert del_resp.status_code == 204

    # List — should be empty
    list_resp = await async_client.get(
        "/api/v1/patients/me/doctors",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert list_resp.status_code == 200
    body = list_resp.json()
    ids = [d["id"] for d in body["doctors"]]
    assert doctor_id not in ids
