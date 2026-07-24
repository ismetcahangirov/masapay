-- Per-item payment state for the item-based split (issue #25 / sub-issue 5.2).
-- Lets a diner pay only for the items they consumed, and marks items paid so the
-- same item cannot be paid twice (race protection is an atomic conditional update).

ALTER TABLE order_items
    ADD COLUMN paid                BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN paid_transaction_id UUID REFERENCES transactions (id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_order_paid ON order_items (order_id, paid);
