"""Verify Alembic migration creates all 8 expected tables."""
import asyncio
import os

os.environ.setdefault("JWT_SECRET_KEY", "synthetic-test-secret")
os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///./verify_migration.db"

from sqlalchemy.ext.asyncio import create_async_engine
from sqlalchemy import text

EXPECTED_TABLES = {
    "patients",
    "doctors",
    "prescriptions",
    "prescription_medicines",
    "duplicate_alerts",
    "medical_history_summaries",
    "reminders",
    "refresh_tokens",
}


async def check_tables():
    engine = create_async_engine("sqlite+aiosqlite:///./verify_migration.db", echo=False)
    async with engine.connect() as conn:
        result = await conn.execute(
            text("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'alembic_%'")
        )
        tables = {row[0] for row in result.fetchall()}

    await engine.dispose()

    missing = EXPECTED_TABLES - tables
    extra = tables - EXPECTED_TABLES

    print(f"Found tables: {sorted(tables)}")
    print(f"Expected tables: {sorted(EXPECTED_TABLES)}")

    if missing:
        print(f"MISSING: {missing}")
    if extra:
        print(f"Extra (not required): {extra}")

    if not missing:
        print("OK — all 8 required tables present")
    else:
        print("FAIL — some tables missing")
        raise SystemExit(1)


asyncio.run(check_tables())
