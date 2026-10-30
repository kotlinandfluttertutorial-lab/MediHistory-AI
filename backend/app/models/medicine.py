"""
PrescriptionMedicine ORM model.

Represents a single medicine extracted from a prescription via OCR.
Each field carries a review_state and confidence_score so the patient
can confirm, edit, or reject OCR output.

review_state values: PENDING_REVIEW | REVIEWED | REJECTED
medicine_status values: CURRENT | PAST | UNKNOWN
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import DateTime, Float, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class PrescriptionMedicine(Base):
    __tablename__ = "prescription_medicines"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    prescription_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("prescriptions.id", ondelete="CASCADE"),
        nullable=False,
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    brand_name: Mapped[str | None] = mapped_column(String(200), nullable=True)
    active_ingredient: Mapped[str | None] = mapped_column(String(200), nullable=True)
    # Lowercased + slugified active ingredient for grouping and duplicate detection.
    active_ingredient_slug: Mapped[str | None] = mapped_column(
        String(200), nullable=True
    )
    strength: Mapped[str | None] = mapped_column(String(100), nullable=True)
    formulation: Mapped[str | None] = mapped_column(String(100), nullable=True)
    dosage: Mapped[str | None] = mapped_column(Text, nullable=True)
    frequency: Mapped[str | None] = mapped_column(String(100), nullable=True)
    route: Mapped[str | None] = mapped_column(String(100), nullable=True)
    duration: Mapped[str | None] = mapped_column(String(100), nullable=True)
    review_state: Mapped[str] = mapped_column(
        String(20), nullable=False, default="PENDING_REVIEW"
    )
    medicine_status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="UNKNOWN"
    )
    confidence_score: Mapped[float | None] = mapped_column(Float, nullable=True)
    raw_value: Mapped[str | None] = mapped_column(Text, nullable=True)
    source_reference: Mapped[str | None] = mapped_column(String(512), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    # Relationships
    prescription: Mapped["Prescription"] = relationship(  # noqa: F821
        "Prescription", back_populates="medicines"
    )

    __table_args__ = (
        Index(
            "ix_medicines_patient_ingredient_status",
            "patient_id",
            "active_ingredient_slug",
            "medicine_status",
        ),
    )
