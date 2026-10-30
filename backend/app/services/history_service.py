"""
HistoryService — business logic for MedicalHistorySummary operations.

Actual generation of the summary content is handled by a background worker
(out of scope for this service). This service creates the DB row and manages
versioning and status.
"""

from __future__ import annotations

import json
import uuid

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import raise_http_error
from app.models.history import MedicalHistorySummary
from app.schemas.history import SummaryContent


class HistoryService:
    """Service class for medical history summary operations."""

    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def generate(self, patient_id: str) -> dict:
        """
        Create a new MedicalHistorySummary row in GENERATING state.

        Checks that no other summary is currently GENERATING for this patient
        (HTTP 409 if so). Sets all previous summaries to is_current=False.

        Returns:
            dict with summary_id, status, and job_id.
        """
        # Check for an in-progress generation
        in_progress_result = await self._db.execute(
            select(MedicalHistorySummary).where(
                MedicalHistorySummary.patient_id == patient_id,
                MedicalHistorySummary.status == "GENERATING",
            )
        )
        if in_progress_result.scalar_one_or_none() is not None:
            raise_http_error(
                409,
                "GENERATION_IN_PROGRESS",
                "A history generation job is already in progress for this patient.",
            )

        # Determine next version number
        max_version_result = await self._db.execute(
            select(MedicalHistorySummary.version_number)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .order_by(MedicalHistorySummary.version_number.desc())
            .limit(1)
        )
        max_version = max_version_result.scalar_one_or_none()
        next_version = (max_version or 0) + 1

        # Mark all existing summaries as not current
        await self._db.execute(
            update(MedicalHistorySummary)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .values(is_current=False)
        )

        # Create the new GENERATING summary
        job_id = str(uuid.uuid4())
        summary = MedicalHistorySummary(
            id=str(uuid.uuid4()),
            patient_id=patient_id,
            version_number=next_version,
            is_current=True,
            status="GENERATING",
            job_id=job_id,
        )
        self._db.add(summary)
        await self._db.commit()
        await self._db.refresh(summary)

        return {
            "summary_id": summary.id,
            "status": summary.status,
            "job_id": summary.job_id,
        }

    async def list_summaries(
        self, patient_id: str
    ) -> tuple[list[MedicalHistorySummary], int]:
        """
        Return all MedicalHistorySummary rows for the patient, newest first.

        Returns:
            Tuple of (summaries, total).
        """
        result = await self._db.execute(
            select(MedicalHistorySummary)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .order_by(MedicalHistorySummary.version_number.desc())
        )
        summaries = list(result.scalars().all())
        return summaries, len(summaries)

    async def get_summary(
        self, summary_id: str, patient_id: str
    ) -> MedicalHistorySummary | None:
        """
        Return a single MedicalHistorySummary, parsing summary_json if present.

        Returns None if not found or if it belongs to another patient.
        """
        result = await self._db.execute(
            select(MedicalHistorySummary).where(
                MedicalHistorySummary.id == summary_id,
                MedicalHistorySummary.patient_id == patient_id,
            )
        )
        summary = result.scalar_one_or_none()

        if summary is None:
            return None

        # Parse summary_json into a SummaryContent if present — store it as a
        # transient attribute so the router can access it without DB coupling.
        if summary.summary_json:
            try:
                parsed = SummaryContent.model_validate(
                    json.loads(summary.summary_json)
                )
                summary._parsed_summary = parsed  # type: ignore[attr-defined]
            except Exception:
                summary._parsed_summary = None  # type: ignore[attr-defined]
        else:
            summary._parsed_summary = None  # type: ignore[attr-defined]

        return summary
