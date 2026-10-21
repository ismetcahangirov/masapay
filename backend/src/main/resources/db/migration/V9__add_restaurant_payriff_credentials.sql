-- Per-restaurant Payriff merchant credentials (issue #46).
-- Nullable: not every restaurant has its own Payriff account yet; those keep
-- using the platform-wide credentials from configuration.
ALTER TABLE restaurants
  ADD COLUMN payriff_merchant_id VARCHAR(128),
  ADD COLUMN payriff_secret_key VARCHAR(512);
