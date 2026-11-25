from __future__ import annotations

import hmac
from fastapi import Header, HTTPException, status

from app.config import ServerSettings


def verify_pairing_token(
    settings: ServerSettings,
    authorization: str | None = Header(default=None),
    x_zeus_token: str | None = Header(default=None),
) -> str:
    """Validates constant-time Bearer or X-Zeus-Token pairing credentials on every endpoint."""
    candidate = ""
    if authorization and authorization.lower().startswith("bearer "):
        candidate = authorization[7:].strip()
    elif x_zeus_token:
        candidate = x_zeus_token.strip()

    if not candidate or not hmac.compare_digest(candidate, settings.pairing_token):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing pairing token",
        )
    return candidate
