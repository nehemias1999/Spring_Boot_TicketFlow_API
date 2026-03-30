-- Improvement 1: Add event capacity tracking
ALTER TABLE events
    ADD COLUMN capacity         INT NOT NULL DEFAULT 0,
    ADD COLUMN available_tickets INT NOT NULL DEFAULT 0;
