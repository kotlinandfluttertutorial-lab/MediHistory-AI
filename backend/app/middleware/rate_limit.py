"""
Rate-limit middleware for MediHistory AI FastAPI backend.

Applies a sliding-window in-process rate limiter to every request.
All public and authenticated endpoints are protected by this middleware.

PRODUCTION NOTE: Replace this in-process dict with a Redis-backed limiter
(e.g. slowapi or fastapi-limiter) before deploying at scale. The in-process
store does not share state across multiple worker processes.
"""

from __future__ import annotations

from collections import defaultdict
from datetime import UTC, datetime, timedelta
from typing import ClassVar

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse, Response


def reset_rate_limit_state() -> None:
    """
    Clear all tracked request timestamps in every live RateLimitMiddleware instance.

    Call this in test fixtures between test functions to prevent accumulated
    request counts from previous tests from triggering 429 responses.
    Never call this in production code.
    """
    for instance in list(RateLimitMiddleware._instances):
        instance._requests.clear()


class RateLimitMiddleware(BaseHTTPMiddleware):
    """
    Simple sliding-window rate limiter keyed by client IP.

    Parameters
    ----------
    app:
        The ASGI application to wrap.
    max_requests:
        Maximum number of requests allowed per *window_seconds* per IP.
        Defaults to 100.
    window_seconds:
        Sliding window size in seconds. Defaults to 60.
    """

    # Class-level registry of all live instances so reset_rate_limit_state()
    # can clear them all.  Uses a list to avoid holding strong references that
    # would prevent garbage collection; tests that build throw-away apps will
    # still be cleaned up when those apps go out of scope if we used weakrefs,
    # but a plain list is fine for the number of instances in this project.
    _instances: ClassVar[list[RateLimitMiddleware]] = []

    def __init__(self, app, max_requests: int = 100, window_seconds: int = 60) -> None:
        super().__init__(app)
        self._requests: dict[str, list[datetime]] = defaultdict(list)
        self._max = max_requests
        self._window = window_seconds
        RateLimitMiddleware._instances.append(self)

    async def dispatch(self, request: Request, call_next) -> Response:
        client_ip: str = request.client.host if request.client else "unknown"
        now = datetime.now(tz=UTC)
        window_start = now - timedelta(seconds=self._window)

        # Evict timestamps outside the current window.
        self._requests[client_ip] = [
            ts for ts in self._requests[client_ip] if ts > window_start
        ]

        if len(self._requests[client_ip]) >= self._max:
            # Return a JSONResponse directly — raising HTTPException inside
            # BaseHTTPMiddleware.dispatch bypasses FastAPI's exception handlers
            # and would produce a 500 instead of the intended 429.
            return JSONResponse(
                status_code=429,
                content={"detail": "Rate limit exceeded"},
            )

        self._requests[client_ip].append(now)
        return await call_next(request)
