"""
Pydantic schemas for the Reminders endpoints.

reminder_times is stored as a JSON string in the DB; a field_validator
deserialises it back to a list[str] for API responses.
"""

from __future__ import annotations

import json
from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, field_validator


class ReminderResponse(BaseModel):
    """Full representation of a Reminder record returned by the API."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    patient_id: str
    prescription_medicine_id: str | None = None
    reminder_times: list[str]
    frequency_display: str | None = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    @field_validator("reminder_times", mode="before")
    @classmethod
    def deserialise_reminder_times(cls, v: Any) -> list[str]:
        """Deserialise a JSON string into a list of time strings if needed."""
        if isinstance(v, str):
            return json.loads(v)
        return v


class CreateReminderRequest(BaseModel):
    """Request body for creating a new reminder."""

    prescription_medicine_id: str
    reminder_times: list[str]
    frequency_display: str | None = None

    @field_validator("reminder_times")
    @classmethod
    def reminder_times_not_empty(cls, v: list[str]) -> list[str]:
        if len(v) < 1:
            raise ValueError("reminder_times must contain at least one entry")
        return v


class UpdateReminderRequest(BaseModel):
    """Request body for updating an existing reminder (all fields optional)."""

    is_active: bool | None = None
    reminder_times: list[str] | None = None
    frequency_display: str | None = None


class ReminderListResponse(BaseModel):
    """Paginated list of reminders."""

    reminders: list[ReminderResponse]
    total: int
