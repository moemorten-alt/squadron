# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Squadron is a squad-allocation tracker: it tracks which people are allocated to which squads, at what percentage, with what roles/technologies. There are two user roles - `ADMIN` (full CRUD, can see/edit private `adminNote` fields) and `VIEWER` (read-only, admin fields hidden).

The repo contains independent applications with no shared build tooling or root-level package manager - always `cd` into the one you're working on:
- `backend/` - Spring Boot 3.3.5 / Java 21 (Maven), package root `com.squadron`
- `frontend/` - Next.js 15 (App Router) / React 18 / TypeScript, Tailwind CSS, TanStack Table, SWR
- `mcp-server/` - Python MCP (`FastMCP`) server exposing squad/person/allocation tools to AI assistants; a thin REST client over `backend/`'s API, no direct DB access (see `mcp-server/README.md`)

## Commands

### Backend (`backend/`)
- Run locally: `mvn spring-boot:run -Dspring-boot.run.profiles=local` - API on `http://localhost:8080`, H2 console at `/h2-console`
- Run all tests: `mvn test`
- Run a single test class: `mvn -Dtest=ClassName test`
- Run a single test method: `mvn -Dtest=ClassName#methodName test`
- Coverage report (JaCoCo, generated automatically by `mvn test`): `target/site/jacoco/index.html`
- Build: `mvn clean package`

There is no Maven wrapper in this repo - use a system-installed `mvn`.

### Frontend (`frontend/`)
- Dev server: `npm run dev` - `http://localhost:3000`
- Build: `npm run build`
- Lint: `npm run lint`
- Tests (Vitest): `npm test` (single run) or `npm run test:watch`
- Run a single test file: `npx vitest run src/lib/utils.test.ts`

## Backend architecture

Standard layering: `controller/` -> `service/` -> `repository/` -> `entity/`, with `dto/` (Java records) as the API boundary - controllers never return entities directly.

- `entity/`: JPA entities - `Person`, `Squad`, `Allocation`, `AppUser`, `Technology`, `DeveloperRole`, `Tag`, `UserRole` (enum: `ADMIN`/`VIEWER`)
- `repository/`: Spring Data JPA repositories
- `service/`: business logic; read methods take an `isAdmin` boolean threaded from the controller (see "ADMIN-only fields" below)
- `controller/`: REST endpoints under `/api/**`; write endpoints (POST/PUT/DELETE) on persons/squads/allocations/users are gated with `@PreAuthorize("hasRole('ADMIN')")` at the method level
- `security/`: `JwtUtil` (mint/validate tokens), `JwtAuthFilter` (extracts the Bearer token, populates `SecurityContext`, runs before `UsernamePasswordAuthenticationFilter`), `SecurityConfig` (stateless filter chain, `@EnableMethodSecurity`), `UserDetailsServiceImpl`

### Central data model
`Allocation` (Person x Squad x %) is the core entity: `@ManyToOne` Person, `@ManyToOne` Squad, `@ManyToMany` `DeveloperRole`/`Technology` via join tables, plus `allocationPercent`, `publicComment`, `adminNote`, `startDate`/`endDate`. `Squad -> Allocation` is `CascadeType.ALL` with orphan removal, so deleting a Squad deletes its allocations.

### ADMIN-only fields
`adminNote` (on `Person` and `Allocation`) is visible to `ADMIN` only, and this isn't enforced by a separate endpoint or serialization annotation. Every relevant service method (`PersonService.findAll/findById`, and the equivalent on `AllocationService`/`SquadService`) takes an `isAdmin` boolean threaded from `Authentication` in the controller, and the DTO's static `.from(entity, isAdmin, ...)` factory conditionally omits `adminNote`. When adding a field that should be ADMIN-only, follow this same threading pattern rather than filtering at the controller.

