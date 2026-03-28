-- V3__add_indexes_and_constraints.sql
-- Add indexes for frequently filtered columns and constraints for data integrity.

-- Composite index used by the soft-delete + filter queries (deleted = false AND title LIKE ?)
CREATE INDEX idx_events_deleted_title    ON events (deleted, title);

-- Composite index used by the soft-delete + location filter (deleted = false AND location LIKE ?)
CREATE INDEX idx_events_deleted_location ON events (deleted, location);

-- Index to speed up pagination queries ordered by created_at
CREATE INDEX idx_events_created_at       ON events (created_at);
