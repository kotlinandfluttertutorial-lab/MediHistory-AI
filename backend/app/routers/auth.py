"""
Authentication router — /auth prefix.

POST /auth/login  — accepts LoginRequest, returns TokenResponse on success,
                    raises HTTP 401 on invalid credentials.

Security: the token value is NEVER written to any log.
"""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from app.schemas.auth import LoginRequest, TokenResponse
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["auth"])


def _get_auth_service() -> AuthService:
    return AuthService()


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="Authenticate and obtain a JWT",
    description=(
        "Accepts patient credentials and returns a signed JWT bearer token. "
        "Returns HTTP 401 for any invalid combination of email and password."
    ),
)
async def login(
    request: LoginRequest,
    auth_service: Annotated[AuthService, Depends(_get_auth_service)],
) -> TokenResponse:
    """
    Authenticate a patient and return a JWT.

    - **email**: registered patient email address
    - **password**: patient password
    """
    token = await auth_service.authenticate(request.email, request.password)
    if token is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    # Token is returned to the caller only; it must not be logged here.
    return TokenResponse(access_token=token)
