# Project: justinus-api

Build a Spring Boot backend for a personal knowledge-tracking system —
tracks what I read (books, articles, papers) and the ideas/connections
between them -- a lightweight Zettelkasten/PKM engine.

This is also a deliberate learning exercise: practicing graph data
modeling, tagging, and querying patterns correctly. That should bias
design decisions below toward the correct/idiomatic way over the
simplest way to build something — see the Tag and Link notes
especially.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.x, Spring Framework 7.x
- **Spring Data Neo4j (SDN) + Neo4j 5.x** — not a relational database.
  **Pivoted from Postgres/JPA after Source + Note were already fully
  built and tested** (see git history for the working Postgres-era
  code). The trigger: once Link's design settled on "anything can link
  to anything" (Obsidian/Zettelkasten-style, no fixed taxonomy of what
  can connect to what), that stopped being a relational-modeling
  exercise worth solving in SQL — nodes and typed relationships are
  Neo4j's native data model, not a workaround bolted onto one. Source
  and Note get rebuilt as graph nodes too, not just Concept + Link, so
  the whole app lives in one consistent model rather than a polyglot
  split.
  - `spring-boot-starter-data-neo4j` — confirmed on Maven Central at
    `4.0.1`, versioned in lockstep with Spring Boot 4.1.x the same way
    `spring-boot-flyway` was for the old stack.
  - Neo4j 5.x Community Edition, run via Docker — same role Postgres
    played: a real local instance to build and test against, not an
    embedded/in-memory stand-in.
- **neo4j-migrations-spring-boot-starter** for versioned schema
  migrations (constraints, indexes) — a real library (Michael Simons,
  "inspired by Flyway"), Cypher scripts under
  `classpath:neo4j/migrations` instead of SQL under `db/migration`.
  Chosen specifically to preserve the "versioned migration files
  checked into git" discipline Flyway gave the Postgres build, rather
  than letting SDN silently auto-create indexes with no history.
  **Verified working** (a real migration applies and shows up as a
  `__Neo4jMigration` node when queried directly, not just "the log
  looked fine") -- but only after a real gotcha:
  - **Pin `4.2.0`, not `2.0.3`.** An earlier web search claimed `2.0.3`
    was "the latest version" -- it wasn't (Maven Central's actual
    metadata shows `4.2.0` current, `2.0.3` is from January 2023). That
    stale search result nearly became a real bug: on `2.0.3`, Spring
    Boot's `--debug` autoconfiguration report showed
    `MigrationsAutoConfiguration`'s `@ConditionalOnBean(Driver.class)`
    silently failing to find the Neo4j driver bean, so the migration
    runner never ran at all -- no error, no log line, exactly the
    failure shape the old Flyway autoconfiguration gotcha had. Bumping
    to `4.2.0` fixed it outright, no other change needed. Lesson worth
    generalizing: a search result claiming "X is the latest version"
    is a claim to verify against the registry directly (Maven Central,
    npm, etc.), not a fact to build on.
- Spring Boot Actuator — unaffected by the pivot, `/actuator/health`
  still exposed.
- springdoc-openapi (Swagger UI) — still planned, not yet added,
  unaffected by the pivot.
