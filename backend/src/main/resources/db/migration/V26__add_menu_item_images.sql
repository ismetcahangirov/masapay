-- Dish photos: the manager adds one per dish and customers see it on the menu. The bytes live
-- in their own table so loading the menu never reads them; image_version (0 = no photo) goes
-- into the photo URL, so browsers may cache a photo forever and a new one gets a new URL.
ALTER TABLE menu_items
  ADD COLUMN image_version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE menu_item_images (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id       UUID          NOT NULL UNIQUE REFERENCES menu_items (id) ON DELETE CASCADE,
    content_type  VARCHAR(30)   NOT NULL,
    data          BYTEA         NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_menu_item_images_updated_at
    BEFORE UPDATE ON menu_item_images
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
