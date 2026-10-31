# Leaver Date Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record the date a person left the company as a nullable `Person.leaverDate`, readable by everyone and editable only by admins, without changing any existing behaviour.

**Architecture:** One nullable `LocalDate` column on `person`, kept strictly independent of the existing `active` soft-delete flag. It is surfaced through the existing `PersonDto`/`CreatePersonRequest` records, so write authorization is inherited from the `@PreAuthorize("hasRole('ADMIN')")` already on `POST`/`PUT`. No capacity, filtering, or allocation logic changes.

**Tech Stack:** Java 21, Spring Boot 3.3.5, JPA/Hibernate, Flyway, H2 (local/test) / PostgreSQL (prod), JUnit 5 + MockMvc, Next.js 15, React 18, TypeScript, TanStack Table v8, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-31-leaver-date-design.md`

## Global Constraints

- `leaverDate` is **nullable**. `null` means "still here". Future dates are valid and must not be rejected.
- `leaverDate` is **independent of `Person.active`**. Never set, clear, or read one based on the other. `active` remains the soft-delete flag used by `DELETE /api/persons/{id}`.
- `leaverDate` is **readable by every authenticated user**. It must NOT be placed behind the `includeAdminFields` flag in `PersonDto.from()`. This is deliberately unlike `adminNote`, which sits beside it.
- `leaverDate` is **writable by ADMIN only**. This is inherited from existing `@PreAuthorize` annotations — do not add new authorization code.
- `PersonDto` is annotated `@JsonInclude(JsonInclude.Include.NON_NULL)`. A null `leaverDate` is **omitted from the JSON entirely**, not sent as `null`. TypeScript types must be optional and all frontend reads must handle `undefined`.
- The serialised wire format is an ISO-8601 date string: `"2026-03-14"`.
- Any new UI element must carry `dark:` variants alongside its light classes — dark mode is already shipped.
- Do not modify `V1__init_schema.sql` or `V2__seed_data.sql`. Migrations are append-only.

---

### Task 1: Database migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__add_leaver_date.sql`

**Interfaces:**
- Consumes: nothing.
- Produces: a `leaver_date DATE` column on table `person`, nullable, no default.

Production runs `ddl-auto: validate` with Flyway enabled, so this file is mandatory there. Local and test profiles run Hibernate DDL with Flyway disabled and will create the column on their own — which is precisely why this task comes first and why Task 7 adds a CI guard.

- [ ] **Step 1: Create the migration**

```sql
-- V3__add_leaver_date.sql
-- The date a person left (or will leave) the company. NULL means still employed.
-- Deliberately independent of person.active, which is the soft-delete flag.
ALTER TABLE person ADD COLUMN leaver_date DATE;
```

- [ ] **Step 2: Verify the file is picked up by Flyway's naming convention**

Run: `ls backend/src/main/resources/db/migration/`
Expected: three files listed — `V1__init_schema.sql`, `V2__seed_data.sql`, `V3__add_leaver_date.sql`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V3__add_leaver_date.sql
git commit -m "feat(db): add person.leaver_date column"
```

---

### Task 2: Entity field

**Files:**
- Modify: `backend/src/main/java/com/squadron/entity/Person.java`

**Interfaces:**
- Consumes: the `leaver_date` column from Task 1.
- Produces: `Person.getLeaverDate()` returning `LocalDate`, `Person.setLeaverDate(LocalDate)`, and `Person.builder().leaverDate(LocalDate)`.

- [ ] **Step 1: Add the import**

In `Person.java`, alongside the existing `java.time.LocalDateTime` import:

```java
import java.time.LocalDate;
```

- [ ] **Step 2: Add the field**

Insert immediately after the `active` field and before `adminNote`:

```java
    /**
     * The date this person left (or will leave) the company. Null means still here.
     * Independent of {@link #active}, which is the soft-delete flag - a leaver stays
     * active and fully visible.
     */
    private LocalDate leaverDate;
