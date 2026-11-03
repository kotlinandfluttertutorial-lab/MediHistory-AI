"""
Tests for the process-review endpoint and ReviewProcessorService.

Uses synthetic data only — no real patient names, medicines, or doctors.
"""

import os
os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

import pytest
from httpx import AsyncClient

# Minimal valid 1×1 PNG
_TINY_PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
    b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde"
    b"\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)


async def _auth(client: AsyncClient, email: str, password: str = "SyntheticReview123!", name: str = "Test Reviewer Alpha") -> dict:
    """Register (idempotent) and log in; return auth headers."""
    await client.post("/api/v1/auth/register", json={"email": email, "password": password, "display_name": name})
    resp = await client.post("/api/v1/auth/login", json={"email": email, "password": password})
    assert resp.status_code == 200, resp.text
    return {"Authorization": f"Bearer {resp.json()['access_token']}"}


async def _upload(client: AsyncClient, headers: dict) -> str:
    resp = await client.post(
        "/api/v1/patients/me/prescriptions",
        headers=headers,
        files={"file": ("rx.png", _TINY_PNG, "image/png")},
    )
    assert resp.status_code == 201, resp.text
    return resp.json()["id"]


async def _process(client: AsyncClient, headers: dict, rx_id: str, fields: list):
    return await client.post(
        f"/api/v1/patients/me/prescriptions/{rx_id}/process-review",
        headers=headers,
        json={"reviewed_fields": fields},
    )


# ---------------------------------------------------------------------------
# Basic process-review tests
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_process_review_creates_medicines_and_doctor(async_client: AsyncClient):
    h = await _auth(async_client, "rv_a@synthetic.medihistory")
    rx = await _upload(async_client, h)
    resp = await _process(async_client, h, rx, [
        {"field_name": "doctor_name", "raw_value": "Dr. Synthetic Test",
         "patient_value": "Dr. Synthetic Test", "review_state": "REVIEWED"},
        {"field_name": "prescription_date", "raw_value": "2025-07-01",
         "patient_value": "2025-07-01", "review_state": "REVIEWED"},
        {"field_name": "medicine_name", "raw_value": "Synthamol 500mg",
         "patient_value": "Synthamol 500mg", "review_state": "REVIEWED"},
    ])
    assert resp.status_code == 200, resp.text
    data = resp.json()
    assert data["ocr_status"] == "COMPLETED"
    assert data["medicines_created"] == 1
    assert data["doctor_id"] is not None


@pytest.mark.asyncio
async def test_process_review_ignores_rejected_fields(async_client: AsyncClient):
    h = await _auth(async_client, "rv_b@synthetic.medihistory")
    rx = await _upload(async_client, h)
    resp = await _process(async_client, h, rx, [
        {"field_name": "medicine_name", "raw_value": "Synthamol 500mg",
         "patient_value": None, "review_state": "REJECTED"},
    ])
    assert resp.status_code == 200, resp.text
    assert resp.json()["medicines_created"] == 0


@pytest.mark.asyncio
async def test_process_review_ignores_pending_fields(async_client: AsyncClient):
    h = await _auth(async_client, "rv_c@synthetic.medihistory")
    rx = await _upload(async_client, h)
    resp = await _process(async_client, h, rx, [
        {"field_name": "medicine_name", "raw_value": "Synthamol 500mg",
         "patient_value": "Synthamol 500mg", "review_state": "PENDING_REVIEW"},
    ])
    assert resp.status_code == 200, resp.text
    assert resp.json()["medicines_created"] == 0


@pytest.mark.asyncio
async def test_process_review_wrong_patient_returns_404(async_client: AsyncClient):
    owner = await _auth(async_client, "rv_d@synthetic.medihistory")
    rx = await _upload(async_client, owner)
    intruder = await _auth(async_client, "rv_intruder@synthetic.medihistory",
                           password="SyntheticIntruder123!", name="Test Intruder")
    resp = await _process(async_client, intruder, rx, [])
    assert resp.status_code == 404


@pytest.mark.asyncio
async def test_process_review_sets_ocr_status_to_completed(async_client: AsyncClient):
    h = await _auth(async_client, "rv_e@synthetic.medihistory")
    rx = await _upload(async_client, h)
    resp = await _process(async_client, h, rx, [])
    assert resp.status_code == 200
    assert resp.json()["ocr_status"] == "COMPLETED"


@pytest.mark.asyncio
async def test_process_review_reuses_existing_doctor_by_name(async_client: AsyncClient):
    """Two prescriptions with the same doctor name should share one doctor row."""
    h = await _auth(async_client, "rv_f@synthetic.medihistory")
    rx1 = await _upload(async_client, h)
    rx2 = await _upload(async_client, h)

    r1 = await _process(async_client, h, rx1, [
        {"field_name": "doctor_name", "raw_value": "Dr. Reuse Test",
         "patient_value": "Dr. Reuse Test", "review_state": "REVIEWED"},
    ])
    r2 = await _process(async_client, h, rx2, [
        {"field_name": "doctor_name", "raw_value": "Dr. Reuse Test",
         "patient_value": "Dr. Reuse Test", "review_state": "REVIEWED"},
    ])
    assert r1.status_code == 200
    assert r2.status_code == 200
    assert r1.json()["doctor_id"] == r2.json()["doctor_id"]


@pytest.mark.asyncio
async def test_process_review_multiple_medicines(async_client: AsyncClient):
    h = await _auth(async_client, "rv_g@synthetic.medihistory")
    rx = await _upload(async_client, h)
    resp = await _process(async_client, h, rx, [
        {"field_name": "medicine_name", "raw_value": "Synthamol 500mg",
         "patient_value": "Synthamol 500mg", "review_state": "REVIEWED"},
        {"field_name": "medicine_name_2", "raw_value": "Testocillin 250mg",
         "patient_value": "Testocillin 250mg", "review_state": "REVIEWED"},
    ])
    assert resp.status_code == 200, resp.text
    assert resp.json()["medicines_created"] == 2
