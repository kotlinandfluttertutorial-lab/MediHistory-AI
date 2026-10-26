"""
FastAPI security dependency: get_current_patient.

Reads and verifies the Bearer JWT from the Authorization header.
Returns the patient_id (JWT sub claim) on success.

HTTP status codes:
  401 — missing, malformed, or expired token
  404 — cross-patient resource access (caller is responsible for this check)

Note: 403 is intentionally not used, to avoid leaking resource existence.
"""

import os
from typing import Annotated

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

_SECRET_KEY: str = os.environ["JWT_SECRET_KEY"]
_ALGORITHM = "HS256"

_bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_patient(
    credentials: Annotated[
        HTTPAuthorizationCredentials | None, Depends(_bearer_scheme)
    ],
) -> str:
    """
    FastAPI dependency that extracts and verifies the patient JWT.

    Returns the patient_id string (JWT ``sub`` claim).
    Raises HTTP 401 for any token problem.
    """
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={"WWW-Authenticate": "Bearer"},
        )

    try:
        payload = jwt.decode(
            credentials.credentials,
            _SECRET_KEY,
            algorithms=[_ALGORITHM],
        )
        patient_id: str | None = payload.get("sub")
        if not patient_id:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid token payload",
                headers={"WWW-Authenticate": "Bearer"},
            )
        return patient_id
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Token has expired",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid token",
            headers={"WWW-Authenticate": "Bearer"},
        )
