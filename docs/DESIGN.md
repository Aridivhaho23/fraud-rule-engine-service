# Design decisions and trade-offs (interview talking points)

| # | Decision | Why | Trade-off / what I would do next |
|---|----------|-----|----------------------------------|
| 1 | **`FraudRule` interface + one `@Component` per rule; `FraudEngine` injects `List<FraudRule>`** | Strategy pattern + open/closed principle: a new rule is a new class, no existing code changes; each rule is unit-testable alone | Rule order is not guaranteed; if rules ever depend on each other add `@Order` or a pipeline |
| 2 | **Thresholds in typed `@ConfigurationProperties`** | Operations can tune rules per environment without code changes; validated at start-up | Changing needs a restart. Next: store rules in the DB with an admin UI and cache them |
| 3 | **Store the transaction BEFORE running the rules** | Rules such as velocity count the customer's history including the current transaction | The ingestion is one DB transaction, so a failure rolls both back |
| 4 | **Synchronous screening in the request** | Simple, immediate verdict, easy to reason about and test | Latency grows with rule count. Next: Kafka consumer + async flags, outbox for events |
| 5 | **A failing rule fails the request (fail loudly)** | A silently skipped rule is an invisible hole in fraud coverage | Could instead catch, record "rule error" and route to manual review |
| 6 | **`fraud_flags` unique on (transaction, rule)** | Idempotency: re-processing cannot create duplicate flags | |
| 7 | **Unique `reference` on transactions** | Safe retries from the sender; duplicate -> 409 | Could return the original result instead of 409 |
| 8 | **Flyway owns the schema, Hibernate `validate`** | Reviewable, versioned, repeatable changes; app refuses to start if code and DB disagree | Needs discipline: never edit an applied migration |
| 9 | **Per-vendor migration folders + Spring profiles (PostgreSQL target, MySQL/H2 fallbacks)** | Same Java code on three databases; fast local runs on H2 | Three SQL copies to maintain; Testcontainers would verify them |
| 10 | **Enums stored through converters (not `@Enumerated`)** | Identical VARCHAR mapping on every database, no native ENUM surprises | A little more code |
| 11 | **UTC everywhere; local time converted only inside the night rule** | No daylight-saving or server-zone bugs | Customer-specific time zones would need a column |
| 12 | **`BigDecimal` / `DECIMAL(19,2)`, ZAR only** | No rounding errors in money | Multi-currency needs conversion before rules |
| 13 | **Roles INGESTION / ANALYST / ADMIN, deny-by-default** | Least privilege: the sender cannot read flags, analysts cannot create data | In-memory Basic auth is a stand-in for OAuth2/OIDC |
| 14 | **Reviewer name from the security context; append-only audit table** | Cannot be forged; full history of who decided what and when | Add tamper-evidence (hash chain) for regulated audits |
| 15 | **Optimistic locking (`@Version`) on flags** | Two analysts cannot silently overwrite each other (409) | Client must retry |
| 16 | **DTO records + one global error format** | Stable API contract, consistent errors, no entity leakage | Mapping code to maintain |
| 17 | **Request-id filter + structured JSON logs + health probes** | Traceability across log lines; ready for Kubernetes/log aggregation | Add metrics export (Prometheus) and tracing |
| 18 | **Multi-stage Dockerfile, non-root, graceful shutdown** | Small, safer image; no dropped requests on deploy | |
| 19 | **Atomic batch ingestion capped at 100 items** | Reduces upstream HTTP overhead while keeping retries and partial-success handling unambiguous; duplicate references reject the whole batch | Large-scale ingestion should move to a durable queue with per-message idempotency |
| 20 | **Admin-only blacklist history endpoint** | Soft-deleted intelligence remains auditable without exposing historical merchant intelligence to every analyst | Add immutable audit events and stricter retention controls for regulated deployments |

## How the system scales (future roadmap)
1. **Stateless service** -> run N replicas behind a load balancer (state is only in the database).
2. **Kafka** between the aggregation system and this service; consumers scale per partition (key = customerId keeps one
   customer's events ordered, which velocity/geography rules need).
3. **Caching** of blacklist and thresholds (Caffeine locally, Redis shared).
4. **Read replicas / partitioning** of `transactions` by month; the `(customer_id, occurred_at)` index serves the rule queries.
5. **Observability**: metrics (flags per rule, rule latency), dashboards, alerting on spikes of CRITICAL flags.
