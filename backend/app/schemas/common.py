"""
Common Pydantic schemas used across multiple routers.

ErrorDetail / ErrorResponse mirror the shape produced by raise_http_error()
in app/core/errors.py and are provided here so clients can generate typed
bindings from the OpenAPI schema.
"""

from typing import Any

from pydantic import BaseModel


class ErrorDetail(BaseModel):
    """Structured error detail embedded in every error response."""

    code: str
    message: str
    details: dict[str, Any] = {}

    model_config = {"from_attributes": True}


class ErrorResponse(BaseModel):
    """Top-level error envelope returned by all error responses."""

    error: ErrorDetail

    model_config = {"from_attributes": True}
