-- Repair missing demo fixtures on databases where V3 was already recorded but data was
-- subsequently removed or only partially imported. This migration is intentionally additive:
-- it never overwrites existing customer, merchant, transaction, flag, or audit data.
-- Flyway runs this once; conflict guards also make the SQL safe if a subset already exists.

INSERT INTO customers (full_name, email, home_country, created_at) VALUES
    ('Thandi Nkosi', 'thandi.nkosi@example.com', 'ZA', TIMESTAMP '2026-01-15 08:00:00'),
    ('Pieter van Wyk', 'pieter.vanwyk@example.com', 'ZA', TIMESTAMP '2026-02-03 09:30:00'),
    ('Ari Nemadodzi', 'ari.nemadodzi@example.com', 'ZA', TIMESTAMP '2026-11-01 08:00:00'),
    ('Junior Mokoena', 'junior.mokoena@example.com', 'ZA', TIMESTAMP '2026-11-01 08:01:00'),
    ('Chuma Dlamini', 'chuma.dlamini@example.com', 'ZA', TIMESTAMP '2026-11-01 08:02:00'),
    ('Tshifhi Ramabulana', 'tshifhi.ramabulana@example.com', 'ZA', TIMESTAMP '2026-11-01 08:03:00'),
    ('Mmasello Mokoena', 'mmasello.mokoena@example.com', 'ZA', TIMESTAMP '2026-11-01 08:04:00'),
    ('Lucas Radebe', 'lucas.radebe@example.com', 'ZA', TIMESTAMP '2026-11-01 08:05:00'),
    ('Benni McCarthy', 'benni.mccarthy@example.com', 'ZA', TIMESTAMP '2026-11-01 08:06:00'),
    ('Steven Pienaar', 'steven.pienaar@example.com', 'ZA', TIMESTAMP '2026-11-01 08:07:00'),
    ('Siphiwe Tshabalala', 'siphiwe.tshabalala@example.com', 'ZA', TIMESTAMP '2026-11-01 08:08:00'),
    ('Percy Tau', 'percy.tau@example.com', 'ZA', TIMESTAMP '2026-11-01 08:09:00'),
    ('Thabo Molefe', 'thabo.molefe@example.com', 'ZA', TIMESTAMP '2026-11-01 08:10:00'),
    ('Lerato Khumalo', 'lerato.khumalo@example.com', 'ZA', TIMESTAMP '2026-11-01 08:11:00'),
    ('Naledi Baloyi', 'naledi.baloyi@example.com', 'ZA', TIMESTAMP '2026-11-01 08:12:00'),
    ('Kabelo Nkosi', 'kabelo.nkosi@example.com', 'ZA', TIMESTAMP '2026-11-01 08:13:00'),
    ('Zanele Mthembu', 'zanele.mthembu@example.com', 'ZA', TIMESTAMP '2026-11-01 08:14:00'),
    ('Sibusiso Dube', 'sibusiso.dube@example.com', 'ZA', TIMESTAMP '2026-11-01 08:15:00'),
    ('Refilwe Ndlovu', 'refilwe.ndlovu@example.com', 'ZA', TIMESTAMP '2026-11-01 08:16:00'),
    ('Kagiso Mokoena', 'kagiso.mokoena@example.com', 'ZA', TIMESTAMP '2026-11-01 08:17:00')
ON CONFLICT (email) DO NOTHING;

