# Build Plan

Companion to `SPEC.md` — the step-by-step sequence for rebuilding this
project on Neo4j/Spring Data Neo4j, one step at a time, with
decisions/questions discussed at each step rather than all at once.

**This supersedes the original Postgres/JPA build plan.** Source + Note
were previously built and fully tested against Postgres (see git
history, commit `67168c1` and earlier) — that work is being redone
here, not extended, per the stack pivot documented in `SPEC.md`. Concept
and Link were never started under the old plan, so nothing there is
lost, only redirected.

## Foundation

1. **Project skeleton** — swap `pom.xml`: remove
   `spring-boot-starter-data-jpa`, the Flyway artifacts, and
   `testcontainers-postgresql`; add `spring-boot-starter-data-neo4j`,
   `neo4j-migrations-spring-boot-starter`, and `testcontainers-neo4j`.
   Swap `docker-compose.yml` from Postgres to Neo4j 5.x Community.
   Confirm the app boots with zero endpoints and SDN can open a
   connection -- this is where any Spring Boot 4.1 + SDN version gotcha
   (the kind this project has hit before with Flyway and Testcontainers)
   would first surface, so don't skip actually running it.
2. **Local Neo4j** — confirm the app can reach it (Neo4j Browser or
   `cypher-shell`, no app code yet), the same "prove the DB is
   reachable before building on it" step the Postgres plan used.
3. **First migration** -- **done, verified.** `V1__baseline.cypher`
   (deliberately minimal, `RETURN 1;` -- no real schema exists yet)
   applied cleanly and shows up as a `__Neo4jMigration` node when
   queried directly. Real gotcha hit and documented in `SPEC.md`: the
   originally-pinned `neo4j-migrations-spring-boot-starter` version
   (`2.0.3`) silently never ran (a `@ConditionalOnBean(Driver.class)`
   check failed) because it was a stale version claim, not an actual
   Spring Boot 4 incompatibility -- `4.2.0` fixed it outright. The next
   real migration (Tag's `nameLower` constraint) lands in step 9, not
   before -- don't add speculative constraints to this file in the
   meantime just because the runner is proven to work now.

## Source, end to end

4. **Source node + repository** -- **done, verified.** `@Node` entity,
   `Neo4jRepository<Source, String>`. `rating` is back to a plain
   `Integer` (the `Short` requirement was Postgres-specific and doesn't
   apply anymore). Real decision made and verified here, not deferred:
   ids are UUID strings via `UUIDStringGenerator`, not Neo4j's internal
   id -- confirmed against SDN's own reference docs (which explicitly
   warn against the `Long`/internal-id default for production) and
   against the actual bytecode of `UUIDStringGenerator` in the SDN jar
   (`IdGenerator<String>`) after two fetched doc pages gave conflicting
   answers about whether it targets `String` or `UUID`. A throwaway
   `CommandLineRunner` saved and reloaded a real `Source` against the
   live Neo4j container, confirmed both via app logs and a direct
   `cypher-shell` query, then was reverted -- not left in the codebase.
5. **Source DTOs + service** -- **done, verified.** Request/response
   records (`SourceRequest`/`SourceResponse`/`SourcePatchRequest`)
   mostly carried over unchanged from the pivot (`String` ids,
   `Integer` rating). Real gotcha caught and fixed in
   `SourceService.patch()`: SDN has no JPA-style dirty checking (see
   `SPEC.md`) -- an explicit `sourceRepository.save(source)` after
   mutating is required, verified empirically with a throwaway
   before/after experiment against the live Neo4j container, not
   assumed from how the old JPA version worked.
6. **Source controller** -- **done, verified.** `GET/POST /sources`,
   `GET/PATCH /sources/{id}` wired up (the `/sources/{id}/notes` routes
   from the old controller come back in step 11, once `NoteService`
   exists again). Verified with real curl requests against the live
   app + Neo4j: create, get, list (paginated), patch -- and the patch
   re-fetched afterward on a separate request to confirm it actually
   persisted, not just that the response looked right.
