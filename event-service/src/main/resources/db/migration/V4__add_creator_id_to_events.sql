ALTER TABLE events
    ADD COLUMN creator_id VARCHAR(36) NULL AFTER base_price;

CREATE INDEX idx_events_creator_id ON events (creator_id);
