"""
Patient ORM model.

Represents a registered application user / patient.
"""

import uuid
from datetime import UTC, datetime

from sqlalchemy import DateTime, Index, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class Patient(Base):
    __tablename__ = "patients"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    email: Mapped[str] = mapped_column(String(254), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(256), nullable=False)
    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow, onupdate=_utcnow
    )

    # Relationships
    doctors: Mapped[list] = relationship(
        "Doctor", back_populates="patient", cascade="all, delete-orphan"
    )
    prescriptions: Mapped[list] = relationship(
        "Prescription", back_populates="patient", cascade="all, delete-orphan"
    )
    refresh_tokens: Mapped[list] = relationship(
        "RefreshToken", back_populates="patient", cascade="all, delete-orphan"
    )

    __table_args__ = (Index("ix_patients_email", "email"),)
