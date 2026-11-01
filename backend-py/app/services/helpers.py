"""Shared DTO-building helpers used by multiple services."""
from app.models.allocation import Allocation


def allocation_to_dict(a: Allocation, is_admin: bool) -> dict:
    dto = {
        "id":                a.id,
        "personId":          a.person_id,
        "personName":        a.person.name,
        "squadId":           a.squad_id,
        "squadName":         a.squad.name,
        "roles":             sorted([r.name for r in a.roles]),
        "technologies":      sorted([t.name for t in a.technologies]),
        "allocationPercent": a.allocation_percent,
        "publicComment":     a.public_comment,
        "startDate":         a.start_date.isoformat() if a.start_date else None,
        "endDate":           a.end_date.isoformat() if a.end_date else None,
        "active":            a.active,
    }
    if is_admin:
        dto["adminNote"] = a.admin_note
    return dto
