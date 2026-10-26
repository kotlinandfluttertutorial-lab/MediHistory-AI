"""
Authentication service.

Stub implementation: only the synthetic test account is accepted.
Real user store and password hashing will be added in a subsequent ticket.

Security invariants:
- SECRET_KEY is read from the environment variable JWT_SECRET_KEY.
  The server WILL NOT start if the variable is absent.
- The signed token is NEVER written to any log.
"""

import os
from datetime import datetime, timezone, timedelta

import jwt


# Raises KeyError at import time if the variable is not set,
# which prevents the server from starting without a secret.
_SECRET_KEY: str = os.environ["JWT_SECRET_KEY"]
_ALGORITHM = "HS256"
_TOKEN_TTL_HOURS = 1

# Synthetic test account — only used in development / CI.
# A real user store is wired in a later ticket.
_SYNTHETIC_EMAIL = "test@synthetic.medihistory"
_SYNTHETIC_PASSWORD = "SyntheticTest123!"
_SYNTHETIC_PATIENT_ID = "synthetic-patient-001"


class AuthService:
    async def authenticate(self, email: str, password: str) -> str | None:
        """
        Validate credentials and return a signed JWT, or None on failure.

        The returned token must NOT be logged by the caller.
        """
        if email != _SYNTHETIC_EMAIL or password != _SYNTHETIC_PASSWORD:
            return None

        now = datetime.now(tz=timezone.utc)
        payload = {
            "sub": _SYNTHETIC_PATIENT_ID,
            "exp": now + timedelta(hours=_TOKEN_TTL_HOURS),
            "iat": now,
        }
        return jwt.encode(payload, _SECRET_KEY, algorithm=_ALGORITHM)
