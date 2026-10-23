-- Encrypted restaurant secrets (#157): the Payriff secret key and the iiko API login are now
-- stored AES-256-GCM encrypted ("enc:<key id>:<base64>"), which is longer than the clear text.
-- Rows written before this migration are encrypted by RestaurantSecretsEncryptor at startup.
ALTER TABLE restaurants
  ALTER COLUMN iiko_api_login TYPE VARCHAR(1024),
  ALTER COLUMN payriff_secret_key TYPE VARCHAR(1024);
