from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt
from app.config import settings

ALGORITHM = "HS256"


def create_access_token(subject: str) -> str:
    expire = datetime.now(timezone.utc) + timedelta(seconds=settings.jwt_expiration_seconds)
    return jwt.encode({"sub": subject, "exp": expire}, settings.jwt_secret, algorithm=ALGORITHM)


def decode_token(token: str) -> str:
    """Returns the email (subject) or raises JWTError."""
    payload = jwt.decode(token, settings.jwt_secret, algorithms=[ALGORITHM])
    sub = payload.get("sub")
    if not sub:
        raise JWTError("No subject in token")
    return sub
