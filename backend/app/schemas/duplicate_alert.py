"""
Pydantic schemas for the Duplicate Alerts endpoints.

All schemas use from_attributes=True to support ORM model serialisation.
"""

from __future__ import annotations

from datetime import date, datetime

from pydantic import BaseModel, ConfigDict


class MedicineSummary(BaseModel):
    """Lightweight summary of one side of a duplicate pair."""

    model_config = ConfigDict(from_attributes=True)

    prescription_medicine_id: str
    brand_name: str | None = None
    active_ingredient: str | None = None
    strength: str | None = None
    prescription_id: str
    prescription_date: date | None = None
    doctor_name: str | None = None


class DuplicateAlertResponse(BaseModel):
    """Full representation of a single DuplicateAlert record."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    matched_active_ingredient: str | None = None
    match_type: str
    match_confidence: float
    medicine_a: MedicineSummary | None = None
    medicine_b: MedicineSummary | None = None
    reviewed_by_patient: bool
    reviewed_at: datetime | None = None
    formulation_notes: str | None = None


class DuplicateAlertListResponse(BaseModel):
    """Paginated list of duplicate alerts with summary counts."""

    alerts: list[DuplicateAlertResponse]
    total: int
    unreviewed_count: int
