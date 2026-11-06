# Fraud Rule Engine Service - Ari Nemadodzi

A Java 21 / Spring Boot REST API that screens incoming ZAR transactions against configurable fraud rules, persists the transactions and resulting fraud flags, and gives analysts an auditable review workflow.

**Stack:** Spring Boot 3 · Java 21 · Project Lombok · Spring Security · Spring Data JPA · PostgreSQL 16 · Flyway · Swagger/OpenAPI · Postman · Docker Compose · JUnit 5.

> This is a learning and demonstration system, not a production banking control. It uses HTTP Basic service accounts, in-memory user definitions, synchronous rule execution and demo seed data. Use TLS, an identity provider, secrets management, operational monitoring and independent security review before any real deployment.

### Lombok and the original Java approach

The JPA entities use Lombok `@Getter` to generate accessor methods during compilation. The original manually written getter implementations have been retained as commented-out examples in each entity, so a reviewer can compare the two approaches. Setters are intentionally not generated: transactions and audit-history records are immutable, while customer, flag and blacklist changes must pass through their domain methods to preserve validation, business rules and audit behaviour. DTOs use Java records, which already provide their accessors.

## Contents

1. [What the system does](#1-what-the-system-does)
2. [Roles and permissions](#2-roles-and-permissions)
3. [Fraud rules](#3-fraud-rules)
4. [Requirements](#4-requirements)
5. [Run locally without Docker](#5-run-locally-without-docker)
6. [Run with Docker Compose and PostgreSQL](#6-run-with-docker-compose-and-postgresql)
7. [Test with Swagger UI](#7-test-with-swagger-ui)
8. [Test with Postman](#8-test-with-postman)
9. [API reference](#9-api-reference)
10. [Database and demo data](#10-database-and-demo-data)
11. [Automated tests](#11-automated-tests)
12. [Project structure](#12-project-structure)
13. [Configuration](#13-configuration)
14. [Troubleshooting](#14-troubleshooting)
15. [Known limitations and next steps](#15-known-limitations-and-next-steps)

---

## 1. What the system does

```text
Upstream transaction sender
          |
          | POST /api/v1/transactions (or /batch)
          v
   TransactionService
          |
          v
      FraudEngine ---- runs each enabled FraudRule
          |                         |
          |                         +-- HIGH_AMOUNT
          |                         +-- VELOCITY
          |                         +-- GEO_CONFLICT
          |                         +-- NIGHT_TRANSACTION
          |                         +-- GAMBLING
          |                         +-- CARD_TESTING
          |                         +-- BLACKLIST_MERCHANT
          v
  PostgreSQL / H2
  transactions + fraud_flags
          |
          v
 Analyst searches flags, reviews them and records a decision
          |
          v
 flag_status_history (append-only audit trail)
```

The screening is synchronous: the ingestion response contains the transaction and any flags raised by the rules. The transaction and its flags are saved in one database transaction. If screening or persistence fails, the request rolls back rather than saving a partial result.

## 2. Roles and permissions

The API follows least privilege. A valid username/password is not enough on its own; the account must also have the correct role.

| Capability | INGESTION | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| Submit one transaction | Yes | No | Yes |
| Submit a batch (up to 100) | Yes | No | Yes |
| Read transactions | No | Yes | Yes |
| Search/read fraud flags | No | Yes | Yes |
| Change flag status and add review note | No | Yes | Yes |
| View flag status audit history | No | Yes | Yes |
| List customers | No | Yes | Yes |
| Register a customer | No | No | Yes |
| List active blacklisted merchants | No | Yes | Yes |
| Add/remove blacklisted merchants | No | No | Yes |
| View full blacklist history, including removed entries | No | No | Yes |
| Swagger/OpenAPI documentation and health check | Public | Public | Public |

**INGESTION** is a service account for trusted upstream systems. It can send transactions but cannot read customer or fraud information. **ANALYST** can investigate and record a decision, but cannot submit transactions or change customer/blacklist reference data. **ADMIN** can perform all application operations and manage reference data.

The three accounts are configured at application startup. This demo does not provide an API for creating users or changing roles; use a proper identity provider for a real system.

### Development credentials

The local H2 profile has development-only defaults:

| Username | Development password |
|---|---|
| `admin` | `admin-dev-password` |
| `analyst` | `analyst-dev-password` |
| `ingestion` | `ingestion-dev-password` |

For PostgreSQL or MySQL, set `FRAUD_ADMIN_PASSWORD`, `FRAUD_ANALYST_PASSWORD` and `FRAUD_INGESTION_PASSWORD` in `.env`. Replace the example values before starting Docker. Passwords are BCrypt-hashed by Spring Security; do not use the development defaults outside local practice.

## 3. Fraud rules

All monetary thresholds are ZAR and are configurable in `src/main/resources/application.properties` or through environment variables.

| Rule | Default trigger | Typical severity |
|---|---|---|
| `HIGH_AMOUNT` | Amount at least R20,000; critical at R50,000 | HIGH / CRITICAL |
| `VELOCITY` | More than 5 transactions for one customer in 10 minutes | HIGH |
| `GEO_CONFLICT` | Same customer transacts in two different countries within 60 minutes | HIGH |
| `NIGHT_TRANSACTION` | Amount at least R500 between 01:00 and 04:59 South African time | MEDIUM |
| `GAMBLING` | Gambling category and amount at least R5,000, or 3+ gambling transactions in 24 hours | MEDIUM / HIGH |
| `CARD_TESTING` | 3+ transactions of R20 or less in 10 minutes | HIGH |
| `BLACKLIST_MERCHANT` | Merchant matches an active blacklist entry | CRITICAL |

A transaction can trigger more than one rule and therefore create more than one fraud flag. `riskScore` is a 0–100 score associated with the most severe matching rule result; it is a prioritisation aid, not a probability of fraud.

Add a new rule by implementing `FraudRule` and registering it as a Spring component. `FraudEngine` discovers the rule beans through dependency injection.

## 4. Requirements

### Option A — Docker (recommended for realistic database testing)

- Docker Desktop installed and running on Windows, macOS or Linux.
- On Windows, Docker Desktop's WSL 2 backend and the Windows **Virtual Machine Platform** feature must be enabled; restart Windows after enabling it.
- Postman Desktop (or Postman Web with its Desktop Agent) for API testing.

You do not need to install Java or Maven on your computer when using Docker Compose; the Docker build compiles the project inside the build container.

### Option B — Run directly on your computer

- JDK 21
- Maven 3.9+
- No database installation is required for the default H2 profile.

Check installations:

```bash
java -version
mvn -version
docker --version
 docker compose version
```

The Docker commands are only needed for Option A.

## 5. Run locally without Docker

Open a terminal in the folder containing `pom.xml` and run:

```bash
mvn clean test
mvn spring-boot:run
```

The default `h2` profile uses an in-memory H2 database and loads a small starter set of demo customers and blacklist merchants. Data is lost when the application stops. To test the PostgreSQL demo dataset, use the Docker Compose instructions in Section 6.

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`
#### Not recommended
- H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:frauddb`, user `sa`, blank password)


Stop the application with `Ctrl+C`.

## 6. Run with Docker Compose and PostgreSQL

This is the recommended setup for repeatable testing because PostgreSQL runs in a persistent Docker volume.

### Step 1 — Prepare the environment file

In PowerShell, from the project root:

```powershell
Copy-Item .env.example .env
notepad .env
```

Change all four example passwords (`DB_PASSWORD`, `FRAUD_ADMIN_PASSWORD`, `FRAUD_ANALYST_PASSWORD`, `FRAUD_INGESTION_PASSWORD`) to values you choose. Keep the values handy because you must enter the three API passwords in Postman/Swagger. The `.env` file is ignored by Git and must not be committed for password safety.

### Step 2 — Start the API and PostgreSQL(recommanded)

```bash
docker compose --profile postgres up --build -d

```
```
If connection fail, 
```
The first build can take a few minutes while Docker downloads the Java and PostgreSQL images and Maven dependencies. Compose waits for PostgreSQL's health check before starting the API.

### Step 3 — Confirm the containers

```bash
docker compose ps
docker compose logs -f fraud-engine
```

Wait until the logs include `Started FraudRuleEngineApplication`. Press `Ctrl+C` to leave the live logs; this does not stop the containers.

### Step 4 — Open the application

- API base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health: `http://localhost:8080/actuator/health`

### Stop and reset

```bash
docker compose down       # stop containers; keep PostgreSQL data
```

To delete the database volume and start with a completely fresh database next time:

```bash
docker compose down -v
```

**Warning:** `down -v` permanently deletes the local PostgreSQL data, including customers, transactions and flags. Do not use it if you want to keep your test records.

### Use MySQL instead (optional)

Set `DB_PROFILE=mysql` and `COMPOSE_PROFILES=mysql` in `.env`, then run:

```bash
docker compose --profile mysql up --build -d
```

PostgreSQL is the primary database for this project. H2 is intended for quick local development and automated tests.

## 7. Test with Swagger UI

1. Start the application using Option A or B.
2. Open `http://localhost:8080/swagger-ui.html`.
3. Click **Authorize**.
4. Enter the username and matching password from Section 2 (for example, `admin` and the admin password you configured in `.env`).
5. Click **Authorize**, then **Close**.
6. Expand an endpoint group, select an endpoint, click **Try it out**, enter the JSON body if required, and click **Execute**.

To test permissions, click **Authorize** again and use a different role. For example, try `GET /api/v1/flags` as `ingestion`; it should return `403 Forbidden`. A `401` means the credentials are missing or incorrect; a `403` means the account authenticated but does not have permission.

## 8. Test with Postman

The project includes a ready-to-import collection and local environment:

- `postman/Fraud-Rule-Engine.postman_collection.json`
- `postman/Fraud-Rule-Engine-Local.postman_environment.json`

### Import

1. Open Postman.
2. Click **Import**.
3. Select both JSON files from the project's `postman` folder.
4. Select the environment **Fraud Rule Engine - Local** in the top-right environment selector.
5. Open the environment's variables and set `adminPassword`, `analystPassword` and `ingestionPassword` to match your `.env` file. For H2 local mode, the development defaults are already filled in.
6. Keep `baseUrl` as `http://localhost:8080` unless you changed the server port.
7. Open the collection and run requests in order, or send individual requests.

### Suggested Postman run order

1. **Public** → Health check.
2. **Admin** → Create customer. The response script stores `customerId` automatically.
3. **Admin** → Add a test merchant to the blacklist. The response script stores `blacklistId` and the unique `testMerchantName`.
4. **Ingestion** → Submit a clean transaction.
5. **Ingestion** → Submit a high-value transaction. The response script stores a matching `flagId` when `HIGH_AMOUNT` fires.
6. **Ingestion** → Submit a transaction to the test blacklisted merchant.
7. **Ingestion** → Submit a two-transaction batch.
8. **Analyst** → Search flags, get a flag, move it to `UNDER_REVIEW`, then `CONFIRMED_FRAUD`, and view its audit history.
9. **Admin** → View blacklist history and remove the test merchant.
10. **Security checks** → Confirm ingestion cannot read flags and analyst cannot create customers or manage the blacklist (expected `403`).

Postman scripts capture generated IDs, so you do not need to guess database IDs. For manual lookups, prefer `/transactions/reference/{reference}` when you know the sender reference; generated numeric IDs are database-local and should not be assumed. Run the setup requests first. If you rerun the collection, it creates a new customer and a uniquely named test merchant.

## 9. API reference

Base path: `/api/v1`. All request bodies are JSON. Amounts are decimal values with up to two fractional digits; timestamps must be ISO-8601 with an offset, e.g. `2026-11-04T09:15:00Z`.

| Method | Endpoint | Roles | Description |
|---|---|---|---|
| `POST` | `/transactions` | INGESTION, ADMIN | Screen one transaction; returns transaction and flags (`201`) |
| `POST` | `/transactions/batch` | INGESTION, ADMIN | Screen 1–100 transactions atomically (`201`) |
| `GET` | `/transactions` | ANALYST, ADMIN | Paged transaction search; optional `customerId` |
| `GET` | `/transactions/{id}` | ANALYST, ADMIN | One transaction by generated database ID and its flags |
| `GET` | `/transactions/reference/{reference}` | ANALYST, ADMIN | One transaction by its stable client-supplied reference and its flags |
| `GET` | `/flags` | ANALYST, ADMIN | Paged flag search |
| `GET` | `/flags/{id}` | ANALYST, ADMIN | One flag |
| `GET` | `/flags/{id}/history` | ANALYST, ADMIN | Audit trail for a flag |
| `PATCH` | `/flags/{id}/status` | ANALYST, ADMIN | Record a review decision |
| `POST` | `/customers` | ADMIN | Register customer |
| `GET` | `/customers` | ANALYST, ADMIN | Paged customer list |
| `GET` | `/customers/{id}` | ANALYST, ADMIN | Customer details |
| `PUT` | `/customers/{id}` | ADMIN | Replace the complete editable customer profile; ID and createdAt stay unchanged |
| `DELETE` | `/customers/{id}` | ADMIN | Delete only when no transactions exist; otherwise `409 Conflict` protects the audit ledger |
| `GET` | `/blacklist` | ANALYST, ADMIN | List active merchants |
| `POST` | `/blacklist` | ADMIN | Add or reactivate a merchant |
| `PUT` | `/blacklist/{id}` | ADMIN | Replace an active merchant name and reason; addedAt stays unchanged |
| `DELETE` | `/blacklist/{id}` | ADMIN | Soft-remove a merchant |
| `GET` | `/blacklist/history` | ADMIN | Full blacklist history, including removed entries |

### PUT, PATCH and DELETE behaviour

- **PUT** replaces all editable fields of a resource. Customer updates require `fullName`, `email` and `homeCountry`; blacklist updates require `merchantName` and `reason`. Missing required fields fail validation.
- **PATCH** remains appropriate for a partial change. Flag review uses `PATCH /flags/{id}/status` because only the status and optional note are being changed. Transaction records are intentionally immutable, so there is no transaction PUT endpoint: changing a screened financial event would damage the audit trail.
- **DELETE customer (ADMIN only)** is allowed only when the customer has no transactions. If any transaction exists, the API returns `409 Conflict` and keeps the customer so the ledger and fraud evidence remain intact. This is deliberate financial-data protection, not a missing permission.
- **DELETE blacklist entry** remains a soft delete: it removes the merchant from active matching but keeps the historical record.

The Postman collection includes the new PUT requests, an isolated disposable-customer DELETE test, and analyst permission checks. Run the setup requests first and run the disposable-customer cleanup request only after the create request has captured `deletableCustomerId`.

**No shell script is required.** The former `scripts/demo.sh` was only an optional command-line demonstration that called the API with `curl`; it did not start Spring Boot, create the database schema, or implement authentication. It has been removed. Use Swagger UI or the supplied Postman collection for the same API workflows. Spring Security continues to authenticate the three service accounts inside the application.

### Flag search filters

`GET /api/v1/flags` accepts optional filters that combine with AND:

- `customerId`, `transactionId`, `ruleCode`
- `severity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` (values supported by the enum)
- `status`: `OPEN`, `UNDER_REVIEW`, `CONFIRMED_FRAUD`, `FALSE_POSITIVE`
- `from`, `to`: ISO-8601 date-time boundaries
- `page`, `size` (maximum size 100), `sort`

Example:

```text
GET http://localhost:8080/api/v1/flags?severity=CRITICAL&status=OPEN&page=0&size=20
```

### Transaction request example

```json
{
  "reference": "TX-POSTMAN-001",
  "customerId": 1,
  "amount": 60000.00,
  "merchantName": "Gadget World",
  "merchantCategory": "RETAIL",
  "country": "ZA",
  "occurredAt": "2026-11-04T09:15:00Z"
}
```

A duplicate `reference` returns `409 Conflict`; the sender must use a new unique reference for a new transaction. The API rejects invalid data (`400`), missing customers (`404`), timestamps more than five minutes in the future (`422`) and forbidden role access (`403`).

### Flag review lifecycle

```text
OPEN -> UNDER_REVIEW -> CONFIRMED_FRAUD
                      -> FALSE_POSITIVE
```

`CONFIRMED_FRAUD` and `FALSE_POSITIVE` are final states. The API derives `reviewedBy` from the authenticated username; clients cannot choose or impersonate the reviewer. Every valid transition is recorded in `flag_status_history`.

## 10. Database and demo data

Flyway creates the schema at startup. Hibernate is configured to validate the schema, not silently create or alter it.

| Table | Purpose |
|---|---|
| `customers` | Customer reference records |
| `transactions` | Immutable received transactions; `reference` is unique |
| `fraud_flags` | One record per fired rule per transaction |
| `flag_status_history` | Append-only audit record of analyst/admin status transitions |
| `blacklist_merchants` | Merchant blacklist; removals are soft deletes |

PostgreSQL uses Flyway migrations to create the schema and seed a ready-to-test dataset. On a fresh PostgreSQL database, the `customers` table contains 20 demo customers (including Ari, Junior, Chuma, Tshifhi, Mmasello and well-known South African soccer players), and each of the other four tables also receives 20 records:

| Table | Demo records | Notes |
|---|---:|---|
| `customers` | 20 | South African customer examples and soccer-player names |
| `transactions` | 20 | Linked to the demo customers, with varied merchants, countries and amounts |
| `fraud_flags` | 20 | Includes a mix of risk levels and rule codes |
| `flag_status_history` | 20 | One example analyst review event for each demo flag |
| `blacklist_merchants` | 20 | Includes the original examples and additional demo merchants |

The original V1, V2 and V3 migrations are left unchanged so an existing database does not encounter a Flyway checksum error. The additional PostgreSQL dataset is added by `V3__seed_20_demo_records_per_table.sql`. A later additive migration, `V4__repair_missing_demo_records.sql`, safely restores missing demo rows on databases where V3 was already recorded but some seed rows were subsequently removed; it does not overwrite existing records. Flyway applies each migration once; it does not duplicate records every time the application restarts. On an already-running database, restart the application after adding the project update and check the Flyway log for V4 (and confirm V1–V3 are already successful). If the database has already been populated manually, check for duplicate demo email addresses/references before applying the migration.

The H2 profile remains a lightweight quick-start profile with its own smaller starter dataset. Use the PostgreSQL profile when you want to inspect all 20 demo rows per table.

For Docker PostgreSQL, the named volume `pgdata` keeps data after `docker compose down`. Flyway does not reinsert seed data on every restart. Deleting the volume with `docker compose down -v` recreates a clean database on the next start.

## 11. Automated tests

Run from the project root:

```bash
mvn clean test
```

The test suite uses H2 and MockMvc, so PostgreSQL and Docker are not required for the standard test run.

| Test area | What it checks |
|---|---|
| Individual rule tests | Thresholds and boundary conditions for all seven rules |
| Fraud engine tests | Rule aggregation and disabled-rule handling |
| Flag status tests | Valid and invalid review-state transitions |
| API integration tests | Ingestion, batch ingestion, fraud flags, analyst review/audit, validation, idempotency and access restrictions |

To run the tests inside Docker instead:

```bash
docker build --target test -t fraud-rule-engine-tests .
```

## 12. Project structure

```text
fraud-rule-engine-service/
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── pom.xml
├── README.md
├── docs/
│   └── DESIGN.md
├── postman/
│   ├── Fraud-Rule-Engine.postman_collection.json
│   └── Fraud-Rule-Engine-Local.postman_environment.json
└── src/
    ├── main/java/com/example/fraud/
    │   ├── config/       # Security, OpenAPI, rule properties, request IDs, Clock
    │   ├── controller/   # HTTP endpoints and role-specific API surface
    │   ├── dto/          # Validated request/response records
    │   ├── entity/       # JPA entities and domain state transitions
    │   ├── exception/    # Domain exceptions and consistent API errors
    │   ├── repository/  # Spring Data repositories and flag filters
    │   ├── rule/         # FraudRule implementations and FraudEngine
    │   └── service/      # Transactional use cases
    ├── main/resources/
    │   ├── application*.properties
    │   └── db/           # Vendor-specific Flyway migrations and H2 seed data
    └── test/java/        # Rule unit tests and API integration tests
```

## 13. Configuration

The common settings live in `src/main/resources/application.properties`; database-specific settings live in `application-h2.properties`, `application-postgres.properties` and `application-mysql.properties`.

| Setting | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `h2` | Select `h2`, `postgres` or `mysql` |
| `DB_HOST` | `localhost` | Database hostname (Compose uses `db`) |
| `DB_PORT` | `5432` / `3306` | PostgreSQL / MySQL port |
| `DB_NAME` | `frauddb` | Database name |
| `DB_USERNAME` | `app` | Database username |
| `DB_PASSWORD` | none | Database password |
| `FRAUD_ADMIN_PASSWORD` | H2 dev default only | Admin service-account password |
| `FRAUD_ANALYST_PASSWORD` | H2 dev default only | Analyst service-account password |
| `FRAUD_INGESTION_PASSWORD` | H2 dev default only | Ingestion service-account password |
| `FRAUD_RULES_HIGH_AMOUNT_THRESHOLD` | `20000` | High-amount rule threshold (ZAR) |
| `FRAUD_RULES_VELOCITY_ENABLED` | `true` | Enable/disable velocity rule |

For example, to change the high-amount threshold in Docker, add `FRAUD_RULES_HIGH_AMOUNT_THRESHOLD=30000` to `.env` and restart the application container.

## 14. Troubleshooting

| Problem | What to check |
|---|---|
| Docker says Virtual Machine Platform is not enabled | Open PowerShell as Administrator, run `Enable-WindowsOptionalFeature -Online -FeatureName VirtualMachinePlatform -All`, then restart Windows. Ensure WSL 2 is installed and updated. |
| Docker daemon is unavailable | Start Docker Desktop and wait until it reports that the engine is running. |
| `mvn` is not recognised | Install Maven 3.9+ or use the Docker build, which runs Maven inside Docker. |
| `cannot find symbol ... method getId()` (or `getAmount()`, `getCustomer()` ...) | Lombok did not run. (1) Use the fixed `pom.xml` (it names Lombok as an annotation processor, needed on JDK 23+). (2) In IntelliJ: *Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Enable annotation processing*, make sure the **Lombok** plugin is enabled, then *Maven -> Reload project* and *Build -> Rebuild Project*. (3) Check `java -version` and `mvn -version` show the same JDK. |
| `release version 21 not supported` | Install JDK 21 and verify `java -version` and `JAVA_HOME`. |
| Missing `FRAUD_*_PASSWORD` placeholder | Set all three API account passwords in `.env` when using PostgreSQL/MySQL. |
| Database connection refused | Check `docker compose ps`, database health and `docker compose logs postgres`. |
| `401 Unauthorized` | Check username/password and Postman Basic Auth variables. |
| `403 Forbidden` | The account is valid but its role cannot access that operation. |
| `409 Conflict` | The transaction reference already exists, a customer email is duplicated, or the merchant is already active on the blacklist. |
| Port 8080 is already in use | Stop the other process or change `server.port` and the Compose port mapping together. |
| Flyway checksum mismatch | Do not edit an applied migration. Add a new versioned migration; for disposable local data, recreate the database volume. |

Useful Docker commands:

```bash
docker compose ps
docker compose logs -f fraud-engine
docker compose logs -f postgres
docker compose restart fraud-engine
docker compose down
```

## 15. Known limitations and next steps

- The three service accounts are in-memory and configured at startup; production should use OAuth2/OIDC and centrally managed identities.
- Fraud rules and thresholds are configuration-driven, not editable live through an admin UI. Restart the service after changing environment-based thresholds.
- Screening is synchronous. High-volume ingestion should use a durable queue (for example Kafka), idempotent consumers and an outbox/event strategy.
- The batch endpoint is limited to 100 and all-or-nothing; it is not a streaming upload endpoint.
- No automatic payment blocking occurs. The engine raises flags for investigation; downstream systems must decide whether to hold or decline transactions.
- Add Testcontainers coverage against real PostgreSQL, metrics/tracing, rate limiting, secrets management, TLS and operational alerting before production use.

See [`docs/DESIGN.md`](docs/DESIGN.md) for design decisions and scaling trade-offs.
