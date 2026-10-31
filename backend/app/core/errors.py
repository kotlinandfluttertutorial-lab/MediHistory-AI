"""
Centralised error response helpers.

All routers raise errors through raise_http_error() so the response shape
is consistent across the entire API.

Error response shape:
    {
        "error": {
            "code":    "RESOURCE_NOT_FOUND",
            "message": "The requested resource was not found.",
            "details": {}
        }
    }
"""

from typing import Any, NoReturn

from fastapi import HTTPException
from pydantic import BaseModel


class ErrorDetail(BaseModel):
    """Structured error detail embedded in every error response."""

    code: str
    message: str
    details: dict[str, Any] = {}


class ErrorResponse(BaseModel):
    """Top-level error envelope returned by all error responses."""

    error: ErrorDetail


def raise_http_error(
    status_code: int,
    code: str,
    message: str,
    details: dict[str, Any] | None = None,
) -> NoReturn:
    """
    Raise an HTTPException whose detail is a dict matching ErrorResponse.

    Args:
        status_code: HTTP status code (e.g. 404, 401, 422).
        code:        Machine-readable error code string (e.g. "NOT_FOUND").
        message:     Human-readable error message.
        details:     Optional extra key/value pairs for structured context.

    Raises:
        HTTPException: always.
    """
    raise HTTPException(
        status_code=status_code,
        detail={
            "error": {
                "code": code,
                "message": message,
                "details": details or {},
            }
        },
    )