- **Testcontainers Neo4j module** (`testcontainers-neo4j`) +
  `@ServiceConnection`, replacing the Postgres module. The same 2.x
  artifact-naming pattern already learned (every module gets a
  `testcontainers-` prefix) should apply here too, but treat that as
  something to verify, not assume — the Postgres setup's lessons don't
  automatically transfer just because the pattern looks the same. The
  Colima/Ryuk fixes below are runtime-level, not database-specific, so
  those genuinely do carry over unchanged:
  - `docker.host=unix:///path/to/colima/docker.sock` in
    `~/.testcontainers.properties` (machine-level, not checked in) and
    `TESTCONTAINERS_RYUK_DISABLED=true` as an environment variable if
    running on Colima rather than Docker Desktop.
  - `*IT.java` classes need `mvn verify` (Failsafe), not `mvn test`.
  - **Don't use `@Testcontainers`/`@Container` once there's more than
    one `*IT` class** — start a true singleton container in a static
    initializer instead, shared for the whole test JVM run, or the
    second test class's cached `ApplicationContext` points at a dead
    container's port and every request "hangs" with `Connection
    refused` after a long retry loop. `@ServiceConnection` still works
    fine on a manually-started container.
- Maven
- Bean Validation (Jakarta Validation 3.1) for request validation —
  unaffected by the pivot, still validates request DTOs before they
  reach the service layer.
- Clean layered architecture: controller -> service -> repository, with
  DTOs separate from graph entities (`@Node` classes now, not JPA
  `@Entity`) — same separation, no leaking domain objects through the
  API.

Known gotcha (unaffected by the pivot, still applies): Spring Framework
7 / Spring Boot 4 ship on **Jackson 3**, which renamed its base packages
(`com.fasterxml.jackson.*` -> `tools.jackson.*` for the new modules,
though `com.fasterxml.jackson.core` low-level types stick around).
Don't copy-paste Jackson imports from Spring Boot 3.x-era code without
checking they still resolve.

Known gotcha, specific to the pivot: **Spring Data Neo4j has no
JPA-style dirty checking.** Hibernate's persistence context tracks
changes to a managed entity and flushes them automatically at
transaction commit -- mutating a loaded entity's fields via setters
inside a `@Transactional` method is enough, no explicit save call
needed. SDN has no equivalent persistence-context tracking: mutating a
loaded `@Node` entity and returning without an explicit
`repository.save(entity)` call silently does nothing to the database --
no error, no warning. Verified empirically (not just read about) via a
throwaway experiment: patched a `Source`'s title without calling
`save()`, reloaded it, confirmed the change never happened; called
`save()` explicitly, reloaded again, confirmed it did. This directly
affects any PATCH-style partial update -- see `SourceService.patch()`.

## Core entities

Everything below is a graph node (`@Node`) connected by typed
relationships, not a table joined by foreign keys.

**All ids are UUID strings** (`@Id @GeneratedValue(UUIDStringGenerator.class) private String id;`),
not Neo4j's internal numeric id. Decided against Spring Data Neo4j's
own reference documentation, which explicitly recommends against the
`Long`/internal-id default for production use -- it's tied to the
database's storage lifecycle, not the application's, and isn't
guaranteed unique/stable the way an application-owned identifier is.
This ripples through every DTO and `@PathVariable` (`String`, not
`Long`) and every URL (`/sources/3fa85f64-...`, not `/sources/1`) --
verified for real on Source in BUILD_PLAN step 4, not just read about.

**Source** — something I read
- id, title, author, type (BOOK / ARTICLE / PAPER), dateStarted,
  dateFinished (nullable), status (READING / FINISHED / ABANDONED),
  rating (nullable, 1-5), generalNotes (nullable)
- `rating` can go back to being a plain `Integer` -- the earlier `Short`
  requirement was purely about matching Postgres's `SMALLINT` column
  type under Hibernate's schema validation, a Postgres-specific
  constraint that has no equivalent in Neo4j. Keeping `Short` anyway
  would just be carrying forward a workaround for a problem that no
  longer exists.

**Note** — an atomic idea or excerpt tied to a Source
- id, content, locationRef (nullable), createdAt
- No more `sourceId` FK field -- the Source relationship is now
  `(Note)-[:FROM_SOURCE]->(Source)`, a real graph edge instead of a
  foreign-key column.
- `locationRef` must still be **String, not Integer** and `createdAt`
  must still be **`Instant`, not `LocalDateTime`** -- both of these were
  domain-correctness reasons (non-numeric location references; timezone
  safety), not Postgres-specific ones, so they carry over unchanged.
- tags: no more Tag entity + `note_tags` join table -- a real graph
  edge, `(Note)-[:TAGGED]->(Tag)`. A relationship *is* the join table
  now, which is the whole appeal of the pivot for exactly this kind of
  association.
  - **Resolved**: Neo4j's native uniqueness constraints only support
    exact property equality, not expressions -- there's no direct
    equivalent of Postgres's `CREATE UNIQUE INDEX ON tags (lower(name))`
    trick. Falling back to "the service layer looks it up
    case-insensitively before creating" would reintroduce exactly the
    race condition (two concurrent creates producing "Stoicism" and
    "stoicism" as different nodes) that the expression index existed to
    prevent at the DB level, not just the app level. Fix: maintain a
    `nameLower` property alongside `name`, with a real `IS UNIQUE`
    constraint on `nameLower` -- this isn't cut for simplicity, since
    cutting it would just trade a schema guarantee for a subtle bug.

**Concept** — a recurring idea/theme (e.g. "free will," "moral luck")
that Notes can reference
- id, name, description (nullable)
- **Resolved: no separate many-to-many with Note.** "This note touches
  this concept" is just a Link (`LINKS_TO {type: RELATES_TO}`), the same
  mechanism used for everything else. Decided in favor of simplicity --
  one relationship mechanism for the whole app rather than two
  overlapping ones, now that relationships carry no schema cost the way
  a second polymorphic table would have.

**Link** — a directed, typed connection between *any two* Source, Note,
or Concept nodes. **This is the most valuable relationship to get right,
not an afterthought** — it's the one thing that makes this a
Zettelkasten backend instead of a reading log with notes attached.

Unlike the Postgres design, Link is **not its own node or table** --
it's expressed as native Neo4j relationships directly between nodes,
since a graph relationship can already connect any two node labels with
no schema changes required (this is what dissolved the earlier
`fromType`/`toType` polymorphic-table problem entirely -- it was a
relational workaround for something a graph database does by default).
- **Resolved: a single generic `:LINKS_TO` relationship type**, with a
  `type` property (SUPPORTS / CONTRADICTS / EXTENDS / RELATES_TO)
  carrying the semantic meaning, rather than four distinct relationship
  types. Decided in favor of simplicity -- one code path creates and
  queries links regardless of type ("all links touching this node" is
  one pattern match, not a four-way union), at the minor cost that
  Cypher queries filter on a property (`MATCH (a)-[l:LINKS_TO
  {type: 'SUPPORTS'}]->(b)`) instead of matching a relationship label
  directly.
- Each `LINKS_TO` relationship needs its own `id` (an explicit property,
  not Neo4j's internal element id, which isn't meant to be relied on as
  a stable public identifier) and `createdAt`, so individual links stay
  addressable via `GET /links/{id}`.

## Endpoints

```
GET/POST     /sources
GET/PATCH    /sources/{id}
GET/POST     /sources/{id}/notes

