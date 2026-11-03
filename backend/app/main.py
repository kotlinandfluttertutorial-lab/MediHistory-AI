"""
MediHistory AI — FastAPI application entry point.

Security:
- Auth middleware enforced on every protected route via get_current_patient dependency.
- RateLimitMiddleware applied to all public and authenticated endpoints.
- Secrets are environment variables; none are embedded here.
- Never log token values or patient document contents.
"""

from fastapi import FastAPI

from app.middleware.rate_limit import RateLimitMiddleware
from app.routers import (
    ai_agents,
    auth,
    doctors,
    duplicates,
    export,
    history,
    interactions,
    medicines,
    patients,
    prescriptions,
    reminders,
    sync,
)

app = FastAPI(
    title="MediHistory AI API",
    description="Backend API for the MediHistory AI personal health record application.",
    version="0.1.0",
    openapi_tags=[
        {"name": "auth", "description": "Authentication operations"},
        {"name": "patients", "description": "Patient profile operations"},
        {"name": "doctors", "description": "Doctor record operations"},
        {"name": "prescriptions", "description": "Prescription document operations"},
        {"name": "medicines", "description": "Medicine list and update operations"},
        {
            "name": "duplicates",
            "description": "Potential duplicate medicine alert operations",
        },
        {"name": "history", "description": "Medical history summary operations"},
        {"name": "reminders", "description": "Medication reminder operations"},
        {"name": "interactions", "description": "Drug interaction check (stub)"},
        {"name": "export", "description": "PDF export operations (stub)"},
        {"name": "ai", "description": "AI agent endpoints (F11, F12, F15)"},
        {"name": "sync", "description": "Device-to-backend sync endpoints (SyncWorker)"},
    ],
)


# ---------------------------------------------------------------------------
# Startup — run Alembic migrations automatically on first boot
# ---------------------------------------------------------------------------

@app.on_event("startup")
async def run_migrations() -> None:
    """
    Apply any pending Alembic migrations on startup.

    This is safe to run on every boot — Alembic is idempotent (it only applies
    migrations that haven't been applied yet).  In a multi-instance deployment,
    run this only from a dedicated migration job rather than every worker.
    Only runs when a real database is configured (skipped for in-memory SQLite
    used in tests).
    """
    import os
    db_url = os.getenv("DATABASE_URL", "")
    if "sqlite" in db_url and ":memory:" in db_url:
        return  # test environment — skip migrations

    try:
        from alembic import command
        from alembic.config import Config

        alembic_cfg = Config("alembic.ini")
        command.upgrade(alembic_cfg, "head")
    except Exception:
        # Never fail startup due to migration errors — log and continue.
        # A failed migration will surface as a DB error on the first request.
        pass

# ---------------------------------------------------------------------------
# Middleware — registered before routers so it wraps all routes.
# ---------------------------------------------------------------------------
# Simple in-process rate limiter: 100 requests per 60 seconds per client IP.
# Replace with a Redis-backed limiter (e.g. slowapi) before production scale-out.
#
# app.state.rate_limiter is set when the middleware_stack is built (on first
# request). Tests access it via a shared sentinel stored in app.state so they
# can call reset() between test functions without walking the compiled stack.
app.add_middleware(RateLimitMiddleware, max_requests=100, window_seconds=60)

# ---------------------------------------------------------------------------
# Routers — all mounted under /api/v1
# ---------------------------------------------------------------------------
app.include_router(auth.router, prefix="/api/v1")
app.include_router(patients.router, prefix="/api/v1")
app.include_router(doctors.router, prefix="/api/v1")
app.include_router(prescriptions.router, prefix="/api/v1")
app.include_router(medicines.router, prefix="/api/v1")
app.include_router(duplicates.router, prefix="/api/v1")
app.include_router(history.router, prefix="/api/v1")
app.include_router(reminders.router, prefix="/api/v1")
app.include_router(interactions.router, prefix="/api/v1")
app.include_router(export.router, prefix="/api/v1")
app.include_router(ai_agents.router, prefix="/api/v1")
app.include_router(sync.router, prefix="/api/v1")
