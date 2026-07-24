-- Application users authenticated via Google (issue #14 / sub-issue 3.1).
-- Roles (SUPER_ADMIN, RESTO_MANAGER, WAITER) are enforced in EPIC 3 (#16). A
-- newly provisioned Google user starts disabled with no role until an admin
-- activates and assigns them.

CREATE TABLE users (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub     VARCHAR(128)  NOT NULL,
    email          VARCHAR(320)  NOT NULL,
    name           VARCHAR(255),
    picture_url    VARCHAR(1024),
    role           VARCHAR(20)
                       CHECK (role IN ('SUPER_ADMIN', 'RESTO_MANAGER', 'WAITER')),
    restaurant_id  UUID          REFERENCES restaurants (id) ON DELETE SET NULL,
    enabled        BOOLEAN       NOT NULL DEFAULT FALSE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub),
    CONSTRAINT uq_users_email      UNIQUE (email)
);

CREATE INDEX idx_users_restaurant_id ON users (restaurant_id);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
