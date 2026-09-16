# Project: justinus-api

Build a Spring Boot backend for a personal knowledge-tracking system —
tracks what I read (books, articles, papers) and the ideas/connections
between them. Think lightweight Zettelkasten/PKM engine, not a toy CRUD app.

This is also explicitly a demonstration piece: a good excuse to show off
relationships between entities, tagging, and querying — not just "log
some books." That framing should bias design decisions below whenever
there's a choice between "simplest to build" and "more interesting/
correct modeling" — see the Tag and Link notes especially.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.x, Spring Framework 7.x
- Spring Data JPA + PostgreSQL 18
- Flyway for migrations (no auto-DDL in prod profile). Two gotchas that
  cost real debugging time on Spring Boot 4.x:
  - Spring Boot 4's autoconfiguration got split per-feature into separate
    artifacts. `spring-boot-starter-jdbc` alone does NOT pull in Flyway
    autoconfiguration anymore — you need `org.springframework.boot:
    spring-boot-flyway` explicitly, or Flyway silently never runs (no
    error, no log line, migrations just don't happen).
  - Flyway 10+ also needs the Postgres dialect as its own artifact
    (`flyway-database-postgresql`) alongside `flyway-core`, or migrations
    fail against Postgres with an "unsupported database" error.
  - Postgres 18's Docker image also changed its volume convention: mount
    at `/var/lib/postgresql`, not `.../data`, or the container
    crash-loops on startup.
- Spring Boot Actuator — added mid-build, not originally planned. Only
  `/actuator/health` is exposed over HTTP by default.
- springdoc-openapi (Swagger UI) — planned addition, not yet added. Given
  this project doubles as a demo piece, a live interactive API explorer
  is worth more than curl/Bruno alone. The sibling `springer` project
  already does this.
- Testcontainers (2.x BOM) for integration tests against real Postgres —
  **expected to work, not yet verified** against Spring Boot 4.1.x. Both
  Flyway and Actuator had their own Spring Boot 4-specific surprises
  (one broke silently, one didn't); don't assume Testcontainers
  integration will be friction-free just because a compatible version
  exists on Maven Central.
- Maven
- Bean Validation (Jakarta Validation 3.1) for request validation
- Clean layered architecture: controller -> service -> repository, with
  DTOs separate from JPA entities (no leaking entities through the API)

Known gotcha: Spring Framework 7 / Spring Boot 4 ship on **Jackson 3**,
which renamed its base packages (`com.fasterxml.jackson.*` ->
`tools.jackson.*` for the new modules, though `com.fasterxml.jackson.core`
low-level types stick around). Don't copy-paste Jackson imports from
Spring Boot 3.x-era code without checking they still resolve.

## Core entities

**Source** — something I read
- id, title, author, type (BOOK / ARTICLE / PAPER), dateStarted,
  dateFinished (nullable), status (READING / FINISHED / ABANDONED),
  rating (nullable, 1-5), generalNotes (nullable)
- Gotcha already hit: `rating` is `SMALLINT` in the migration, which
  means the Java field must be `Short`, not `Integer` — Hibernate's
  schema validation treats those as different SQL types and fails
  loudly (correctly) if they don't match.

**Note** — an atomic idea or excerpt tied to a Source
- id, sourceId (FK), content, locationRef (e.g. page number, nullable),
  createdAt
- tags: a real **Tag** entity with a proper many-to-many, not a string
  array/`@ElementCollection`. This was previously left as "your call" —
  given the project's explicit goal of showing off relationship
  modeling, a string array demonstrates nothing relationally and isn't
  the right default here. A real Tag entity also gets you tag reuse,
  "all notes with tag X," and tag popularity queries for free.

**Concept** — a recurring idea/theme (e.g. "free will," "moral luck")
that Notes can reference
- id, name, description (nullable)
- many-to-many with Note

**Link** — a directed connection between two Notes, or two Concepts.
**This is the centerpiece entity, not an afterthought** — it's the one
thing that makes this a Zettelkasten backend instead of a reading log
with notes attached, and it's the most interesting relationship-modeling
exercise in the whole project. Don't apply "whichever is simplest" here
the way it's fine to elsewhere; give the polymorphic association
(NOTE/CONCEPT, and possibly self-referential Note<->Note) real design
attention.
- id, fromId, toId, type (SUPPORTS / CONTRADICTS / EXTENDS / RELATES_TO),
  targetType (NOTE or CONCEPT)

## Endpoints

```
GET/POST     /sources
GET/PATCH    /sources/{id}
GET/POST     /sources/{id}/notes

GET/POST     /notes
GET          /notes/{id}
GET/POST     /notes/{id}/links

GET/POST     /concepts
GET          /concepts/{id}
GET          /concepts/{id}/notes       # all notes touching this concept
GET          /concepts/{id}/graph       # concept + linked concepts/notes,
                                         # returned as {nodes: [...], edges: [...]}
                                         # for future graph visualization.
                                         # This should be a real graph
                                         # traversal (Postgres recursive
                                         # CTE, `WITH RECURSIVE`), depth-
                                         # limited (e.g. "within 2 hops"),
                                         # not a single-level join --
                                         # this is a showcase query, worth
                                         # doing properly.

GET          /search?q=...              # full-text search across
                                         # notes + sources. Use real
                                         # Postgres full-text search
                                         # (tsvector + GIN index +
                                         # ts_rank), not LIKE/ILIKE --
                                         # this is a showcase query too,
                                         # and FTS isn't much more work.
```

Use proper HTTP status codes, pagination on all list endpoints (Spring
Data `Pageable`, wrapped as `PagedModel` rather than returning `Page`
directly from a controller — Spring's own docs flag raw `Page` as an
unstable wire format), and consistent error responses (a single
`ErrorResponse` DTO with status, message, timestamp — no leaking stack
traces). Any exception that falls through to a generic 500 handler must
still be logged server-side — never swallow it silently.

## Non-functional requirements

- Integration tests (Testcontainers + Postgres) for at least the
  Source and Note controllers — don't just unit test with mocks
  everywhere, prove the real DB interactions work
- Flyway migrations checked into `src/main/resources/db/migration`,
  versioned properly (V1__init.sql, etc.)
- application.yml with separate `dev` and `test` profiles
- Basic input validation (e.g. title required, rating 1-5 if present)
- README in the repo explaining: what this is, how to run it locally
  (docker-compose for Postgres would be nice), and the API shape

## What I care about

Code quality over feature breadth. I'd rather have Source + Note fully
correct, tested, and cleanly structured than all four entities half-done.
Build incrementally: get Source + Note working end-to-end first (entity,
migration, repository, service, controller, tests), then move to Concept
and Link once that's solid.

Sequencing risk worth naming: Source + Note is the least novel part of
this project -- a plain one-to-many, the kind of thing every Spring
tutorial builds. Concept and Link are where the actual "show off
relationships" value lives. Getting Source/Note fully right first is
still correct (don't skip it), but don't let the same slow, fully-tested
pace applied to the boring entities eat all the time before reaching the
interesting ones.

## Out of scope for now

- Auth (single-user, local project for now) — **but this needs an actual
  decision, not just deferral.** "Personal local tool" and "project I
  want to show off" want different answers here: a private tool can
  reasonably skip auth forever; a demo piece gets shown to other people
  eventually, which changes the calculus. Decide which future this is
  before this API is reachable from anywhere other than localhost (e.g.
  if it ends up paired with the public Docusaurus site, or just linked
  from a portfolio).
- Frontend (this is backend-only; may pair with the existing Docusaurus
  site later)
