from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth.dependencies import require_admin
from app.schemas.user import CreateUserRequest, UpdateUserRequest
from app.services.user_service import UserService

router = APIRouter(prefix="/api/users", tags=["users"], dependencies=[Depends(require_admin)])


@router.get("")
def list_users(db: Session = Depends(get_db)):
    return UserService(db).find_all()


@router.post("", status_code=201)
def create_user(req: CreateUserRequest, db: Session = Depends(get_db)):
    return UserService(db).create(req)


@router.put("/{user_id}")
def update_user(user_id: int, req: UpdateUserRequest, db: Session = Depends(get_db)):
    return UserService(db).update(user_id, req)


@router.delete("/{user_id}", status_code=204)
def delete_user(user_id: int, db: Session = Depends(get_db)):
    UserService(db).delete(user_id)
