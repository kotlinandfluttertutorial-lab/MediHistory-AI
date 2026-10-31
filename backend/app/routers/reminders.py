"""
Reminders router.

GET    /patients/me/reminders              — list all reminders
POST   /patients/me/reminders              — create a reminder
PUT    /patients/me/reminders/{id}         — update a reminder
DELETE /patients/me/reminders/{id}         — delete a reminder

All routes require a valid JWT (Depends(get_current_patient)).
"""

from typing import Annotated

from fastapi import APIRouter, Depends, Response
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_patient
from app.schemas.reminder import (
    CreateReminderRequest,
    ReminderListResponse,
    ReminderResponse,
    UpdateReminderRequest,
)
from app.services.reminder_service import ReminderService

router = APIRouter(prefix="/patients/me/reminders", tags=["reminders"])


def _get_reminder_service(
    db: AsyncSession = Depends(get_db),
) -> ReminderService:
    return ReminderService(db=db)


@router.get(
    "",
    response_model=ReminderListResponse,
    summary="List all medication reminders",
)
async def list_reminders(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[ReminderService, Depends(_get_reminder_service)],
) -> ReminderListResponse:
    reminders, total = await service.list_reminders(patient_id=patient_id)
    return ReminderListResponse(
        reminders=[ReminderResponse.model_validate(r) for r in reminders],
        total=total,
    )


@router.post(
    "",
    response_model=ReminderResponse,
    status_code=201,
    summary="Create a medication reminder",
)
async def create_reminder(
    body: CreateReminderRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[ReminderService, Depends(_get_reminder_service)],
) -> ReminderResponse:
    reminder = await service.create(patient_id=patient_id, data=body)
    return ReminderResponse.model_validate(reminder)


@router.put(
    "/{reminder_id}",
    response_model=ReminderResponse,
    summary="Update a medication reminder",
)
async def update_reminder(
    reminder_id: str,
    body: UpdateReminderRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[ReminderService, Depends(_get_reminder_service)],
) -> ReminderResponse:
    reminder = await service.update(
        reminder_id=reminder_id, patient_id=patient_id, data=body
    )
    return ReminderResponse.model_validate(reminder)


@router.delete(
    "/{reminder_id}",
    status_code=204,
    summary="Delete a medication reminder",
)
async def delete_reminder(
    reminder_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[ReminderService, Depends(_get_reminder_service)],
) -> Response:
    await service.delete(reminder_id=reminder_id, patient_id=patient_id)
    return Response(status_code=204)
