# Squadron MCP Server

Exposes the Squadron allocation tracker to AI assistants via the [Model Context Protocol](https://modelcontextprotocol.io/): tools for querying/managing squads, people, and allocations, plus a few resources and staffing prompts. Talks to the Java backend (`../backend/`) over its REST API - it has no direct database access and no business logic of its own beyond simple search filtering.

## Setup

```
cd mcp-server
python -m venv .venv
.venv\Scripts\pip install -r requirements.txt
copy .env.example .env   # then edit if your backend isn't on the defaults below
```

Defaults (see `.env.example`): API at `http://localhost:8080`, logs in as the seeded local admin (`admin@squadron.local` / `admin123`).

## Running

The backend must be running first: `cd ../backend && mvn spring-boot:run -Dspring-boot.run.profiles=local`.

This server is launched by Claude Code itself via stdio - see `.mcp.json` (also mirrored at the repo root) - so there's no separate "start" step; it starts on demand when a `squadron` tool is called. To run it standalone for debugging: `.venv\Scripts\python server.py`.

## Layout
- `server.py` - tool/resource/prompt definitions (`FastMCP`)
- `client.py` - thin `httpx` wrapper handling login + bearer-token auth against the backend

Read-only "analytics" tools (`squad_capacity_summary`, `person_allocation_summary`, `find_available_people`, `search_allocations`) call the backend directly rather than fetching everything and filtering here - `GET /api/persons` and `GET /api/allocations` both accept the relevant query params server-side.
