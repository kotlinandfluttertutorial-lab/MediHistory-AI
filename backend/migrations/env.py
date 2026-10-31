"""
Alembic env.py — supports both offline (sync) and online (async) migration runs.

The DATABASE_URL is read from the DATABASE_URL environment variable.
Default: sqlite+aiosqlite:///./medihistory.db

All ORM models are imported via app.models so that Alembic autogenerate
can discover every table registered with Base.metadata.
"""

import asyncio
import os
import sys
from logging.config import fileConfig
from pathlib import Path

from alembic import context
from sqlalchemy import pool
from sqlalchemy.ext.asyncio import async_engine_from_config

# ---------------------------------------------------------------------------
# Ensure the backend/ package root is on sys.path so app.* imports resolve.
# This file lives at backend/migrations/env.py; backend/ is one level up.
# ---------------------------------------------------------------------------
_backend_root = str(Path(__file__).resolve().parent.parent)
if _backend_root not in sys.path:
    sys.path.insert(0, _backend_root)

# ---------------------------------------------------------------------------
# Import all models so Base.metadata is fully populated for autogenerate.
# ---------------------------------------------------------------------------
import app.models  # noqa: E402, F401 — side-effect import registers all tables
from app.core.database import Base  # noqa: E402

# ---------------------------------------------------------------------------
# Alembic config object
# ---------------------------------------------------------------------------
config = context.config

if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# Override the sqlalchemy.url with the environment variable so the .ini
# placeholder is never used in practice.
_database_url = os.getenv("DATABASE_URL", "sqlite+aiosqlite:///./medihistory.db")
config.set_main_option("sqlalchemy.url", _database_url)

target_metadata = Base.metadata


# ---------------------------------------------------------------------------
# Offline migrations (generates SQL without a live connection)
# ---------------------------------------------------------------------------


def run_migrations_offline() -> None:
    """Run migrations in offline mode (emits SQL to stdout)."""
    url = config.get_main_option("sqlalchemy.url")
    context.configure(
        url=url,
        target_metadata=target_metadata,
        literal_binds=True,
        dialect_opts={"paramstyle": "named"},
        render_as_batch=True,  # Required for SQLite ALTER TABLE support.
    )

    with context.begin_transaction():
        context.run_migrations()


# ---------------------------------------------------------------------------
# Online async migrations
# ---------------------------------------------------------------------------


def do_run_migrations(connection):
    context.configure(
        connection=connection,
        target_metadata=target_metadata,
        render_as_batch=True,  # Required for SQLite ALTER TABLE support.
    )
    with context.begin_transaction():
        context.run_migrations()


async def run_async_migrations() -> None:
    """Create an async engine and run migrations within it."""
    connectable = async_engine_from_config(
        config.get_section(config.config_ini_section, {}),
        prefix="sqlalchemy.",
        poolclass=pool.NullPool,
    )

    async with connectable.connect() as connection:
        await connection.run_sync(do_run_migrations)

    await connectable.dispose()


def run_migrations_online() -> None:
    """Entry point for online (async) migration runs."""
    asyncio.run(run_async_migrations())


# ---------------------------------------------------------------------------
# Dispatch
# ---------------------------------------------------------------------------

if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
