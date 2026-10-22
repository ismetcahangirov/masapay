-- Staff management by the restaurant manager (#145). A manager invites a waiter or chef by
-- email before they ever sign in, so such a row has no Google account yet; the first Google
-- sign-in with that email links it (UNIQUE still holds for the linked rows).
ALTER TABLE users ALTER COLUMN google_sub DROP NOT NULL;

-- Set when a manager switches a staff account off, cleared when it is approved again. It
-- tells "switched off" apart from "signed in, waiting for approval".
ALTER TABLE users ADD COLUMN deactivated_at TIMESTAMPTZ;
