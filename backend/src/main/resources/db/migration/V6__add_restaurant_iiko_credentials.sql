-- iiko Cloud API credentials per restaurant (issue #40).
-- Only restaurants with pos_type = 'IIKO' fill these; CUSTOM and MICROS
-- restaurants leave them NULL, so the columns are nullable.
ALTER TABLE restaurants
  ADD COLUMN iiko_api_login VARCHAR(255),
  ADD COLUMN iiko_organization_id UUID;
