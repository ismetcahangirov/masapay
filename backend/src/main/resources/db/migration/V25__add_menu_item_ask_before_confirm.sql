-- Extras asked about before the customer confirms (#185), KFC style: "Ketçup və ya su
-- istəyirsiniz?". The manager marks a few dishes (sauces, water) and every customer is asked
-- about them once, before "Əminsiniz?".
ALTER TABLE menu_items
  ADD COLUMN ask_before_confirm BOOLEAN NOT NULL DEFAULT FALSE;
