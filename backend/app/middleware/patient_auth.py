"""
Patient-scoped authorisation helpers for MediHistory AI.

Every endpoint that accesses patient-owned data must call
``require_patient_owns_resource`` after resolving the resource.

HTTP 404 is used intentionally (not 403) to avoid leaking whether a
resource with the requested ID exists for a different patient.
"""

from fastapi import HTTPException, status


def require_patient_owns_resource(
    resource_patient_id: str,
    current_patient_id: str,
) -> None:
    """
    Enforce patient-scoped authorisation.

    Raises HTTP 404 (not 403) when *current_patient_id* does not match
    *resource_patient_id*, so that a caller cannot infer whether a
    resource with the requested ID exists at all.

    Parameters
    ----------
    resource_patient_id:
        The patient ID recorded on the resource being accessed.
    current_patient_id:
        The patient ID extracted from the verified JWT (``get_current_patient``).

    Raises
    ------
    HTTPException(404):
        When the caller does not own the requested resource.
    """
    if resource_patient_id != current_patient_id:
        # 404 intentional — do not reveal whether the resource exists
        # for any patient other than the authenticated one.
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Resource not found",
        )
