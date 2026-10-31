"""
Export router — PDF export stubs (MVP: not implemented).

POST /patients/me/export/pdf          — 501 Not Implemented
GET  /patients/me/export/{export_id}  — 501 Not Implemented
GET  /patients/me/export/{export_id}/download — 501 Not Implemented

PDF generation will be implemented in a future milestone.
All routes require a valid JWT (Depends(get_current_patient)).
"""

from typing import Annotated

from fastapi import APIRouter, Depends

from app.core.errors import raise_http_error
from app.core.security import get_current_patient

router = APIRouter(prefix="/patients/me/export", tags=["export"])

_NOT_IMPLEMENTED_MSG = "PDF export is not available in MVP"


@router.post(
    "/pdf",
    summary="Export medical history as PDF (not implemented in MVP)",
    status_code=501,
    response_model=None,
)
async def export_pdf(
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> None:
    raise_http_error(501, "NOT_IMPLEMENTED", _NOT_IMPLEMENTED_MSG)


@router.get(
    "/{export_id}",
    summary="Get export job status (not implemented in MVP)",
    status_code=501,
    response_model=None,
)
async def get_export_status(
    export_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> None:
    raise_http_error(501, "NOT_IMPLEMENTED", _NOT_IMPLEMENTED_MSG)


@router.get(
    "/{export_id}/download",
    summary="Download an exported PDF (not implemented in MVP)",
    status_code=501,
    response_model=None,
)
async def download_export(
    export_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> None:
    raise_http_error(501, "NOT_IMPLEMENTED", _NOT_IMPLEMENTED_MSG)
