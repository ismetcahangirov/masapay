-- The main ingredients of a dish ("toyuq, düyü, zəfəran"), shown to customers next to the photo
-- so they know what the dish is. Kept apart from the free-text description.
ALTER TABLE menu_items
  ADD COLUMN ingredients VARCHAR(300);
