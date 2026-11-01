from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.schemas.auth import AuthRequest
from app.services.auth_service import AuthService

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/login")
def login(req: AuthRequest, db: Session = Depends(get_db)):
    return AuthService(db).login(req.email, req.password)
