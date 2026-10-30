"""
Prescriptions router.

GET    /patients/me/prescriptions                                 — list prescriptions
POST   /patients/me/prescriptions                                 — upload new prescription
GET    /patients/me/prescriptions/{prescription_id}               — get single prescription
GET    /patients/me/prescriptions/{prescription_id}/document      — stream source document
PUT    /patients/me/prescriptions/{prescription_id}               — update metadata
DELETE /patients/me/prescriptions/{prescription_id}               — delete
POST   /patients/me/prescriptions/{prescription_id}/ocr           — trigger OCR
PUT    /patients/me/prescriptions/{prescription_id}/medicines/{medicine_id} — update medicine

All routes require a valid JWT (Depends(get_current_patient)).
Cross-patient access returns 404 (not 403) to avoid leaking resource existence.
"""

from typing import Annotated, Optional

from fastapi import APIRouter, Depends, File, Form, Response, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.medicine import PrescriptionMedicineResponse, UpdateMedicineRequest
from app.schemas.prescription import (
    CreatePrescriptionResponse,
    OcrTriggerResponse,
    PrescriptionDetail,
    PrescriptionListItem,
    PrescriptionListResponse,
    UpdatePrescriptionRequest,
)
from app.services.medicine_service import MedicineService
from app.services.prescription_service import PrescriptionService

router = APIRouter(prefix="/patients/me/prescriptions", tags=["prescriptions"])


def _get_prescription_service(
    db: AsyncSession = Depends(get_db),
) -> PrescriptionService:
    settings = get_settings()
    return PrescriptionService(db=db, upload_dir=settings.UPLOAD_DIR)


def _get_medicine_service(
    db: AsyncSession = Depends(get_db),
) -> MedicineService:
    return MedicineService(db=db)


# ---------------------------------------------------------------------------
# List
# ---------------------------------------------------------------------------


@router.get(
    "",
    response_model=PrescriptionListResponse,
    summary="List prescriptions for the authenticated patient",
)
async def list_prescriptions(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
    page: int = 1,
    page_size: int = 20,
    ocr_status: Optional[str] = None,
    doctor_id: Optional[str] = None,
) -> PrescriptionListResponse:
    items, total = await service.list_prescriptions(
        patient_id=patient_id,
        page=page,
        page_size=page_size,
        ocr_status_filter=ocr_status,
        doctor_id_filter=doctor_id,
    )

    prescription_items = []
    for p in items:
        doctor_name = p.doctor.name if p.doctor else None
        medicine_count = len(p.medicines) if p.medicines else 0
        prescription_items.append(
            PrescriptionListItem(
                id=p.id,
                doctor_id=p.doctor_id,
                doctor_name=doctor_name,
                prescription_date=p.prescription_date,
                ocr_status=p.ocr_status,
                patient_notes=p.patient_notes,
                medicine_count=medicine_count,
                created_at=p.created_at,
                updated_at=p.updated_at,
            )
        )

    return PrescriptionListResponse(
        prescriptions=prescription_items,
        total=total,
        page=page,
        page_size=page_size,
    )


# ---------------------------------------------------------------------------
# Upload / create
# ---------------------------------------------------------------------------


@router.post(
    "",
    response_model=CreatePrescriptionResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Upload a new prescription document",
)
async def upload_prescription(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
    file: UploadFile = File(...),
    prescription_date: Optional[str] = Form(default=None),
    doctor_id: Optional[str] = Form(default=None),
    patient_notes: Optional[str] = Form(default=None),
) -> CreatePrescriptionResponse:
    """
    Upload a prescription document (image/jpeg, image/png, or application/pdf).
    Returns 413 if the file exceeds the size limit.
    Returns 415 if the MIME type is not supported.
    """
    from datetime import date

    file_bytes = await file.read()
    content_type = file.content_type or ""
    filename = file.filename or "upload"

    # Parse optional prescription_date string
    parsed_date: Optional[date] = None
    if prescription_date:
        try:
            parsed_date = date.fromisoformat(prescription_date)
        except ValueError:
            raise_http_error(422, "INVALID_DATE", "prescription_date must be ISO format YYYY-MM-DD.")

    prescription = await service.create(
        patient_id=patient_id,
        file_bytes=file_bytes,
        filename=filename,
        content_type=content_type,
        prescription_date=parsed_date,
        doctor_id=doctor_id,
        patient_notes=patient_notes,
    )

    return CreatePrescriptionResponse(
        id=prescription.id,
        ocr_status=prescription.ocr_status,
        created_at=prescription.created_at,
    )


