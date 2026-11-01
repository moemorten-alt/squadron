from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth.dependencies import get_current_user, require_admin, get_is_admin
from app.schemas.squad import CreateSquadRequest
from app.services.squad_service import SquadService

router = APIRouter(prefix="/api/squads", tags=["squads"])


@router.get("", dependencies=[Depends(get_current_user)])
def list_squads(is_admin: bool = Depends(get_is_admin), db: Session = Depends(get_db)):
    return SquadService(db).find_all(is_admin)


@router.get("/{squad_id}", dependencies=[Depends(get_current_user)])
def get_squad(squad_id: int, is_admin: bool = Depends(get_is_admin), db: Session = Depends(get_db)):
    return SquadService(db).find_by_id(squad_id, is_admin)


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_squad(req: CreateSquadRequest, db: Session = Depends(get_db)):
    return SquadService(db).create(req)


@router.put("/{squad_id}", dependencies=[Depends(require_admin)])
def update_squad(squad_id: int, req: CreateSquadRequest, db: Session = Depends(get_db)):
    return SquadService(db).update(squad_id, req)


@router.delete("/{squad_id}", status_code=204, dependencies=[Depends(require_admin)])
def delete_squad(squad_id: int, db: Session = Depends(get_db)):
    SquadService(db).delete(squad_id)
