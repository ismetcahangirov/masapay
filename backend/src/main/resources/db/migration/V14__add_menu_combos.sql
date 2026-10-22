-- Combos and business lunches (#138). A combo is an ordinary menu item (own name,
-- category and price) that also lists the dishes it is made of, so it can be
-- ordered like any other item. A dish that is part of a combo cannot be
-- deleted (RESTRICT); deleting the combo removes its component rows.

CREATE TABLE menu_combo_components (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    combo_id       UUID         NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    component_id   UUID         NOT NULL REFERENCES menu_items (id) ON DELETE RESTRICT,
    quantity       INTEGER      NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_menu_combo_components UNIQUE (combo_id, component_id),
    CONSTRAINT ck_menu_combo_not_self CHECK (combo_id <> component_id)
);

CREATE INDEX idx_menu_combo_components_component_id ON menu_combo_components (component_id);

CREATE TRIGGER trg_menu_combo_components_updated_at
    BEFORE UPDATE ON menu_combo_components
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
