import bcrypt as _bcrypt
from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.user import AppUser
from app.auth.jwt import create_access_token


class AuthService:
    def __init__(self, db: Session):
        self.db = db

    def login(self, email: str, password: str) -> dict:
        user = self.db.query(AppUser).filter(
            AppUser.email == email, AppUser.active == True
        ).first()
        if not user or not _bcrypt.checkpw(password.encode(), user.password_hash.encode()):
            raise HTTPException(status_code=401, detail="Invalid credentials")
        return {
            "token": create_access_token(user.email),
            "email": user.email,
            "role":  user.user_role.value,
        }
