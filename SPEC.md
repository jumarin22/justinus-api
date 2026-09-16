# Project: justinus-api

Build a Spring Boot backend for a personal knowledge-tracking system —
tracks what I read (books, articles, papers) and the ideas/connections
between them. Think lightweight Zettelkasten/PKM engine, not a toy CRUD app.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.x, Spring Framework 7.x
- Spring Data JPA + PostgreSQL 18
- Flyway for migrations (no auto-DDL in prod profile) — Flyway 10+ needs the
  Postgres dialect as its own artifact (`flyway-database-postgresql`)
  alongside `flyway-core`; don't drop it and wonder why migrations fail
  against Postgres with an "unsupported database" error
- Testcontainers (2.x BOM) for integration tests against real Postgres
- Maven (or Gradle — your call, pick one and be consistent)
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
  rating (nullable), generalNotes (nullable)

**Note** — an atomic idea or excerpt tied to a Source
- id, sourceId (FK), content, locationRef (e.g. page number, nullable),
  createdAt
- tags (many-to-many with a simple Tag entity, or just a string array —
  your call)

**Concept** — a recurring idea/theme (e.g. "free will," "moral luck")
that Notes can reference
- id, name, description (nullable)
- many-to-many with Note

**Link** — a directed connection between two Notes, or two Concepts
- id, fromId, toId, type (SUPPORTS / CONTRADICTS / EXTENDS / RELATES_TO),
  targetType (NOTE or CONCEPT — decide if Links are polymorphic or if
  you want separate NoteLink/ConceptLink tables; prefer whichever is
  simpler to implement well over the "more correct" option)

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
                                         # for future graph visualization

GET          /search?q=...              # full-text search across
                                         # notes + sources
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

## Out of scope for now

- Auth (single-user, local project for now — but revisit this before this
  API is reachable from anywhere other than localhost, e.g. if it ends up
  paired with the public Docusaurus site)
- Frontend (this is backend-only; may pair with the existing Docusaurus
  site later)
- Full-text search can start as simple `LIKE`/`ILIKE` — Postgres
  `tsvector` is a nice-to-have upgrade later, not a blocker
