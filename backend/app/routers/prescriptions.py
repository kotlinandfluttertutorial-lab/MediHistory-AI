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
from pydantic import BaseModel
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
    ProcessReviewRequest,
    ProcessReviewResponse,
    UpdatePrescriptionRequest,
)
from app.services.medicine_service import MedicineService
from app.services.prescription_service import PrescriptionService
from app.services.review_processor_service import ReviewProcessorService

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


def _get_review_processor_service(
    db: AsyncSession = Depends(get_db),
) -> ReviewProcessorService:
    return ReviewProcessorService(db=db)


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
            raise_http_error(
                422, "INVALID_DATE", "prescription_date must be ISO format YYYY-MM-DD."
            )

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
# Process review (server-side PostReviewProcessor — F-Migration)
# ---------------------------------------------------------------------------


@router.post(
    "/{prescription_id}/process-review",
    response_model=ProcessReviewResponse,
    status_code=status.HTTP_200_OK,
    summary="Apply confirmed OCR fields to doctor, prescription date, and medicines",
)
async def process_review(
    prescription_id: str,
    request: ProcessReviewRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    review_service: Annotated[ReviewProcessorService, Depends(_get_review_processor_service)],
) -> ProcessReviewResponse:
    """
    Server-side PostReviewProcessor endpoint.

    After the patient confirms all OCR fields in the app, call this endpoint
    with the reviewed fields. The server will:
    - Resolve/create a Doctor row from confirmed doctor_name
    - Update Prescription.doctor_id and prescription_date
    - Create PrescriptionMedicine rows from confirmed medicine_name fields
    - Run duplicate detection and persist new DuplicateAlert rows
    - Advance ocr_status to COMPLETED

    Medical safety: only REVIEWED fields with a non-null patient_value are used.
    """
    result = await review_service.process(
        prescription_id=prescription_id,
        patient_id=patient_id,
        reviewed_fields=[f.model_dump() for f in request.reviewed_fields],
    )
    if "error" in result:
        raise_http_error(404, "PRESCRIPTION_NOT_FOUND", "Prescription not found.")
    return ProcessReviewResponse(**result)


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


# ---------------------------------------------------------------------------
# Server-side OCR fields extraction (Feature A — Gemini Vision)
# ---------------------------------------------------------------------------


class OcrField(BaseModel):
    field_name: str
    raw_value: Optional[str] = None
    confidence: float = 0.0
    source_reference: Optional[str] = None


class OcrFieldsResponse(BaseModel):
    prescription_id: str
    fields: list[OcrField]


@router.get(
    "/{prescription_id}/ocr/fields",
    response_model=OcrFieldsResponse,
    summary="Run server-side OCR on a prescription document and return extracted fields",
)
async def get_ocr_fields(
    prescription_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[PrescriptionService, Depends(_get_prescription_service)],
) -> OcrFieldsResponse:
    """
    Runs Gemini Vision on the prescription document stored server-side and returns
    structured field extractions.  The Gemini API key lives in the server environment
    (GEMINI_API_KEY) — it is never embedded in the Android APK.

    Returns HTTP 503 when the Gemini SDK is unavailable or the key is not set,
    so the Android app can fall back to on-device ML Kit gracefully.

    Medical safety:
    - Returns raw OCR output only — never infers or fabricates absent fields.
    - Absent fields are represented with raw_value=null, confidence=0.
    - The Android app must put all fields through the patient review flow before use.

    Privacy: document content and file paths are never logged.
    """
    import os as _os
    import json as _json

    # Guard: Gemini SDK must be importable
    try:
        import google.generativeai as genai  # type: ignore[import]
    except ImportError:
        raise_http_error(503, "OCR_UNAVAILABLE", "OCR service not configured on this server.")

    api_key = _os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise_http_error(503, "OCR_UNAVAILABLE", "OCR service not configured on this server.")

    file_path = await service.get_document_path(prescription_id, patient_id)
    if file_path is None:
        raise_http_error(404, "DOCUMENT_NOT_FOUND", "Prescription document not found.")

    import os
    if not os.path.isfile(file_path):
        raise_http_error(404, "DOCUMENT_NOT_FOUND", "Prescription document not found on disk.")

    try:
        # Prepare image for Gemini Vision
        mime = _infer_mime(file_path)
        image_bytes = _read_image_bytes(file_path, mime)
        if image_bytes is None:
            raise_http_error(503, "OCR_UNAVAILABLE", "Could not read prescription document.")

        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(
            model_name="gemini-1.5-flash",
            generation_config={"temperature": 0.1, "max_output_tokens": 1024},
        )

        prompt = _OCR_PROMPT
        response = model.generate_content([
            {"mime_type": mime, "data": image_bytes},
            prompt,
        ])
        raw_text = (response.text or "").strip()
        fields = _parse_ocr_response(raw_text)

    except Exception:
        # Never log exception — may contain document content.
        raise_http_error(503, "OCR_FAILED", "OCR processing failed. Please try again.")

    return OcrFieldsResponse(prescription_id=prescription_id, fields=fields)


