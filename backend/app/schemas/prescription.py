"""
Pydantic schemas for prescription CRUD and OCR endpoints.
"""

from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel

from app.schemas.medicine import PrescriptionMedicineResponse


class PrescriptionListItem(BaseModel):
    """Lightweight prescription summary used in list responses."""

    id: str
    doctor_id: Optional[str]
    doctor_name: Optional[str]
    prescription_date: Optional[date]
    ocr_status: str
    patient_notes: Optional[str]
    medicine_count: int
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class PrescriptionDetail(BaseModel):
    """Full prescription detail including embedded medicines."""

    id: str
    doctor_id: Optional[str]
    doctor_name: Optional[str]
    prescription_date: Optional[date]
    ocr_status: str
    document_url: Optional[str]
    patient_notes: Optional[str]
    medicines: list[PrescriptionMedicineResponse]
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class CreatePrescriptionResponse(BaseModel):
    """Minimal response returned after a successful prescription upload."""

    id: str
    ocr_status: str
    created_at: datetime

    model_config = {"from_attributes": True}


class UpdatePrescriptionRequest(BaseModel):
    """Payload for updating prescription metadata. All fields are optional."""

    prescription_date: Optional[date] = None
    doctor_id: Optional[str] = None
    patient_notes: Optional[str] = None


class OcrTriggerResponse(BaseModel):
    """Response returned when an OCR job is triggered."""

    prescription_id: str
    ocr_status: str
    job_id: str


class PrescriptionListResponse(BaseModel):
    """Paginated list of prescription summaries."""

    prescriptions: list[PrescriptionListItem]
    total: int
    page: int
    page_size: int
