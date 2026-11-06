-- DEMO DATA for the local PostgreSQL profile. These records make the API testable immediately.
-- Production deployments should replace this demo migration with controlled reference-data provisioning.
INSERT INTO customers (full_name, email, home_country, created_at) VALUES
    ('Thandi Nkosi', 'thandi.nkosi@example.com', 'ZA', TIMESTAMP '2026-01-15 08:00:00'),
    ('Pieter van Wyk', 'pieter.vanwyk@example.com', 'ZA', TIMESTAMP '2026-02-03 09:30:00');

INSERT INTO blacklist_merchants (merchant_key, merchant_name, reason, added_at) VALUES
    ('darkbet offshore', 'DarkBet Offshore', 'Demo: unlicensed gambling operator', TIMESTAMP '2026-03-01 10:00:00'),
    ('fastcoin exchange', 'FastCoin Exchange', 'Demo: suspected fraud cash-out merchant', TIMESTAMP '2026-03-05 11:00:00');
