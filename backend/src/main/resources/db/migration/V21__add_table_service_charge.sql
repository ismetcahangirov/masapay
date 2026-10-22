-- Per-table service charge (#146), e.g. 15% in the VIP room or none on the terrace.
--   NULL  the table follows the restaurant's service charge
--   0     no service charge at this table, whatever the restaurant charges
--   > 0   this table's own percentage
ALTER TABLE tables
  ADD COLUMN service_charge_percent NUMERIC(5,2)
    CHECK (service_charge_percent >= 0 AND service_charge_percent <= 100);
