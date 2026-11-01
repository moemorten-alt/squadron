"""Squadron MCP Server — exposes tools, resources, and prompts for Claude."""
from __future__ import annotations

import httpx
from mcp.server.fastmcp import FastMCP
from mcp.server.fastmcp.exceptions import ToolError
from pydantic import BaseModel, Field

from client import ApiClient

mcp = FastMCP(
    "Squadron",
    instructions=(
        "You have access to the Squadron allocation tracker — a system that manages "
        "~77 developers across ~20 squads. Use these tools to query and manage "
        "developer squad allocations. For write operations, you must be authenticated "
        "as an admin. Allocation lines show their id as [alloc N]. To change "
        "someone's split, prefer set_person_allocations (one atomic call, with "
        "dry_run); update_allocation and remove_allocation accept person_id + "
        "squad instead of an allocation id."
    ),
)
api = ApiClient()


# ─── HELPERS ──────────────────────────────────────────────────────────────────

def _fmt_alloc(a: dict, show_person: bool = True, show_squad: bool = True) -> str:
    # The allocation's own id leads the line. update_allocation and
    # remove_allocation both take it, and without it there is no way to obtain
    # one through this server - those tools become unreachable.
    parts = [f"[alloc {a['id']}]"]
    if show_person:
        parts.append(f"{a['personName']} (id={a['personId']})")
    if show_squad:
        parts.append(f"→ {a['squadName']} (id={a['squadId']})")
    parts.append(f"{a['allocationPercent']}%")
    if a.get("roles"):
        parts.append(f"roles: {', '.join(a['roles'])}")
    if a.get("technologies"):
        parts.append(f"tech: {', '.join(a['technologies'])}")
    if a.get("publicComment"):
        parts.append(f"note: {a['publicComment']}")
    if a.get("startDate"):
        parts.append(f"from {a['startDate']}")
    if a.get("endDate"):
        parts.append(f"to {a['endDate']}")
    return "  " + " | ".join(parts)


# ─── TOOLS: READ ──────────────────────────────────────────────────────────────

@mcp.tool()
async def list_squads() -> str:
    """List all squads with headcount and total allocation percentage."""
    squads = await api.get("/api/squads")
    if not squads:
        return "No squads found."
    lines = [f"{'id':>4}  {'Squad':<35} {'People':>6}  {'Total %':>7}"]
    lines.append("-" * 58)
    for s in squads:
        lines.append(
            f"{s['id']:>4}  {s['name']:<35} {s['totalHeadcount']:>6}  {s['totalAllocationPercent']:>6}%"
        )
    return "\n".join(lines)


@mcp.tool()
async def get_squad(squad_id: int) -> str:
    """Get full details for a squad including all member allocations."""
    squad = await api.get(f"/api/squads/{squad_id}")
    lines = [
        f"Squad: {squad['name']} (id={squad['id']})",
        f"Description: {squad.get('description') or '—'}",
        f"Headcount: {squad['totalHeadcount']}  |  Total allocation: {squad['totalAllocationPercent']}%",
        "",
        "Members:",
    ]
    for a in squad.get("allocations", []):
        lines.append(_fmt_alloc(a, show_squad=False))
    return "\n".join(lines)


@mcp.tool()
async def list_persons(search: str = "") -> str:
    """List all people, optionally filtered by name. Shows squad memberships and total allocation."""
    persons = await api.get("/api/persons")
    if search:
        persons = [p for p in persons if search.lower() in p["name"].lower()]
    if not persons:
        return f"No people found{' matching ' + repr(search) if search else ''}."
    lines = [f"{'Name':<30} {'Total %':>7}  Squads"]
    lines.append("-" * 70)
    for p in persons:
        squads = [a["squadName"] for a in p.get("allocations", [])]
        tags = f"  [{', '.join(p['tags'])}]" if p.get("tags") else ""
        lines.append(
            f"[{p['id']:>3}] {p['name']:<26} {p.get('totalAllocation', 0):>6}%  {', '.join(squads) or '—'}{tags}"
        )
    return "\n".join(lines)


