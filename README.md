# justinus-api

A personal knowledge-tracking backend -- tracks what I read (books,
articles, papers) and the ideas/connections between them. A lightweight
Zettelkasten/PKM engine. See `SPEC.md` for the full design rationale
and `BUILD_PLAN.md` for the step-by-step build log.

Currently implemented, all end-to-end (node, migration, repository,
service, controller, validation, error handling, integration tests):
**Source**, **Note** (with tagging), **Concept**, **Link** (a generic
`LINKS_TO` relationship between any two of them), the concept graph
query, and full-text search.

Built on **Neo4j / Spring Data Neo4j**, not a relational database --
pivoted from an earlier, fully working Postgres/JPA build once Link's
design settled on "anything can link to anything" (Obsidian/
Zettelkasten-style), which fits a graph database's native data model
far better than a relational one. See `SPEC.md`'s Stack section for
the full rationale and the real gotchas hit along the way.

## Stack

- Java 25 (LTS)
- Spring Boot 4.1.1, Spring Framework 7
- Spring Data Neo4j (SDN) + Neo4j 5.x
- neo4j-migrations for versioned schema migrations (constraints,
  indexes) -- Cypher scripts, not SQL
- Testcontainers for integration tests against a real Neo4j container
- Maven
- Bean Validation (Jakarta) for request validation

## Running locally

Requires Java 25 and Docker (Neo4j via docker-compose, and
Testcontainers for tests).

```bash
docker-compose up -d          # starts Neo4j on localhost:7687 (bolt), 7474 (browser)
mvn spring-boot:run           # runs the app on localhost:8080
```

neo4j-migrations runs automatically on startup
(`src/main/resources/neo4j/migrations`).

Username/password default to `neo4j`/`justinus` for zero-setup local
dev, via `${NEO4J_USER:-neo4j}`-style env var substitution in both
`docker-compose.yml` and `application.yml` -- override
`NEO4J_USER`/`NEO4J_PASSWORD` for anything beyond local dev instead of
editing either file.

You can browse the graph directly at
[localhost:7474](http://localhost:7474) (Neo4j Browser) once
docker-compose is up.

## Running tests

Integration tests (`*IT.java`) use **Failsafe**, not Surefire -- run
them with:

```bash
mvn verify
```

(`mvn test` alone won't run them; `*IT` is Failsafe's naming
convention, separate from unit tests.) All `*IT` classes share one
real Neo4j container via Testcontainers -- no local Neo4j needed for
tests, but Docker must be running.

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

You may see `WARN ... property key does not exist` lines during test
runs -- harmless. Neo4j omits `null`-valued properties from a node
entirely rather than storing `null`, so querying a property that's
absent on some node logs that warning even though the result is
correctly `null`. Not a bug.

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
`localhost:8080`. The collection uses `{{sourceId}}`-style variables
rather than hardcoded ids, so it's unaffected by the switch to UUID
string ids.

## API shape

```
GET/POST     /sources
GET/PATCH    /sources/{id}
GET/POST     /sources/{id}/notes

GET/POST     /notes
GET          /notes/{id}

GET/POST     /concepts
GET/PATCH    /concepts/{id}
GET          /concepts/{id}/notes
GET          /concepts/{id}/graph?depth=2

POST         /links
GET          /links/{id}
GET          /{sources,notes,concepts}/{id}/links

GET          /search?q=...
```

- **All ids are UUID strings** (e.g. `3fa85f64-5717-4562-b3fc-
  2c963f66afa6`), not sequential numbers -- decided against Spring
  Data Neo4j's own guidance, which recommends against relying on
  Neo4j's internal id for anything long-lived. See `SPEC.md`.
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
- A link is `{fromType, fromId, toType, toId, type}` where the types
  are `SOURCE`/`NOTE`/`CONCEPT` and `type` is `SUPPORTS`/`CONTRADICTS`/
  `EXTENDS`/`RELATES_TO`. A missing endpoint (or an id that doesn't
  match its stated type) is a 404; linking a node to itself is a 400.
  `/{kind}/{id}/links` lists links in either direction.
- `/concepts/{id}/graph` returns `{nodes, edges}` for everything within
  `depth` hops (default 2, max 5) in either direction.
- `/search` runs a Neo4j full-text index over Note content and Source
  title/author/notes, ranked by score. Query text is treated as plain
  words: Lucene syntax characters and `AND`/`OR`/`NOT` are neutralized,
  so operators and quoted phrases are not supported.
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
curl -X PATCH localhost:8080/sources/<id-from-the-response-above> \
  -H 'Content-Type: application/json' \
  -d '{ "status": "FINISHED", "rating": 5 }'
```

### Example: add a Note to it, with tags

```bash
curl -X POST localhost:8080/sources/<id-from-the-response-above>/notes \
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

## Vulnerability scanning

- **GitHub Dependabot alerts** are enabled on the repo -- automatic,
  no local setup, flags known-CVE dependency versions and can open
  fix PRs.
- **[Trivy](https://github.com/aquasecurity/trivy)** for an on-demand
  local scan against the current `pom.xml`:
  ```bash
  trivy fs --scanners vuln .
  ```
  No API key or account needed. This is how the CVE-2026-65182/
  CVE-2026-65905/CVE-2026-68525 Tomcat vulnerabilities (fixed via the
  `tomcat.version` override in `pom.xml`) were actually found.
- OWASP Dependency-Check is a reasonable alternative but needs a free
  NVD API key (self-service signup at
  [nvd.nist.gov](https://nvd.nist.gov/developers/request-an-api-key))
  to fetch CVE data as of v13 -- not wired up here since Trivy already
  covers the same need with zero setup.
- `neo4j-migrations-spring-boot-starter` is the one dependency in
  `pom.xml` not managed by Spring Boot's own BOM (pinned manually) --
  worth an occasional manual re-scan as that one ages on its own
  schedule.

## Not yet built

- Auth (deliberately deferred -- see `SPEC.md`'s "Out of scope"
  section for the concrete plan and why)
