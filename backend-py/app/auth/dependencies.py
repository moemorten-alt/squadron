from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from jose import JWTError
from app.database import get_db
from app.models.user import AppUser, UserRole
from app.auth.jwt import decode_token

_bearer = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(_bearer),
    db: Session = Depends(get_db),
) -> AppUser:
    token = credentials.credentials
    try:
        email = decode_token(token)
    except JWTError:
        raise HTTPException(status_code=401, detail="Invalid or expired token")

    user = db.query(AppUser).filter(AppUser.email == email, AppUser.active == True).first()
    if not user:
        raise HTTPException(status_code=401, detail="User not found or inactive")
    return user


def require_admin(current_user: AppUser = Depends(get_current_user)) -> AppUser:
    if current_user.user_role != UserRole.ADMIN:
        raise HTTPException(status_code=403, detail="Access denied")
    return current_user


def get_is_admin(current_user: AppUser = Depends(get_current_user)) -> bool:
    return current_user.user_role == UserRole.ADMIN
