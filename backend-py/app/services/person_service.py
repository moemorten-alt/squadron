from fastapi import HTTPException
from sqlalchemy.orm import Session
from app.models.person import Person
from app.models.lookup import Tag
from app.services.helpers import allocation_to_dict
from app.schemas.person import CreatePersonRequest


class PersonService:
    def __init__(self, db: Session):
        self.db = db

    def find_all(self, is_admin: bool) -> list:
        persons = (
            self.db.query(Person)
            .filter(Person.active == True)
            .order_by(Person.name)
            .all()
        )
        return [self._to_dict(p, is_admin) for p in persons]

    def find_by_id(self, person_id: int, is_admin: bool) -> dict:
        person = self.db.get(Person, person_id)
        if not person or not person.active:
            raise HTTPException(status_code=404, detail=f"Person {person_id} not found")
        return self._to_dict(person, is_admin)

    def create(self, req: CreatePersonRequest) -> dict:
        tags = self._resolve_tags(req.tagIds)
        person = Person(name=req.name, email=req.email, admin_note=req.adminNote)
        person.tags = tags
        self.db.add(person)
        self.db.commit()
        self.db.refresh(person)
        return self._to_dict(person, is_admin=True)

    def update(self, person_id: int, req: CreatePersonRequest) -> dict:
        person = self.db.get(Person, person_id)
        if not person or not person.active:
            raise HTTPException(status_code=404, detail=f"Person {person_id} not found")
        person.name       = req.name
        person.email      = req.email
        person.admin_note = req.adminNote
        person.tags       = self._resolve_tags(req.tagIds)
        self.db.commit()
        self.db.refresh(person)
        return self._to_dict(person, is_admin=True)

    def deactivate(self, person_id: int) -> None:
        person = self.db.get(Person, person_id)
        if not person or not person.active:
            raise HTTPException(status_code=404, detail=f"Person {person_id} not found")
        person.active = False
        self.db.commit()

    # ── helpers ──────────────────────────────────────────────────────────────

    def _resolve_tags(self, tag_ids) -> list:
        if not tag_ids:
            return []
        return self.db.query(Tag).filter(Tag.id.in_(tag_ids)).all()

    def _to_dict(self, person: Person, is_admin: bool) -> dict:
        active_allocs = [a for a in person.allocations if a.active]
        total = sum(a.allocation_percent for a in active_allocs)
        dto = {
            "id":              person.id,
            "name":            person.name,
            "email":           person.email,
            "active":          person.active,
            "tags":            sorted([t.name for t in person.tags]),
            "totalAllocation": total,
            "allocations":     [allocation_to_dict(a, is_admin) for a in active_allocs],
        }
        if is_admin:
            dto["adminNote"] = person.admin_note
        return dto
