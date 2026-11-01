from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.auth.dependencies import get_current_user, require_admin, get_is_admin
from app.schemas.allocation import CreateAllocationRequest
from app.services.allocation_service import AllocationService

router = APIRouter(prefix="/api/allocations", tags=["allocations"])


@router.get("", dependencies=[Depends(get_current_user)])
def list_allocations(is_admin: bool = Depends(get_is_admin), db: Session = Depends(get_db)):
    return AllocationService(db).find_all(is_admin)


@router.post("", status_code=201, dependencies=[Depends(require_admin)])
def create_allocation(req: CreateAllocationRequest, db: Session = Depends(get_db)):
    return AllocationService(db).create(req)


@router.put("/{alloc_id}", dependencies=[Depends(require_admin)])
def update_allocation(alloc_id: int, req: CreateAllocationRequest, db: Session = Depends(get_db)):
    return AllocationService(db).update(alloc_id, req)


@router.delete("/{alloc_id}", status_code=204, dependencies=[Depends(require_admin)])
def delete_allocation(alloc_id: int, db: Session = Depends(get_db)):
    AllocationService(db).delete(alloc_id)
