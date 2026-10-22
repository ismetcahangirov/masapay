-- Restaurant menu (#137): categories in the manager's order, and the dishes in them.
-- A category with dishes cannot be dropped by accident (RESTRICT); the service
-- answers 409 instead. Deleting a restaurant removes its whole menu.

-- menu_categories -----------------------------------------------------------
CREATE TABLE menu_categories (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id  UUID          NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    name           VARCHAR(100)  NOT NULL,
    sort_order     INTEGER       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_menu_categories_restaurant_name UNIQUE (restaurant_id, name)
);

CREATE INDEX idx_menu_categories_restaurant_id ON menu_categories (restaurant_id);

CREATE TRIGGER trg_menu_categories_updated_at
    BEFORE UPDATE ON menu_categories
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- menu_items ----------------------------------------------------------------
CREATE TABLE menu_items (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id  UUID           NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    category_id    UUID           NOT NULL REFERENCES menu_categories (id) ON DELETE RESTRICT,
    name           VARCHAR(150)   NOT NULL,
    description    VARCHAR(500),
    price          NUMERIC(10,2)  NOT NULL CHECK (price >= 0),
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_menu_items_category_id ON menu_items (category_id);
CREATE INDEX idx_menu_items_restaurant_id ON menu_items (restaurant_id);

CREATE TRIGGER trg_menu_items_updated_at
    BEFORE UPDATE ON menu_items
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
