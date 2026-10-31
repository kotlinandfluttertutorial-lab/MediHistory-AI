"""
Doctor service.

Provides CRUD operations for doctor records.
All operations are patient-scoped: a patient can only access their own doctors.
Deletion is soft-delete (is_deleted=True).
"""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.doctor import Doctor
from app.schemas.doctor import CreateDoctorRequest, UpdateDoctorRequest


class DoctorService:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def list_doctors(self, patient_id: str) -> list[Doctor]:
        """Return all non-deleted doctors for the given patient."""
        result = await self._db.execute(
            select(Doctor).where(
                Doctor.patient_id == patient_id,
                Doctor.is_deleted == False,  # noqa: E712
            )
        )
        return list(result.scalars().all())

    async def get(self, doctor_id: str, patient_id: str) -> Doctor | None:
        """
        Return the doctor if it belongs to the given patient and is not deleted.
        Returns None otherwise (caller should raise 404).
        """
        result = await self._db.execute(
            select(Doctor).where(
                Doctor.id == doctor_id,
                Doctor.patient_id == patient_id,
                Doctor.is_deleted == False,  # noqa: E712
            )
        )
        return result.scalar_one_or_none()

    async def create(self, patient_id: str, data: CreateDoctorRequest) -> Doctor:
        """Create a new doctor record for the given patient."""
        doctor = Doctor(
            patient_id=patient_id,
            name=data.name,
            specialty=data.specialty,
            specialty_custom=data.specialty_custom,
            clinic_name=data.clinic_name,
            contact_notes=data.contact_notes,
        )
        self._db.add(doctor)
        await self._db.commit()
        await self._db.refresh(doctor)
        return doctor

    async def update(
        self, doctor_id: str, patient_id: str, data: UpdateDoctorRequest
    ) -> Doctor:
        """
        Update an existing doctor record.

        Only non-None fields in data are applied.
        Returns the updated Doctor. Caller must ensure the doctor exists first.
        """
        doctor = await self.get(doctor_id, patient_id)
        if doctor is None:
            from app.core.errors import raise_http_error

            raise_http_error(404, "DOCTOR_NOT_FOUND", "Doctor not found.")

        if data.name is not None:
            doctor.name = data.name
        if data.specialty is not None:
            doctor.specialty = data.specialty
        if data.specialty_custom is not None:
            doctor.specialty_custom = data.specialty_custom
        if data.clinic_name is not None:
            doctor.clinic_name = data.clinic_name
        if data.contact_notes is not None:
            doctor.contact_notes = data.contact_notes

        await self._db.commit()
        await self._db.refresh(doctor)
        return doctor

    async def delete(self, doctor_id: str, patient_id: str) -> None:
        """
        Soft-delete a doctor by setting is_deleted=True.

        Raises 404 if the doctor is not found or does not belong to the patient.
        """
        doctor = await self.get(doctor_id, patient_id)
        if doctor is None:
            from app.core.errors import raise_http_error

            raise_http_error(404, "DOCTOR_NOT_FOUND", "Doctor not found.")

        doctor.is_deleted = True
        await self._db.commit()
