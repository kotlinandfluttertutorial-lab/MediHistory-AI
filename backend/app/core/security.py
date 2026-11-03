"""
FastAPI security dependency: get_current_patient.

Reads and verifies the Bearer JWT from the Authorization header.
Returns the patient_id (JWT sub claim) on success.

HTTP status codes:
  401 — missing, malformed, or expired token
  404 — cross-patient resource access (caller is responsible for this check)

Note: 403 is intentionally not used, to avoid leaking resource existence.

Algorithm: RS256 when JWT_PRIVATE_KEY / JWT_PUBLIC_KEY env vars are set;
falls back to HS256 + JWT_SECRET_KEY for development/CI environments.
This allows a zero-downtime migration path: generate an RSA key pair,
set the env vars, and the server switches automatically.
"""

import os
from typing import Annotated

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

# ---------------------------------------------------------------------------
# Algorithm selection — RS256 preferred, HS256 for dev/CI fallback
# ---------------------------------------------------------------------------

_JWT_PRIVATE_KEY: str | None = os.environ.get("JWT_PRIVATE_KEY")
_JWT_PUBLIC_KEY: str | None = os.environ.get("JWT_PUBLIC_KEY")
_SECRET_KEY: str = os.environ["JWT_SECRET_KEY"]

# Use RS256 when both RSA keys are present; otherwise HS256.
_ALGORITHM = "RS256" if (_JWT_PRIVATE_KEY and _JWT_PUBLIC_KEY) else "HS256"
_SIGN_KEY = _JWT_PRIVATE_KEY if _ALGORITHM == "RS256" else _SECRET_KEY
_VERIFY_KEY = _JWT_PUBLIC_KEY if _ALGORITHM == "RS256" else _SECRET_KEY

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
            _VERIFY_KEY,
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
