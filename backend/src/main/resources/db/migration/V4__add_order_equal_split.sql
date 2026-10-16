-- Equal-split state for the "Bərabər Böl" mode (issue #26 / sub-issue 5.3).
-- The first equal payer fixes how many parts the bill is split into and the
-- amount being split; later payers claim shares of that same split, so every
-- diner pays the same share regardless of when they pay. paid parts are
-- claimed with an atomic conditional update (race protection).

ALTER TABLE orders
    ADD COLUMN equal_split_parts      INTEGER,
    ADD COLUMN equal_split_base       NUMERIC(12, 2),
    ADD COLUMN equal_split_paid_parts INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_orders_equal_split CHECK (
        (equal_split_parts IS NULL AND equal_split_base IS NULL AND equal_split_paid_parts = 0)
        OR (equal_split_parts >= 2 AND equal_split_base >= 0
            AND equal_split_paid_parts BETWEEN 0 AND equal_split_parts)
    );
