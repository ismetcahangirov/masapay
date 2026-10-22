-- Manager view of a table's bill (#144): which staff member added a line. NULL when a
-- customer ordered it from the menu or it came from a POS sync. Keeping the line when the
-- staff account is removed (SET NULL) keeps the bill intact.
ALTER TABLE order_items
  ADD COLUMN added_by UUID REFERENCES users (id) ON DELETE SET NULL;