_OCR_PROMPT = """
You are an OCR assistant extracting structured data from a prescription image.
Return ONLY a JSON array with these fields. For each field:
- field_name: one of: doctor_name, prescription_date, medicine_name, medicine_name_2,
  medicine_name_3, dose, dose_2, dose_3, frequency, frequency_2, frequency_3
- raw_value: the text exactly as it appears on the prescription, or null if absent/unreadable
- confidence: a number 0.0-1.0 indicating how confident you are in the extraction
- source_reference: "page:1"

Rules:
- Extract the doctor name exactly as written (include "Dr." prefix).
- Extract the prescription date as written (do not convert format).
- Extract each medicine name separately as medicine_name, medicine_name_2, etc.
- If a field is absent or unreadable, use null for raw_value and 0.0 for confidence.
- NEVER infer or guess absent values.
- Return only the JSON array, no other text.

Example:
[
  {"field_name": "doctor_name", "raw_value": "Dr. Smith", "confidence": 0.95, "source_reference": "page:1"},
  {"field_name": "medicine_name", "raw_value": "Amoxicillin 500mg", "confidence": 0.87, "source_reference": "page:1"}
]
"""


def _infer_mime(file_path: str) -> str:
    ext = file_path.rsplit(".", 1)[-1].lower()
    return {
        "jpg": "image/jpeg", "jpeg": "image/jpeg",
        "png": "image/png",
        "pdf": "application/pdf",
    }.get(ext, "image/jpeg")


def _read_image_bytes(file_path: str, mime: str) -> bytes | None:
    """Read file bytes. For PDF, render first page to PNG using PIL if available."""
    import base64
    try:
        if mime == "application/pdf":
            try:
                from PIL import Image
                import io
                # Try pypdfium2 first, then fall back to pdf2image
                try:
                    import pypdfium2 as pdfium  # type: ignore[import]
                    pdf = pdfium.PdfDocument(file_path)
                    page = pdf[0]
                    pil_image = page.render(scale=2).to_pil()
                    buf = io.BytesIO()
                    pil_image.save(buf, format="PNG")
                    return buf.getvalue()
                except ImportError:
                    pass
                # If no PDF renderer available, can't extract
                return None
            except Exception:
                return None
        else:
            with open(file_path, "rb") as f:
                return f.read()
    except OSError:
        return None


def _parse_ocr_response(raw_text: str) -> list[OcrField]:
    """Parse Gemini JSON response into OcrField list. Returns empty list on parse failure."""
    import json
    try:
        # Find JSON array in the response
        start = raw_text.find("[")
        end = raw_text.rfind("]") + 1
        if start < 0 or end <= start:
            return []
        data = json.loads(raw_text[start:end])
        fields = []
        for item in data:
            if not isinstance(item, dict):
                continue
            fields.append(OcrField(
                field_name=str(item.get("field_name", "unknown")),
                raw_value=item.get("raw_value"),
                confidence=float(item.get("confidence", 0.0)),
                source_reference=item.get("source_reference"),
            ))
        return fields
    except Exception:
        return []
