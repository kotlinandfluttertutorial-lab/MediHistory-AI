"""
MedicalHistorySummary ORM model.

Stores versioned snapshots of a patient's medical history summary.
Only one version has is_current=True at a time.

status values: GENERATING | COMPLETED | FAILED
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class MedicalHistorySummary(Base):
    __tablename__ = "medical_history_summaries"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    version_number: Mapped[int] = mapped_column(Integer, nullable=False)
    is_current: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    # status: GENERATING | COMPLETED | FAILED
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="GENERATING"
    )
    # Serialised SummaryContent JSON blob.
    summary_json: Mapped[str | None] = mapped_column(Text, nullable=True)
    medicine_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    prescription_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    job_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    generated_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )

    __table_args__ = (
        Index(
            "ix_history_summaries_patient_current",
            "patient_id",
            "is_current",
        ),
    )
