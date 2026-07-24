-- masapay core schema (issue #9 / sub-issue 2.2)
-- PostgreSQL 16. UUID primary keys, NUMERIC(12,2) monetary amounts, AZN default.
-- Status/type columns use VARCHAR + CHECK constraints so new values can be added
-- with a plain migration and map cleanly to JPA @Enumerated(EnumType.STRING) in #10.

-- Auto-maintain updated_at on every row update, independent of the write path
-- (JPA, Payriff webhook, or direct SQL).
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- restaurants -------------------------------------------------------------
CREATE TABLE restaurants (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    pos_type    VARCHAR(20)  NOT NULL DEFAULT 'CUSTOM'
                    CHECK (pos_type IN ('IIKO', 'MICROS', 'CUSTOM')),
    currency    CHAR(3)      NOT NULL DEFAULT 'AZN',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_restaurants_updated_at
    BEFORE UPDATE ON restaurants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- tables (physical restaurant tables) -------------------------------------
CREATE TABLE tables (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id  UUID         NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    label          VARCHAR(50)  NOT NULL,
    qr_token       UUID         NOT NULL DEFAULT gen_random_uuid(),
    status         VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                       CHECK (status IN ('AVAILABLE', 'OCCUPIED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_tables_qr_token          UNIQUE (qr_token),
    CONSTRAINT uq_tables_restaurant_label  UNIQUE (restaurant_id, label)
);

CREATE INDEX idx_tables_restaurant_id ON tables (restaurant_id);

CREATE TRIGGER trg_tables_updated_at
    BEFORE UPDATE ON tables
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- orders (adisyon) --------------------------------------------------------
CREATE TABLE orders (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id  UUID          NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    table_id       UUID          NOT NULL REFERENCES tables (id) ON DELETE RESTRICT,
    pos_order_id   VARCHAR(128),
    status         VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                       CHECK (status IN ('OPEN', 'CLOSED', 'PAID', 'CANCELLED')),
    total_amount   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    currency       CHAR(3)       NOT NULL DEFAULT 'AZN',
    opened_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    closed_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_restaurant_status ON orders (restaurant_id, status);
CREATE INDEX idx_orders_table_id          ON orders (table_id);

-- A given POS order maps to at most one masapay order per restaurant.
CREATE UNIQUE INDEX uq_orders_restaurant_pos_order
    ON orders (restaurant_id, pos_order_id)
    WHERE pos_order_id IS NOT NULL;

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- order_items -------------------------------------------------------------
CREATE TABLE order_items (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    pos_item_id  VARCHAR(128),
    name         VARCHAR(255)  NOT NULL,
    quantity     NUMERIC(10,3) NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price   NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    total_price  NUMERIC(12,2) NOT NULL CHECK (total_price >= 0),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TRIGGER trg_order_items_updated_at
    BEFORE UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- transactions (payments via Payriff) -------------------------------------
CREATE TABLE transactions (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id           UUID          NOT NULL REFERENCES orders (id) ON DELETE RESTRICT,
    restaurant_id      UUID          NOT NULL REFERENCES restaurants (id) ON DELETE RESTRICT,
    amount             NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    tip_amount         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tip_amount >= 0),
    total_amount       NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    refunded_amount    NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    currency           CHAR(3)       NOT NULL DEFAULT 'AZN',
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                           CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    split_type         VARCHAR(20)
                           CHECK (split_type IN ('FULL', 'ITEM', 'EQUAL')),
    payment_method     VARCHAR(30),
    payriff_order_id   VARCHAR(128),
    payriff_payment_id VARCHAR(128),
    paid_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_order_id           ON transactions (order_id);
CREATE INDEX idx_transactions_restaurant_status  ON transactions (restaurant_id, status);

-- Payriff order id is the idempotency handle for webhook processing (#35).
CREATE UNIQUE INDEX uq_transactions_payriff_order_id
    ON transactions (payriff_order_id)
    WHERE payriff_order_id IS NOT NULL;

CREATE TRIGGER trg_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
