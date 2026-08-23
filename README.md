# PrintMomentum backend

Spring Boot API for ranking **printable** Etsy t-shirts by how fast they climb, not how long they have sat on a bestseller list.

## Stack

- Java 21
- Spring Boot 4.1.1
- MariaDB (H2 in-memory for local/tests)
- Flyway
- Official Etsy Open API v3 only (no scraping)

## Run

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw test
./mvnw spring-boot:run
```

Health: `GET http://localhost:8080/api/v1/health`

## Cursor

Project skills live in `.cursor/skills/`. See `AGENTS.md`.
