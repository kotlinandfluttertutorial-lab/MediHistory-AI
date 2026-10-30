"""
Pydantic schemas for authentication endpoints.

LoginRequest: incoming credentials from the client.
TokenResponse: returned on successful authentication.
RegisterRequest / RegisterResponse: new patient registration.
RefreshRequest / RefreshResponse: token refresh.

Security: TokenResponse must NEVER be logged — it contains the bearer token.
"""

from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    """Credentials submitted by the client to obtain a JWT."""

    email: EmailStr
    password: str

    model_config = {"str_max_length": 256}


class TokenResponse(BaseModel):
    """Returned to the client on successful authentication."""

    access_token: str
    token_type: str = "bearer"
    refresh_token: str = ""
    expires_in: int = 1800


class RegisterRequest(BaseModel):
    """Payload for creating a new patient account."""

    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    display_name: str = Field(max_length=100)


class RegisterResponse(BaseModel):
    """Returned after successful registration."""

    patient_id: str
    email: str
    display_name: str
    created_at: datetime

    model_config = {"from_attributes": True}


class RefreshRequest(BaseModel):
    """Payload for refreshing an access token."""

    refresh_token: str


class RefreshResponse(BaseModel):
    """Returned after a successful token refresh."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int = 1800