GET/POST     /notes
GET          /notes/{id}

GET/POST     /concepts
GET          /concepts/{id}
GET          /concepts/{id}/notes       # all notes touching this concept
GET          /concepts/{id}/graph       # concept + everything linked to it
                                         # (any type, any hop up to the
                                         # limit), returned as
                                         # {nodes: [...], edges: [...]}
                                         # for future graph visualization.
                                         # A native Cypher variable-length
                                         # path query (`(c)-[*1..2]-(n)`),
                                         # depth-limited (e.g. "within 2
                                         # hops") -- this is now a much
                                         # thinner exercise than the old
                                         # Postgres recursive CTE, which
                                         # is itself worth noticing: this
                                         # is the query the graph pivot
                                         # was made for.

POST         /links                     # create a link between any two
                                         # entities (fromType/fromId,
                                         # toType/toId, type) -- there's
                                         # no natural "owning side"
                                         # anymore now that either end
                                         # can be Source, Note, or
                                         # Concept, so this isn't nested
                                         # under one entity's routes.
GET          /links/{id}
GET          /sources/{id}/links        # all links touching this source
GET          /notes/{id}/links          # all links touching this note
GET          /concepts/{id}/links       # all links touching this concept

GET          /search?q=...              # full-text search across
                                         # notes + sources. Use Neo4j's
                                         # native full-text schema index
                                         # (`CREATE FULLTEXT INDEX ...`,
                                         # queried via
                                         # `db.index.fulltext.queryNodes`)
                                         # -- the direct equivalent of
                                         # the Postgres tsvector/GIN/
                                         # ts_rank approach, worth
                                         # learning properly rather than
                                         # a plain `CONTAINS` scan.
```

Use proper HTTP status codes, pagination on all list endpoints (Spring
Data `Pageable`, wrapped as `PagedModel` rather than returning `Page`
directly from a controller — Spring's own docs flag raw `Page` as an
unstable wire format), and consistent error responses (a single
`ErrorResponse` DTO with status, message, timestamp — no leaking stack
traces). Any exception that falls through to a generic 500 handler must
still be logged server-side — never swallow it silently.

## Non-functional requirements

- Integration tests (Testcontainers + Neo4j) for at least the Source
  and Note controllers — don't just unit test with mocks everywhere,
  prove the real DB interactions work
- neo4j-migrations checked into `src/main/resources/neo4j/migrations`,
  versioned properly
- application.yml with separate `dev` and `test` profiles
- Basic input validation (e.g. title required, rating 1-5 if present)
- README in the repo explaining: what this is, how to run it locally
  (docker-compose for Neo4j would be nice), and the API shape

## What I care about

Code quality over feature breadth. I'd rather have Source + Note fully
correct, tested, and cleanly structured than all four entities half-done.
Build incrementally: get Source + Note working end-to-end first (node,
migration, repository, service, controller, tests), then move to Concept
and Link once that's solid.

Sequencing risk worth naming: Source + Note is the least novel part of
this project -- a plain one-to-many, the kind of thing every Spring
tutorial builds, graph or relational. Concept and Link are where the
real learning value is -- native graph relationship modeling, Cypher
variable-length traversal, full-text search. Getting Source/Note fully
right first is still correct (don't skip it), but don't let the same
slow, fully-tested pace applied to the simpler entities eat all the time
before reaching the parts that are actually worth learning from.

## Out of scope for now (but decided, not just deferred)

- **Auth**: skip for now, but not forever. Confirmed: this stays a
  single-user project even after a live deployment eventually happens
  -- so the reason to eventually add auth isn't "other people might use
  or see this," it's plain security hygiene: don't leave a
  database-backed API with zero protection reachable on the open
  internet. When deployment gets real, add one thing and nothing more:
  a static API key checked via a single lightweight filter
  (`X-API-Key` header), the key itself in an environment variable,
  never committed. No OAuth2/JWT/user accounts -- that would be solving
  a multi-tenant problem this project doesn't have. Build this
  immediately before deployment, not before -- it doesn't block or get
  blocked by anything else here.
- Frontend (this is backend-only; may pair with the existing Docusaurus
  site later)
- **Person** as a fourth linkable entity (e.g. linking a Source or Note
  to its author, or to a philosopher discussed in it, as a first-class
  record rather than a free-text `author` field): confirmed as a real
  future direction, not built now. Adding it later should just mean a
  new `:Person` node label plus letting the existing relationship types
  point at it -- no schema redesign required, which is a real advantage
  of the graph model over the old polymorphic-table plan for this exact
  kind of future extension.
