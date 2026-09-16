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

9. **Note + Tag nodes, relationships** — `Note` and `Tag` as `@Node`
   entities; `(Note)-[:FROM_SOURCE]->(Source)` and
   `(Note)-[:TAGGED]->(Tag)` as real graph relationships, no join
   tables. Tag's `nameLower` property + `IS UNIQUE` constraint (decided
   in `SPEC.md`) gets written here, before Note's migration touches
   Tag at all -- same sequencing discipline the original plan used.
10. **Note DTOs + service** — including the
    sourceId-required-on-top-level-create decision (still applies,
    pivot-independent), and how tags get attached when creating a Note
    (find-existing-or-create-new Tag by `nameLower`, not requiring the
    caller to already know a tag's id).
11. **Note controller** — both `/notes` and `/sources/{id}/notes` routes.
12. **Note integration tests** — including at least one test that
    exercises tags (reusing an existing tag via its `nameLower` lookup,
    not just creating notes with no tags).

## Wrap-up for this slice

13. **README** — rewritten for the new stack: what it is, how to run it
    (docker-compose for Neo4j, not Postgres), the API shape (unchanged
    at the HTTP level despite the storage swap).

**Source + Note slice: complete once all 13 steps are done and verified
for real** (cypher-shell/curl/Testcontainers, not assumed) -- same bar
the original Postgres build held itself to.

## Next round: Concept + Link (not started)

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
