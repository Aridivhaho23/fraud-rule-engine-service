-- Run with:
-- docker compose exec -T postgres psql -U app -d frauddb < scripts/verify-postgres.sql

SELECT current_database() AS database_name, current_user AS database_user;

SELECT 'customers' AS table_name, COUNT(*) AS row_count FROM customers
UNION ALL SELECT 'transactions', COUNT(*) FROM transactions
UNION ALL SELECT 'fraud_flags', COUNT(*) FROM fraud_flags
UNION ALL SELECT 'flag_status_history', COUNT(*) FROM flag_status_history
UNION ALL SELECT 'blacklist_merchants', COUNT(*) FROM blacklist_merchants
ORDER BY table_name;

SELECT version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;

-- Check whether every demo transaction is linked to a customer.
SELECT t.reference, t.customer_id, c.email
FROM transactions t
LEFT JOIN customers c ON c.id = t.customer_id
WHERE t.reference LIKE 'DEMO-TX-%'
  AND c.id IS NULL;

-- List the stable demo references and their generated database IDs.
SELECT id, reference, customer_id
FROM transactions
WHERE reference LIKE 'DEMO-TX-%'
ORDER BY reference;

-- Check for missing demo references without assuming generated IDs start at 1.
WITH expected(reference) AS (
    SELECT 'DEMO-TX-' || lpad(n::text, 3, '0')
    FROM generate_series(1, 20) AS n
)
SELECT e.reference AS missing_reference
FROM expected e
LEFT JOIN transactions t ON t.reference = e.reference
WHERE t.id IS NULL
ORDER BY e.reference;