INSERT INTO blacklist_merchants (merchant_key, merchant_name, reason, added_at) VALUES
    ('darkbet offshore', 'DarkBet Offshore', 'Demo: unlicensed gambling operator', TIMESTAMP '2026-03-01 10:00:00'),
    ('fastcoin exchange', 'FastCoin Exchange', 'Demo: suspected fraud cash-out merchant', TIMESTAMP '2026-03-05 11:00:00'),
    ('quickcash online', 'QuickCash Online', 'Suspicious unregulated cash advance service', TIMESTAMP '2026-10-01 10:00:00'),
    ('luckygoals betting', 'LuckyGoals Betting', 'Demo: unlicensed sports betting operator', TIMESTAMP '2026-10-02 10:00:00'),
    ('cryptoflash exchange', 'CryptoFlash Exchange', 'Suspicious cryptocurrency cash-out service', TIMESTAMP '2026-10-03 10:00:00'),
    ('prizevault rewards', 'PrizeVault Rewards', 'Reported prize and advance-fee scam', TIMESTAMP '2026-10-04 10:00:00'),
    ('rapidfunds za', 'RapidFunds ZA', 'Suspicious loan and cash-out merchant', TIMESTAMP '2026-10-05 10:00:00'),
    ('offshorespin casino', 'OffshoreSpin Casino', 'Demo: unlicensed offshore gambling operator', TIMESTAMP '2026-10-06 10:00:00'),
    ('giftcard depot', 'GiftCard Depot', 'Reported gift-card fraud merchant', TIMESTAMP '2026-10-07 10:00:00'),
    ('easypay reloads', 'EasyPay Reloads', 'Suspicious prepaid-value reseller', TIMESTAMP '2026-10-08 10:00:00'),
    ('globalforex pro', 'GlobalForex Pro', 'Reported unauthorised investment scheme', TIMESTAMP '2026-10-09 10:00:00'),
    ('megajackpot club', 'MegaJackpot Club', 'Demo: unlicensed gambling operator', TIMESTAMP '2026-10-10 10:00:00'),
    ('fasttrack loans', 'FastTrack Loans', 'Suspicious loan-fee collection merchant', TIMESTAMP '2026-10-11 10:00:00'),
    ('digitalcoin hub', 'DigitalCoin Hub', 'Reported crypto fraud cash-out service', TIMESTAMP '2026-10-12 10:00:00'),
    ('superodds africa', 'SuperOdds Africa', 'Demo: unlicensed sports betting operator', TIMESTAMP '2026-10-13 10:00:00'),
    ('cashprize network', 'CashPrize Network', 'Reported advance-fee scheme', TIMESTAMP '2026-10-14 10:00:00'),
    ('flashvoucher store', 'FlashVoucher Store', 'Suspicious voucher resale activity', TIMESTAMP '2026-10-15 10:00:00'),
    ('betking offshore', 'BetKing Offshore', 'Demo: unlicensed offshore betting operator', TIMESTAMP '2026-10-16 10:00:00'),
    ('primereturns capital', 'PrimeReturns Capital', 'Reported investment scam merchant', TIMESTAMP '2026-10-17 10:00:00'),
    ('walletboost services', 'WalletBoost Services', 'Suspicious digital-wallet cash-out service', TIMESTAMP '2026-10-18 10:00:00')
ON CONFLICT (merchant_key) DO NOTHING;

