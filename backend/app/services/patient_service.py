"""
Patient service.

Provides CRUD operations for patient profiles.
All operations are patient-scoped.
"""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.patient import Patient


class PatientService:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def get_by_id(self, patient_id: str) -> Patient | None:
        """Return the Patient with the given id, or None if not found."""
        result = await self._db.execute(
            select(Patient).where(Patient.id == patient_id)
        )
        return result.scalar_one_or_none()

    async def update(self, patient_id: str, display_name: str) -> Patient:
        """
        Update the patient's display_name.

        Returns the updated Patient.
        Callers should raise 404 if get_by_id returns None before calling this.
        """
        patient = await self.get_by_id(patient_id)
        if patient is None:
            # Should not happen if caller guards correctly, but be safe.
            from app.core.errors import raise_http_error
            raise_http_error(404, "PATIENT_NOT_FOUND", "Patient not found.")

        patient.display_name = display_name
        await self._db.commit()
        await self._db.refresh(patient)
        return patient
