# PrintMomentum backend

Spring Boot API for ranking **printable** Etsy t-shirts by how fast they climb, not how long they have sat on a bestseller list.

## Stack

- Java 21 (`JAVA_HOME` must point at JDK 21)
- Spring Boot 4.1.1
- MariaDB 11 via Docker Compose (`local` profile)
- H2 in-memory for default run and tests
- Flyway
- Official Etsy Open API v3 only (no scraping)

## Java 21

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## Run (H2, default)

```bash
./mvnw test
./mvnw spring-boot:run
```

Health: `GET http://localhost:8080/api/v1/health`

## Run (local MariaDB)

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Compose starts MariaDB 11 on `localhost:3307` (host 3307 → container 3306) with database/user `printmomentum` (password `printmomentum`). Host 3306 stays free for any existing MySQL. Override with `SPRING_DATASOURCE_*` if needed. Tests stay on H2 even when this profile exists.

## Cursor

Project skills live in `.cursor/skills/`. See `AGENTS.md`.