@mcp.tool()
async def get_person(person_id: int) -> str:
    """Get full profile for a person: squad allocations, roles, technologies, tags."""
    p = await api.get(f"/api/persons/{person_id}")
    lines = [
        f"Person: {p['name']} (id={p['id']})",
        f"Email: {p.get('email') or '—'}",
        f"Total allocation: {p.get('totalAllocation', 0)}%",
    ]
    if p.get("tags"):
        lines.append(f"Tags: {', '.join(p['tags'])}")
    lines += ["", "Allocations:"]
    for a in p.get("allocations", []):
        lines.append(_fmt_alloc(a, show_person=False))
    if not p.get("allocations"):
        lines.append("  (none)")
    return "\n".join(lines)


@mcp.tool()
async def search_allocations(
    squad_name: str = "",
    person_name: str = "",
    role: str = "",
    technology: str = "",
    min_percent: int = 0,
) -> str:
    """Search allocations with optional filters: squad_name, person_name, role, technology, min_percent."""
    results = await api.get("/api/allocations")
    if squad_name:
        results = [a for a in results if squad_name.lower() in a["squadName"].lower()]
    if person_name:
        results = [a for a in results if person_name.lower() in a["personName"].lower()]
    if role:
        results = [a for a in results if any(role.lower() in r.lower() for r in a.get("roles", []))]
    if technology:
        results = [a for a in results if any(technology.lower() in t.lower() for t in a.get("technologies", []))]
    if min_percent:
        results = [a for a in results if a["allocationPercent"] >= min_percent]
    if not results:
        return "No allocations found matching the given filters."
    lines = [f"Found {len(results)} allocation(s):"]
    for a in results:
        lines.append(_fmt_alloc(a))
    return "\n".join(lines)


@mcp.tool()
async def list_lookup(kind: str = "all") -> str:
    """List lookup values. kind = 'technologies' | 'roles' | 'tags' | 'all'."""
    lines = []
    if kind in ("technologies", "all"):
        techs = await api.get("/api/lookup/technologies")
        lines.append("Technologies:")
        lines.append("  " + ", ".join(f"{t['name']} (id={t['id']})" for t in techs))
    if kind in ("roles", "all"):
        roles = await api.get("/api/lookup/roles")
        lines.append("Developer Roles:")
        lines.append("  " + ", ".join(f"{r['name']} (id={r['id']})" for r in roles))
    if kind in ("tags", "all"):
        tags = await api.get("/api/lookup/tags")
        lines.append("Tags:")
        lines.append("  " + ", ".join(f"{t['name']} (id={t['id']})" for t in tags))
    if not lines:
        return "Invalid kind. Use 'technologies', 'roles', 'tags', or 'all'."
    return "\n".join(lines)


# ─── TOOLS: ANALYTICS ─────────────────────────────────────────────────────────

@mcp.tool()
async def find_available_people(max_total_allocation: int = 80) -> str:
    """Find people with total allocation at or below a threshold (default 80%). Sorted by capacity."""
    persons = await api.get("/api/persons")
    available = [p for p in persons if p.get("totalAllocation", 0) <= max_total_allocation]
    available.sort(key=lambda p: p.get("totalAllocation", 0))
    if not available:
        return f"No people found with total allocation ≤ {max_total_allocation}%."
    lines = [f"People with ≤ {max_total_allocation}% total allocation ({len(available)} found):", ""]
    for p in available:
        free = 100 - p.get("totalAllocation", 0)
        squads = [a["squadName"] for a in p.get("allocations", [])]
        squad_str = f"  currently in: {', '.join(squads)}" if squads else "  no current squads"
        lines.append(f"  [{p['id']:>3}] {p['name']:<30} {p.get('totalAllocation', 0):>3}% used  {free:>3}% free{squad_str}")
    return "\n".join(lines)


