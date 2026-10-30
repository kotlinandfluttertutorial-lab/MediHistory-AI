"""
Reminder ORM model.

Represents a medication reminder for a patient.
reminder_times is a JSON-encoded list of 'HH:MM' strings.
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class Reminder(Base):
    __tablename__ = "reminders"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    prescription_medicine_id: Mapped[str | None] = mapped_column(
        String(36),
        ForeignKey("prescription_medicines.id", ondelete="SET NULL"),
        nullable=True,
    )
    # JSON-encoded list of 'HH:MM' strings, e.g. '["08:00", "20:00"]'.
    reminder_times: Mapped[str] = mapped_column(Text, nullable=False)
    frequency_display: Mapped[str | None] = mapped_column(String(100), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    __table_args__ = (Index("ix_reminders_patient_active", "patient_id", "is_active"),)
