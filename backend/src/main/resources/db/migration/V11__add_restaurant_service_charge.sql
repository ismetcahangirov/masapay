-- Restaurant-wide service charge (xidmət haqqı) as a percentage of the items.
-- NULL means the restaurant does not charge one; managers switch it on/off in the
-- admin panel. Applied to the bill total whenever an item is added.
ALTER TABLE restaurants
  ADD COLUMN service_charge_percent NUMERIC(5,2)
    CHECK (service_charge_percent > 0 AND service_charge_percent <= 100);
