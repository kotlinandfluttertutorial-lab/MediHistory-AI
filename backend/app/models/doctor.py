"""
Doctor ORM model.

Represents a doctor associated with a patient's prescription history.
Each doctor record is patient-scoped; a patient can never access another
patient's doctors.
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class Doctor(Base):
    __tablename__ = "doctors"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    specialty: Mapped[str | None] = mapped_column(String(100), nullable=True)
    specialty_custom: Mapped[str | None] = mapped_column(String(200), nullable=True)
    clinic_name: Mapped[str | None] = mapped_column(String(200), nullable=True)
    contact_notes: Mapped[str | None] = mapped_column(Text, nullable=True)
    is_deleted: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    # Relationships
    patient: Mapped["Patient"] = relationship("Patient", back_populates="doctors")  # noqa: F821
    prescriptions: Mapped[list] = relationship(
        "Prescription", back_populates="doctor"
    )

    __table_args__ = (Index("ix_doctors_patient_deleted", "patient_id", "is_deleted"),)
