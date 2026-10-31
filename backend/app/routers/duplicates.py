"""
Duplicates router.

GET  /patients/me/duplicates              — list potential duplicate alerts
GET  /patients/me/duplicates/{alert_id}   — get a single alert
PUT  /patients/me/duplicates/{alert_id}/reviewed — mark alert as reviewed

All routes require a valid JWT (Depends(get_current_patient)).
Alerts are labelled "Potential duplicate" — never a clinical conclusion.
"""

from typing import Annotated, Optional

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.duplicate_alert import (
    DuplicateAlertListResponse,
    DuplicateAlertResponse,
)
from app.services.duplicate_service import DuplicateService

router = APIRouter(prefix="/patients/me/duplicates", tags=["duplicates"])


def _get_duplicate_service(
    db: AsyncSession = Depends(get_db),
) -> DuplicateService:
    return DuplicateService(db=db)


@router.get(
    "",
    response_model=DuplicateAlertListResponse,
    summary="List potential duplicate medicine alerts",
)
async def list_duplicates(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[DuplicateService, Depends(_get_duplicate_service)],
    reviewed: Optional[bool] = None,
    page: int = 1,
    page_size: int = 20,
) -> DuplicateAlertListResponse:
    alerts, total, unreviewed_count = await service.list_alerts(
        patient_id=patient_id,
        reviewed_filter=reviewed,
        page=page,
        page_size=page_size,
    )
    return DuplicateAlertListResponse(
        alerts=[DuplicateAlertResponse.model_validate(a) for a in alerts],
        total=total,
        unreviewed_count=unreviewed_count,
    )


@router.get(
    "/{alert_id}",
    response_model=DuplicateAlertResponse,
    summary="Get a single duplicate alert",
)
async def get_duplicate(
    alert_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[DuplicateService, Depends(_get_duplicate_service)],
) -> DuplicateAlertResponse:
    alert = await service.get_alert(alert_id=alert_id, patient_id=patient_id)
    if alert is None:
        raise_http_error(404, "ALERT_NOT_FOUND", "Duplicate alert not found.")
    return DuplicateAlertResponse.model_validate(alert)


@router.put(
    "/{alert_id}/reviewed",
    response_model=DuplicateAlertResponse,
    summary="Mark a duplicate alert as reviewed by the patient",
    responses={
        409: {"description": "Alert already reviewed"},
    },
)
async def mark_reviewed(
    alert_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[DuplicateService, Depends(_get_duplicate_service)],
) -> DuplicateAlertResponse:
    alert = await service.mark_reviewed(alert_id=alert_id, patient_id=patient_id)
    return DuplicateAlertResponse.model_validate(alert)
