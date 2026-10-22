-- Subscriptions (#148): a restaurant's current plan with the platform. Set and extended by the
-- super admin. NULL plan means none was ever set (restaurants from before subscriptions), which
-- blocks nothing. Once the end date has passed (Asia/Baku), customers can no longer start
-- payments. monthly_fee is what the restaurant pays, for platform revenue (#149).
ALTER TABLE restaurants
  ADD COLUMN subscription_plan         VARCHAR(20) CHECK (subscription_plan IN ('BASIC', 'PRO')),
  ADD COLUMN subscription_trial        BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN subscription_starts_on    DATE,
  ADD COLUMN subscription_ends_on      DATE,
  ADD COLUMN subscription_monthly_fee  NUMERIC(10,2) CHECK (subscription_monthly_fee >= 0),
  ADD CONSTRAINT ck_restaurants_subscription_dates
    CHECK (subscription_plan IS NULL
      OR (subscription_starts_on IS NOT NULL AND subscription_ends_on IS NOT NULL
          AND subscription_ends_on >= subscription_starts_on AND subscription_monthly_fee IS NOT NULL));
