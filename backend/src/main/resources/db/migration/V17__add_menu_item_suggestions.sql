-- Upsell (#141): the extras offered with a dish before the customer confirms, e.g. a sauce
-- and a drink with a kebab, in the manager's order. Deleting either dish drops the pairing.
-- The list is stored by position, so reordering updates rows in place; the "no duplicate"
-- rule is checked at commit (DEFERRABLE) so swapping two entries does not trip it halfway.
CREATE TABLE menu_item_suggestions (
    item_id       UUID     NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    suggested_id  UUID     NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    sort_order    INTEGER  NOT NULL,
    PRIMARY KEY (item_id, sort_order),
    CONSTRAINT uq_menu_item_suggestions UNIQUE (item_id, suggested_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_menu_item_suggestions_not_self CHECK (item_id <> suggested_id)
);

CREATE INDEX idx_menu_item_suggestions_suggested_id ON menu_item_suggestions (suggested_id);
