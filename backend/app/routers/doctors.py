"""
Doctors router.

GET    /patients/me/doctors              — list all doctors for the patient.
POST   /patients/me/doctors              — create a new doctor record.
GET    /patients/me/doctors/{doctor_id}  — get a single doctor.
PUT    /patients/me/doctors/{doctor_id}  — update a doctor.
DELETE /patients/me/doctors/{doctor_id}  — soft-delete a doctor.

All routes require a valid JWT (Depends(get_current_patient)).
Cross-patient access returns 404 (not 403) to avoid leaking resource existence.
"""

from typing import Annotated

from fastapi import APIRouter, Depends, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.doctor import (
    CreateDoctorRequest,
    DoctorListResponse,
    DoctorResponse,
    UpdateDoctorRequest,
)
from app.services.doctor_service import DoctorService

router = APIRouter(prefix="/patients/me/doctors", tags=["doctors"])


def get_doctor_service(db: AsyncSession = Depends(get_db)) -> DoctorService:
    return DoctorService(db=db)


@router.get(
    "",
    response_model=DoctorListResponse,
    summary="List all doctors for the authenticated patient",
)
async def list_doctors(
    patient_id: Annotated[str, Depends(get_current_patient)],
    doctor_service: Annotated[DoctorService, Depends(get_doctor_service)],
) -> DoctorListResponse:
    """Return all non-deleted doctors belonging to the authenticated patient."""
    doctors = await doctor_service.list_doctors(patient_id)
    return DoctorListResponse(
        doctors=[DoctorResponse.model_validate(d) for d in doctors],
        total=len(doctors),
    )


@router.post(
    "",
    response_model=DoctorResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create a doctor record",
)
async def create_doctor(
    request: CreateDoctorRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    doctor_service: Annotated[DoctorService, Depends(get_doctor_service)],
) -> DoctorResponse:
    """Create a new doctor record for the authenticated patient."""
    doctor = await doctor_service.create(patient_id, request)
    return DoctorResponse.model_validate(doctor)


@router.get(
    "/{doctor_id}",
    response_model=DoctorResponse,
    summary="Get a single doctor record",
)
async def get_doctor(
    doctor_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    doctor_service: Annotated[DoctorService, Depends(get_doctor_service)],
) -> DoctorResponse:
    """Return a specific doctor record. Returns 404 if not owned by the patient."""
    doctor = await doctor_service.get(doctor_id, patient_id)
    if doctor is None:
        raise_http_error(404, "DOCTOR_NOT_FOUND", "Doctor not found.")
    return DoctorResponse.model_validate(doctor)


@router.put(
    "/{doctor_id}",
    response_model=DoctorResponse,
    summary="Update a doctor record",
)
async def update_doctor(
    doctor_id: str,
    request: UpdateDoctorRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    doctor_service: Annotated[DoctorService, Depends(get_doctor_service)],
) -> DoctorResponse:
    """Update a doctor record. Returns 404 if not owned by the patient."""
    doctor = await doctor_service.update(doctor_id, patient_id, request)
    return DoctorResponse.model_validate(doctor)


@router.delete(
    "/{doctor_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a doctor record",
)
async def delete_doctor(
    doctor_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    doctor_service: Annotated[DoctorService, Depends(get_doctor_service)],
) -> Response:
    """Soft-delete a doctor record. Returns 404 if not owned by the patient."""
    await doctor_service.delete(doctor_id, patient_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
