"""
MediHistory AI — FastAPI application entry point.

Security:
- Auth middleware enforced on every protected route via get_current_patient dependency.
- Secrets are environment variables; none are embedded here.
- Never log token values or patient document contents.
"""

from fastapi import FastAPI

from app.routers import auth

app = FastAPI(
    title="MediHistory AI API",
    description="Backend API for the MediHistory AI personal health record application.",
    version="0.1.0",
)

# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------
app.include_router(auth.router)
