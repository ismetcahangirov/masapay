-- iiko payment type that masapay payments are booked under (issue #41).
-- Set up once per restaurant in iiko (/api/1/payment_types); NULL for non-iiko
-- restaurants.
ALTER TABLE restaurants
  ADD COLUMN iiko_payment_type_id UUID;
