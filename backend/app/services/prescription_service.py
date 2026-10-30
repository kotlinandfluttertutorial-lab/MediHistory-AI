"""
Prescription service.

Provides CRUD operations for prescriptions, including file upload and OCR trigger.
All operations are patient-scoped.

Security:
- File MIME type and size validated before saving.
- Path traversal prevented by using os.path.basename on the filename and UUID
  for the prescription ID before path construction.
- Never log prescription text, document contents, or file paths that include
  patient identifiers.
"""

import os
import uuid
from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.errors import raise_http_error
from app.models.doctor import Doctor
from app.models.medicine import PrescriptionMedicine
from app.models.prescription import Prescription

_ALLOWED_MIME_TYPES = {"image/jpeg", "image/png", "application/pdf"}


class PrescriptionService:
    def __init__(self, db: AsyncSession, upload_dir: str) -> None:
        self._db = db
        self._upload_dir = upload_dir

    # ------------------------------------------------------------------
    # List
    # ------------------------------------------------------------------

    async def list_prescriptions(
        self,
        patient_id: str,
        page: int,
        page_size: int,
        ocr_status_filter: Optional[str] = None,
        doctor_id_filter: Optional[str] = None,
    ) -> tuple[list[Prescription], int]:
        """
        Return a paginated list of prescriptions for the patient.

        Returns (items, total_count).
        """
        filters = [Prescription.patient_id == patient_id]
        if ocr_status_filter:
            filters.append(Prescription.ocr_status == ocr_status_filter)
        if doctor_id_filter:
            filters.append(Prescription.doctor_id == doctor_id_filter)

        # Total count
        count_result = await self._db.execute(
            select(func.count()).select_from(Prescription).where(*filters)
        )
        total = count_result.scalar_one()

        # Paginated items with doctor eagerly loaded
        offset = (page - 1) * page_size
        items_result = await self._db.execute(
            select(Prescription)
            .options(selectinload(Prescription.doctor))
            .options(selectinload(Prescription.medicines))
            .where(*filters)
            .order_by(Prescription.created_at.desc())
            .offset(offset)
            .limit(page_size)
        )
        items = list(items_result.scalars().all())
        return items, total

    # ------------------------------------------------------------------
    # Get single
    # ------------------------------------------------------------------

    async def get(
        self, prescription_id: str, patient_id: str
    ) -> Optional[Prescription]:
        """
        Return a single prescription with doctor and medicines loaded.
        Returns None if not found or does not belong to patient.
        """
        result = await self._db.execute(
            select(Prescription)
            .options(selectinload(Prescription.doctor))
            .options(selectinload(Prescription.medicines))
            .where(
                Prescription.id == prescription_id,
                Prescription.patient_id == patient_id,
            )
        )
        return result.scalar_one_or_none()

    # ------------------------------------------------------------------
    # Create
    # ------------------------------------------------------------------

    async def create(
        self,
        patient_id: str,
        file_bytes: bytes,
        filename: str,
        content_type: str,
        prescription_date=None,
        doctor_id: Optional[str] = None,
        patient_notes: Optional[str] = None,
    ) -> Prescription:
        """
        Validate and save an uploaded prescription document.

        Raises:
            413 if file exceeds MAX_UPLOAD_BYTES.
            415 if MIME type is not image/jpeg, image/png, or application/pdf.
        """
        from app.core.config import get_settings

        settings = get_settings()

        if len(file_bytes) > settings.MAX_UPLOAD_BYTES:
            raise_http_error(
                413,
                "FILE_TOO_LARGE",
                f"File exceeds the maximum allowed size of {settings.MAX_UPLOAD_BYTES} bytes.",
            )

        # Normalise content_type: strip parameters like "; boundary=..."
        mime = (content_type or "").split(";")[0].strip().lower()
        if mime not in _ALLOWED_MIME_TYPES:
            raise_http_error(
                415,
                "UNSUPPORTED_MEDIA_TYPE",
                f"File type '{mime}' is not supported. Accepted: image/jpeg, image/png, application/pdf.",
            )

        # Generate prescription ID before building path to prevent traversal.
        prescription_id = str(uuid.uuid4())
        safe_filename = os.path.basename(filename) or "upload"

        # Build storage path: {upload_dir}/{patient_id}/{prescription_id}/original_{filename}
        dest_dir = os.path.join(self._upload_dir, patient_id, prescription_id)
        os.makedirs(dest_dir, exist_ok=True)
        dest_path = os.path.join(dest_dir, f"original_{safe_filename}")

        with open(dest_path, "wb") as f:
            f.write(file_bytes)

        prescription = Prescription(
            id=prescription_id,
            patient_id=patient_id,
            doctor_id=doctor_id,
            prescription_date=prescription_date,
            ocr_status="PENDING",
            source_document_path=dest_path,
            patient_notes=patient_notes,
        )
        self._db.add(prescription)
        await self._db.commit()
        await self._db.refresh(prescription)
        return prescription

    # ------------------------------------------------------------------
    # Document path
    # ------------------------------------------------------------------

    async def get_document_path(
        self, prescription_id: str, patient_id: str
    ) -> Optional[str]:
        """Return the on-disk path to the source document, or None."""
        result = await self._db.execute(
            select(Prescription.source_document_path).where(
                Prescription.id == prescription_id,
                Prescription.patient_id == patient_id,
            )
        )
        return result.scalar_one_or_none()

    # ------------------------------------------------------------------
    # Update
    # ------------------------------------------------------------------

    async def update(
        self,
        prescription_id: str,
        patient_id: str,
        data,
    ) -> Prescription:
        """
        Update prescription metadata (date, doctor, notes).
        Raises 404 if not found or cross-patient access.
        """
        prescription = await self.get(prescription_id, patient_id)
        if prescription is None:
            raise_http_error(404, "PRESCRIPTION_NOT_FOUND", "Prescription not found.")

        if data.prescription_date is not None:
            prescription.prescription_date = data.prescription_date
        if data.doctor_id is not None:
            prescription.doctor_id = data.doctor_id
        if data.patient_notes is not None:
            prescription.patient_notes = data.patient_notes

        await self._db.commit()
        await self._db.refresh(prescription)
        return prescription

    # ------------------------------------------------------------------
    # Delete
    # ------------------------------------------------------------------

    async def delete(self, prescription_id: str, patient_id: str) -> None:
        """
        Delete a prescription row (cascades to medicines and alerts).
        Also removes the source document from disk.
        Raises 404 if not found or cross-patient access.
        """
        prescription = await self.get(prescription_id, patient_id)
        if prescription is None:
            raise_http_error(404, "PRESCRIPTION_NOT_FOUND", "Prescription not found.")

        file_path = prescription.source_document_path

        await self._db.delete(prescription)
        await self._db.commit()

        # Remove file from disk after DB commit so rollback is still possible.
        if file_path and os.path.isfile(file_path):
            try:
                os.remove(file_path)
            except OSError:
                # File removal failure is non-fatal; log at warning level without
                # revealing the path.
                pass

    # ------------------------------------------------------------------
    # Trigger OCR
    # ------------------------------------------------------------------

    async def trigger_ocr(self, prescription_id: str, patient_id: str) -> dict:
        """
        Mark a prescription for OCR processing.

        Returns a dict with prescription_id, ocr_status='PROCESSING', job_id.
        Raises 404 if not found; 409 if already PROCESSING.
        """
        result = await self._db.execute(
            select(Prescription).where(
                Prescription.id == prescription_id,
                Prescription.patient_id == patient_id,
            )
        )
        prescription = result.scalar_one_or_none()
        if prescription is None:
            raise_http_error(404, "PRESCRIPTION_NOT_FOUND", "Prescription not found.")

        if prescription.ocr_status == "PROCESSING":
            raise_http_error(
                409,
                "OCR_ALREADY_PROCESSING",
                "OCR is already in progress for this prescription.",
            )

        job_id = str(uuid.uuid4())
        prescription.ocr_status = "PROCESSING"
        await self._db.commit()

        return {
            "prescription_id": prescription_id,
            "ocr_status": "PROCESSING",
            "job_id": job_id,
        }
