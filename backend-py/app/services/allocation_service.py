from datetime import date
from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.allocation import Allocation
from app.models.person import Person
from app.models.squad import Squad
from app.models.lookup import DeveloperRole, Technology
from app.services.helpers import allocation_to_dict
from app.schemas.allocation import CreateAllocationRequest


class AllocationService:
    def __init__(self, db: Session):
        self.db = db

    def find_all(self, is_admin: bool) -> list:
        allocations = (
            self.db.query(Allocation)
            .filter(Allocation.active == True)
            .order_by(Allocation.squad_id, Allocation.person_id)
            .all()
        )
        return [allocation_to_dict(a, is_admin) for a in allocations]

    def create(self, req: CreateAllocationRequest) -> dict:
        person = self.db.get(Person, req.personId)
        if not person:
            raise HTTPException(status_code=404, detail=f"Person {req.personId} not found")
        squad = self.db.get(Squad, req.squadId)
        if not squad:
            raise HTTPException(status_code=404, detail=f"Squad {req.squadId} not found")

        alloc = Allocation(
            person_id=req.personId,
            squad_id=req.squadId,
            allocation_percent=req.allocationPercent,
            public_comment=req.publicComment,
            admin_note=req.adminNote,
            start_date=_parse_date(req.startDate),
            end_date=_parse_date(req.endDate),
        )
        alloc.roles        = self._resolve_roles(req.roleIds)
        alloc.technologies = self._resolve_technologies(req.technologyIds)
        self.db.add(alloc)
        self.db.commit()
        self.db.refresh(alloc)
        return allocation_to_dict(alloc, is_admin=True)

    def update(self, alloc_id: int, req: CreateAllocationRequest) -> dict:
        alloc = self.db.get(Allocation, alloc_id)
        if not alloc:
            raise HTTPException(status_code=404, detail=f"Allocation {alloc_id} not found")
        alloc.person_id          = req.personId
        alloc.squad_id           = req.squadId
        alloc.allocation_percent = req.allocationPercent
        alloc.public_comment     = req.publicComment
        alloc.admin_note         = req.adminNote
        alloc.start_date         = _parse_date(req.startDate)
        alloc.end_date           = _parse_date(req.endDate)
        alloc.roles              = self._resolve_roles(req.roleIds)
        alloc.technologies       = self._resolve_technologies(req.technologyIds)
        self.db.commit()
        self.db.refresh(alloc)
        return allocation_to_dict(alloc, is_admin=True)

    def delete(self, alloc_id: int) -> None:
        alloc = self.db.get(Allocation, alloc_id)
        if not alloc:
            raise HTTPException(status_code=404, detail=f"Allocation {alloc_id} not found")
        alloc.active = False
        self.db.commit()

    # ── helpers ──────────────────────────────────────────────────────────────

    def _resolve_roles(self, role_ids) -> list:
        if not role_ids:
            return []
        return self.db.query(DeveloperRole).filter(DeveloperRole.id.in_(role_ids)).all()

    def _resolve_technologies(self, tech_ids) -> list:
        if not tech_ids:
            return []
        return self.db.query(Technology).filter(Technology.id.in_(tech_ids)).all()


def _parse_date(value) -> date | None:
    if not value:
        return None
    if isinstance(value, date):
        return value
    return date.fromisoformat(str(value))
