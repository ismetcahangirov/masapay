-- Customer ordering from the menu (#140): which menu dish a bill line came from. The line
-- keeps its own name and prices, so the bill stays correct after the dish is renamed,
-- repriced or deleted (then the link is simply cleared). Staff-typed lines have no link.
ALTER TABLE order_items
  ADD COLUMN menu_item_id UUID REFERENCES menu_items (id) ON DELETE SET NULL;

CREATE INDEX idx_order_items_menu_item_id ON order_items (menu_item_id);