```

Do not annotate it `@Column(nullable = false)`. Lombok's `@Getter`/`@Setter`/`@Builder` on the class generate the accessors; no extra annotations are needed.

- [ ] **Step 3: Verify it compiles**

Run: `cd backend && mvn -B compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/squadron/entity/Person.java
git commit -m "feat(backend): add leaverDate to the Person entity"
```

---

### Task 3: Expose leaverDate on the read DTO

**Files:**
- Modify: `backend/src/main/java/com/squadron/dto/PersonDto.java`
- Test: `backend/src/test/java/com/squadron/person/LeaverDateIntegrationTest.java` (create)

**Interfaces:**
- Consumes: `Person.getLeaverDate()` from Task 2.
- Produces: `PersonDto.leaverDate()` accessor; JSON field `leaverDate` present on every `GET /api/persons` and `GET /api/persons/{id}` response where the value is non-null.

The repository has exactly one established backend test style: `@SpringBootTest` + `MockMvc` + real JWTs minted by `JwtUtil` (see `SecurityIntegrationTest`). Follow it rather than introducing mocks.

- [ ] **Step 1: Write the failing test**

Create `backend/src/test/java/com/squadron/person/LeaverDateIntegrationTest.java`:

```java
package com.squadron.person;

import com.squadron.entity.AppUser;
import com.squadron.entity.Person;
import com.squadron.entity.UserRole;
import com.squadron.repository.AppUserRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.security.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaverDateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String viewerToken;
    private Long leaverId;

    @BeforeAll
    void setUp() {
        appUserRepository.deleteAll();

        AppUser viewer = appUserRepository.save(AppUser.builder()
                .email("viewer@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.VIEWER)
                .active(true)
                .build());
        viewerToken = jwtUtil.generateToken(
                new User(viewer.getEmail(), viewer.getPasswordHash(), Collections.emptyList()));

        Person leaver = personRepository.save(Person.builder()
                .name("Departing Dev")
                .email("departing@test.local")
                .active(true)
                .leaverDate(LocalDate.of(2026, 3, 14))
                .build());
        leaverId = leaver.getId();
    }

    @Test
    void leaverDate_isVisibleToViewers_unlikeAdminNote() throws Exception {
        // leaverDate deliberately sits OUTSIDE includeAdminFields. It is the field
        // most likely to be "tidied" into the admin-only branch beside adminNote,
        // so this pins the intent down.
        mockMvc.perform(get("/api/persons/" + leaverId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").value("2026-03-14"));
    }

    @Test
    void leaverDate_serialisesAsAnIsoDateString() throws Exception {
        mockMvc.perform(get("/api/persons/" + leaverId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").isString());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd backend && mvn -B test -Dtest=LeaverDateIntegrationTest`
Expected: FAIL. `jsonPath("$.leaverDate")` finds no such field, reporting "No value at JSON path".

- [ ] **Step 3: Add the field to PersonDto**

In `PersonDto.java`, add the import:

```java
import java.time.LocalDate;
```

Add `leaverDate` to the record components, placed after `active` and **before** `adminNote`:

```java
public record PersonDto(
        Long id,
        String name,
        String email,
        boolean active,
        LocalDate leaverDate,   // visible to ALL roles - NOT an admin field
        List<String> tags,
        String adminNote,       // null for VIEWER role
        Integer totalAllocation,
        List<AllocationDto> allocations
) {
```

And in `from()`, pass it straight through — note it is NOT wrapped in `includeAdminFields`:

```java
        return new PersonDto(
                p.getId(),
                p.getName(),
                p.getEmail(),
                p.isActive(),
                p.getLeaverDate(),
                p.getTags().stream().map(t -> t.getName()).sorted().toList(),
                includeAdminFields ? p.getAdminNote() : null,
                totalAllocation,
                allocations
        );
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd backend && mvn -B test -Dtest=LeaverDateIntegrationTest`
Expected: PASS, 2 tests.

- [ ] **Step 5: Run the full suite for regressions**

Run: `cd backend && mvn -B test`
Expected: `Tests run: 17, Failures: 0, Errors: 0` — the 15 existing plus the 2 new.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/squadron/dto/PersonDto.java backend/src/test/java/com/squadron/person/LeaverDateIntegrationTest.java
git commit -m "feat(api): expose leaverDate on PersonDto for all roles"
```

---

### Task 4: Accept leaverDate on write

**Files:**
- Modify: `backend/src/main/java/com/squadron/dto/CreatePersonRequest.java`
- Modify: `backend/src/main/java/com/squadron/service/PersonService.java`
- Test: `backend/src/test/java/com/squadron/person/LeaverDateIntegrationTest.java` (extend)

**Interfaces:**
- Consumes: `PersonDto.leaverDate()` from Task 3; `Person.setLeaverDate` from Task 2.
- Produces: `CreatePersonRequest.leaverDate()` accepted on `POST /api/persons` and `PUT /api/persons/{id}`.

`CreatePersonRequest` backs both create and update, and both endpoints already carry `@PreAuthorize("hasRole('ADMIN')")`. Write gating therefore requires no new code — Step 1 proves that claim rather than assuming it.

- [ ] **Step 1: Write the failing tests**

Add these imports to `LeaverDateIntegrationTest.java`:

```java
import com.squadron.entity.UserRole;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
```

Add an admin token. In `setUp()`, after the viewer is created:

```java
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
        adminToken = jwtUtil.generateToken(
                new User(admin.getEmail(), admin.getPasswordHash(), List.of(() -> "ROLE_ADMIN")));
```

And the field beside `viewerToken`:

```java
    private String adminToken;
```

Then add these tests:

```java
    @Test
    void admin_canSetLeaverDateOnCreate() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Leaver\",\"leaverDate\":\"2026-06-30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaverDate").value("2026-06-30"));
    }

    @Test
    void admin_canClearLeaverDateByOmittingIt() throws Exception {
        // PersonService.update assigns unconditionally, so a null in the request
        // clears the column. This is the documented, intended behaviour.
        //
        // This test MUTATES its subject, so it creates its own person rather than
        // touching the shared leaverId fixture. JUnit does not guarantee method
        // order - reusing the fixture here would intermittently break
        // leaverDate_isVisibleToViewers_unlikeAdminNote depending on run order.
        Person temp = personRepository.save(Person.builder()
                .name("Temp Clear").active(true)
                .leaverDate(LocalDate.of(2026, 1, 1)).build());

        mockMvc.perform(put("/api/persons/" + temp.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temp Clear\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").doesNotExist());
    }

    @Test
    void update_withLeaverDateIncluded_preservesIt() throws Exception {
        // What the edit form actually does: round-trip the existing value while
        // changing something else. This is the behaviour users depend on, and the
        // reason PersonForm must always submit leaverDate.
        Person temp = personRepository.save(Person.builder()
                .name("Temp Preserve").active(true)
                .leaverDate(LocalDate.of(2026, 5, 20)).build());

        mockMvc.perform(put("/api/persons/" + temp.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"leaverDate\":\"2026-05-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.leaverDate").value("2026-05-20"));
    }

    @Test
    void futureLeaverDate_isAccepted() throws Exception {
        // A resignation handed in now for a departure months away must not be rejected.
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Future Leaver\",\"leaverDate\":\"2099-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaverDate").value("2099-12-31"));
    }

    @Test
    void viewer_cannotWriteLeaverDate() throws Exception {
        // Authorization is inherited from @PreAuthorize on the controller, not
        // written for this field. This proves the inheritance actually holds.
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\",\"leaverDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leaverDate_isOmittedFromJsonWhenNull() throws Exception {
        // PersonDto is @JsonInclude(NON_NULL): the key is absent, not null.
        // The TypeScript type depends on this.
        Person stayer = personRepository.save(Person.builder()
                .name("Staying Dev").active(true).build());

        mockMvc.perform(get("/api/persons/" + stayer.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").doesNotExist());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `cd backend && mvn -B test -Dtest=LeaverDateIntegrationTest`
Expected: FAIL. `admin_canSetLeaverDateOnCreate` and `futureLeaverDate_isAccepted` fail because the request field is ignored and `$.leaverDate` does not exist. `viewer_cannotWriteLeaverDate` and `leaverDate_isOmittedFromJsonWhenNull` should already pass.

- [ ] **Step 3: Add the field to CreatePersonRequest**

```java
package com.squadron.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreatePersonRequest(
        @NotBlank String name,
        String email,
        LocalDate leaverDate,
        String adminNote,
        List<Long> tagIds
) {}
```

No validation annotation on `leaverDate` — future dates are explicitly allowed.

- [ ] **Step 4: Wire it through PersonService**

In `create()`, add `.leaverDate(req.leaverDate())` to the builder:

```java
        Person person = Person.builder()
                .name(req.name())
                .email(req.email())
                .leaverDate(req.leaverDate())
                .adminNote(req.adminNote())
                .active(true)
                .build();
```

In `update()`, add the setter beside the existing unconditional assignments:

```java
        person.setName(req.name());
        person.setEmail(req.email());
        person.setLeaverDate(req.leaverDate());
        person.setAdminNote(req.adminNote());
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `cd backend && mvn -B test -Dtest=LeaverDateIntegrationTest`
Expected: PASS, 8 tests.

- [ ] **Step 6: Run the full suite**

Run: `cd backend && mvn -B test`
Expected: `Tests run: 23, Failures: 0, Errors: 0`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/squadron/dto/CreatePersonRequest.java backend/src/main/java/com/squadron/service/PersonService.java backend/src/test/java/com/squadron/person/LeaverDateIntegrationTest.java
git commit -m "feat(api): accept leaverDate on person create and update"
```

---

### Task 5: TypeScript type and the person detail page

**Files:**
- Modify: `frontend/src/types/index.ts:39-48`
- Modify: `frontend/src/app/persons/[id]/page.tsx`
- Create: `frontend/src/lib/formatLeaverDate.ts`
- Test: `frontend/src/lib/formatLeaverDate.test.ts` (create)

**Interfaces:**
- Consumes: the `leaverDate` JSON field from Task 3.
- Produces: `PersonDto.leaverDate?: string`; `formatLeaverDate(iso?: string): string | null` returning `"Left 2026-03-14"` or `null`.

A tiny formatter module exists so the badge text is testable without rendering React — the project has no component-testing setup, only Vitest with plain modules.

- [ ] **Step 1: Write the failing test**

Create `frontend/src/lib/formatLeaverDate.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { formatLeaverDate } from './formatLeaverDate';

describe('formatLeaverDate', () => {
  it('formats an ISO date as a Left label', () => {
    expect(formatLeaverDate('2026-03-14')).toBe('Left 2026-03-14');
  });

  it('returns null when the field is absent', () => {
    // PersonDto is @JsonInclude(NON_NULL), so the key is missing, not null.
    expect(formatLeaverDate(undefined)).toBeNull();
  });

  it('returns null for an empty string', () => {
    expect(formatLeaverDate('')).toBeNull();
  });
});
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `cd frontend && npx vitest run src/lib/formatLeaverDate.test.ts`
Expected: FAIL — cannot resolve `./formatLeaverDate`.

- [ ] **Step 3: Write the formatter**

Create `frontend/src/lib/formatLeaverDate.ts`:

```ts
/**
 * Builds the leaver badge label. Returns null when the person has no leaver
 * date, which includes the key being absent entirely - PersonDto is serialised
 * with @JsonInclude(NON_NULL), so null dates are omitted from the JSON.
 */
export function formatLeaverDate(iso?: string): string | null {
  if (!iso) return null;
  return `Left ${iso}`;
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `cd frontend && npx vitest run src/lib/formatLeaverDate.test.ts`
Expected: PASS, 3 tests.

- [ ] **Step 5: Add the optional type**

In `frontend/src/types/index.ts`, add to the `PersonDto` interface after `active`:

```ts
export interface PersonDto {
  id: number;
  name: string;
  email?: string;
  active: boolean;
  leaverDate?: string;   // ISO date, omitted entirely when the person has not left
  tags: string[];
  adminNote?: string;
  totalAllocation: number;
  allocations: AllocationDto[];
}
```

- [ ] **Step 6: Show it on the detail page**

In `frontend/src/app/persons/[id]/page.tsx`, import the formatter:

```tsx
import { formatLeaverDate } from '@/lib/formatLeaverDate';
```

Immediately after the `{person.email && ( ... )}` block (around line 71-75), add:

```tsx
                  {formatLeaverDate(person.leaverDate) && (
                    <span className="inline-flex items-center mt-1 ml-2 text-xs font-medium px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 border border-gray-200 dark:bg-white/10 dark:text-white/60 dark:border-white/15">
                      {formatLeaverDate(person.leaverDate)}
                    </span>
                  )}
```

Note this is rendered unconditionally with respect to role — unlike the `{isAdmin && person.adminNote && ...}` block below it, leaver dates are visible to everyone.

- [ ] **Step 7: Verify types and tests**

Run: `cd frontend && npx tsc --noEmit && npx vitest run`
Expected: no type errors; `Tests 11 passed` — the 8 existing plus 3 new.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/lib/formatLeaverDate.ts frontend/src/lib/formatLeaverDate.test.ts "frontend/src/app/persons/[id]/page.tsx"
git commit -m "feat(frontend): show the leaver date on the person detail page"
```

---

### Task 6: Form input and people-table badge

**Files:**
- Modify: `frontend/src/components/persons/PersonForm.tsx`
- Modify: `frontend/src/app/persons/page.tsx`

**Interfaces:**
- Consumes: `PersonDto.leaverDate` from Task 5; `formatLeaverDate` from Task 5; `CreatePersonRequest.leaverDate` from Task 4.
- Produces: no new exports.

The form must always submit the current value. `PersonService.update()` assigns unconditionally, so omitting the field from the payload silently clears a stored leaver date.

- [ ] **Step 1: Add form state**

In `PersonForm.tsx`, beside the existing `useState` calls (around line 20):

```tsx
  const [leaverDate, setLeaverDate] = useState(initial?.leaverDate ?? '');
```

- [ ] **Step 2: Include it in the submit payload**

In `handleSubmit`, add to the payload object — this is what prevents the value being wiped on edit:

```tsx
      const payload = {
        name: name.trim(),
        email: email.trim() || undefined,
        leaverDate: leaverDate || undefined,
        adminNote: adminNote.trim() || undefined,
        tagIds: selectedTagIds,
      };
```

- [ ] **Step 3: Add the input**

Insert a new block after the Email field (after roughly line 100, before the Tags block):

```tsx
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-white/80 mb-1">Leaver date</label>
            <input
              type="date"
              value={leaverDate}
              onChange={e => setLeaverDate(e.target.value)}
              className="w-full border border-gray-300 dark:border-white/15 dark:bg-white/5 dark:text-white rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500"
            />
            <p className="text-xs text-gray-500 dark:text-white/40 mt-1">
              Leave empty if they are still here. Future dates are fine.
            </p>
          </div>
```

The form is only reachable by admins (the New/Edit controls are already admin-gated), so no extra `isAdmin` check is needed here.

- [ ] **Step 4: Add the table column**

In `frontend/src/app/persons/page.tsx`, import the formatter:

```tsx
import { formatLeaverDate } from '@/lib/formatLeaverDate';
```

Add this column to the `columns` array, immediately before the `col.display({ id: 'adminNote' ... })` entry:

```tsx
      col.accessor('leaverDate', {
        id: 'leaverDate',
        header: 'Leaver',
        sortUndefined: 'last',
        cell: ({ row }) => {
          const label = formatLeaverDate(row.original.leaverDate);
          return label
            ? <span className="text-xs whitespace-nowrap px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 border border-gray-200 dark:bg-white/10 dark:text-white/60 dark:border-white/15">{label}</span>
            : <span className="text-xs text-gray-300 dark:text-white/30">—</span>;
        },
      }),
```

This column is read-only. It deliberately does not join the inline-edit flow used by `adminNote`, because editing happens in `PersonForm`.

- [ ] **Step 5: Verify types, tests and build**

Run: `cd frontend && npx tsc --noEmit && npx vitest run && npm run build`
Expected: no type errors; `Tests 11 passed`; `Compiled successfully`.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/persons/PersonForm.tsx frontend/src/app/persons/page.tsx
git commit -m "feat(frontend): edit the leaver date and show it in the people table"
```

---

### Task 7: CI guard against missing migrations

**Files:**
- Modify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: the migrations in `backend/src/main/resources/db/migration/`.
- Produces: a `migration-check` CI job that fails when the JPA entities and the Flyway schema disagree.

Local and test profiles both create schema via Hibernate with Flyway disabled, so a forgotten migration passes everything a developer can run. Only production catches it, at startup. This job closes that gap permanently — for this change and every future one.

Postgres rather than H2 because `V1__init_schema.sql` uses `BIGSERIAL` and other Postgres-specific DDL.

- [ ] **Step 1: Add the job**

Append to `.github/workflows/ci.yml`:

```yaml
  migration-check:
    name: Flyway migrations match the entities
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_USER: squadron
          POSTGRES_PASSWORD: squadron
          POSTGRES_DB: squadron
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      # Runs Flyway against real Postgres, then starts the context with
      # ddl-auto=validate. If an entity has a field with no matching column -
      # a forgotten migration - the context fails to start and this job fails.
      # The local and test profiles cannot catch this: both build the schema
      # with Hibernate and have Flyway disabled.
      - name: Verify migrations satisfy the entity mappings
        working-directory: backend
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/squadron
          SPRING_DATASOURCE_USERNAME: squadron
          SPRING_DATASOURCE_PASSWORD: squadron
          SPRING_FLYWAY_ENABLED: 'true'
          SPRING_JPA_HIBERNATE_DDL_AUTO: validate
          JWT_SECRET: c3F1YWRyb25TZWNyZXRLZXlGb3JEZXZPbmx5LU5vdEZvclByb2R1Y3Rpb24h
        run: mvn -B spring-boot:run -Dspring-boot.run.arguments=--spring.main.web-application-type=none
```

- [ ] **Step 2: Verify the YAML parses**

Run: `python -c "import yaml,io; yaml.safe_load(io.open('.github/workflows/ci.yml', encoding='utf-8')); print('YAML OK')"`
Expected: `YAML OK`

- [ ] **Step 3: Confirm the guard actually catches a missing migration**

Temporarily rename the migration so the column disappears from the Flyway schema while the entity still declares it:

```bash
git stash push backend/src/main/resources/db/migration/V3__add_leaver_date.sql
```

Push the branch and confirm `migration-check` **fails** with a schema validation error naming `leaver_date`. Then restore it:

```bash
git stash pop
```

A guard never seen to fail is not a guard. Do not skip this step.

- [ ] **Step 4: Confirm it passes with the migration restored**

Push again. Expected: `migration-check` passes, along with `java-tests` and `frontend-tests`.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: fail the build when migrations and entities disagree"
```

---

### Task 8: End-to-end verification

**Files:** none modified.

**Interfaces:**
- Consumes: everything from Tasks 1-7.
- Produces: nothing.

- [ ] **Step 1: Start the stack**

```bash
cd backend && mvn -B spring-boot:run -Dspring-boot.run.profiles=local
cd frontend && npm run dev
```

If the backend reports "Port 8080 already in use" or hangs with an empty log, an orphaned JVM is holding the port and the H2 lock. Kill stray `java.exe` processes, then delete `backend/data/squadron.lock.db`.

- [ ] **Step 2: Confirm the field round-trips through the API**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@squadron.local","password":"admin123"}' \
  | python -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s -X POST http://localhost:8080/api/persons \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"E2E Leaver","leaverDate":"2026-09-01"}'
```

Expected: a 201 response whose body contains `"leaverDate":"2026-09-01"`.

- [ ] **Step 3: Confirm a VIEWER can read it**

```bash
curl -s -X POST http://localhost:8080/api/users   -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json'   -d '{"email":"viewer@squadron.local","password":"viewer123","userRole":"VIEWER"}'

VTOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login   -H 'Content-Type: application/json'   -d '{"email":"viewer@squadron.local","password":"viewer123"}'   | python -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8080/api/persons -H "Authorization: Bearer $VTOKEN"   | python -c "import sys,json; p=[x for x in json.load(sys.stdin) if x['name']=='E2E Leaver'][0]; print('leaverDate:', p.get('leaverDate')); print('adminNote present:', 'adminNote' in p)"
```

Expected: `leaverDate: 2026-09-01` and `adminNote present: False`. This is the role split the whole design rests on.

- [ ] **Step 4: Check the UI**

Open http://localhost:3000/persons. Expected: a `Left 2026-09-01` badge on the E2E Leaver row, `—` on everyone else, and the column sorts with blanks last. Open the person, click Edit, confirm the date populates, change only the name, save, and confirm the leaver date **survives**.

Toggle dark mode and confirm the badge remains legible.

- [ ] **Step 5: Full suite**

Run: `cd backend && mvn -B test` → expect `Tests run: 23, Failures: 0`
Run: `cd frontend && npm test && npx tsc --noEmit && npm run build` → expect 11 passing, no type errors, successful build.

- [ ] **Step 6: Clean up the test record**

```bash
ID=$(curl -s http://localhost:8080/api/persons -H "Authorization: Bearer $TOKEN"   | python -c "import sys,json; print([x['id'] for x in json.load(sys.stdin) if x['name']=='E2E Leaver'][0])")
curl -s -o /dev/null -w '%{http_code}
' -X DELETE "http://localhost:8080/api/persons/$ID"   -H "Authorization: Bearer $TOKEN"
```

Expected: `204`.

Note this soft-deletes by setting `active = false`; it does not clear `leaver_date`. That independence is intended.
