"""
Patient profile router.

GET /patients/me   — return the authenticated patient's profile.
PUT /patients/me   — update the authenticated patient's display name.

All routes require a valid JWT (Depends(get_current_patient)).
"""

from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.patient import PatientResponse, UpdatePatientRequest
from app.services.patient_service import PatientService

router = APIRouter(prefix="/patients", tags=["patients"])


def get_patient_service(db: AsyncSession = Depends(get_db)) -> PatientService:
    return PatientService(db=db)


@router.get(
    "/me",
    response_model=PatientResponse,
    summary="Get authenticated patient profile",
)
async def get_me(
    patient_id: Annotated[str, Depends(get_current_patient)],
    patient_service: Annotated[PatientService, Depends(get_patient_service)],
) -> PatientResponse:
    """Return the authenticated patient's profile."""
    patient = await patient_service.get_by_id(patient_id)
    if patient is None:
        raise_http_error(404, "PATIENT_NOT_FOUND", "Patient not found.")
    return PatientResponse.model_validate(patient)


@router.put(
    "/me",
    response_model=PatientResponse,
    summary="Update authenticated patient profile",
)
async def update_me(
    request: UpdatePatientRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    patient_service: Annotated[PatientService, Depends(get_patient_service)],
) -> PatientResponse:
    """Update the authenticated patient's display name."""
    patient = await patient_service.get_by_id(patient_id)
    if patient is None:
        raise_http_error(404, "PATIENT_NOT_FOUND", "Patient not found.")
    updated = await patient_service.update(patient_id, request.display_name)
    return PatientResponse.model_validate(updated)
