-- Mock data for local development and manual API checks.
-- Requires the Flyway schema: start the backend once before running this.
--
--   psql -U masapay -h localhost -d masapay -f scripts/db/2-seed-mock-data.sql
--
-- Fixed ids and QR tokens make the data easy to call from the API, e.g.
--   GET http://localhost:8080/api/public/tables/11111111-0000-0000-0000-000000000001/order
-- Safe to re-run: existing rows are left as they are (ON CONFLICT DO NOTHING).
--
-- QR tokens (tables.qr_token):
--   Dolma Restoran (IIKO)
--     Masa 1  11111111-0000-0000-0000-000000000001  OPEN order, 4 items        -> full / item / equal split
--     Masa 2  11111111-0000-0000-0000-000000000002  OPEN order + service charge -> item split with guests
--     Masa 3  11111111-0000-0000-0000-000000000003  OPEN order, 1 item already paid
--     Masa 4  11111111-0000-0000-0000-000000000004  no open order ("order": null)
--   Bulvar Cafe (CUSTOM)
--     B1      22222222-0000-0000-0000-000000000001  OPEN order, 2 items
--     B2      22222222-0000-0000-0000-000000000002  last order PAID (history only)

BEGIN;

-- restaurants -------------------------------------------------------------
INSERT INTO restaurants (id, name, pos_type, currency, active) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Dolma Restoran', 'IIKO',   'AZN', TRUE),
    ('a0000000-0000-0000-0000-000000000002', 'Bulvar Cafe',    'CUSTOM', 'AZN', TRUE)
ON CONFLICT (id) DO NOTHING;

-- tables ------------------------------------------------------------------
INSERT INTO tables (id, restaurant_id, label, qr_token, status) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Masa 1', '11111111-0000-0000-0000-000000000001', 'OCCUPIED'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Masa 2', '11111111-0000-0000-0000-000000000002', 'OCCUPIED'),
    ('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Masa 3', '11111111-0000-0000-0000-000000000003', 'OCCUPIED'),
    ('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Masa 4', '11111111-0000-0000-0000-000000000004', 'AVAILABLE'),
    ('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 'B1',     '22222222-0000-0000-0000-000000000001', 'OCCUPIED'),
    ('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000002', 'B2',     '22222222-0000-0000-0000-000000000002', 'AVAILABLE')
ON CONFLICT (id) DO NOTHING;

-- orders ------------------------------------------------------------------
-- total_amount = sum of items, except Masa 2 which adds a 10% service charge.
INSERT INTO orders (id, restaurant_id, table_id, pos_order_id, status, total_amount, currency, opened_at, closed_at) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'IIKO-1001', 'OPEN', 61.00, 'AZN', now() - interval '50 minutes', NULL),
    ('c0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000002', 'IIKO-1002', 'OPEN', 49.50, 'AZN', now() - interval '35 minutes', NULL),
    ('c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000003', 'IIKO-1003', 'OPEN', 27.00, 'AZN', now() - interval '20 minutes', NULL),
    ('c0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000005', NULL,        'OPEN', 14.50, 'AZN', now() - interval '10 minutes', NULL),
    ('c0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000006', NULL,        'PAID', 18.00, 'AZN', now() - interval '2 hours',    now() - interval '90 minutes')
ON CONFLICT (id) DO NOTHING;

-- transactions ------------------------------------------------------------
-- Inserted before order_items because order_items.paid_transaction_id refers to them.
INSERT INTO transactions (id, order_id, restaurant_id, amount, tip_amount, total_amount, currency, status, split_type, payment_method, paid_at) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',  9.00, 0.00,  9.00, 'AZN', 'SUCCESS', 'ITEM', 'CARD', now() - interval '5 minutes'),
    ('d0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000002', 18.00, 2.00, 20.00, 'AZN', 'SUCCESS', 'FULL', 'CARD', now() - interval '90 minutes')
ON CONFLICT (id) DO NOTHING;

-- order_items -------------------------------------------------------------
INSERT INTO order_items (id, order_id, pos_item_id, name, quantity, unit_price, total_price, paid, paid_transaction_id) VALUES
    -- Masa 1: 61.00
    ('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'P-101', 'Yarpaq dolması', 2, 12.00, 24.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'P-102', 'Toyuq kababı',   1, 15.00, 15.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'P-103', 'Çoban salatı',   1,  7.00,  7.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'P-104', 'Ayran',          3,  5.00, 15.00, FALSE, NULL),
    -- Masa 2: items 45.00 + 4.50 service charge = 49.50
    ('e0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000002', 'P-201', 'Plov',           1, 18.00, 18.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'P-202', 'Qutab',          3,  4.00, 12.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', 'P-203', 'Çay dəsti',      1, 15.00, 15.00, FALSE, NULL),
    -- Masa 3: 27.00, first item already paid by transaction d...01
    ('e0000000-0000-0000-0000-000000000008', 'c0000000-0000-0000-0000-000000000003', 'P-301', 'Mərci şorbası',  1,  9.00,  9.00, TRUE,  'd0000000-0000-0000-0000-000000000001'),
    ('e0000000-0000-0000-0000-000000000009', 'c0000000-0000-0000-0000-000000000003', 'P-302', 'Lülə kabab',     1, 18.00, 18.00, FALSE, NULL),
    -- B1: 14.50
    ('e0000000-0000-0000-0000-000000000010', 'c0000000-0000-0000-0000-000000000004', NULL,    'Kapuçino',       2,  4.50,  9.00, FALSE, NULL),
    ('e0000000-0000-0000-0000-000000000011', 'c0000000-0000-0000-0000-000000000004', NULL,    'Paxlava',        1,  5.50,  5.50, FALSE, NULL),
    -- B2 (PAID): 18.00
    ('e0000000-0000-0000-0000-000000000012', 'c0000000-0000-0000-0000-000000000005', NULL,    'Səhər yeməyi',   2,  9.00, 18.00, TRUE,  'd0000000-0000-0000-0000-000000000002')
ON CONFLICT (id) DO NOTHING;

-- users -------------------------------------------------------------------
-- Placeholder Google accounts; real logins provision their own rows.
INSERT INTO users (id, google_sub, email, name, role, restaurant_id, enabled) VALUES
    ('f0000000-0000-0000-0000-000000000001', 'mock-google-sub-admin',   'admin@masapay.local',   'Mock Super Admin', 'SUPER_ADMIN',   NULL,                                   TRUE),
    ('f0000000-0000-0000-0000-000000000002', 'mock-google-sub-manager', 'manager@dolma.local',   'Mock Manager',     'RESTO_MANAGER', 'a0000000-0000-0000-0000-000000000001', TRUE),
    ('f0000000-0000-0000-0000-000000000003', 'mock-google-sub-waiter',  'waiter@dolma.local',    'Mock Waiter',      'WAITER',        'a0000000-0000-0000-0000-000000000001', TRUE),
    ('f0000000-0000-0000-0000-000000000004', 'mock-google-sub-pending', 'pending@bulvar.local',  'Mock Pending',     NULL,            NULL,                                   FALSE)
ON CONFLICT (id) DO NOTHING;

COMMIT;
