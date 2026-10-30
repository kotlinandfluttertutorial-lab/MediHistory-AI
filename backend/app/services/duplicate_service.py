"""
DuplicateService — business logic for DuplicateAlert operations.

All queries are patient-scoped; cross-patient access is never possible
through this service layer.
"""

from __future__ import annotations

from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import raise_http_error
from app.models.duplicate_alert import DuplicateAlert


class DuplicateService:
    """Service class for duplicate alert CRUD and review operations."""

    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def list_alerts(
        self,
        patient_id: str,
        reviewed_filter: bool | None,
        page: int,
        page_size: int,
    ) -> tuple[list[DuplicateAlert], int, int]:
        """
        Return a page of DuplicateAlert rows for the patient, the total count,
        and the count of unreviewed alerts.

        Args:
            patient_id:      Authenticated patient's ID (enforces data ownership).
            reviewed_filter: If set, filters by reviewed_by_patient value.
            page:            1-based page number.
            page_size:       Number of results per page.

        Returns:
            Tuple of (alerts, total, unreviewed_count).
        """
        base_q = select(DuplicateAlert).where(
            DuplicateAlert.patient_id == patient_id
        )
        if reviewed_filter is not None:
            base_q = base_q.where(
                DuplicateAlert.reviewed_by_patient == reviewed_filter
            )

        count_result = await self._db.execute(
            select(func.count()).select_from(base_q.subquery())
        )
        total: int = count_result.scalar_one()

        unreviewed_result = await self._db.execute(
            select(func.count()).where(
                DuplicateAlert.patient_id == patient_id,
                DuplicateAlert.reviewed_by_patient == False,  # noqa: E712
            )
        )
        unreviewed_count: int = unreviewed_result.scalar_one()

        offset = (page - 1) * page_size
        page_q = base_q.order_by(DuplicateAlert.created_at.desc()).offset(offset).limit(page_size)
        result = await self._db.execute(page_q)
        alerts = list(result.scalars().all())

        return alerts, total, unreviewed_count

    async def get_alert(
        self, alert_id: str, patient_id: str
    ) -> DuplicateAlert | None:
        """
        Return a single DuplicateAlert if it belongs to the patient.

        Returns None if the alert does not exist or belongs to another patient.
        Cross-patient queries always return None so that the router can raise
        HTTP 404 without leaking resource existence.
        """
        result = await self._db.execute(
            select(DuplicateAlert).where(
                DuplicateAlert.id == alert_id,
                DuplicateAlert.patient_id == patient_id,
            )
        )
        return result.scalar_one_or_none()

    async def mark_reviewed(
        self, alert_id: str, patient_id: str
    ) -> DuplicateAlert:
        """
        Mark a DuplicateAlert as reviewed by the patient.

        Raises:
            HTTP 404 if the alert is not found or belongs to another patient.
            HTTP 409 if the alert is already marked as reviewed.
        """
        alert = await self.get_alert(alert_id, patient_id)
        if alert is None:
            raise_http_error(404, "ALERT_NOT_FOUND", "Duplicate alert not found.")

        if alert.reviewed_by_patient:
            raise_http_error(
                409,
                "ALREADY_REVIEWED",
                "This duplicate alert has already been marked as reviewed.",
            )

        alert.reviewed_by_patient = True
        alert.reviewed_at = datetime.now(tz=UTC)
        await self._db.commit()
        await self._db.refresh(alert)
        return alert
