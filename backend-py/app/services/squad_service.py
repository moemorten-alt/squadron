from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.squad import Squad
from app.services.helpers import allocation_to_dict
from app.schemas.squad import CreateSquadRequest


class SquadService:
    def __init__(self, db: Session):
        self.db = db

    def find_all(self, is_admin: bool) -> list:
        squads = self.db.query(Squad).order_by(Squad.name).all()
        return [self._to_dict(s, is_admin) for s in squads]

    def find_by_id(self, squad_id: int, is_admin: bool) -> dict:
        squad = self.db.get(Squad, squad_id)
        if not squad:
            raise HTTPException(status_code=404, detail=f"Squad {squad_id} not found")
        return self._to_dict(squad, is_admin)

    def create(self, req: CreateSquadRequest) -> dict:
        squad = Squad(name=req.name, description=req.description)
        self.db.add(squad)
        self.db.commit()
        self.db.refresh(squad)
        return self._to_dict(squad, is_admin=True)

    def update(self, squad_id: int, req: CreateSquadRequest) -> dict:
        squad = self.db.get(Squad, squad_id)
        if not squad:
            raise HTTPException(status_code=404, detail=f"Squad {squad_id} not found")
        squad.name        = req.name
        squad.description = req.description
        self.db.commit()
        self.db.refresh(squad)
        return self._to_dict(squad, is_admin=True)

    def delete(self, squad_id: int) -> None:
        squad = self.db.get(Squad, squad_id)
        if not squad:
            raise HTTPException(status_code=404, detail=f"Squad {squad_id} not found")
        self.db.delete(squad)
        self.db.commit()

    # ── helpers ──────────────────────────────────────────────────────────────

    def _to_dict(self, squad: Squad, is_admin: bool) -> dict:
        active_allocs = [a for a in squad.allocations if a.active]
        return {
            "id":                   squad.id,
            "name":                 squad.name,
            "description":          squad.description,
            "totalHeadcount":       len(active_allocs),
            "totalAllocationPercent": sum(a.allocation_percent for a in active_allocs),
            "allocations":          [allocation_to_dict(a, is_admin) for a in active_allocs],
        }
