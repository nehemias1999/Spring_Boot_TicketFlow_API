-- Increase id and event_id column lengths from 20 to 36 to support UUID format
ALTER TABLE tickets MODIFY COLUMN id VARCHAR(36) NOT NULL;
ALTER TABLE tickets MODIFY COLUMN event_id VARCHAR(36) NOT NULL;
