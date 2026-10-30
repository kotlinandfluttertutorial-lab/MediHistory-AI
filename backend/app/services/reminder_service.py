"""
ReminderService — business logic for Reminder CRUD operations.

reminder_times is serialised to/from a JSON string in the database.
"""

from __future__ import annotations

import json
import uuid

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import raise_http_error
from app.models.reminder import Reminder
from app.schemas.reminder import CreateReminderRequest, UpdateReminderRequest


class ReminderService:
    """Service class for reminder operations."""

    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def list_reminders(
        self, patient_id: str
    ) -> tuple[list[Reminder], int]:
        """
        Return all Reminder rows for the patient.

        Returns:
            Tuple of (reminders, total).
        """
        result = await self._db.execute(
            select(Reminder)
            .where(Reminder.patient_id == patient_id)
            .order_by(Reminder.created_at.desc())
        )
        reminders = list(result.scalars().all())
        return reminders, len(reminders)

    async def create(
        self, patient_id: str, data: CreateReminderRequest
    ) -> Reminder:
        """
        Create a new Reminder row, serialising reminder_times to JSON.

        Returns:
            The newly created Reminder ORM object.
        """
        reminder = Reminder(
            id=str(uuid.uuid4()),
            patient_id=patient_id,
            prescription_medicine_id=data.prescription_medicine_id,
            reminder_times=json.dumps(data.reminder_times),
            frequency_display=data.frequency_display,
            is_active=True,
        )
        self._db.add(reminder)
        await self._db.commit()
        await self._db.refresh(reminder)
        return reminder

    async def update(
        self,
        reminder_id: str,
        patient_id: str,
        data: UpdateReminderRequest,
    ) -> Reminder:
        """
        Update an existing Reminder.

        Raises:
            HTTP 404 if the reminder is not found or belongs to another patient.
        """
        result = await self._db.execute(
            select(Reminder).where(
                Reminder.id == reminder_id,
                Reminder.patient_id == patient_id,
            )
        )
        reminder = result.scalar_one_or_none()
        if reminder is None:
            raise_http_error(404, "REMINDER_NOT_FOUND", "Reminder not found.")

        if data.is_active is not None:
            reminder.is_active = data.is_active
        if data.reminder_times is not None:
            reminder.reminder_times = json.dumps(data.reminder_times)
        if data.frequency_display is not None:
            reminder.frequency_display = data.frequency_display

        await self._db.commit()
        await self._db.refresh(reminder)
        return reminder

    async def delete(self, reminder_id: str, patient_id: str) -> None:
        """
        Delete a Reminder row after verifying ownership.

        Raises:
            HTTP 404 if the reminder is not found or belongs to another patient.
        """
        result = await self._db.execute(
            select(Reminder).where(
                Reminder.id == reminder_id,
                Reminder.patient_id == patient_id,
            )
        )
        reminder = result.scalar_one_or_none()
        if reminder is None:
            raise_http_error(404, "REMINDER_NOT_FOUND", "Reminder not found.")

        await self._db.delete(reminder)
        await self._db.commit()