-- Seed 20 transactions, linked by customer email so generated IDs remain portable.
INSERT INTO transactions (customer_id, reference, amount, merchant_name, merchant_category, country, occurred_at, created_at)
SELECT c.id, v.reference, v.amount, v.merchant_name, v.merchant_category, v.country, v.occurred_at, v.occurred_at
FROM (VALUES
    ('thandi.nkosi@example.com', 'DEMO-TX-001', 249.90, 'Corner Grocer', 'GROCERIES', 'ZA', TIMESTAMP '2026-11-01 08:00:00'),
    ('pieter.vanwyk@example.com', 'DEMO-TX-002', 850.00, 'Shell Rosebank', 'FUEL', 'ZA', TIMESTAMP '2026-11-02 09:03:00'),
    ('ari.nemadodzi@example.com', 'DEMO-TX-003', 60000.00, 'Gadget World', 'RETAIL', 'ZA', TIMESTAMP '2026-11-03 10:06:00'),
    ('junior.mokoena@example.com', 'DEMO-TX-004', 24500.00, 'City Electronics', 'RETAIL', 'ZA', TIMESTAMP '2026-11-04 11:09:00'),
    ('chuma.dlamini@example.com', 'DEMO-TX-005', 7500.00, 'DarkBet Offshore', 'GAMBLING', 'MT', TIMESTAMP '2026-11-01 12:12:00'),
    ('tshifhi.ramabulana@example.com', 'DEMO-TX-006', 12500.00, 'QuickCash Online', 'OTHER', 'ZA', TIMESTAMP '2026-11-02 13:15:00'),
    ('mmasello.mokoena@example.com', 'DEMO-TX-007', 1200.00, 'Fuel Stop', 'FUEL', 'ZA', TIMESTAMP '2026-11-03 14:18:00'),
    ('lucas.radebe@example.com', 'DEMO-TX-008', 18500.00, 'Travel Hub', 'TRAVEL', 'GB', TIMESTAMP '2026-11-04 15:21:00'),
    ('benni.mccarthy@example.com', 'DEMO-TX-009', 5500.00, 'LuckyGoals Betting', 'GAMBLING', 'ZA', TIMESTAMP '2026-11-01 16:24:00'),
    ('steven.pienaar@example.com', 'DEMO-TX-010', 399.99, 'Fresh Market', 'GROCERIES', 'ZA', TIMESTAMP '2026-11-02 17:27:00'),
    ('siphiwe.tshabalala@example.com', 'DEMO-TX-011', 9000.00, 'MegaJackpot Club', 'GAMBLING', 'ZA', TIMESTAMP '2026-11-03 08:30:00'),
    ('percy.tau@example.com', 'DEMO-TX-012', 22000.00, 'DigitalCoin Hub', 'OTHER', 'ZA', TIMESTAMP '2026-11-04 09:33:00'),
    ('thabo.molefe@example.com', 'DEMO-TX-013', 680.00, 'Restaurant 88', 'RESTAURANTS', 'ZA', TIMESTAMP '2026-11-01 10:36:00'),
    ('lerato.khumalo@example.com', 'DEMO-TX-014', 51000.00, 'GlobalForex Pro', 'OTHER', 'US', TIMESTAMP '2026-11-02 11:39:00'),
    ('naledi.baloyi@example.com', 'DEMO-TX-015', 350.00, 'Movie House', 'ENTERTAINMENT', 'ZA', TIMESTAMP '2026-11-03 12:42:00'),
    ('kabelo.nkosi@example.com', 'DEMO-TX-016', 12500.00, 'OffshoreSpin Casino', 'GAMBLING', 'MT', TIMESTAMP '2026-11-04 13:45:00'),
    ('zanele.mthembu@example.com', 'DEMO-TX-017', 950.00, 'Utility Payments', 'UTILITIES', 'ZA', TIMESTAMP '2026-11-01 14:48:00'),
    ('sibusiso.dube@example.com', 'DEMO-TX-018', 7800.00, 'GiftCard Depot', 'RETAIL', 'ZA', TIMESTAMP '2026-11-02 15:51:00'),
    ('refilwe.ndlovu@example.com', 'DEMO-TX-019', 1499.00, 'Supermarket Central', 'GROCERIES', 'ZA', TIMESTAMP '2026-11-03 16:54:00'),
    ('kagiso.mokoena@example.com', 'DEMO-TX-020', 30000.00, 'FastCoin Exchange', 'OTHER', 'ZA', TIMESTAMP '2026-11-04 17:57:00')
) AS v(email, reference, amount, merchant_name, merchant_category, country, occurred_at)
JOIN customers c ON c.email = v.email
ON CONFLICT (reference) DO NOTHING;


