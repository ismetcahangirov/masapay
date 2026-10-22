-- Daily menu (#139): when a dish can be ordered. Days are Asia/Baku calendar days.
--   active = false            switched off until the manager switches it back on
--   off_on = <day>            off for that one day only (e.g. the chef decided so today)
--   available_from / _until   only on these days, both ends included; NULL = open-ended
ALTER TABLE menu_items
  ADD COLUMN active          BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN off_on          DATE,
  ADD COLUMN available_from  DATE,
  ADD COLUMN available_until DATE,
  ADD CONSTRAINT ck_menu_items_available_range
    CHECK (available_from IS NULL OR available_until IS NULL OR available_from <= available_until);
