"""
DuplicateAlert ORM model.

Records a potential duplicate medicine pair detected by the duplicate
detection engine.  Alerts are always labelled "Potential duplicate" —
never a clinical conclusion.

match_type values: EXACT | PARTIAL | SYNONYM
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class DuplicateAlert(Base):
    __tablename__ = "duplicate_alerts"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    medicine_a_id: Mapped[str | None] = mapped_column(
        String(36),
        ForeignKey("prescription_medicines.id", ondelete="SET NULL"),
        nullable=True,
    )
    medicine_b_id: Mapped[str | None] = mapped_column(
        String(36),
        ForeignKey("prescription_medicines.id", ondelete="SET NULL"),
        nullable=True,
    )
    matched_active_ingredient: Mapped[str | None] = mapped_column(
        String(200), nullable=True
    )
    match_type: Mapped[str] = mapped_column(String(20), nullable=False)
    match_confidence: Mapped[float] = mapped_column(Float, nullable=False, default=0.0)
    formulation_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    reviewed_by_patient: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False
    )
    reviewed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    __table_args__ = (
        Index(
            "ix_duplicate_alerts_patient_reviewed",
            "patient_id",
            "reviewed_by_patient",
        ),
    )
