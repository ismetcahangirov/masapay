-- Kitchen (#142): a CHEF role, and where each ordered dish is in the kitchen.

ALTER TABLE users DROP CONSTRAINT users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
  CHECK (role IN ('SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER', 'CHEF'));

-- NEW when a customer orders it from the menu, then PREPARING and READY as the chef marks
-- it. NULL for lines that never go to the kitchen (typed in by staff or synced from a POS).
ALTER TABLE order_items
  ADD COLUMN kitchen_status VARCHAR(20)
    CHECK (kitchen_status IN ('NEW', 'PREPARING', 'READY'));

-- The kitchen screen lists the open work of one restaurant, oldest first.
CREATE INDEX idx_order_items_kitchen_open ON order_items (kitchen_status, created_at)
  WHERE kitchen_status IS NOT NULL;
