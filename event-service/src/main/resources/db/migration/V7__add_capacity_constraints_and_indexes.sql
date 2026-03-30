-- -------------------------------------------------------
-- V7: Add capacity integrity constraints and missing indexes.
--
-- Constraints:
--   - available_tickets cannot go below 0 (prevent overselling)
--   - available_tickets cannot exceed capacity (data consistency)
--   - capacity must be at least 1 (an event with 0 capacity makes no sense)
--
-- Indexes:
--   - creator_id: used by GET /events/my (SELLER's own events)
--   - available_tickets: used by capacity-check queries
-- -------------------------------------------------------

ALTER TABLE events
    ADD CONSTRAINT chk_events_available_tickets_non_negative
        CHECK (available_tickets >= 0),
    ADD CONSTRAINT chk_events_available_tickets_within_capacity
        CHECK (available_tickets <= capacity),
    ADD CONSTRAINT chk_events_capacity_positive
        CHECK (capacity >= 1);

-- Index for seller-scoped event queries (GET /events/my)
CREATE INDEX idx_events_creator_id ON events (creator_id);

-- Composite index for seller + soft-delete filter
CREATE INDEX idx_events_creator_deleted ON events (creator_id, deleted);

-- Index for capacity availability queries
CREATE INDEX idx_events_available_tickets ON events (available_tickets);
