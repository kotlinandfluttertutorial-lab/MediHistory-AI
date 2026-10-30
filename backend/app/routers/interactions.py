"""
Interactions router — drug-interaction checking stub.

POST /patients/me/interactions/check

When ENABLE_INTERACTION_CHECK is False (default), returns a safe unavailable
response. When True (future integration), returns a stub indicating the check
is not yet connected to a real reference provider.

Disclaimer: This feature never indicates that a medicine combination is safe.
All interaction checking must be confirmed with a doctor or pharmacist.
"""

from typing import Annotated

from fastapi import APIRouter, Depends

from app.core.config import get_settings
from app.core.security import get_current_patient

router = APIRouter(prefix="/patients/me/interactions", tags=["interactions"])


@router.post(
    "/check",
    summary="Check for potential drug interactions (stub)",
    response_model=None,
)
async def check_interactions(
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> dict:
    """
    Drug-interaction checking endpoint.

    Returns a safe unavailable response when the feature flag is off.
    When the flag is on, returns a stub acknowledging the check has not yet
    been connected to a reference database.

    This endpoint NEVER indicates that any combination of medicines is safe.
    """
    settings = get_settings()

    if not settings.ENABLE_INTERACTION_CHECK:
        return {
            "checked": False,
            "reason": "unavailable",
            "message": (
                "Interaction checking is not available. "
                "Please discuss your medicines with your doctor or pharmacist."
            ),
        }

    # Feature flag on but integration not yet wired up — return a stub response.
    return {
        "checked": True,
        "medicines_checked": [],
        "interactions": [],
        "disclaimer": (
            "This check is not yet integrated with a reference provider."
        ),
    }
