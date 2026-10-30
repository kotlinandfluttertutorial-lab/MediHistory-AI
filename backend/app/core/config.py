"""
Application configuration.

All sensitive values (JWT_SECRET_KEY) must be set as environment variables.
The server refuses to start if JWT_SECRET_KEY is absent.

Usage:
    from app.core.config import get_settings
    settings = get_settings()
"""

import functools
import os


class Settings:
    """
    Application settings resolved from environment variables.

    Fields:
        DATABASE_URL            — SQLAlchemy async database URL.
        JWT_SECRET_KEY          — HMAC signing secret for JWTs (required).
        UPLOAD_DIR              — Directory for uploaded prescription documents.
        MAX_UPLOAD_BYTES        — Maximum accepted file size in bytes.
        ACCESS_TOKEN_TTL_SECONDS — Lifetime of an access token in seconds.
        REFRESH_TOKEN_TTL_DAYS  — Lifetime of a refresh token in days.
        ENABLE_INTERACTION_CHECK — Feature flag for drug-interaction checking.
    """

    def __init__(self) -> None:
        self.DATABASE_URL: str = os.getenv(
            "DATABASE_URL", "sqlite+aiosqlite:///./medihistory.db"
        )
        # Raises KeyError immediately if not set — prevents silent mis-configs.
        self.JWT_SECRET_KEY: str = os.environ["JWT_SECRET_KEY"]
        self.UPLOAD_DIR: str = os.getenv("UPLOAD_DIR", "./uploads")
        self.MAX_UPLOAD_BYTES: int = int(
            os.getenv("MAX_UPLOAD_BYTES", str(10 * 1024 * 1024))
        )
        self.ACCESS_TOKEN_TTL_SECONDS: int = int(
            os.getenv("ACCESS_TOKEN_TTL_SECONDS", "1800")
        )
        self.REFRESH_TOKEN_TTL_DAYS: int = int(
            os.getenv("REFRESH_TOKEN_TTL_DAYS", "30")
        )
        self.ENABLE_INTERACTION_CHECK: bool = (
            os.getenv("ENABLE_INTERACTION_CHECK", "false").lower() == "true"
        )


@functools.lru_cache(maxsize=1)
def get_settings() -> Settings:
    """
    Return the cached Settings instance.

    Cached so the environment is read once per process.
    Clear the cache in tests with get_settings.cache_clear().
    """
    return Settings()
