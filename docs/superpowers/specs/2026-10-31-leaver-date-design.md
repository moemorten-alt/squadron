# Leaver date — design

**Date:** 2026-10-31
**Status:** Approved, ready for implementation planning

## Problem

Squadron has no way to record that someone has left the company. The only
signal today is `Person.active`, and that flag is already taken: `DELETE
/api/persons/{id}` sets `active = false` as a soft delete, and
`PersonRepository` filters `WHERE p.active = true`, so a deactivated person
disappears from the application entirely.

Reusing `active` would conflate two different facts — "removed, probably by
mistake" and "worked here until March, then left". It would also destroy the
departure date itself, which is the thing worth knowing.

## Decisions

Three decisions were settled before design, and they bound the scope:

| Question | Decision |
|---|---|
| What does the date *do*? | **Records a fact. It changes no behaviour.** |
| Who can see it? | **Everyone reads it. Only ADMIN edits it.** |
| Where does it appear? | **Person detail page + a badge in the people table.** |

"Records a fact" is the load-bearing decision. A leaver keeps `active = true`,
keeps their allocations, keeps counting toward squad capacity, and keeps
appearing in every list. Nothing is filtered or auto-ended. An admin who wants
a leaver out of the rosters still does that by hand, exactly as today.

This was chosen over date-aware behaviour (person drops out of capacity on the
date) because it is far smaller and introduces no scheduled evaluation. The
cost is that the recorded date can drift out of sync with reality — someone may
be marked as having left while still holding 100% of a squad. That is accepted.

## Data model

Add one field to `Person`:

```java
private LocalDate leaverDate;   // null = still here
```

Nullable. Independent of `active`. The two fields answer different questions
and neither derives from the other:

- `active = false` → soft-deleted, hidden from the app.
- `leaverDate != null` → left (or is leaving) the company, still fully visible.

A future date is valid and expected — a resignation handed in now for a
departure in two months. No upper or lower bound is enforced.

## API

### Reading

`PersonDto` gains `LocalDate leaverDate`, as a **plain field, deliberately not
behind `includeAdminFields`**. VIEWERs see it. This is the opposite of
`adminNote`, which `PersonDto.from()` nulls out for non-admins, so the two
fields sitting next to each other in the same record will invite someone to
"fix" the inconsistency later. The test suite must pin the intent down.

**Gotcha:** `PersonDto` is annotated `@JsonInclude(JsonInclude.Include.NON_NULL)`.
A null `leaverDate` is therefore **omitted from the JSON object entirely**, not
serialised as `null`. The TypeScript type must be optional (`leaverDate?:
string`) and every read site must handle `undefined`.

### Writing

`CreatePersonRequest` gains `LocalDate leaverDate`. This record serves **both**
`POST /api/persons` and `PUT /api/persons/{id}`, and both are already annotated
`@PreAuthorize("hasRole('ADMIN')")`.

The "everyone reads, ADMIN edits" requirement therefore needs **no new
authorization code**. It falls out of the existing gating.

**Gotcha:** `PersonService.update()` assigns every field unconditionally:

```java
person.setName(req.name());
person.setEmail(req.email());
person.setAdminNote(req.adminNote());
```

A `PUT` that omits `leaverDate` will null it. This is pre-existing behaviour
shared by `adminNote`, so the new field follows the established pattern rather
than introducing partial-update semantics for one field alone. The consequence
is that the edit form **must round-trip the current value**, and a test must
cover "update another field, leaver date survives".

### Serialisation

No new configuration. `Allocation.startDate` and `Allocation.endDate` are
already `LocalDate` and serialise as ISO-8601 strings (`"2026-03-14"`), so the
path is proven in this codebase.

## Database migration

```sql
-- V3__add_leaver_date.sql
ALTER TABLE person ADD COLUMN leaver_date DATE;
```

Additive and nullable, so it needs no backfill and is safe to apply to a live
table.

### The real risk

The schema strategy differs by environment, and the difference hides exactly
this kind of mistake:

| Profile | `ddl-auto` | Flyway | Effect |
|---|---|---|---|
| production (`application.yml`) | `validate` | **enabled** | migration is mandatory |
| local (`application-local.yml`) | `update` | **disabled** | Hibernate adds the column silently |
| test (`application-test.yml`) | `create-drop` | **disabled** | Hibernate adds the column silently |

Both runnable environments create the column on their own. **If
`V3__add_leaver_date.sql` is forgotten, every local run and all 15 tests pass,
and production fails at startup on schema validation.** Nothing in the current
setup detects it.

### Mitigation

Add a CI job that runs the Flyway migrations against a **real Postgres service
container** and then starts the context with `ddl-auto: validate`.

Postgres rather than H2 because `V1__init_schema.sql` uses `BIGSERIAL` and
other Postgres-flavoured DDL; validating against H2 would be exercising a
different dialect than production and could pass or fail for reasons unrelated
to the change.

This guard is not specific to the leaver date. It closes the gap for every
future migration, which is why it is worth building now rather than relying on
a reviewer noticing.

## Frontend

| File | Change |
|---|---|
| `src/types/index.ts` | `leaverDate?: string` on `PersonDto` (optional — see NON_NULL above) |
| `src/components/persons/PersonForm.tsx` | date input, rendered only when `isAdmin`; must submit the existing value when unchanged |
| `src/app/persons/page.tsx` | sortable column / `Left 2026-03-14` badge, nulls sorting last |
| `src/app/persons/[id]/page.tsx` | display on the detail page |

Dark mode already landed, so any new badge needs its `dark:` variant alongside
the light classes, consistent with `allocationBadgeClass`.

## Testing

**Backend**

1. Service round-trip — create with a date, read it back.
2. Service clear — set to null, persists as null.
3. **Update preserves** — change `name` only; `leaverDate` survives. Guards the
   unconditional-assignment hazard above.
4. **VIEWER sees `leaverDate`** — the inverse of the existing `adminNote`
   assertion. The most likely thing to regress, because the field sits beside
   an admin-only one.
5. VIEWER `PUT` returns 403 — confirms write gating, which is inherited rather
   than written, and so is otherwise untested for this field.

**Frontend**

6. Badge renders a date when present.
7. Nothing renders when the key is absent (not merely null — see NON_NULL).

**Migration**

8. The CI Postgres job described above.

## Out of scope

Explicitly excluded, to be requested separately if wanted:

- Backfilling leaver dates into `V2__seed_data.sql`.
- Any "hide ex-employees" filter or default on the people list.
- Reporting, exports, or leaver analytics.
- Auto-ending allocations, or excluding leavers from squad capacity. This is
  the deliberate consequence of the "records a fact" decision; revisiting it
  means revisiting that decision first.
