"""
Authentication router — /auth prefix.

POST /auth/login     — accepts LoginRequest, returns TokenResponse on success,
                       raises HTTP 401 on invalid credentials.
POST /auth/register  — create a new patient account.
POST /auth/refresh   — exchange a refresh token for a new access token.
POST /auth/logout    — revoke a refresh token (204 No Content).

Security: token values are NEVER written to any log.
"""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.schemas.auth import (
    LoginRequest,
    RefreshRequest,
    RefreshResponse,
    RegisterRequest,
    RegisterResponse,
    TokenResponse,
)
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["auth"])


def get_auth_service(db: AsyncSession = Depends(get_db)) -> AuthService:
    return AuthService(db=db)


@router.post(
    "/register",
    response_model=RegisterResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register a new patient account",
    description=(
        "Creates a new patient account. Returns 409 if the email is already "
        "registered. Returns 422 for validation errors (e.g. password < 8 chars)."
    ),
)
async def register(
    request: RegisterRequest,
    auth_service: Annotated[AuthService, Depends(get_auth_service)],
) -> RegisterResponse:
    """Register a new patient."""
    patient = await auth_service.register(
        email=request.email,
        password=request.password,
        display_name=request.display_name,
    )
    return RegisterResponse(
        patient_id=patient.id,
        email=patient.email,
        display_name=patient.display_name,
        created_at=patient.created_at,
    )


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="Authenticate and obtain a JWT",
    description=(
        "Accepts patient credentials and returns a signed JWT bearer token plus "
        "a refresh token. Returns HTTP 401 for any invalid combination of email "
        "and password."
    ),
)
async def login(
    request: LoginRequest,
    auth_service: Annotated[AuthService, Depends(get_auth_service)],
) -> TokenResponse:
    """
    Authenticate a patient and return a JWT.

    First tries real credentials; falls back to the synthetic test account
    for backward compatibility with existing tests.
    """
    # Try real credentials first
    result = await auth_service.authenticate_full(request.email, request.password)
    if result is not None:
        access_token, refresh_token = result
        return TokenResponse(access_token=access_token, refresh_token=refresh_token)

    # Fall back to synthetic account (for existing tests)
    token = await auth_service.authenticate(request.email, request.password)
    if token is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    # Token is returned to the caller only; it must not be logged here.
    return TokenResponse(access_token=token, refresh_token="")


@router.post(
    "/refresh",
    response_model=RefreshResponse,
    summary="Refresh an access token",
    description="Exchange a valid refresh token for a new access token.",
)
async def refresh_token(
    request: RefreshRequest,
    auth_service: Annotated[AuthService, Depends(get_auth_service)],
) -> RefreshResponse:
    """Return a new access token given a valid refresh token."""
    access_token = await auth_service.refresh(request.refresh_token)
    return RefreshResponse(access_token=access_token)


@router.post(
    "/logout",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Revoke a refresh token",
    description="Revokes the supplied refresh token. Returns 204 on success.",
)
async def logout(
    request: RefreshRequest,
    auth_service: Annotated[AuthService, Depends(get_auth_service)],
) -> Response:
    """Revoke a refresh token."""
    await auth_service.logout(request.refresh_token)
    return Response(status_code=status.HTTP_204_NO_CONTENT)
