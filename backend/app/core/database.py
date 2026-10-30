"""
Async SQLAlchemy engine, session factory, and declarative base.

Usage:
    from app.core.database import get_db, Base

    # In a router:
    async def my_route(db: AsyncSession = Depends(get_db)):
        ...

Notes:
    - Schema creation in production is handled exclusively by Alembic.
    - create_all_tables() is provided for use in test fixtures only.
    - Never call create_all_tables() in production startup.
"""

from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy.orm import declarative_base

from app.core.config import get_settings

# ---------------------------------------------------------------------------
# Engine — created lazily from settings so tests can override DATABASE_URL
# before the module is imported.
# ---------------------------------------------------------------------------


def _make_engine():
    settings = get_settings()
    return create_async_engine(
        settings.DATABASE_URL,
        echo=False,  # Set to True locally for SQL tracing; never in prod.
        future=True,
    )


def _make_session_factory(engine):
    return async_sessionmaker(
        bind=engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
        autocommit=False,
    )


# Module-level singletons.  Tests that need a fresh engine should call
# _make_engine() themselves after overriding DATABASE_URL.
engine = _make_engine()
AsyncSessionLocal = _make_session_factory(engine)

# ---------------------------------------------------------------------------
# Declarative base — all ORM models inherit from this.
# ---------------------------------------------------------------------------
Base = declarative_base()


# ---------------------------------------------------------------------------
# FastAPI dependency
# ---------------------------------------------------------------------------


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """
    Yield an AsyncSession per request and close it when the request ends.

    The session is yielded inside a begin() context so that a raised exception
    triggers a rollback automatically.
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise


# ---------------------------------------------------------------------------
# Test helper — NOT for production use
# ---------------------------------------------------------------------------


async def create_all_tables() -> None:
    """
    Create all tables that are registered with Base.metadata.

    For use in test fixtures only.  Production schema management is done
    exclusively via Alembic migrations.
    """
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
