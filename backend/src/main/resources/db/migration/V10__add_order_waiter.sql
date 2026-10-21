-- Waiter serving an order (issue #48), used to attribute tips per waiter.
-- Nullable: POS-synced orders may arrive without a known waiter, and deleting
-- a staff account must not delete its orders (the waiter is cleared instead).
ALTER TABLE orders
  ADD COLUMN waiter_id UUID REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_orders_waiter_id ON orders (waiter_id);
