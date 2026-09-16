# justinus-api

A personal knowledge-tracking backend -- tracks what I read (books,
articles, papers) and the ideas/connections between them. A lightweight
Zettelkasten/PKM engine. See `SPEC.md` for the full design rationale
and `BUILD_PLAN.md` for the step-by-step build log.

Currently implemented: **Source** and **Note** (with tagging), both
end-to-end -- entity, migration, repository, service, controller,
validation, error handling, integration tests. **Concept** and
**Link** are specced but not yet built.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.1, Spring Framework 7
- Spring Data JPA + PostgreSQL 18
- Flyway for schema migrations (no Hibernate auto-DDL --
  `ddl-auto: validate`)
- Testcontainers for integration tests against a real Postgres
- Maven
- Bean Validation (Jakarta) for request validation

## Running locally

Requires Java 25 and Docker (Postgres via docker-compose, and
Testcontainers for tests).

```bash
docker-compose up -d          # starts Postgres on localhost:5432
mvn spring-boot:run           # runs the app on localhost:8080
```

Flyway runs migrations automatically on startup against the `justinus`
database (`src/main/resources/db/migration`).

DB name/user/password default to `justinus`/`justinus`/`justinus` for
zero-setup local dev, via `${POSTGRES_DB:-justinus}`-style env var
substitution in both `docker-compose.yml` and `application.yml` --
override `POSTGRES_DB`/`POSTGRES_USER`/`POSTGRES_PASSWORD` for
anything beyond local dev instead of editing either file.

## Running tests

Integration tests (`*IT.java`) use **Failsafe**, not Surefire -- run
them with:

```bash
mvn verify
```

(`mvn test` alone won't run them; `*IT` is Failsafe's naming
convention, separate from unit tests.) All `*IT` classes share one
real Postgres container via Testcontainers -- no local Postgres needed
for tests, but Docker must be running.

**If you add a new `*IT` class:** extend `AbstractIntegrationTest`,
don't add `@Testcontainers`/`@Container` to it or anywhere else. Those
JUnit lifecycle annotations restart the container (new port) between
test classes, but Spring's test context caching keeps reusing the
previous class's `ApplicationContext` -- which still points at the
now-dead old port. Every request in the new class fails with
`Connection refused`, and a retry loop underneath makes it look like a
multi-minute hang rather than a clear error. `AbstractIntegrationTest`
already starts one singleton container in a static initializer,
shared for the whole test run -- just extend it and nothing else is
needed.

### If your Docker runtime is Colima (not Docker Desktop)

Testcontainers' built-in Docker detection only knows about the standard
socket path and Docker Desktop's socket -- it won't find Colima's on
its own. One-time setup, per machine (not something the project can
check in, since the path is specific to your machine):

1. Find your Colima socket path:
   ```bash
   docker context inspect colima --format '{{.Endpoints.docker.Host}}'
   ```
2. Add it to `~/.testcontainers.properties` (create the file if it
   doesn't exist):
   ```
   docker.host=unix:///path/from/step/1/docker.sock
   ```

That's the only manual step needed -- the other Colima-specific fix
(disabling Testcontainers' Ryuk cleanup sidecar, which fails to start
under Colima's Linux VM) is already baked into `pom.xml`'s
`maven-failsafe-plugin` config, so nothing else to do there.

## Manually exploring the API

A [Bruno](https://www.usebruno.com/) collection lives in `bruno/` --
open that folder as a collection, select the **Local** environment,
and the existing Source and Note requests are ready to run against
`localhost:8080`.

## API shape

```
GET/POST     /sources
GET/PATCH    /sources/{id}
GET/POST     /sources/{id}/notes

GET/POST     /notes
GET          /notes/{id}
```

- List endpoints return a `PagedModel` body: `{content: [...], page:
  {size, number, totalElements, totalPages}}` -- standard Spring Data
  `Pageable` query params (`page`, `size`, `sort`) work as usual.
- `PATCH /sources/{id}` applies only the fields present in the request
  body; omitted/null fields are left unchanged. There's no way to
  explicitly clear a field back to null via PATCH.
- `POST /notes` requires `sourceId` in the body; `POST
  /sources/{id}/notes` takes the source from the path instead.
- `tags` on a Note are plain names, not ids -- an existing tag is
  matched case-insensitively ("Stoicism" and "stoicism" are the same
  tag) and reused; a genuinely new name creates a new Tag. Callers
  never need to know a tag's id up front.
- `GET /sources/{id}/notes` (and note creation under a missing source)
  return 404, not an empty list, if the source doesn't exist.
- Errors return a consistent body: `{ "status": <int>, "message":
  <string>, "timestamp": <ISO-8601> }`. No stack traces are ever
  returned; unexpected (500) errors are logged server-side.

### Example: create a Source

```bash
curl -X POST localhost:8080/sources \
  -H 'Content-Type: application/json' \
  -d '{
        "title": "Meditations",
        "author": "Marcus Aurelius",
        "type": "BOOK",
        "dateStarted": "2026-01-01",
        "status": "READING"
      }'
```

### Example: mark it finished and rate it

```bash
curl -X PATCH localhost:8080/sources/1 \
  -H 'Content-Type: application/json' \
  -d '{ "status": "FINISHED", "rating": 5 }'
```

### Example: add a Note to it, with tags

```bash
curl -X POST localhost:8080/sources/1/notes \
  -H 'Content-Type: application/json' \
  -d '{
        "content": "The dichotomy of control is the whole of the philosophy.",
        "locationRef": "Book 1, Ch. 2",
        "tags": ["control", "core-idea"]
      }'
```

## Health check

Spring Boot Actuator is included; `GET /actuator/health` reports `UP`
(only `health` is exposed over HTTP by default).

## Not yet built

- `Concept` and `Link` entities/endpoints (see `SPEC.md`)
- `GET /search`
- Auth (deliberately deferred -- see `SPEC.md`'s "Out of scope"
  section for the concrete plan and why)
