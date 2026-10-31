"""
Pydantic schemas for patient profile endpoints.
"""

from datetime import datetime

from pydantic import BaseModel, Field


class PatientResponse(BaseModel):
    """Patient profile returned to the client."""

    id: str
    email: str
    display_name: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class UpdatePatientRequest(BaseModel):
    """Payload for updating a patient's profile."""

    display_name: str = Field(max_length=100)
