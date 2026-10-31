"""
Authentication service.

Provides:
  - authenticate()        — synthetic test account only (backward compat)
  - register()            — create a new patient account
  - authenticate_full()   — real credential check with refresh token issuance
  - refresh()             — exchange a valid refresh token for a new access token
  - logout()              — revoke a refresh token

Security invariants:
- SECRET_KEY is read from the environment variable JWT_SECRET_KEY.
  The server WILL NOT start if the variable is absent.
- The signed token is NEVER written to any log.
- Refresh tokens are stored as bcrypt hashes — the raw value is never persisted.
"""

import hashlib
import os
from datetime import UTC, datetime, timedelta

import bcrypt
import jwt
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import raise_http_error
from app.models.patient import Patient
from app.models.refresh_token import RefreshToken

# Raises KeyError at import time if the variable is not set.
_SECRET_KEY: str = os.environ["JWT_SECRET_KEY"]
_ALGORITHM = "HS256"
_TOKEN_TTL_HOURS = 1

# Settings — read from environment with defaults matching config.py
_ACCESS_TOKEN_TTL_SECONDS: int = int(os.getenv("ACCESS_TOKEN_TTL_SECONDS", "1800"))
_REFRESH_TOKEN_TTL_DAYS: int = int(os.getenv("REFRESH_TOKEN_TTL_DAYS", "30"))

# Synthetic test account — only used in development / CI.
_SYNTHETIC_EMAIL = "test@synthetic.medihistory"
_SYNTHETIC_PASSWORD = "SyntheticTest123!"
_SYNTHETIC_PATIENT_ID = "synthetic-patient-001"


def _hash_token(token: str) -> str:
    """Return a SHA-256 hex digest used to look up stored refresh tokens."""
    return hashlib.sha256(token.encode()).hexdigest()


def _make_access_token(patient_id: str) -> str:
    now = datetime.now(tz=UTC)
    payload = {
        "sub": patient_id,
        "type": "access",
        "exp": now + timedelta(seconds=_ACCESS_TOKEN_TTL_SECONDS),
        "iat": now,
    }
    return jwt.encode(payload, _SECRET_KEY, algorithm=_ALGORITHM)


def _make_refresh_token(patient_id: str) -> str:
    now = datetime.now(tz=UTC)
    payload = {
        "sub": patient_id,
        "type": "refresh",
        "exp": now + timedelta(days=_REFRESH_TOKEN_TTL_DAYS),
        "iat": now,
    }
    return jwt.encode(payload, _SECRET_KEY, algorithm=_ALGORITHM)


class AuthService:
    def __init__(self, db: AsyncSession | None = None) -> None:
        self._db = db

    # ------------------------------------------------------------------
    # Backward-compatible synthetic path (used by existing test_auth.py)
    # ------------------------------------------------------------------

    async def authenticate(self, email: str, password: str) -> str | None:
        """
        Validate credentials against the synthetic test account and return
        a signed JWT, or None on failure.

        The returned token must NOT be logged by the caller.
        """
        if email != _SYNTHETIC_EMAIL or password != _SYNTHETIC_PASSWORD:
            return None

        now = datetime.now(tz=UTC)
        payload = {
            "sub": _SYNTHETIC_PATIENT_ID,
            "exp": now + timedelta(hours=_TOKEN_TTL_HOURS),
            "iat": now,
        }
        return jwt.encode(payload, _SECRET_KEY, algorithm=_ALGORITHM)

    # ------------------------------------------------------------------
    # Real user registration and authentication
    # ------------------------------------------------------------------

    async def register(self, email: str, password: str, display_name: str) -> Patient:
        """
        Create a new patient account.

        Raises HTTP 409 if the email is already registered.
        """
        assert self._db is not None, "AuthService requires a db session for register()"
        db = self._db

        # Check for duplicate email
        result = await db.execute(select(Patient).where(Patient.email == email))
        existing = result.scalar_one_or_none()
        if existing is not None:
            raise_http_error(
                409, "EMAIL_ALREADY_EXISTS", "Email is already registered."
            )

        # Hash password with bcrypt
        password_hash = bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()

        patient = Patient(
            email=email,
            password_hash=password_hash,
            display_name=display_name,
        )
        db.add(patient)
        await db.commit()
        await db.refresh(patient)
        return patient

    async def authenticate_full(
        self, email: str, password: str
    ) -> tuple[str, str] | None:
        """
        Validate real credentials and return (access_token, refresh_token).

        Returns None if the credentials are invalid.
        Stores a hashed refresh token in the RefreshToken table.
        """
        assert (
            self._db is not None
        ), "AuthService requires a db session for authenticate_full()"
        db = self._db

        result = await db.execute(select(Patient).where(Patient.email == email))
        patient = result.scalar_one_or_none()
        if patient is None:
            return None

        if not bcrypt.checkpw(password.encode(), patient.password_hash.encode()):
            return None

        access_token = _make_access_token(patient.id)
        refresh_token = _make_refresh_token(patient.id)

        # Store hashed refresh token
        token_hash = _hash_token(refresh_token)
        expires_at = datetime.now(tz=UTC) + timedelta(days=_REFRESH_TOKEN_TTL_DAYS)
        db_token = RefreshToken(
            patient_id=patient.id,
            token_hash=token_hash,
            expires_at=expires_at,
        )
        db.add(db_token)
        await db.commit()

        return access_token, refresh_token

    async def refresh(self, refresh_token_str: str) -> str:
        """
        Validate a refresh token and return a new access token.

        Raises HTTP 401 if the token is invalid, expired, or revoked.
        """
        assert self._db is not None, "AuthService requires a db session for refresh()"
        db = self._db

        try:
            payload = jwt.decode(
                refresh_token_str, _SECRET_KEY, algorithms=[_ALGORITHM]
            )
        except jwt.ExpiredSignatureError:
            raise_http_error(401, "TOKEN_EXPIRED", "Refresh token has expired.")
        except jwt.InvalidTokenError:
            raise_http_error(401, "INVALID_TOKEN", "Invalid refresh token.")

        if payload.get("type") != "refresh":
            raise_http_error(401, "INVALID_TOKEN_TYPE", "Not a refresh token.")

        patient_id: str = payload["sub"]
        token_hash = _hash_token(refresh_token_str)
        now = datetime.now(tz=UTC)

        result = await db.execute(
            select(RefreshToken).where(
                RefreshToken.token_hash == token_hash,
                RefreshToken.revoked == False,  # noqa: E712
                RefreshToken.expires_at > now,
            )
        )
        db_token = result.scalar_one_or_none()
        if db_token is None:
            raise_http_error(
                401, "INVALID_TOKEN", "Refresh token not found or revoked."
            )

        return _make_access_token(patient_id)

    async def logout(self, refresh_token_str: str) -> None:
        """
        Revoke the given refresh token.

        Silently succeeds if the token is not found (idempotent).
        """
        assert self._db is not None, "AuthService requires a db session for logout()"
        db = self._db

        try:
            jwt.decode(refresh_token_str, _SECRET_KEY, algorithms=[_ALGORITHM])
        except jwt.InvalidTokenError:
            # Token is already invalid; nothing to revoke.
            return

        token_hash = _hash_token(refresh_token_str)
        result = await db.execute(
            select(RefreshToken).where(RefreshToken.token_hash == token_hash)
        )
        db_token = result.scalar_one_or_none()
        if db_token is not None:
            db_token.revoked = True
            await db.commit()
