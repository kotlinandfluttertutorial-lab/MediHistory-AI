"""
Medicines router.

GET /patients/me/medicines                        — list medicines grouped by active ingredient
GET /patients/me/medicines/{active_ingredient_slug} — single group by slug

All routes require a valid JWT (Depends(get_current_patient)).
Medicine update (PUT) is handled by the prescriptions router at
/patients/me/prescriptions/{id}/medicines/{id}.
"""

from typing import Annotated, Optional

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.errors import raise_http_error
from app.core.security import get_current_patient
from app.schemas.medicine import MedicineGroup, MedicineListResponse
from app.services.medicine_service import MedicineService

router = APIRouter(prefix="/patients/me/medicines", tags=["medicines"])


def _get_medicine_service(
    db: AsyncSession = Depends(get_db),
) -> MedicineService:
    return MedicineService(db=db)


@router.get(
    "",
    response_model=MedicineListResponse,
    summary="List medicines grouped by active ingredient",
)
async def list_medicines(
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[MedicineService, Depends(_get_medicine_service)],
    search: Optional[str] = None,
    doctor_id: Optional[str] = None,
    medicine_status: Optional[str] = None,
    page: int = 1,
    page_size: int = 20,
) -> MedicineListResponse:
    groups, total = await service.list_grouped(
        patient_id=patient_id,
        search=search,
        doctor_id_filter=doctor_id,
        medicine_status_filter=medicine_status,
        page=page,
        page_size=page_size,
    )
    return MedicineListResponse(
        medicines=groups,
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get(
    "/{active_ingredient_slug}",
    response_model=MedicineGroup,
    summary="Get medicines grouped by a specific active ingredient slug",
)
async def get_medicine_by_slug(
    active_ingredient_slug: str,
    patient_id: Annotated[str, Depends(get_current_patient)],
    service: Annotated[MedicineService, Depends(_get_medicine_service)],
) -> MedicineGroup:
    group = await service.get_by_slug(
        patient_id=patient_id, slug=active_ingredient_slug
    )
    if group is None:
        raise_http_error(
            404,
            "MEDICINE_GROUP_NOT_FOUND",
            f"No medicines found for ingredient slug '{active_ingredient_slug}'.",
        )
    return group
