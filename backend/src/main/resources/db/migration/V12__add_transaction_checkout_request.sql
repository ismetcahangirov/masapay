-- What a pending Payriff payment is for (issue #37). The split is applied only when Payriff
-- confirms the payment, so the request is stored with the PENDING transaction:
--   ITEM  -> checkout_item_ids (comma-separated order_items ids) and checkout_guests
--   EQUAL -> checkout_parts and checkout_parts_to_pay
--   FULL  -> nothing extra.
ALTER TABLE transactions
  ADD COLUMN checkout_item_ids     TEXT,
  ADD COLUMN checkout_guests       INTEGER,
  ADD COLUMN checkout_parts        INTEGER,
  ADD COLUMN checkout_parts_to_pay INTEGER;