@mcp.tool()
async def squad_capacity_summary() -> str:
    """Show all squads ranked by total allocation %, useful for spotting over/under-loaded squads."""
    squads = await api.get("/api/squads")
    squads.sort(key=lambda s: s.get("totalAllocationPercent", 0), reverse=True)
    lines = [f"{'Squad':<35} {'People':>6}  {'Total %':>7}  Load"]
    lines.append("-" * 65)
    for s in squads:
        pct = s.get("totalAllocationPercent", 0)
        bar = "█" * min(pct // 10, 20)
        lines.append(f"{s['name']:<35} {s['totalHeadcount']:>6}  {pct:>6}%  {bar}")
    return "\n".join(lines)


@mcp.tool()
async def person_allocation_summary() -> str:
    """Show all people with total %, flagging over-allocated (>100%) and under-used (<20%)."""
    persons = await api.get("/api/persons")
    persons.sort(key=lambda p: p.get("totalAllocation", 0), reverse=True)
    over, under, normal = 0, 0, 0
    lines = [f"{'Name':<30} {'Total %':>7}  Status"]
    lines.append("-" * 55)
    for p in persons:
        total = p.get("totalAllocation", 0)
        if total > 100:
            flag = "⚠ OVER-ALLOCATED"
            over += 1
        elif total < 20:
            flag = "○ under-used"
            under += 1
        else:
            flag = ""
            normal += 1
        lines.append(f"[{p['id']:>3}] {p['name']:<26} {total:>6}%  {flag}")
    lines += ["", f"Summary: {over} over-allocated | {under} under-used (<20%) | {normal} normal"]
    return "\n".join(lines)


# ─── TOOLS: WRITE (ADMIN) ─────────────────────────────────────────────────────

@mcp.tool()
async def add_person(
    name: str,
    email: str = "",
    admin_note: str = "",
    leaver_date: str = "",
    tag_ids: list[int] = [],
) -> str:
    """Create a new person. Admin only.

    leaver_date is an ISO date (YYYY-MM-DD) for when they left or will leave
    the company; leave it empty if they are still here. Future dates are
    valid. It records a fact only - it does not remove them from squads or
    change capacity.
    """
    try:
        payload: dict = {"name": name}
        if email:
            payload["email"] = email
        if admin_note:
            payload["adminNote"] = admin_note
        if leaver_date:
            payload["leaverDate"] = leaver_date
        if tag_ids:
            payload["tagIds"] = tag_ids
        result = await api.post("/api/persons", payload)
        return f"Created person: {result['name']} (id={result['id']})"
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def add_squad(name: str, description: str = "") -> str:
    """Create a new squad. Admin only."""
    try:
        payload: dict = {"name": name}
        if description:
            payload["description"] = description
        result = await api.post("/api/squads", payload)
        return f"Created squad: {result['name']} (id={result['id']})"
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


# ─── ALLOCATION WRITES ────────────────────────────────────────────────────────
#
# These return structured data (dicts, surfaced as JSON) rather than prose, so
# a caller can read allocation_id / squad_id / percent without parsing text.
# Failures raise ToolError so the client sees a real error, not a string that
# looks like success.

class SplitItem(BaseModel):
    """One squad in a person's split."""
    squad: int | str = Field(description="Squad id, or squad name (case-insensitive; a unique partial name also works).")
    percent: int = Field(ge=1, le=100, description="Allocation percent for this squad.")
    role_ids: list[int] | None = Field(default=None, description="Optional. Omit to keep current roles, or for a new squad to copy roles from the person's largest allocation.")
    technology_ids: list[int] | None = Field(default=None, description="Optional. Same defaulting as role_ids.")


def _api_error(e: httpx.HTTPStatusError) -> ToolError:
    try:
        msg = e.response.json().get("error") or e.response.text
    except ValueError:
        msg = e.response.text
    return ToolError(f"Squadron API {e.response.status_code}: {msg}")


def _alloc_json(a: dict) -> dict:
    return {
        "allocation_id": a["id"],
        "person_id": a["personId"],
        "person_name": a["personName"],
        "squad_id": a["squadId"],
        "squad_name": a["squadName"],
        "percent": a["allocationPercent"],
        "roles": a.get("roles", []),
        "technologies": a.get("technologies", []),
        "public_comment": a.get("publicComment"),
        "start_date": a.get("startDate"),
        "end_date": a.get("endDate"),
    }


async def _resolve_squad_id(squad: int | str) -> int:
    """Accept a squad id or a name. Exact (case-insensitive) name wins; else a unique substring."""
    if isinstance(squad, int) or (isinstance(squad, str) and squad.strip().isdigit()):
        return int(squad)
    needle = squad.strip().lower()
    squads = await api.get("/api/squads")
    exact = [s for s in squads if s["name"].lower() == needle]
    if len(exact) == 1:
        return exact[0]["id"]
    partial = [s for s in squads if needle in s["name"].lower()]
    if len(partial) == 1:
        return partial[0]["id"]
    if not partial:
        raise ToolError(f"No squad matches {squad!r}. Use list_squads to see names and ids.")
    names = ", ".join(f"{s['name']} (id={s['id']})" for s in partial)
    raise ToolError(f"{squad!r} matches several squads: {names}. Pass the squad id.")


async def _find_allocation(
    allocation_id: int | None, person_id: int | None, squad: int | str | None
) -> dict:
    """Locate one active allocation, by its id or by person + squad."""
    all_allocs = await api.get("/api/allocations")
    if allocation_id is not None:
        if person_id is not None or squad is not None:
            raise ToolError("Pass either allocation_id, or person_id + squad - not both.")
        found = next((a for a in all_allocs if a["id"] == allocation_id), None)
        if not found:
            raise ToolError(f"No active allocation with id={allocation_id}.")
        return found
    if person_id is None or squad is None:
        raise ToolError("Identify the allocation with allocation_id, or with person_id + squad.")
    squad_id = await _resolve_squad_id(squad)
    matches = [a for a in all_allocs if a["personId"] == person_id and a["squadId"] == squad_id]
    if not matches:
        raise ToolError(
            f"Person id={person_id} has no active allocation in squad id={squad_id}. "
            "Use add_allocation or set_person_allocations to create one."
        )
    if len(matches) > 1:
        ids = ", ".join(str(a["id"]) for a in matches)
        raise ToolError(f"Person id={person_id} has several allocations in squad id={squad_id} ({ids}); pass allocation_id.")
    return matches[0]


@mcp.tool()
async def set_person_allocations(
    person_id: int, allocations: list[SplitItem], dry_run: bool = False
) -> dict:
    """Set a person's WHOLE split in one atomic call. Admin only.

    The best tool for reallocation requests like "Diego: Plan 75%, Resolut 15%,
    Imagevault 10%". Each listed squad is updated (keeping its dates, notes and
    allocation id) or created. Any squad the person is in that is NOT listed is
    removed. All of it happens in one database transaction: if any item is
    invalid nothing changes.

    Returns before/after splits, a per-squad list of changes
    (created | updated | removed | unchanged), totals, and warnings (e.g. over
    100%). Use dry_run=true to preview without saving - recommended when the
    change removes a squad.
    """
    try:
        items = []
        for item in allocations:
            body: dict = {
                "squadId": await _resolve_squad_id(item.squad),
                "allocationPercent": item.percent,
            }
            if item.role_ids is not None:
                body["roleIds"] = item.role_ids
            if item.technology_ids is not None:
                body["technologyIds"] = item.technology_ids
            items.append(body)
        r = await api.put(f"/api/persons/{person_id}/allocations", {"items": items, "dryRun": dry_run})
    except httpx.HTTPStatusError as e:
        raise _api_error(e)

    def entry(e: dict) -> dict:
        return {"allocation_id": e.get("allocationId"), "squad_id": e["squadId"],
                "squad_name": e["squadName"], "percent": e["allocationPercent"], "roles": e.get("roles", [])}

    return {
        "dry_run": r["dryRun"],
        "applied": not r["dryRun"],
        "person_id": r["personId"],
        "person_name": r["personName"],
        "total_before": r["totalBefore"],
        "total_after": r["totalAfter"],
        "before": [entry(e) for e in r["before"]],
        "after": [entry(e) for e in r["after"]],
        "changes": [
            {"action": c["action"], "allocation_id": c.get("allocationId"), "squad_id": c["squadId"],
             "squad_name": c["squadName"], "from_percent": c.get("fromPercent"), "to_percent": c.get("toPercent")}
            for c in r["changes"]
        ],
        "warnings": r["warnings"],
    }


@mcp.tool()
async def add_allocation(
    person_id: int,
    squad: int | str,
    allocation_percent: int,
    role_ids: list[int],
    technology_ids: list[int] = [],
    public_comment: str = "",
    start_date: str = "",
    end_date: str = "",
    dry_run: bool = False,
) -> dict:
    """Assign a person to one more squad (squad = id or name). Admin only.

    Use list_lookup for role/technology ids. To change someone's whole split,
    prefer set_person_allocations. dry_run=true validates and previews only.
    """
    try:
        squad_id = await _resolve_squad_id(squad)
        payload: dict = {
            "personId": person_id,
            "squadId": squad_id,
            "allocationPercent": allocation_percent,
            "roleIds": role_ids,
            "technologyIds": technology_ids,
            "publicComment": public_comment or None,
            "startDate": start_date or None,
            "endDate": end_date or None,
        }
        if dry_run:
            person = await api.get(f"/api/persons/{person_id}")
            target = await api.get(f"/api/squads/{squad_id}")
            existing = [a for a in person.get("allocations", []) if a["squadId"] == squad_id]
            warnings = []
            if existing:
                warnings.append(f"{person['name']} is already in {target['name']} (allocation_id={existing[0]['id']}); "
                                "use update_allocation instead.")
            total_after = person.get("totalAllocation", 0) + allocation_percent
            if total_after > 100:
                warnings.append(f"{person['name']} would be allocated {total_after}%, over 100%.")
            return {"dry_run": True, "applied": False, "person_id": person_id, "person_name": person["name"],
                    "squad_id": squad_id, "squad_name": target["name"], "percent": allocation_percent,
                    "total_after": total_after, "warnings": warnings}
        result = await api.post("/api/allocations", payload)
        return {"dry_run": False, "applied": True, "created": _alloc_json(result)}
    except httpx.HTTPStatusError as e:
        raise _api_error(e)


@mcp.tool()
async def update_allocation(
    allocation_id: int | None = None,
    person_id: int | None = None,
    squad: int | str | None = None,
    allocation_percent: int | None = None,
    role_ids: list[int] | None = None,
    technology_ids: list[int] | None = None,
    public_comment: str | None = None,
    dry_run: bool = False,
) -> dict:
    """Change one allocation. Admin only.

    Identify it EITHER by allocation_id OR by person_id + squad (squad id or
    name), e.g. update_allocation(person_id=46, squad="Plan", allocation_percent=75).
    Only the fields you pass change; dates, admin note and everything else are
    kept. Passing no fields to change is an error. To change several of a
    person's squads at once, use set_person_allocations instead.
    """
    if all(v is None for v in (allocation_percent, role_ids, technology_ids, public_comment)):
        raise ToolError(
            "Nothing to update: pass at least one of allocation_percent, role_ids, "
            "technology_ids, public_comment."
        )
    try:
        current = await _find_allocation(allocation_id, person_id, squad)

        lookup_roles = await api.get("/api/lookup/roles")
        lookup_techs = await api.get("/api/lookup/technologies")
        role_name_to_id = {r["name"]: r["id"] for r in lookup_roles}
        tech_name_to_id = {t["name"]: t["id"] for t in lookup_techs}
        current_role_ids = [role_name_to_id[r] for r in current.get("roles", []) if r in role_name_to_id]
        current_tech_ids = [tech_name_to_id[t] for t in current.get("technologies", []) if t in tech_name_to_id]

        # The API's PUT replaces the whole allocation: every field must be sent,
        # including the dates and admin note, or they are silently erased.
        payload = {
            "personId": current["personId"],
            "squadId": current["squadId"],
            "allocationPercent": current["allocationPercent"] if allocation_percent is None else allocation_percent,
            "roleIds": current_role_ids if role_ids is None else role_ids,
            "technologyIds": current_tech_ids if technology_ids is None else technology_ids,
            "publicComment": current.get("publicComment") if public_comment is None else (public_comment or None),
            "adminNote": current.get("adminNote"),
            "startDate": current.get("startDate"),
            "endDate": current.get("endDate"),
        }

        changed = []
        if payload["allocationPercent"] != current["allocationPercent"]:
            changed.append("percent")
        if sorted(payload["roleIds"]) != sorted(current_role_ids):
            changed.append("roles")
        if sorted(payload["technologyIds"]) != sorted(current_tech_ids):
            changed.append("technologies")
        if (payload["publicComment"] or None) != (current.get("publicComment") or None):
            changed.append("public_comment")

        before = _alloc_json(current)
        if not changed:
            return {"dry_run": dry_run, "applied": False, "changed_fields": [],
                    "message": "Values already match; nothing was written.", "allocation": before}
        if dry_run:
            return {"dry_run": True, "applied": False, "changed_fields": changed, "before": before,
                    "would_set": {"percent": payload["allocationPercent"], "role_ids": payload["roleIds"],
                                  "technology_ids": payload["technologyIds"],
                                  "public_comment": payload["publicComment"]}}
        result = await api.put(f"/api/allocations/{current['id']}", payload)
        return {"dry_run": False, "applied": True, "changed_fields": changed,
                "before": before, "after": _alloc_json(result)}
    except httpx.HTTPStatusError as e:
        raise _api_error(e)


@mcp.tool()
async def update_person(
    person_id: int,
    name: str | None = None,
    email: str | None = None,
    admin_note: str | None = None,
    leaver_date: str | None = None,
    tag_ids: list[int] | None = None,
) -> str:
    """Update a person. Only the arguments you pass are changed. Admin only.

    leaver_date is an ISO date (YYYY-MM-DD); pass an empty string to clear it.
    """
    try:
        # The API's PUT replaces the whole person - any field missing from the
        # body is set to null. So read the current state and merge, or updating
        # one field would silently erase the others.
        current = await api.get(f"/api/persons/{person_id}")

        # PersonDto returns tags as NAMES, not ids, but the PUT body wants
        # tagIds. Resolve them back through the lookup table, or every update
        # would silently strip the person's tags.
        current_tag_ids: list[int] = []
        if current.get("tags"):
            all_tags = await api.get("/api/lookup/tags")
            by_name = {t["name"]: t["id"] for t in all_tags}
            current_tag_ids = [by_name[n] for n in current["tags"] if n in by_name]

        payload: dict = {
            "name": current["name"],
            "email": current.get("email"),
            "adminNote": current.get("adminNote"),
            "leaverDate": current.get("leaverDate"),
            "tagIds": current_tag_ids,
        }
        if name is not None:
            payload["name"] = name
        if email is not None:
            payload["email"] = email or None
        if admin_note is not None:
            payload["adminNote"] = admin_note or None
        if leaver_date is not None:
            payload["leaverDate"] = leaver_date or None
        if tag_ids is not None:
            payload["tagIds"] = tag_ids

        result = await api.put(f"/api/persons/{person_id}", payload)
        return (
            f"Updated person: {result['name']} (id={result['id']})"
            f" | leaver date: {result.get('leaverDate') or 'none'}"
        )
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def remove_person(person_id: int) -> str:
    """Deactivate a person (soft delete - they stop appearing in listings). Admin only.

    This is reversible in the database but there is no API to undo it, so
    confirm the id with get_person first.
    """
    try:
        person = await api.get(f"/api/persons/{person_id}")
        await api.delete(f"/api/persons/{person_id}")
        return f"Deactivated person: {person['name']} (id={person_id})."
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def update_squad(
    squad_id: int, name: str | None = None, description: str | None = None
) -> str:
    """Update a squad's name or description. Only what you pass is changed. Admin only."""
    try:
        # Same whole-object PUT semantics as persons - merge, do not replace.
        current = await api.get(f"/api/squads/{squad_id}")
        payload: dict = {
            "name": current["name"],
            "description": current.get("description"),
        }
        if name is not None:
            payload["name"] = name
        if description is not None:
            payload["description"] = description or None

        result = await api.put(f"/api/squads/{squad_id}", payload)
        return f"Updated squad: {result['name']} (id={result['id']})"
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def remove_squad(squad_id: int, confirm_name: str) -> str:
    """PERMANENTLY delete a squad and every allocation in it. Admin only. NOT REVERSIBLE.

    Unlike remove_person, this is a hard delete: the squad row is destroyed and
    the delete cascades to all of its allocations, which are gone for good. The
    people themselves are not deleted.

    confirm_name must exactly match the squad's current name. This is a guard
    against deleting the wrong id - look the squad up first and pass its name
    back. Ask the user before calling this.
    """
    try:
        squad = await api.get(f"/api/squads/{squad_id}")
        actual = squad["name"]
        count = len(squad.get("allocations", []))
        if confirm_name != actual:
            return (
                f"Refusing to delete. confirm_name {confirm_name!r} does not match "
                f"squad id={squad_id}, which is named {actual!r} and holds {count} "
                f"allocation(s). Re-run with the exact name if this is really intended."
            )
        await api.delete(f"/api/squads/{squad_id}")
        return (
            f"Permanently deleted squad {actual!r} (id={squad_id}) and its "
            f"{count} allocation(s). This cannot be undone."
        )
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def remove_allocation(
    allocation_id: int | None = None,
    person_id: int | None = None,
    squad: int | str | None = None,
    dry_run: bool = False,
) -> dict:
    """Remove a person from a squad (soft delete). Admin only.

    Identify the allocation EITHER by allocation_id OR by person_id + squad
    (id or name). Returns what was removed; dry_run=true previews only.
    """
    try:
        current = await _find_allocation(allocation_id, person_id, squad)
        if not dry_run:
            await api.delete(f"/api/allocations/{current['id']}")
        return {"dry_run": dry_run, "applied": not dry_run, "removed": _alloc_json(current)}
    except httpx.HTTPStatusError as e:
        raise _api_error(e)


# ─── RESOURCES ────────────────────────────────────────────────────────────────

@mcp.resource("squadron://squads")
async def squads_resource() -> str:
    """All squads with headcount and total allocation."""
    squads = await api.get("/api/squads")
    lines = ["# Squads\n"]
    for s in squads:
        lines.append(f"## {s['name']} (id={s['id']})")
        if s.get("description"):
            lines.append(s["description"])
        lines.append(f"- Headcount: {s['totalHeadcount']}")
        lines.append(f"- Total Allocation: {s['totalAllocationPercent']}%")
        lines.append("")
    return "\n".join(lines)


@mcp.resource("squadron://squads/{squad_id}")
async def squad_resource(squad_id: str) -> str:
    """Full detail for a squad including all allocations."""
    squad = await api.get(f"/api/squads/{squad_id}")
    lines = [
        f"# {squad['name']}",
        f"**Description:** {squad.get('description') or '—'}",
        f"**Headcount:** {squad['totalHeadcount']}  |  **Total Allocation:** {squad['totalAllocationPercent']}%",
        "",
        "## Members",
    ]
    for a in squad.get("allocations", []):
        line = f"- **{a['personName']}** — {a['allocationPercent']}%"
        if a.get("roles"):
            line += f" | {', '.join(a['roles'])}"
        if a.get("technologies"):
            line += f" | {', '.join(a['technologies'])}"
        if a.get("publicComment"):
            line += f" | _{a['publicComment']}_"
        lines.append(line)
    return "\n".join(lines)


@mcp.resource("squadron://persons")
async def persons_resource() -> str:
    """All people with squad memberships and allocation totals."""
    persons = await api.get("/api/persons")
    lines = ["# People\n"]
    for p in persons:
        lines.append(f"## {p['name']} (id={p['id']})")
        lines.append(f"- Total Allocation: {p.get('totalAllocation', 0)}%")
        if p.get("tags"):
            lines.append(f"- Tags: {', '.join(p['tags'])}")
        if p.get("allocations"):
            squads = [f"{a['squadName']} ({a['allocationPercent']}%)" for a in p["allocations"]]
            lines.append(f"- Squads: {', '.join(squads)}")
        lines.append("")
    return "\n".join(lines)


@mcp.resource("squadron://persons/{person_id}")
async def person_resource(person_id: str) -> str:
    """Full profile for a person including allocations, roles, and technologies."""
    p = await api.get(f"/api/persons/{person_id}")
    lines = [
        f"# {p['name']}",
        f"**Email:** {p.get('email') or '—'}",
        f"**Total Allocation:** {p.get('totalAllocation', 0)}%",
    ]
    if p.get("tags"):
        lines.append(f"**Tags:** {', '.join(p['tags'])}")
    lines += ["", "## Allocations"]
    for a in p.get("allocations", []):
        line = f"- **{a['squadName']}** — {a['allocationPercent']}%"
        if a.get("roles"):
            line += f" | {', '.join(a['roles'])}"
        if a.get("technologies"):
            line += f" | {', '.join(a['technologies'])}"
        if a.get("publicComment"):
            line += f" | _{a['publicComment']}_"
        lines.append(line)
    return "\n".join(lines)


@mcp.resource("squadron://lookup")
async def lookup_resource() -> str:
    """All lookup tables: technologies, roles, and tags with their IDs."""
    techs = await api.get("/api/lookup/technologies")
    roles = await api.get("/api/lookup/roles")
    tags = await api.get("/api/lookup/tags")
    return "\n".join([
        "# Lookup Tables",
        "",
        "## Technologies",
        ", ".join(f"{t['name']} (id={t['id']})" for t in techs),
        "",
        "## Developer Roles",
        ", ".join(f"{r['name']} (id={r['id']})" for r in roles),
        "",
        "## Tags",
        ", ".join(f"{t['name']} / {t['slug']} (id={t['id']})" for t in tags),
    ])


# ─── PROMPTS ──────────────────────────────────────────────────────────────────

@mcp.prompt()
def squad_health_report(squad_name: str) -> str:
    """Generate a full health report for a squad."""
    return f"""Using the squadron tools, generate a health report for the **{squad_name}** squad.

Steps:
1. Call `list_squads` to find the squad ID for "{squad_name}"
2. Call `get_squad` with that ID to get all members and allocations
3. For any member of interest, call `get_person` to check their total allocation across all squads

Report should include:
- Member list with roles, technologies, and allocation %
- Over-allocated members (total across all squads > 100%)
- Missing or underrepresented roles (e.g. no Tech Lead, no QA)
- Technology coverage gaps
- Members with very low allocation in this squad (possibly wasted slots)
- Specific recommendations"""


@mcp.prompt()
def find_person_for_role(role: str, technology: str = "", squad_name: str = "") -> str:
    """Find the best available person for a given role and optional technology."""
    context = f"**{role}** role"
    if technology:
        context += f" with **{technology}** experience"
    if squad_name:
        context += f" for the **{squad_name}** squad"
    return f"""I need to staff a {context}.

Steps:
1. Call `find_available_people` to get people with capacity (default ≤80% total allocation)
2. Call `search_allocations` filtered by role="{role}"{f' and technology="{technology}"' if technology else ''} to find people with the right experience
3. Cross-reference: who has both capacity AND the right skills?
4. Rank suggestions by fit and availability
5. Note any trade-offs (e.g. great fit but nearly fully allocated)"""


@mcp.prompt()
def rebalance_squad(squad_name: str) -> str:
    """Analyse a squad and suggest rebalancing changes."""
    return f"""Analyse the **{squad_name}** squad and suggest how to rebalance it.

Steps:
1. Call `list_squads` to find the squad ID
2. Call `get_squad` to see current members and allocations
3. For each member, call `get_person` to check their total allocation across all squads
4. Call `find_available_people` to see who could potentially be added

Flag:
- Anyone over 100% total (needs immediate reduction)
- Anyone under 20% in this squad (possibly a wasted slot)
- Missing key roles (no Tech Lead, no QA, etc.)

Propose specific changes with reasoning."""


@mcp.prompt()
def onboarding_summary(person_name: str) -> str:
    """Generate an onboarding summary for a person."""
    return f"""Generate an onboarding summary for **{person_name}**.

Steps:
1. Call `list_persons` with search="{person_name}" to find their ID
2. Call `get_person` to get their full profile
3. For each squad they're in, call `get_squad` to get full squad details

Produce a friendly summary covering:
- Which squads they're in and at what percentage
- Their roles and technologies
- Teammates in each squad (with a special note on Tech Leads)
- Any tags or notes visible to you
- Key contacts they should know"""


if __name__ == "__main__":
    mcp.run()
