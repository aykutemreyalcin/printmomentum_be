---
name: spring-boot-rest
description: Spring Boot 4 REST and JPA conventions for PrintMomentum. Use when adding controllers, entities, jobs, tests, or Flyway migrations. Adapted from common Spring REST agent skills (lean, project-specific).
---

# Spring Boot REST (PrintMomentum)

## Layout

```
com.printmomentum
  web/          # controllers, API records
  domain/       # entities, domain services
  ingest/       # Etsy client, jobs
  config/       # CORS, RestClient, scheduling
```

- Java 21 records for request/response DTOs. No entity leak to JSON.
- `@Valid` on request bodies. Fail 400 at the boundary.
- Pagination: `page`, `size` (max 100), `sort`. Return `{items, page, size, total}`.
- Instants as ISO-8601 UTC.
- Controllers stay thin. Scoring lives in `domain`.

## HTTP

| Action | Method | Status |
|---|---|---|
| Read | GET | 200 |
| Create | POST | 201 + Location |
| Missing | GET | 404 |
| Validation | * | 400 |
| Etsy/upstream | * | 502/503, never 500 with Etsy body leaked |

Once error-handling task lands: RFC 7807 `application/problem+json`.

## Persistence

- Flyway only (`db/migration/V{n}__{name}.sql`). No Hibernate `update` in prod.
- MariaDB in AWS; H2 `MODE=MySQL` for tests/local default.
- `ddl-auto=validate`.
- Snapshot tables are append-only. Do not update history rows.

## HTTP client

Use Spring `RestClient` (Boot 4). Timeouts, 429 retry, structured logging of status + remaining quota. Never log API keys.

## Tests

- Web slice: `@WebMvcTest` for controllers.
- `@SpringBootTest` for Flyway + context.
- Classifier/scorer: pure unit tests, no Spring.
- Do not hit live Etsy in CI. Stub `EtsyClient`.

Run: `./mvnw test`
