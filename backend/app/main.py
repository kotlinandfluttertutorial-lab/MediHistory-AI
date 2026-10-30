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
from app.routers import auth, doctors, patients

app = FastAPI(
    title="MediHistory AI API",
    description="Backend API for the MediHistory AI personal health record application.",
    version="0.1.0",
    openapi_tags=[
        {"name": "auth", "description": "Authentication operations"},
        {"name": "patients", "description": "Patient profile operations"},
        {"name": "doctors", "description": "Doctor record operations"},
    ],
)

# ---------------------------------------------------------------------------
# Middleware — registered before routers so it wraps all routes.
# ---------------------------------------------------------------------------
# Simple in-process rate limiter: 100 requests per 60 seconds per client IP.
# Replace with a Redis-backed limiter (e.g. slowapi) before production scale-out.
app.add_middleware(RateLimitMiddleware, max_requests=100, window_seconds=60)

# ---------------------------------------------------------------------------
# Routers — all mounted under /api/v1
# ---------------------------------------------------------------------------
app.include_router(auth.router, prefix="/api/v1")
app.include_router(patients.router, prefix="/api/v1")
app.include_router(doctors.router, prefix="/api/v1")
