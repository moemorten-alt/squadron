from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth.dependencies import get_current_user, require_admin, get_is_admin
from app.schemas.person import CreatePersonRequest
from app.services.person_service import PersonService

router = APIRouter(prefix="/api/persons", tags=["persons"])


@router.get("", dependencies=[Depends(get_current_user)])
def list_persons(is_admin: bool = Depends(get_is_admin), db: Session = Depends(get_db)):
    return PersonService(db).find_all(is_admin)


@router.get("/{person_id}", dependencies=[Depends(get_current_user)])
def get_person(person_id: int, is_admin: bool = Depends(get_is_admin), db: Session = Depends(get_db)):
    return PersonService(db).find_by_id(person_id, is_admin)


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_person(req: CreatePersonRequest, db: Session = Depends(get_db)):
    return PersonService(db).create(req)


@router.put("/{person_id}", dependencies=[Depends(require_admin)])
def update_person(person_id: int, req: CreatePersonRequest, db: Session = Depends(get_db)):
    return PersonService(db).update(person_id, req)


@router.delete("/{person_id}", status_code=204, dependencies=[Depends(require_admin)])
def deactivate_person(person_id: int, db: Session = Depends(get_db)):
    PersonService(db).deactivate(person_id)
