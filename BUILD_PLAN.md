# Build Plan

Companion to `SPEC.md` — the step-by-step sequence for building the
Source + Note slice, one step at a time, with decisions/questions
discussed at each step rather than all at once. Concept, Link, and
search are a separate round after this slice is solid.

## Foundation

1. **Project skeleton** — `pom.xml`, directory layout, a bare
   `@SpringBootApplication` that boots with zero endpoints. Confirms
   Java 25 + Spring Boot 4.1 actually work together on this machine
   before building anything on top.
2. **Local Postgres** — `docker-compose.yml`, confirm we can connect to
   it (no app code yet, just proving the DB is reachable).
3. **First migration** — Flyway, `V1__init.sql` for just the `sources`
   table. Establishes the migration pattern before any Java touches it.

## Source, end to end

4. **Source entity + repository** — JPA entity, `SourceRepository`.
   Verify it actually reads/writes against the real Postgres (a quick
   throwaway check, not full tests yet).
5. **Source DTOs + service** — request/response records, business logic
   (create, get, list), separate from the entity.
6. **Source controller** — wire up `GET/POST /sources`,
   `GET/PATCH /sources/{id}`. Verify manually with curl.
7. **Validation + error handling** — Bean Validation on the request DTO,
   `GlobalExceptionHandler` + `ErrorResponse`, decide the PATCH semantics
   (partial update) together.
8. **First integration test** — Testcontainers base class, one real test
   against Source (now fully done: create/get/list/patch, validation,
   error handling all verified manually via curl in step 7 -- this is
   about locking that behavior in with an automated test, not verifying
   it for the first time). This is where the whole stack (Flyway + JPA +
   Postgres + Testcontainers) gets proven together, deliberately done
   now, before Note adds more surface area to test.

## Note, end to end

(repeats the same shape as Source, faster since the pattern's established
-- except Note also brings in Tag, which Source didn't need)

9. **Note + Tag migration, entities, repositories** — `notes` table,
   `tags` table, and a `note_tags` join table for the many-to-many
   (per the spec reframe: a real Tag entity, not a string array). Three
   entities/repos total: `Note`, `Tag`, plus the join is handled by the
   `@ManyToMany` mapping itself.
10. **Note DTOs + service** — including the
    sourceId-required-on-top-level-create decision, and how tags get
    attached when creating a Note (find-existing-or-create-new Tag by
    name, not requiring the caller to already know tag ids)
11. **Note controller** — both `/notes` and `/sources/{id}/notes` routes
12. **Note integration tests** — including at least one test that
    exercises tags (reusing an existing tag, not just creating notes
    with no tags)

## Wrap-up for this slice

13. **README** — what it is, how to run it, API shape.