7. **Validation + error handling** -- **done, verified as a side effect
   of step 6's curl pass.** `GlobalExceptionHandler` + `ErrorResponse`
   + Bean Validation on `SourceRequest` were untouched by the pivot and
   didn't need rebuilding -- confirmed directly: a `POST` with a blank
   body returned `400` with real per-field validation messages, and a
   `GET` on a nonexistent id returned `404`. PATCH semantics (partial
   update, no way to explicitly null a field) already decided and
   carried over unchanged from the original build.
8. **First integration test** -- **done, verified.**
   `AbstractIntegrationTest` (singleton `Neo4jContainer`, same
   not-`@Testcontainers`/`@Container` pattern as the old Postgres base
   class) + `SourceControllerIT` covering everything verified manually
   via curl in steps 6-7: create/get, validation (400), date-order
   validation, 404, patch (re-fetched on a separate request to prove
   persistence, not just trust the PATCH response), out-of-range PATCH
   rejection, and paginated list. All 7 tests pass on `mvn verify`.
   This is where the whole new stack (neo4j-migrations + SDN + Neo4j +
   Testcontainers) got proven together for real, before Note adds more
   surface area -- same reasoning the original plan used for Postgres.
   One cosmetic gotcha surfaced and documented in `SPEC.md`: Neo4j
   logs a harmless `WARN ... property key does not exist` when
   querying a property that's absent on a node (nulls aren't stored as
   properties at all) -- looks alarming, isn't a real problem.

## Note, end to end

