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
   against Source. This is where the whole stack (Flyway + JPA +
   Postgres + Testcontainers) gets proven together, before it's buried
   under more code.

## Note, end to end

(repeats the same shape as Source, faster since the pattern's established)

9. **Note migration + entity + repository**
10. **Note DTOs + service** — including the
    sourceId-required-on-top-level-create decision
11. **Note controller** — both `/notes` and `/sources/{id}/notes` routes
12. **Note integration tests**

## Wrap-up for this slice

13. **README** — what it is, how to run it, API shape.