### Auth behavior to know before touching it
- JWT-based, stateless (`SessionCreationPolicy.STATELESS`).
- `JwtAuthFilter` must catch both `io.jsonwebtoken.JwtException` and `IllegalArgumentException` when parsing a token - jjwt throws `IllegalArgumentException` (not `JwtException`) for a blank/empty token, and letting either escape crashes the filter into an uncaught 500 instead of a clean auth rejection (see `SecurityIntegrationTest`).
- No custom `AuthenticationEntryPoint` is configured, so unauthenticated requests currently return **403, not 401**. This is existing, tested behavior (pinned down in `SecurityIntegrationTest`), not a bug to silently "fix."

### Config profiles
- `application.yml` (base, always loaded): Postgres datasource for prod, plus a shared `squadron.h2.compat-params` property consumed by the H2 profiles below.
- `application-local.yml` (`local` profile): file-based H2 (`./data/squadron`), `ddl-auto: update`, Flyway disabled. Seeded by `LocalDataInitializer` (`@Profile("local")`), which runs `data.sql` via `ResourceDatabasePopulator` when the `squad` table is empty, then ensures a default admin user exists (`admin@squadron.local` / `admin123`, local/dev only).
- `src/test/resources/application-test.yml` (`test` profile): in-memory H2, `ddl-auto: create-drop`, isolated from local dev data.
- No profile / prod: Postgres, `ddl-auto: validate` - schema is owned by Flyway migrations in `src/main/resources/db/migration/`, which must be kept in sync with the entities by hand (there's no auto-DDL safety net in prod).
- All H2 profiles deliberately keep an explicit `hibernate.dialect: org.hibernate.dialect.H2Dialect` even though Hibernate's deprecation warning says it's unnecessary: dialect auto-detection picks the wrong dialect when `MODE=PostgreSQL` is in the JDBC URL and emits `insert ... returning id`, which H2 rejects.

## Frontend architecture

- App Router (`src/app/**/page.tsx`); routes are thin compositions of components from `src/components/`.
- `src/lib/api.ts` is the single fetch wrapper for the whole app - all backend calls go through the `api.*` object. It attaches the JWT from `localStorage` on every request and dispatches an `auth:expired` `CustomEvent` on a 401 response (consumed by `AuthContext` to force logout).
- `src/context/`: `AuthContext` (session/token), `ThemeContext`, `ToastContext`.

## MCP server (`mcp-server/`)

Exposes `backend/`'s data to AI assistants (tools like `list_squads`, `get_person`, `search_allocations`, `find_available_people`, plus a few resources and staffing prompts). Registered in `.mcp.json` (repo root, mirrored in `mcp-server/.mcp.json`) as a stdio server - Claude Code launches it on demand, there's no separate process to start manually.

- `search_allocations` and `find_available_people` filter server-side via query params on `GET /api/allocations` (`personName`/`squadName`/`technology`/`role`/`minPercent`, all case-insensitive substring match) and `GET /api/persons` (`maxAllocation`) - don't reintroduce client-side fetch-all-and-filter in `server.py` for these; extend the backend query params instead, matching the existing pattern in `AllocationService.findAll`/`PersonService.findAll`.
- `squad_capacity_summary` and `person_allocation_summary` need no backend query params - `GET /api/squads`/`GET /api/persons` already return `totalAllocationPercent`/`totalAllocation` per item (computed via `AllocationRepository.sumAllocationPercentByPersonId` etc.); the Python tools just sort/format for display.
- JSON field names match 1:1 with the Java DTOs (`personName`, `squadName`, `allocationPercent`, `totalHeadcount`, etc.) - `mcp-server/client.py`/`server.py` were originally written against a separate, uncommitted Python/FastAPI backend prototype (`backend-py/`, still not tracked in git) but happen to expose an identical route/field shape, so no remapping was needed when retargeting to the Java backend.

## CI

`.github/workflows/ci.yml` runs `mvn test` (backend) and `npm test` (frontend) on push/PR to `main`. There's no CI job for `mcp-server/` (thin wrapper, verified by hand against a running backend) or for `backend-py/` (an in-progress Python backend migration, not yet tracked in this git repository).
