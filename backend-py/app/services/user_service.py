import bcrypt as _bcrypt
from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.user import AppUser, UserRole
from app.schemas.user import CreateUserRequest, UpdateUserRequest


def _hash(password: str) -> str:
    return _bcrypt.hashpw(password.encode(), _bcrypt.gensalt()).decode()


class UserService:
    def __init__(self, db: Session):
        self.db = db

    def find_all(self) -> list:
        users = self.db.query(AppUser).order_by(AppUser.email).all()
        return [_to_dict(u) for u in users]

    def create(self, req: CreateUserRequest) -> dict:
        if self.db.query(AppUser).filter(AppUser.email == req.email).first():
            raise HTTPException(status_code=400, detail=f"Email {req.email} already exists")
        user = AppUser(
            email=req.email,
            password_hash=_hash(req.password),
            user_role=UserRole(req.role),
        )
        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)
        return _to_dict(user)

    def update(self, user_id: int, req: UpdateUserRequest) -> dict:
        user = self.db.get(AppUser, user_id)
        if not user:
            raise HTTPException(status_code=404, detail=f"User {user_id} not found")
        if req.role is not None:
            user.user_role = UserRole(req.role)
        if req.password:
            user.password_hash = _hash(req.password)
        if req.active is not None:
            user.active = req.active
        self.db.commit()
        self.db.refresh(user)
        return _to_dict(user)

    def delete(self, user_id: int) -> None:
        user = self.db.get(AppUser, user_id)
        if not user:
            raise HTTPException(status_code=404, detail=f"User {user_id} not found")
        self.db.delete(user)
        self.db.commit()


def _to_dict(user: AppUser) -> dict:
    return {
        "id":     user.id,
        "email":  user.email,
        "role":   user.user_role.value,
        "active": user.active,
    }
