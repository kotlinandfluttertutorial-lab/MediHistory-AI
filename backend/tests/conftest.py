"""
Shared pytest fixtures for MediHistory AI backend tests.

Provides:
  - create_tables        — session-scoped autouse fixture that creates DB tables
  - async_engine         — in-memory SQLite engine (per test)
  - async_db             — AsyncSession backed by in-memory SQLite (per test)
  - async_client         — httpx.AsyncClient with DB override (per test)
  - register_and_login   — helper that registers a synthetic patient and returns
                           the access token

asyncio_mode = 'auto' is set in pyproject.toml (see [tool.pytest.ini_options]).

Security: all test data is synthetic. No real patient names, emails, or tokens
are used anywhere in this file.
"""

import os

# Set JWT secret before any app module is imported (security.py reads it at
# module load time and raises KeyError if absent).
os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret-key-for-ci-only")
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///./test_shared.db")

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

# Import models to register them with Base.metadata before any fixture runs
import app.models.doctor  # noqa: F401
import app.models.duplicate_alert  # noqa: F401
import app.models.history  # noqa: F401
import app.models.medicine  # noqa: F401
import app.models.patient  # noqa: F401
import app.models.prescription  # noqa: F401
import app.models.refresh_token  # noqa: F401
import app.models.reminder  # noqa: F401
from app.core.database import Base, create_all_tables, get_db
from app.main import app
from app.middleware.rate_limit import reset_rate_limit_state

# Create DB tables at module load time so TestClient-based tests (test_auth.py,
# test_security.py) can find them. This is safe to call multiple times.
import asyncio as _asyncio

_asyncio.run(create_all_tables())

# ---------------------------------------------------------------------------
# Synthetic test data constants
# ---------------------------------------------------------------------------
SYNTHETIC_EMAIL = "testpatient@synthetic.medihistory"
SYNTHETIC_PASSWORD = "SyntheticPass123!"
SYNTHETIC_DISPLAY_NAME = "Test Patient Alpha"


# ---------------------------------------------------------------------------
# Session-scoped DB setup for TestClient-based (sync) tests
# ---------------------------------------------------------------------------


@pytest.fixture(scope="session", autouse=True)
def create_tables_for_sync_tests(event_loop_policy):
    """
    Create all database tables once per test session.

    This ensures TestClient-based tests (test_auth.py, test_security.py) can
    find the patients / refresh_tokens tables even though they do not use the
    async_client fixture.
    """
    import asyncio

    asyncio.run(create_all_tables())


# ---------------------------------------------------------------------------
# Engine and session fixtures
# ---------------------------------------------------------------------------


@pytest_asyncio.fixture(scope="function")
async def async_engine():
    """Create a fresh in-memory SQLite engine for each test function."""
    engine = create_async_engine(
        "sqlite+aiosqlite:///:memory:",
        echo=False,
        future=True,
        connect_args={"check_same_thread": False},
    )
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)

    yield engine

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def async_db(async_engine):
    """Yield an AsyncSession backed by the in-memory engine."""
    factory = async_sessionmaker(
        bind=async_engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
        autocommit=False,
    )
    async with factory() as session:
        yield session


@pytest_asyncio.fixture(scope="function")
async def async_client(async_engine):
    """
    Return an httpx.AsyncClient that uses the in-memory database.

    The app's get_db dependency is overridden to use the test session factory.
    The module-level rate limiter state is reset before each test so that
    accumulated request counts from earlier tests do not trigger 429 responses.
    """
    factory = async_sessionmaker(
        bind=async_engine,
        class_=AsyncSession,
        expire_on_commit=False,
        autoflush=False,
        autocommit=False,
    )

    async def override_get_db():
        async with factory() as session:
            try:
                yield session
                await session.commit()
            except Exception:
                await session.rollback()
                raise

    app.dependency_overrides[get_db] = override_get_db

    # Reset the shared rate limiter state so each test starts with a clean slate.
    reset_rate_limit_state()

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        yield client

    # Clean up override after each test
    app.dependency_overrides.pop(get_db, None)


# ---------------------------------------------------------------------------
# Auth helper fixtures
# ---------------------------------------------------------------------------


async def get_auth_token(client: AsyncClient, email: str, password: str) -> str:
    """Login and return the access token."""
    response = await client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": password},
    )
    assert response.status_code == 200, f"Login failed: {response.text}"
    return response.json()["access_token"]


async def register_and_login(client: AsyncClient) -> str:
    """
    Register a synthetic patient account and return the access token.

    Uses SYNTHETIC_EMAIL / SYNTHETIC_PASSWORD / SYNTHETIC_DISPLAY_NAME.
    """
    reg_response = await client.post(
        "/api/v1/auth/register",
        json={
            "email": SYNTHETIC_EMAIL,
            "password": SYNTHETIC_PASSWORD,
            "display_name": SYNTHETIC_DISPLAY_NAME,
        },
    )
    assert reg_response.status_code == 201, f"Registration failed: {reg_response.text}"
    return await get_auth_token(client, SYNTHETIC_EMAIL, SYNTHETIC_PASSWORD)
