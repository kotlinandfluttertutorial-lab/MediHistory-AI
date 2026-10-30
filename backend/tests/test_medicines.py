"""
Medicine endpoint tests.

All tests use synthetic data only.
Synthetic medicine: brand_name='Synthamol 500mg', active_ingredient='synthamol'.
"""

import io
import os

import pytest

os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from tests.conftest import register_and_login

# Minimal 1×1 PNG for prescription upload
_TINY_PNG = (
    b"\x89PNG\r\n\x1a\n"
    b"\x00\x00\x00\rIHDR"
    b"\x00\x00\x00\x01"
    b"\x00\x00\x00\x01"
    b"\x08\x02"
    b"\x00\x00\x00"
    b"\x90wS\xde"
    b"\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)

SYNTHETIC_EMAIL_B = "testpatient_b_med@synthetic.medihistory"
SYNTHETIC_PASSWORD_B = "SyntheticPass456Med!"
SYNTHETIC_DISPLAY_NAME_B = "Synthetic User Beta Med"


async def _register_second_patient(client: AsyncClient) -> str:
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
    assert login.status_code == 200
    return login.json()["access_token"]


async def _upload_prescription(client: AsyncClient, token: str) -> str:
    """Upload a synthetic prescription and return its ID."""
    resp = await client.post(
        "/api/v1/patients/me/prescriptions",
        files={"file": ("synth.png", io.BytesIO(_TINY_PNG), "image/png")},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert resp.status_code == 201, f"Prescription upload failed: {resp.text}"
    return resp.json()["id"]


async def _add_medicine_directly(db: AsyncSession, prescription_id: str, patient_id_str: str) -> str:
    """Insert a PrescriptionMedicine row directly into the test DB and return its id."""
    from app.models.medicine import PrescriptionMedicine

    med = PrescriptionMedicine(
        prescription_id=prescription_id,
        patient_id=patient_id_str,
        brand_name="Synthamol 500mg",
        active_ingredient="synthamol",
        active_ingredient_slug="synthamol",
        strength="500mg",
        formulation="tablet",
        dosage="1 tablet twice daily",
        review_state="PENDING_REVIEW",
        medicine_status="CURRENT",
    )
    db.add(med)
    await db.commit()
    await db.refresh(med)
    return med.id


async def test_list_medicines_returns_empty_initially(async_client: AsyncClient) -> None:
    """GET /patients/me/medicines returns empty list for a new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/medicines",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["medicines"] == []
    assert body["total"] == 0


async def test_list_medicines_returns_grouped_by_active_ingredient(
    async_client: AsyncClient, async_db: AsyncSession
) -> None:
    """
    GET /patients/me/medicines returns medicines grouped by active ingredient.

    Two synthamol prescriptions should produce one group with two occurrences.
    """
    token = await register_and_login(async_client)

    # Decode patient_id from the token to use for direct DB insertion
    import jwt as _jwt
    payload = _jwt.decode(token, options={"verify_signature": False})
    patient_id_str = payload["sub"]

    # Upload two prescriptions
    presc_id_1 = await _upload_prescription(async_client, token)
    presc_id_2 = await _upload_prescription(async_client, token)

    # Add synthetic medicines directly to the DB
    await _add_medicine_directly(async_db, presc_id_1, patient_id_str)
    await _add_medicine_directly(async_db, presc_id_2, patient_id_str)

    response = await async_client.get(
        "/api/v1/patients/me/medicines",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["total"] >= 1

    # Find the synthamol group
    synthamol_groups = [
        g for g in body["medicines"]
        if (g.get("active_ingredient") or "").lower() == "synthamol"
    ]
    assert len(synthamol_groups) == 1
    assert len(synthamol_groups[0]["occurrences"]) == 2


async def test_update_medicine_review_state_returns_200(
    async_client: AsyncClient, async_db: AsyncSession
) -> None:
    """PUT /patients/me/prescriptions/{id}/medicines/{med_id} updates review_state."""
    token = await register_and_login(async_client)

    import jwt as _jwt
    payload = _jwt.decode(token, options={"verify_signature": False})
    patient_id_str = payload["sub"]

    presc_id = await _upload_prescription(async_client, token)
    medicine_id = await _add_medicine_directly(async_db, presc_id, patient_id_str)

    response = await async_client.put(
        f"/api/v1/patients/me/prescriptions/{presc_id}/medicines/{medicine_id}",
        json={"review_state": "REVIEWED"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["review_state"] == "REVIEWED"
    assert body["id"] == medicine_id


async def test_update_medicine_returns_404_for_other_patient(
    async_client: AsyncClient, async_db: AsyncSession
) -> None:
    """PUT .../medicines/{id} returns 404 when medicine belongs to another patient."""
    token_a = await register_and_login(async_client)

    import jwt as _jwt
    payload_a = _jwt.decode(token_a, options={"verify_signature": False})
    patient_id_a = payload_a["sub"]

    presc_id = await _upload_prescription(async_client, token_a)
    medicine_id = await _add_medicine_directly(async_db, presc_id, patient_id_a)

    token_b = await _register_second_patient(async_client)

    response = await async_client.put(
        f"/api/v1/patients/me/prescriptions/{presc_id}/medicines/{medicine_id}",
        json={"review_state": "REVIEWED"},
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert response.status_code == 404, response.text


async def test_get_medicine_by_slug_returns_404_for_missing_slug(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/medicines/{slug} returns 404 for a slug that does not exist."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/medicines/nonexistent-slug-xyz",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 404, response.text
