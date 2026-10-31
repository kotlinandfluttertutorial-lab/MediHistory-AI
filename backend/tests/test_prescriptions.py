"""
Prescription endpoint tests.

All tests use synthetic data only.
No real patient names, medicine names, or document contents.

File upload tests use a minimal 1×1 pixel PNG in memory — no real files.
"""

import io
import os


os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")

from httpx import AsyncClient

from tests.conftest import register_and_login

# Minimal valid 1×1 pixel PNG (67 bytes, no external dependency required)
_TINY_PNG = (
    b"\x89PNG\r\n\x1a\n"  # PNG signature
    b"\x00\x00\x00\rIHDR"  # IHDR chunk length + type
    b"\x00\x00\x00\x01"  # width = 1
    b"\x00\x00\x00\x01"  # height = 1
    b"\x08\x02"  # bit depth=8, colour type=2 (RGB)
    b"\x00\x00\x00"  # compression, filter, interlace
    b"\x90wS\xde"  # CRC
    b"\x00\x00\x00\x0cIDATx\x9cc\xf8\x0f\x00\x00\x01\x01\x00\x05\x18\xd8N"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)

SYNTHETIC_EMAIL_B = "testpatient_b_presc@synthetic.medihistory"
SYNTHETIC_PASSWORD_B = "SyntheticPass456B!"
SYNTHETIC_DISPLAY_NAME_B = "Synthetic User Beta Presc"


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


def _png_upload_files(
    data: bytes = _TINY_PNG, filename: str = "synthetic_prescription.png"
):
    """Return files dict for multipart upload with a PNG."""
    return {"file": (filename, io.BytesIO(data), "image/png")}


async def test_upload_prescription_returns_201(async_client: AsyncClient) -> None:
    """POST /patients/me/prescriptions with valid PNG returns 201 with PENDING status."""
    token = await register_and_login(async_client)
    response = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert "id" in body
    assert body["ocr_status"] == "PENDING"
    assert "created_at" in body


async def test_upload_prescription_returns_401_unauthenticated(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/prescriptions without token returns 401."""
    response = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
    )
    assert response.status_code == 401, response.text


async def test_upload_prescription_returns_413_for_oversized_file(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/prescriptions with file > 10 MB returns 413."""
    token = await register_and_login(async_client)
    # 11 MB of zeros
    oversized = b"\x00" * (11 * 1024 * 1024)
    response = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files={"file": ("big.png", io.BytesIO(oversized), "image/png")},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 413, response.text


async def test_upload_prescription_returns_415_for_unsupported_type(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/prescriptions with text/plain file returns 415."""
    token = await register_and_login(async_client)
    response = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files={"file": ("notes.txt", io.BytesIO(b"synthetic text"), "text/plain")},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 415, response.text


async def test_get_prescription_returns_404_for_other_patient(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/prescriptions/{id} returns 404 when the prescription belongs to another patient."""
    token_a = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token_a}"},
    )
    assert create_resp.status_code == 201, create_resp.text
    prescription_id = create_resp.json()["id"]

    token_b = await _register_second_patient(async_client)
    response = await async_client.get(
        f"/api/v1/patients/me/prescriptions/{prescription_id}",
        headers={"Authorization": f"Bearer {token_b}"},
    )
    assert response.status_code == 404, response.text


async def test_list_prescriptions_returns_empty_initially(
    async_client: AsyncClient,
) -> None:
    """GET /patients/me/prescriptions returns empty list for new patient."""
    token = await register_and_login(async_client)
    response = await async_client.get(
        "/api/v1/patients/me/prescriptions",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["prescriptions"] == []
    assert body["total"] == 0


async def test_update_prescription_metadata_returns_200(
    async_client: AsyncClient,
) -> None:
    """PUT /patients/me/prescriptions/{id} updates patient_notes and returns 200."""
    token = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    prescription_id = create_resp.json()["id"]

    response = await async_client.put(
        f"/api/v1/patients/me/prescriptions/{prescription_id}",
        json={"patient_notes": "Synthetic note for testing"},
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["patient_notes"] == "Synthetic note for testing"


async def test_delete_prescription_returns_204_and_removes_from_list(
    async_client: AsyncClient,
) -> None:
    """DELETE /patients/me/prescriptions/{id} returns 204; subsequent GET returns 404."""
    token = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    prescription_id = create_resp.json()["id"]

    del_resp = await async_client.delete(
        f"/api/v1/patients/me/prescriptions/{prescription_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert del_resp.status_code == 204, del_resp.text

    get_resp = await async_client.get(
        f"/api/v1/patients/me/prescriptions/{prescription_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert get_resp.status_code == 404, get_resp.text


async def test_trigger_ocr_returns_202(async_client: AsyncClient) -> None:
    """POST /patients/me/prescriptions/{id}/ocr returns 202 with PROCESSING status."""
    token = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    prescription_id = create_resp.json()["id"]

    ocr_resp = await async_client.post(
        f"/api/v1/patients/me/prescriptions/{prescription_id}/ocr",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert ocr_resp.status_code == 202, ocr_resp.text
    body = ocr_resp.json()
    assert body["ocr_status"] == "PROCESSING"
    assert body["prescription_id"] == prescription_id
    assert "job_id" in body


async def test_trigger_ocr_returns_409_when_already_processing(
    async_client: AsyncClient,
) -> None:
    """POST /patients/me/prescriptions/{id}/ocr returns 409 when already PROCESSING."""
    token = await register_and_login(async_client)
    create_resp = await async_client.post(
        "/api/v1/patients/me/prescriptions",
        files=_png_upload_files(),
        headers={"Authorization": f"Bearer {token}"},
    )
    assert create_resp.status_code == 201
    prescription_id = create_resp.json()["id"]

    # First trigger — should succeed
    first = await async_client.post(
        f"/api/v1/patients/me/prescriptions/{prescription_id}/ocr",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert first.status_code == 202, first.text

    # Second trigger — should fail with 409
    second = await async_client.post(
        f"/api/v1/patients/me/prescriptions/{prescription_id}/ocr",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert second.status_code == 409, second.text
