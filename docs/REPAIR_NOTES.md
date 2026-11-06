# Audit and repair notes

## Scope reviewed in this pass

- Docker Compose, Dockerfile, environment templates, ignore rules
- Spring Boot profile and PostgreSQL configuration
- PostgreSQL schema and V1–V3 migration strategy
- Transaction controller, service, repository, DTO and integration-test coverage
- README endpoint and migration documentation
- Postman collection/environment JSON syntax

## Changes made

1. Added PostgreSQL Flyway migration `V4__repair_missing_demo_records.sql`. It is additive and conflict-safe: it restores missing demo customers, merchants, transactions, flags and seed audit-history entries without deleting or overwriting existing rows. V1–V3 remain unchanged to preserve Flyway checksums.
2. Added transaction lookup by stable external reference: `GET /api/v1/transactions/reference/{reference}`. Numeric database IDs are generated implementation details and should not be guessed or assumed to be `1`.
3. Added an integration test for transaction lookup by reference.
4. Updated the README with the reference lookup and V4 migration behavior.
5. Removed the uploaded `.env` file from the distributable project because it contained live credentials and a duplicate `DB_PASSWORD` key. Keep secrets in a local, unshared `.env` created from `.env.example`; rotate credentials that were previously shared.
6. Added `scripts/verify-postgres.sql` for checking the active database, Flyway history, record counts, orphaned transaction-customer relationships and missing demo references.
7. Extended `.gitignore` to exclude environment-specific secret files while allowing `.env.example`.

## Verification status

- Postman collection and environment JSON parse successfully.
- `pom.xml` parses as valid XML.
- Static source review completed for the files listed above.
- Full Maven tests and Docker image build were **not run in this environment**: Maven and the Docker CLI are not installed here. Run `docker compose --profile postgres up --build -d` and inspect the application logs on the target machine, then run the test stage with `docker build --target test -t fraud-rule-engine-tests .`.
- The V4 migration has not been executed against the user's live PostgreSQL volume from this environment. It will be applied automatically by Flyway when the updated application starts against that database.

## Important safety note

Do not run `docker compose down -v` for this repair. The new migration is designed to preserve the existing PostgreSQL volume and user data.
