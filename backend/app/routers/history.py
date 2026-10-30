"""
Medical History router.

POST /patients/me/history/generate  — enqueue a new history generation job
GET  /patients/me/history           — list all history summary versions
GET  /patients/me/history/{id}      — get a specific summary version

All routes require a valid JWT (Depends(get_current_patient)).
AI-generated content is always labelled as such with a disclaimer.
"""

from typing import Annotated

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.history import (
    GenerateHistoryResponse,
    HistoryDetail,
    HistoryListResponse,
    HistorySummaryItem,
)
from app.services.history_service import HistoryService

router = APIRouter(prefix="/patients/me/history", tags=["history"])


def _get_history_service(
    db: AsyncSession = Depends(get_db),
) -> HistoryService:
    return HistoryService(db=db)


@router.post(
    "/generate",
    response_model=GenerateHistoryResponse,
    status_code=202,
    summary="Enqueue a medical history generation job",
    responses={
        409: {"description": "A generation job is already in progress"},
    },
)
async def generate_history(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[HistoryService, Depends(_get_history_service)],
) -> GenerateHistoryResponse:
    result = await service.generate(patient_id=patient_id)
    return GenerateHistoryResponse(
        summary_id=result["summary_id"],
        status=result["status"],
        job_id=result.get("job_id"),
    )


@router.get(
    "",
    response_model=HistoryListResponse,
    summary="List all medical history summary versions",
)
async def list_history(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[HistoryService, Depends(_get_history_service)],
) -> HistoryListResponse:
    summaries, total = await service.list_summaries(patient_id=patient_id)
    return HistoryListResponse(
        summaries=[HistorySummaryItem.model_validate(s) for s in summaries],
        total=total,
    )


@router.get(
    "/{summary_id}",
    response_model=HistoryDetail,
    summary="Get a specific medical history summary",
)
async def get_history(
    summary_id: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[HistoryService, Depends(_get_history_service)],
) -> HistoryDetail:
    summary = await service.get_summary(summary_id=summary_id, patient_id=patient_id)
    if summary is None:
        raise_http_error(404, "SUMMARY_NOT_FOUND", "Medical history summary not found.")

    return HistoryDetail(
        id=summary.id,
        version_number=summary.version_number,
        generated_at=summary.generated_at,
        is_current=summary.is_current,
        summary=getattr(summary, "_parsed_summary", None),
    )
