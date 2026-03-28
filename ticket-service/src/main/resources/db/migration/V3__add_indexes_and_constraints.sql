-- V3__add_indexes_and_constraints.sql
-- Add indexes for frequently filtered columns and constraints for data integrity.

-- Index used by filter queries on event_id
CREATE INDEX idx_tickets_event_id        ON tickets (event_id);

-- Index used by filter queries on user_id
CREATE INDEX idx_tickets_user_id         ON tickets (user_id);

-- Index used by filter queries on status
CREATE INDEX idx_tickets_status          ON tickets (status);

-- Composite index for the most common combined filter (event_id + user_id)
CREATE INDEX idx_tickets_event_user      ON tickets (event_id, user_id);

-- Composite index for soft-delete + status queries (deleted = false AND status = ?)
CREATE INDEX idx_tickets_deleted_status  ON tickets (deleted, status);

-- Index to speed up pagination queries ordered by created_at
CREATE INDEX idx_tickets_created_at      ON tickets (created_at);

-- Constraint: status must be one of the valid enum values
ALTER TABLE tickets
    ADD CONSTRAINT chk_tickets_status CHECK (status IN ('CONFIRMED', 'CANCELLED'));
