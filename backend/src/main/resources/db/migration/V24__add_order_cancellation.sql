-- Cancelling a confirmed order (#186): only a manager (or the super admin) may, with a reason.
-- Who and when are kept for the record. A cancelled order frees the table.
ALTER TABLE orders
  ADD COLUMN cancelled_at TIMESTAMPTZ,
  ADD COLUMN cancelled_by UUID REFERENCES users (id),
  ADD COLUMN cancel_reason VARCHAR(500);
