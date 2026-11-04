"""
Sync router — upsert endpoints called by the Android SyncWorker.

POST /patients/me/sync/prescriptions  — upsert prescription metadata
POST /patients/me/sync/doctors        — upsert doctor records
POST /patients/me/sync/medicines      — upsert medicine records

All routes require a valid JWT.
All operations are INSERT OR REPLACE (last-write-wins per entity ID).
Cross-patient writes are prevented by scoping every INSERT to patient_id from the JWT.

Privacy: entity IDs and field values are never logged.
"""

from __future__ import annotations

import uuid
from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_patient
from app.models.doctor import Doctor
from app.models.medicine import PrescriptionMedicine
from app.models.prescription import Prescription

router = APIRouter(prefix="/patients/me/sync", tags=["sync"])


# ---------------------------------------------------------------------------
# Schemas
# ---------------------------------------------------------------------------

class SyncPrescriptionItem(BaseModel):
    id: str
    doctor_id: str | None = None
    prescription_date: str | None = None
    notes: str | None = None
    ocr_status: str = "PENDING"
    created_at: int
    updated_at: int


class SyncDoctorItem(BaseModel):
    id: str
    name: str
    specialty: str = "OTHER"
    specialty_other: str | None = None
    clinic_name: str | None = None
    created_at: int
    updated_at: int


class SyncMedicineItem(BaseModel):
    id: str
    prescription_id: str
    medicine_name: str
    active_ingredient: str | None = None
    strength: str | None = None
    dosage_instructions: str | None = None
    status: str = "UNKNOWN"
    created_at: int
    updated_at: int


class SyncPrescriptionsRequest(BaseModel):
    prescriptions: list[SyncPrescriptionItem]


class SyncDoctorsRequest(BaseModel):
    doctors: list[SyncDoctorItem]


class SyncMedicinesRequest(BaseModel):
    medicines: list[SyncMedicineItem]


class SyncResponse(BaseModel):
    synced: int
    skipped: int = 0


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@router.post("/prescriptions", response_model=SyncResponse, summary="Upsert prescriptions from device")
async def sync_prescriptions(
    request: SyncPrescriptionsRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    db: AsyncSession = Depends(get_db),
) -> SyncResponse:
    """
    Insert or update prescriptions from the Android device.
    Each prescription must belong to the authenticated patient.
    Medical safety: we store only metadata — never prescription image content.
    """
    synced = 0
    for item in request.prescriptions:
        result = await db.execute(
            select(Prescription).where(
                Prescription.id == item.id,
                Prescription.patient_id == patient_id,
            )
        )
        existing = result.scalar_one_or_none()

        parsed_date: date | None = None
        if item.prescription_date:
            try:
                parsed_date = date.fromisoformat(item.prescription_date)
            except ValueError:
                pass

        if existing is None:
            prescription = Prescription(
                id=item.id,
                patient_id=patient_id,
                doctor_id=item.doctor_id,
                prescription_date=parsed_date,
                ocr_status=item.ocr_status,
                patient_notes=item.notes,
            )
            db.add(prescription)
        else:
            # Only update if the device version is newer
            if item.updated_at > existing.updated_at.timestamp() * 1000:
                existing.doctor_id = item.doctor_id
                existing.prescription_date = parsed_date
                existing.ocr_status = item.ocr_status
                existing.patient_notes = item.notes

        synced += 1

    await db.commit()
    return SyncResponse(synced=synced)


@router.post("/doctors", response_model=SyncResponse, summary="Upsert doctors from device")
async def sync_doctors(
    request: SyncDoctorsRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    db: AsyncSession = Depends(get_db),
) -> SyncResponse:
    synced = 0
    for item in request.doctors:
        result = await db.execute(
            select(Doctor).where(
                Doctor.id == item.id,
                Doctor.patient_id == patient_id,
            )
        )
        existing = result.scalar_one_or_none()

        if existing is None:
            doctor = Doctor(
                id=item.id,
                patient_id=patient_id,
                name=item.name,
                specialty=item.specialty,
                specialty_custom=item.specialty_other,
                clinic_name=item.clinic_name,
            )
            db.add(doctor)
        else:
            if item.updated_at > existing.updated_at.timestamp() * 1000:
                existing.name = item.name
                existing.specialty = item.specialty
                existing.specialty_custom = item.specialty_other
                existing.clinic_name = item.clinic_name

        synced += 1

    await db.commit()
    return SyncResponse(synced=synced)


@router.post("/medicines", response_model=SyncResponse, summary="Upsert medicines from device")
async def sync_medicines(
    request: SyncMedicinesRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    db: AsyncSession = Depends(get_db),
) -> SyncResponse:
    synced = 0
    for item in request.medicines:
        result = await db.execute(
            select(PrescriptionMedicine).where(
                PrescriptionMedicine.id == item.id,
                PrescriptionMedicine.patient_id == patient_id,
            )
        )
        existing = result.scalar_one_or_none()

        if existing is None:
            medicine = PrescriptionMedicine(
                id=item.id,
                prescription_id=item.prescription_id,
                patient_id=patient_id,
                brand_name=item.medicine_name,
                active_ingredient=item.active_ingredient,
                strength=item.strength,
                dosage=item.dosage_instructions,
                medicine_status=item.status,
            )
            db.add(medicine)
        else:
            if item.updated_at > existing.updated_at.timestamp() * 1000:
                existing.brand_name = item.medicine_name
                existing.active_ingredient = item.active_ingredient
                existing.strength = item.strength
                existing.dosage = item.dosage_instructions
                existing.medicine_status = item.status

        synced += 1

    await db.commit()
    return SyncResponse(synced=synced)
