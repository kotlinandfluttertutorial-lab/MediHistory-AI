# MediHistory AI — FastAPI backend
# Multi-stage build: builder installs deps, final image is minimal.
#
# Security:
# - Runs as non-root user (appuser).
# - No secrets in this file — all secrets via environment variables at runtime.
# - Source code is copied after dependency installation for better layer caching.
#
# Build:   docker build -t medihistory-backend .
# Run:     docker run -p 8000:8000 --env-file .env medihistory-backend

FROM python:3.12-slim AS builder

WORKDIR /app

# Install system deps required by some Python packages (psycopg2-binary alternative,
# cryptography, Pillow)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# ── Final image ───────────────────────────────────────────────────────────────
FROM python:3.12-slim

WORKDIR /app

# Runtime system deps only
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    && rm -rf /var/lib/apt/lists/*

# Copy installed packages from builder
COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# Copy application source
COPY . .

# Non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
RUN chown -R appuser:appgroup /app
USER appuser

# Uploads directory (bind-mount a volume in production)
RUN mkdir -p uploads

EXPOSE 8000

# Uvicorn with 2 workers by default; override via WORKERS env var.
# --no-access-log: never log request paths that could contain patient IDs.
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers ${WORKERS:-2} --no-access-log"]
