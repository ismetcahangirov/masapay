-- Payriff checkout session of a transaction (issue #33). Payriff returns it with the order id
-- when the payment page is created; NULL for transactions settled outside Payriff.
ALTER TABLE transactions
  ADD COLUMN payriff_session_id VARCHAR(128);