INSERT INTO fraud_flags (transaction_id, rule_code, severity, risk_score, reason, status, flagged_at, reviewed_by, reviewed_at, review_note, version)
SELECT t.id, v.rule_code, v.severity, v.risk_score, v.reason, 'UNDER_REVIEW', t.occurred_at, 'analyst', TIMESTAMP '2026-11-04 12:00:00', 'Demo seed: awaiting analyst decision', 0
FROM (VALUES
    ('DEMO-TX-001', 'NIGHT_TRANSACTION', 'MEDIUM', 55, 'Demo seed: unusual night-time transaction'),
    ('DEMO-TX-002', 'BLACKLIST_MERCHANT', 'CRITICAL', 100, 'Demo seed: merchant appears on blacklist'),
    ('DEMO-TX-003', 'HIGH_AMOUNT', 'CRITICAL', 98, 'Demo seed: amount exceeds critical threshold'),
    ('DEMO-TX-004', 'HIGH_AMOUNT', 'HIGH', 85, 'Demo seed: amount exceeds high threshold'),
    ('DEMO-TX-005', 'GAMBLING', 'HIGH', 82, 'Demo seed: high-value gambling transaction'),
    ('DEMO-TX-006', 'BLACKLIST_MERCHANT', 'CRITICAL', 100, 'Demo seed: blacklisted merchant match'),
    ('DEMO-TX-007', 'VELOCITY', 'HIGH', 78, 'Demo seed: unusual transaction velocity'),
    ('DEMO-TX-008', 'GEO_CONFLICT', 'HIGH', 88, 'Demo seed: cross-border geography conflict'),
    ('DEMO-TX-009', 'GAMBLING', 'HIGH', 80, 'Demo seed: gambling activity requires review'),
    ('DEMO-TX-010', 'CARD_TESTING', 'HIGH', 75, 'Demo seed: repeated low-value card attempts'),
    ('DEMO-TX-011', 'GAMBLING', 'HIGH', 86, 'Demo seed: repeated gambling activity'),
    ('DEMO-TX-012', 'HIGH_AMOUNT', 'HIGH', 90, 'Demo seed: high-value transaction'),
    ('DEMO-TX-013', 'NIGHT_TRANSACTION', 'MEDIUM', 60, 'Demo seed: unusual night-time transaction'),
    ('DEMO-TX-014', 'HIGH_AMOUNT', 'CRITICAL', 99, 'Demo seed: amount exceeds critical threshold'),
    ('DEMO-TX-015', 'CARD_TESTING', 'HIGH', 74, 'Demo seed: repeated low-value card attempts'),
    ('DEMO-TX-016', 'BLACKLIST_MERCHANT', 'CRITICAL', 100, 'Demo seed: blacklisted merchant match'),
    ('DEMO-TX-017', 'NIGHT_TRANSACTION', 'MEDIUM', 58, 'Demo seed: unusual night-time transaction'),
    ('DEMO-TX-018', 'BLACKLIST_MERCHANT', 'CRITICAL', 100, 'Demo seed: blacklisted merchant match'),
    ('DEMO-TX-019', 'VELOCITY', 'HIGH', 79, 'Demo seed: unusual transaction velocity'),
    ('DEMO-TX-020', 'HIGH_AMOUNT', 'HIGH', 92, 'Demo seed: high-value transaction')
) AS v(reference, rule_code, severity, risk_score, reason)
JOIN transactions t ON t.reference = v.reference
ON CONFLICT (transaction_id, rule_code) DO NOTHING;

-- Using Join to insert into flag_status_history for all demo records that are currently UNDER_REVIEW, simulating an analyst review action.
INSERT INTO flag_status_history (flag_id, from_status, to_status, changed_by, changed_at, note)
SELECT f.id, 'OPEN', 'UNDER_REVIEW', 'analyst', TIMESTAMP '2026-11-04 12:00:00', 'Demo seed: analyst review started'
FROM fraud_flags f
JOIN transactions t ON t.id = f.transaction_id
WHERE t.reference LIKE 'DEMO-TX-%'
  AND NOT EXISTS (
      SELECT 1 FROM flag_status_history h
      WHERE h.flag_id = f.id
        AND h.from_status = 'OPEN'
        AND h.to_status = 'UNDER_REVIEW'
        AND h.changed_by = 'analyst'
        AND h.changed_at = TIMESTAMP '2026-11-04 12:00:00'
        AND h.note = 'Demo seed: analyst review started'
  );
