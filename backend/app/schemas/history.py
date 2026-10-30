"""
Pydantic schemas for the Medical History endpoints.

All ORM-backed schemas use from_attributes=True.
"""

from __future__ import annotations

from datetime import date, datetime

from pydantic import BaseModel, ConfigDict


class HistorySection(BaseModel):
    """A single named section within a medical history summary."""

    type: str
    label: str
    content: str
    source_refs: list[str] = []


class SourceReference(BaseModel):
    """Back-reference to a specific prescription document."""

    ref_id: str
    prescription_id: str
    prescription_date: date | None = None
    doctor_name: str | None = None


class SummaryContent(BaseModel):
    """The full structured content of a generated summary."""

    sections: list[HistorySection] = []
    source_references: list[SourceReference] = []


class HistorySummaryItem(BaseModel):
    """Lightweight list item returned by GET /history."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    version_number: int
    generated_at: datetime | None = None
    is_current: bool
    medicine_count: int
    prescription_count: int


class HistoryDetail(BaseModel):
    """Full detail of a single history summary, including parsed content."""

    model_config = ConfigDict(from_attributes=True)

    id: str
    version_number: int
    generated_at: datetime | None = None
    is_current: bool
    summary: SummaryContent | None = None


class GenerateHistoryResponse(BaseModel):
    """Response returned when a new history generation job is enqueued."""

    summary_id: str
    status: str
    job_id: str | None = None


class HistoryListResponse(BaseModel):
    """Paginated list of history summary items."""

    summaries: list[HistorySummaryItem]
    total: int
