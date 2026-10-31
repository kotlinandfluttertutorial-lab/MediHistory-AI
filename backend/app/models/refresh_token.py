"""
RefreshToken ORM model.

Stores hashed refresh tokens to allow explicit invalidation on logout
without requiring a separate token blacklist.

The token_hash column stores a bcrypt hash — never the raw token value.
"""

import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base

if TYPE_CHECKING:
    from app.models.patient import Patient


def _utcnow() -> datetime:
    return datetime.now(tz=UTC)


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    patient_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("patients.id", ondelete="CASCADE"), nullable=False
    )
    # bcrypt hash of the opaque refresh token — never the raw value.
    token_hash: Mapped[str] = mapped_column(String(256), nullable=False, unique=True)
    expires_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    revoked: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=_utcnow
    )

    # Relationships
    patient: Mapped["Patient"] = relationship(
        "Patient", back_populates="refresh_tokens"
    )  # noqa: F821

    __table_args__ = (
        Index("ix_refresh_tokens_patient_revoked", "patient_id", "revoked"),
    )
