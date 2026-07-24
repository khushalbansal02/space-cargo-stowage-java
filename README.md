# Space Cargo Stowage — Backend

![CI](https://github.com/khushalbansal02/space-cargo-stowage-java/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green)

A Spring Boot backend for managing cargo aboard a space station. It automates the
three hardest parts of stowage on a crewed vehicle: **packing items into limited
volume**, **retrieving a specific item with the fewest possible moves**, and
**tracking and returning waste** on resupply undockings.

> Rewritten in Java/Spring Boot from an original PHP prototype, with a clean layered
> architecture, a fully unit-tested packing engine, and a real PostgreSQL datastore.

---

## The problem

On a space station, thousands of items are stored across containers in different
zones. Volume is scarce, crew time is expensive, and expired or used-up items must
be shipped back on resupply vehicles. This service answers three operational
questions automatically:

1. **Where does each item go?** — a 3D bin-packing engine that respects item
   priority and preferred zones while keeping high-priority items accessible.
2. **How do I get item X out?** — the minimal ordered list of items to move aside,
   computed from the actual geometry.
3. **What is waste, and how do we return it?** — identification of expired/consumed
   items and a mass-bounded return manifest for undocking.

---

## Features

| Domain | Endpoint(s) | Notes |
|---|---|---|
| **Placement** | `POST /api/v1/placements` | 3D bin-packing; priority-ordered; zone-preference aware |
| **Retrieval** | `GET /api/v1/search`, `POST /api/v1/items/{id}/retrieve` | obstruction plan + transactional use-consumption |
| **Containers/Items** | `GET/POST /api/v1/containers`, `.../items` | validated CRUD |
| **Waste** | `GET /api/v1/waste`, `POST /api/v1/waste/return-plan`, `.../undock` | expiry/usage classification, mass budgeting |
| **Simulation** | `POST /api/v1/simulate/day`, `GET /api/v1/simulate/current-date` | advance the clock, age & consume items |
| **Import/Export** | `POST /api/v1/import/{containers,items}`, `GET /api/v1/export/arrangement` | CSV in/out |
| **Audit log** | `GET /api/v1/logs` | every mutation recorded, filterable & paginated |
| **Analytics** | `GET /api/v1/metrics` | container utilisation %, avg retrieval steps, waste, status counts |
| **Ops** | `GET /actuator/{health,info,metrics,prometheus}` | health checks and Micrometer metrics |

Interactive API docs (Swagger UI) at **`/swagger-ui.html`** once running.

Every request carries an `X-Correlation-Id` (generated if not supplied) that is
echoed on the response and stamped on every log line for that request.

---

## The placement algorithm

Each container is a 3D volume with its open face at depth `Y = 0`. The engine packs
one item at a time, highest priority first, using a deterministic **extreme-point /
surface heuristic**:

1. **Candidate anchors** — the container origin plus the right, top, and back corner
   of every already-placed box.
2. **Orientations** — each item is tried in all of its distinct axis-aligned
   orientations (up to 6), so a box is rotated to fit where it can.
3. **Feasibility** — positions that leave the container or collide with an existing
   box are discarded.
4. **Accessibility score** — the surviving spot with the lowest score wins:

   ```
   score = y·10⁶ + z·10³ + x        (lower is better)
   ```

   Depth dominates, so items land as close to the open face as possible; ties break
   toward lower, then left, giving tight, deterministic packings.

Across containers, a spot outside the item's preferred container/zone gets a large
score penalty — so a preferred location always wins when one exists, while the engine
still falls back to any container rather than failing to place.

See [`PlacementHeuristic`](src/main/java/com/spacecargo/stowage/placement/PlacementHeuristic.java)
and [`PlacementService`](src/main/java/com/spacecargo/stowage/service/PlacementService.java).

## The retrieval algorithm

To retrieve a target item, the planner finds every item that both sits **in front of
it** (smaller `Y`) **and** overlaps its **front-face (X–Z) projection** — those are
the only items that actually block sliding it out. Blockers are returned
nearest-first as `REMOVE` steps, followed by a final `RETRIEVE` step. An unobstructed
item is a single step.

See [`RetrievalPlanner`](src/main/java/com/spacecargo/stowage/retrieval/RetrievalPlanner.java).

---

## Architecture

Layered, with a **pure, framework-free core** (geometry + packing + retrieval) that
is unit-tested in isolation, wrapped by Spring services and thin REST controllers.

```
web/          REST controllers + DTOs (validation, RFC-7807 errors)
  dto/
service/      transactional orchestration (placement, retrieval, waste, simulation, import/export)
placement/    pure bin-packing engine (no Spring, no DB)
retrieval/    pure obstruction planner
waste/        waste value types
simulate/     simulation value types
domain/       JPA entities + immutable geometry value objects
  geometry/     Dimensions, BoundingBox
repository/    Spring Data JPA repositories
config/       clock, OpenAPI
exception/    domain exceptions + global handler
```

**Tech stack:** Java 17 · Spring Boot 3.3 (Web, Data JPA, Validation) ·
PostgreSQL 16 · Flyway migrations · OpenCSV · springdoc OpenAPI · JUnit 5 + AssertJ +
MockMvc · Docker / Docker Compose.

---

## Getting started

### Run everything with Docker Compose

```bash
docker compose up --build
```

This starts PostgreSQL and the API together. The API listens on
**http://localhost:8000** — open **http://localhost:8000/swagger-ui.html**.

### Run the API locally against Docker Postgres

```bash
docker compose up -d db          # Postgres only
mvn spring-boot:run              # API on :8000
```

Datasource settings are overridable via `DB_URL`, `DB_USER`, `DB_PASSWORD`.

### Browse the database (pgAdmin)

```bash
docker compose --profile tools up -d pgadmin
```

Open **http://localhost:5050** — no login. The **Cargo (local)** server is
pre-registered and auto-connects; expand *Servers → Cargo (local) → Databases →
cargo → Schemas → public → Tables* to browse `containers`, `items`, `action_logs`,
and `simulation_state`. Right-click a table → *View/Edit Data* to see live rows.

> Prefer a native app? Point **DBeaver** or **TablePlus** at
> `localhost:5432`, database `cargo`, user `cargo`, password `cargo`.

### Try it

```bash
# 1. Import sample containers and items
curl -F file=@sample-data/containers.csv http://localhost:8000/api/v1/import/containers
curl -F file=@sample-data/items.csv      http://localhost:8000/api/v1/import/items

# 2. Run the placement engine
curl -X POST http://localhost:8000/api/v1/placements

# 3. Find an item and see how to reach it
curl "http://localhost:8000/api/v1/search?itemId=ITM-001"

# 4. Export the arrangement
curl http://localhost:8000/api/v1/export/arrangement
```

---

## Testing

```bash
mvn test
```

The bulk of the suite runs against an in-memory **H2 (PostgreSQL mode)** database, so
it needs no external services. It covers the geometry primitives, the packing
heuristic (including a property test that 50 packed cubes never overlap), the
retrieval planner, every service against a real transactional context, and the REST
layer end-to-end via MockMvc. A separate **Testcontainers** test boots the app
against a real `postgres:16-alpine` (real Flyway migrations, real driver); it runs in
CI and is skipped automatically where Docker is unavailable.

CI runs the whole suite on every push and pull request
([workflow](.github/workflows/ci.yml)).

---

## Notes on the design

- **Schema is owned by Flyway**, not Hibernate (`ddl-auto: validate`) — migrations are
  reviewable and reproducible.
- **The packing/retrieval core has no Spring or JDBC dependency**, which is what makes
  it fast and pleasant to unit-test.
- **Retrieval consumes uses inside a single transaction**, closing a read-modify-write
  race that the original prototype had.
- **An injectable `Clock`** makes expiry and simulation deterministic in tests.

## License

MIT
