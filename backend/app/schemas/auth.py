"""
Pydantic schemas for authentication endpoints.

LoginRequest: incoming credentials from the client.
TokenResponse: returned on successful authentication.

Security: TokenResponse must NEVER be logged — it contains the bearer token.
"""

from pydantic import BaseModel, EmailStr


class LoginRequest(BaseModel):
    """Credentials submitted by the client to obtain a JWT."""

    email: EmailStr
    password: str

    model_config = {"str_max_length": 256}


class TokenResponse(BaseModel):
    """Returned to the client on successful authentication."""

    access_token: str
    token_type: str = "bearer"
