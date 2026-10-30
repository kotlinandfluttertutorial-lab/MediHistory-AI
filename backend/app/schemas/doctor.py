"""
Pydantic schemas for doctor CRUD endpoints.
"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class DoctorResponse(BaseModel):
    """Doctor record returned to the client."""

    id: str
    patient_id: str
    name: str
    specialty: Optional[str]
    specialty_custom: Optional[str]
    clinic_name: Optional[str]
    contact_notes: Optional[str]
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class CreateDoctorRequest(BaseModel):
    """Payload for creating a new doctor record."""

    name: str = Field(max_length=200)
    specialty: Optional[str] = Field(default=None, max_length=100)
    specialty_custom: Optional[str] = Field(default=None, max_length=200)
    clinic_name: Optional[str] = Field(default=None, max_length=200)
    contact_notes: Optional[str] = None


class UpdateDoctorRequest(BaseModel):
    """Payload for updating an existing doctor record. All fields are optional."""

    name: Optional[str] = Field(default=None, max_length=200)
    specialty: Optional[str] = Field(default=None, max_length=100)
    specialty_custom: Optional[str] = Field(default=None, max_length=200)
    clinic_name: Optional[str] = Field(default=None, max_length=200)
    contact_notes: Optional[str] = None


class DoctorListResponse(BaseModel):
    """Paginated list of doctor records."""

    doctors: list[DoctorResponse]
    total: int