(repeats the same shape as Source, faster since the pattern's
established -- except Note also brings in Tag, which Source didn't need)

9. **Note + Tag nodes, relationships** -- **done, verified.** `Note`
   and `Tag` as `@Node` entities, `@Relationship`-annotated fields for
   `(Note)-[:FROM_SOURCE]->(Source)` and `(Note)-[:TAGGED]->(Tag)` --
   no join tables. `V2__tag_name_lower_unique.cypher` adds the real
   `IS UNIQUE` constraint on `Tag.nameLower` decided in `SPEC.md`.
   Verified for real, not assumed: reloaded a saved `Note` and
   confirmed both relationships actually traverse (`note.getSource()`,
   `note.getTags()`); confirmed `NoteRepository.findBySourceId` derives
   correctly across the relationship; confirmed
   `TagRepository.findByNameLower` finds "Stoicism" via a
   lowercase-input lookup; and confirmed the constraint is real by
   deliberately trying to save a second Tag with a colliding
   `nameLower` ("STOICISM" after "Stoicism") and catching the resulting
   `DataIntegrityViolationException` -- then checked `SHOW CONSTRAINTS`
   and a direct relationship query in `cypher-shell` to see both the
   constraint and the real graph edges, not just trust the app log.
10. **Note DTOs + service** -- **done, verified.**
    `NoteRequest`/`NoteResponse` updated to `String` ids;
    `NoteResponse.from(Note)` restored. `sourceId`-required-on-
    top-level-create carried over unchanged. Tag resolution now looks
    up by `nameLower` (not `findByNameIgnoreCase` as the old JPA
    version did) -- deliberately matching the property the real
    uniqueness constraint targets, rather than introducing a second,
    unconstrained way to compare names case-insensitively. Lowercases
    the input at the call site, matching what `Tag`'s constructor
    already does when creating one.
11. **Note controller** -- **done, verified.** Both `/notes` and
    `/sources/{id}/notes` routes wired up (the latter required adding
    `NoteService` back into `SourceController`'s constructor). Verified
    with curl: nested create, top-level create, missing-sourceId 400,
    missing-content 400, 404 on both create-under-unknown-source and
    list-under-unknown-source, paginated list, and tag reuse --
    creating a second note with `"stoicism"` (different case) correctly
    reused the existing `"Stoicism"` tag rather than duplicating it,
    confirmed by querying `Tag` nodes directly in `cypher-shell`
    (exactly 3 tags total, not 4).
12. **Note integration tests** -- **done, verified.** `NoteControllerIT`
    ported from the old Postgres version (ids adapted to `String`),
    covering everything curl-verified in step 11 including the tag
    case-insensitive-reuse test (asserts exact tag count, not just "no
    error"). `mvn verify` now runs 15 tests total (7 Source + 8 Note),
    all passing.

## Wrap-up for this slice

13. **README** -- **done.** Rewritten for the new stack: Neo4j/SDN in
    the Stack section, docker-compose/bolt/browser instructions, the
    UUID-string-id note in the API shape section, curl examples updated
    to not hardcode a numeric id, the benign `property key does not
    exist` test warning explained so it isn't mistaken for a bug, and
    the Concept/Link "not yet built" section updated to reflect the
    single-`LINKS_TO`-relationship design instead of the old
    polymorphic-table one.

**Source + Note slice: complete.** All 13 steps done and verified for
real (cypher-shell/curl/`mvn verify` against a live Neo4j container,
never assumed) -- same bar the original Postgres build held itself to.
`mvn verify` runs 15 tests, all passing. Every real gotcha hit along
the way (stale migrations-library version, no JPA-style dirty
checking, Neo4j's schema-optional-per-property warning) is documented
in `SPEC.md` rather than left to be rediscovered.

## Next round: Concept + Link (in progress)

14. **Concept node, repository, DTOs, service, controller, tests** --
    **done, verified.** `Concept` is a plain `@Node` (UUID string id,
    name, nullable description), with `/concepts` list/create/get/patch
    following the Source pattern. `mvn verify` runs 22 tests (7 Source
    + 8 Note + 7 Concept), all passing. `/concepts/{id}/notes`, `/graph`
    and `/links` are deferred until Link exists.
15. **Link** -- **done, verified.** `LINKS_TO` relationships with
    `id`/`type`/`createdAt` between any two Source/Note/Concept nodes.
    SDN's relationship mapping needs a statically typed target entity,
    which doesn't fit "any of three labels", so `LinkRepository` uses
    `Neo4jClient` with plain Cypher (labels come from the closed
    `LinkableType` enum, never user input). The service looks up each
    endpoint by its stated type first: a missing node or a type/id
    mismatch is a 404, self-links are a 400. Endpoints: `POST /links`,
    `GET /links/{id}`, and paginated `GET /{sources,notes,concepts}/{id}/links`
    (either direction, reported in stored direction). Found and fixed a
    pre-existing gap along the way: an invalid enum value or malformed
    JSON returned 500 -- `GlobalExceptionHandler` now maps
    `HttpMessageNotReadableException` to 400. `mvn verify`: 31 tests
    passing.
    **Next: `/concepts/{id}/graph` (variable-length Cypher path), then
    `/concepts/{id}/notes`, then full-text `/search`.**


Start with a fresh planning pass again once Source + Note is solid on
the new stack, the same way this file did. What's already decided in
`SPEC.md` for this round, so the planning pass builds from it rather
than re-litigating:

- **Concept** is a plain `@Node` (id, name, description) with **no
  separate many-to-many relationship to Note** -- that was collapsed
  into Link for simplicity (see `SPEC.md`).
- **Link is a single generic `LINKS_TO` relationship type** with a
  `type` property (SUPPORTS/CONTRADICTS/EXTENDS/RELATES_TO), connecting
  any two Source/Note/Concept nodes, each with its own `id` and
  `createdAt` properties. One service method creates/queries links
  regardless of the two endpoint types or the link's semantic type --
  but the service still has to look up each endpoint by its stated type
  before creating the relationship (Cypher needs a label to `MATCH`
  against), so there's real logic here even though there's no
  polymorphic-table workaround needed anymore.
- **`GET /concepts/{id}/graph`** should be a native Cypher
  variable-length path query (`(c)-[*1..2]-(n)`), depth-limited --
  already specified in `SPEC.md`, and genuinely simpler now than the
  Postgres recursive-CTE version would have been. Worth noticing that
  simplification rather than over-building it out of habit.
- **`GET /search`** should use a Neo4j full-text schema index
  (`CREATE FULLTEXT INDEX ...`, queried via
  `db.index.fulltext.queryNodes`) across Note and Source content --
  same role Postgres's `tsvector`/GIN/`ts_rank` would have played,
  specified but not yet designed in detail.
- Same sequencing-risk note as before: this is where the real learning
  value in the project lives (native graph relationship modeling,
  Cypher traversal, full-text search). Give it the same unhurried,
  fully-verified pace Source + Note gets, not a rushed afterthought
  once the "foundational" part feels done.