# ---------------------------------------------------------------------------
# Get single
# ---------------------------------------------------------------------------


@router.get(
    "/{prescription_id}",
    response_model=PrescriptionDetail,
    summary="Get a single prescription with medicines",
)
async def get_prescription(
    prescription_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> PrescriptionDetail:
    prescription = await service.get(prescription_id, patient_id)
    if prescription is None:
        raise_http_error(404, "PRESCRIPTION_NOT_FOUND", "Prescription not found.")

    from app.schemas.medicine import PrescriptionMedicineResponse

    doctor_name = prescription.doctor.name if prescription.doctor else None
    medicines = [
        PrescriptionMedicineResponse.model_validate(m)
        for m in (prescription.medicines or [])
    ]
    return PrescriptionDetail(
        id=prescription.id,
        doctor_id=prescription.doctor_id,
        doctor_name=doctor_name,
        prescription_date=prescription.prescription_date,
        ocr_status=prescription.ocr_status,
        document_url=None,  # URL generation deferred; path is app-internal
        patient_notes=prescription.patient_notes,
        medicines=medicines,
        created_at=prescription.created_at,
        updated_at=prescription.updated_at,
    )


# ---------------------------------------------------------------------------
# Stream document
# ---------------------------------------------------------------------------


@router.get(
    "/{prescription_id}/document",
    summary="Stream the original prescription document",
)
async def get_document(
    prescription_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> FileResponse:
    import os

    file_path = await service.get_document_path(prescription_id, patient_id)
    if file_path is None or not os.path.isfile(file_path):
        raise_http_error(404, "DOCUMENT_NOT_FOUND", "Document not found.")
    return FileResponse(file_path)


# ---------------------------------------------------------------------------
# Update metadata
# ---------------------------------------------------------------------------


@router.put(
    "/{prescription_id}",
    response_model=PrescriptionDetail,
    summary="Update prescription metadata",
)
async def update_prescription(
    prescription_id: str,
    request: UpdatePrescriptionRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> PrescriptionDetail:
    prescription = await service.update(prescription_id, patient_id, request)

    from app.schemas.medicine import PrescriptionMedicineResponse

    doctor_name = prescription.doctor.name if prescription.doctor else None
    medicines = [
        PrescriptionMedicineResponse.model_validate(m)
        for m in (prescription.medicines or [])
    ]
    return PrescriptionDetail(
        id=prescription.id,
        doctor_id=prescription.doctor_id,
        doctor_name=doctor_name,
        prescription_date=prescription.prescription_date,
        ocr_status=prescription.ocr_status,
        document_url=None,
        patient_notes=prescription.patient_notes,
        medicines=medicines,
        created_at=prescription.created_at,
        updated_at=prescription.updated_at,
    )


# ---------------------------------------------------------------------------
# Delete
# ---------------------------------------------------------------------------


@router.delete(
    "/{prescription_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a prescription",
)
async def delete_prescription(
    prescription_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> Response:
    await service.delete(prescription_id, patient_id)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


# ---------------------------------------------------------------------------
# Trigger OCR
# ---------------------------------------------------------------------------


@router.post(
    "/{prescription_id}/ocr",
    response_model=OcrTriggerResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Trigger OCR processing for a prescription",
)
async def trigger_ocr(
    prescription_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> OcrTriggerResponse:
    result = await service.trigger_ocr(prescription_id, patient_id)
    return OcrTriggerResponse(**result)


# ---------------------------------------------------------------------------
# Update medicine (via prescriptions router)
# ---------------------------------------------------------------------------


@router.put(
    "/{prescription_id}/medicines/{medicine_id}",
    response_model=PrescriptionMedicineResponse,
    summary="Update a medicine extracted from a prescription",
)
async def update_medicine(
    prescription_id: str,
    medicine_id: str,
    request: UpdateMedicineRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    medicine_service: Annotated[MedicineService, Depends(_get_medicine_service)],
) -> PrescriptionMedicineResponse:
    medicine = await medicine_service.update(
        medicine_id=medicine_id,
        patient_id=patient_id,
        data=request,
    )
    return PrescriptionMedicineResponse.model_validate(medicine)
