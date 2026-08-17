"""Squadron MCP Server — exposes tools, resources, and prompts for Claude."""
from __future__ import annotations

import httpx
from mcp.server.fastmcp import FastMCP

from client import ApiClient

mcp = FastMCP(
    "Squadron",
    instructions=(
        "You have access to the Squadron allocation tracker — a system that manages "
        "~77 developers across ~20 squads. Use these tools to query and manage "
        "developer squad allocations. For write operations, you must be authenticated "
        "as an admin."
    ),
)
api = ApiClient()


# ─── HELPERS ──────────────────────────────────────────────────────────────────

def _fmt_alloc(a: dict, show_person: bool = True, show_squad: bool = True) -> str:
    parts = []
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
    lines = [f"{'Squad':<35} {'People':>6}  {'Total %':>7}"]
    lines.append("-" * 52)
    for s in squads:
        lines.append(f"{s['name']:<35} {s['totalHeadcount']:>6}  {s['totalAllocationPercent']:>6}%")
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
    params = {}
    if squad_name:
        params["squadName"] = squad_name
    if person_name:
        params["personName"] = person_name
    if role:
        params["role"] = role
    if technology:
        params["technology"] = technology
    if min_percent:
        params["minPercent"] = min_percent
    results = await api.get("/api/allocations", params=params)
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
    available = await api.get("/api/persons", params={"maxAllocation": max_total_allocation})
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
async def add_person(name: str, email: str = "", admin_note: str = "", tag_ids: list[int] = []) -> str:
    """Create a new person. Admin only."""
    try:
        payload: dict = {"name": name}
        if email:
            payload["email"] = email
        if admin_note:
            payload["adminNote"] = admin_note
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


@mcp.tool()
async def add_allocation(
    person_id: int,
    squad_id: int,
    allocation_percent: int,
    role_ids: list[int],
    technology_ids: list[int] = [],
    public_comment: str = "",
    start_date: str = "",
    end_date: str = "",
) -> str:
    """Assign a person to a squad. Use list_lookup to get role/technology IDs. Admin only."""
    try:
        payload: dict = {
            "personId": person_id,
            "squadId": squad_id,
            "allocationPercent": allocation_percent,
            "roleIds": role_ids,
        }
        if technology_ids:
            payload["technologyIds"] = technology_ids
        if public_comment:
            payload["publicComment"] = public_comment
        if start_date:
            payload["startDate"] = start_date
        if end_date:
            payload["endDate"] = end_date
        result = await api.post("/api/allocations", payload)
        return (
            f"Created allocation id={result['id']}: "
            f"{result['personName']} → {result['squadName']} at {result['allocationPercent']}%"
        )
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def update_allocation(
    allocation_id: int,
    allocation_percent: int | None = None,
    role_ids: list[int] | None = None,
    technology_ids: list[int] | None = None,
    public_comment: str | None = None,
) -> str:
    """Update an existing allocation's percent, roles, or technologies. Admin only.
    Only provide the fields you want to change; others will be kept as-is."""
    try:
        all_allocs = await api.get("/api/allocations")
        current = next((a for a in all_allocs if a["id"] == allocation_id), None)
        if not current:
            return f"Allocation id={allocation_id} not found."

        # Map role/tech names back to IDs for fields not being overridden
        lookup_roles = await api.get("/api/lookup/roles")
        lookup_techs = await api.get("/api/lookup/technologies")
        role_name_to_id = {r["name"]: r["id"] for r in lookup_roles}
        tech_name_to_id = {t["name"]: t["id"] for t in lookup_techs}

        payload = {
            "personId": current["personId"],
            "squadId": current["squadId"],
            "allocationPercent": allocation_percent if allocation_percent is not None else current["allocationPercent"],
            "roleIds": role_ids if role_ids is not None else [role_name_to_id[r] for r in current.get("roles", []) if r in role_name_to_id],
            "technologyIds": technology_ids if technology_ids is not None else [tech_name_to_id[t] for t in current.get("technologies", []) if t in tech_name_to_id],
            "publicComment": public_comment if public_comment is not None else (current.get("publicComment") or ""),
        }
        result = await api.put(f"/api/allocations/{allocation_id}", payload)
        return (
            f"Updated allocation id={result['id']}: "
            f"{result['personName']} → {result['squadName']} at {result['allocationPercent']}%"
        )
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


@mcp.tool()
async def remove_allocation(allocation_id: int) -> str:
    """Remove a person from a squad (soft delete). Admin only."""
    try:
        await api.delete(f"/api/allocations/{allocation_id}")
        return f"Removed allocation id={allocation_id}."
    except httpx.HTTPStatusError as e:
        return f"Error {e.response.status_code}: {e.response.text}"


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
