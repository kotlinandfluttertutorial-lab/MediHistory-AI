"""
Pydantic schemas for medicine list and update endpoints.
"""

from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel


class PrescriptionMedicineResponse(BaseModel):
    """Single medicine extracted from a prescription."""

    id: str
    prescription_id: str
    brand_name: Optional[str]
    active_ingredient: Optional[str]
    active_ingredient_slug: Optional[str]
    strength: Optional[str]
    formulation: Optional[str]
    dosage: Optional[str]
    frequency: Optional[str]
    route: Optional[str]
    duration: Optional[str]
    review_state: str
    medicine_status: str
    confidence_score: Optional[float]
    source_reference: Optional[str]
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class MedicineOccurrence(BaseModel):
    """One occurrence of a medicine across prescriptions."""

    prescription_medicine_id: str
    brand_name: Optional[str]
    strength: Optional[str]
    formulation: Optional[str]
    dosage: Optional[str]
    doctor_name: Optional[str]
    prescription_date: Optional[date]
    prescription_id: str
    medicine_status: str


class MedicineGroup(BaseModel):
    """All occurrences of a medicine grouped by active ingredient."""

    active_ingredient: Optional[str]
    generic_name: Optional[str]
    occurrences: list[MedicineOccurrence]


class MedicineListResponse(BaseModel):
    """Paginated grouped medicine list."""

    medicines: list[MedicineGroup]
    total: int
    page: int
    page_size: int


class UpdateMedicineRequest(BaseModel):
    """Payload for updating a medicine record. All fields are optional."""

    brand_name: Optional[str] = None
    active_ingredient: Optional[str] = None
    strength: Optional[str] = None
    formulation: Optional[str] = None
    dosage: Optional[str] = None
    frequency: Optional[str] = None
    route: Optional[str] = None
    duration: Optional[str] = None
    review_state: Optional[str] = None
    medicine_status: Optional[str] = None
