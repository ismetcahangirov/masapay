-- Service-charge split for the item-based mode (issue #25 / sub-issue 5.2).
-- When the order total is more than its line items (e.g. a service charge), the
-- extra is shared equally between the guests at the table: each item payer pays
-- their items plus one share. The first item payer fixes the guest count.

ALTER TABLE orders
    ADD COLUMN service_charge_guests      INTEGER,
    ADD COLUMN service_charge_paid_shares INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_orders_service_charge_split CHECK (
        (service_charge_guests IS NULL AND service_charge_paid_shares = 0)
        OR (service_charge_guests >= 1
            AND service_charge_paid_shares BETWEEN 0 AND service_charge_guests)
    );
