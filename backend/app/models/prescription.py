"""
Prescription ORM model.

Represents a single prescription document uploaded by a patient.
Source documents are preserved unchanged — OCR output is stored in
related PrescriptionMedicine records.
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import Date, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class Prescription(Base):
    __tablename__ = "prescriptions"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    doctor_id: Mapped[str | None] = mapped_column(
        String(36),
        ForeignKey("doctors.id", ondelete="SET NULL"),
        nullable=True,
    )
    prescription_date: Mapped[datetime | None] = mapped_column(Date, nullable=True)
    # ocr_status values: PENDING | PROCESSING | COMPLETED | FAILED
    ocr_status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="PENDING"
    )
    source_document_path: Mapped[str | None] = mapped_column(
        String(512), nullable=True
    )
    patient_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    # Relationships
    patient: Mapped["Patient"] = relationship("Patient", back_populates="prescriptions")  # noqa: F821
    doctor: Mapped["Doctor | None"] = relationship("Doctor", back_populates="prescriptions")  # noqa: F821
    medicines: Mapped[list] = relationship(
        "PrescriptionMedicine",
        back_populates="prescription",
        cascade="all, delete-orphan",
    )

    __table_args__ = (
        Index("ix_prescriptions_patient_ocr_status", "patient_id", "ocr_status"),
    )
