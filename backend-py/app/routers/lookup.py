from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth.dependencies import get_current_user
from app.models.lookup import Technology, DeveloperRole, Tag

router = APIRouter(prefix="/api/lookup", tags=["lookup"])


@router.get("/technologies")  # public — no auth required
def list_technologies(db: Session = Depends(get_db)):
    rows = db.query(Technology).order_by(Technology.name).all()
    return [{"id": r.id, "name": r.name} for r in rows]


@router.get("/roles", dependencies=[Depends(get_current_user)])
def list_roles(db: Session = Depends(get_db)):
    rows = db.query(DeveloperRole).order_by(DeveloperRole.name).all()
    return [{"id": r.id, "name": r.name} for r in rows]


@router.get("/tags", dependencies=[Depends(get_current_user)])
def list_tags(db: Session = Depends(get_db)):
    rows = db.query(Tag).order_by(Tag.name).all()
    return [{"id": r.id, "name": r.name, "slug": r.slug} for r in rows]
