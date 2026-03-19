-- Increase id column length from 20 to 36 to support UUID format
ALTER TABLE events MODIFY COLUMN id VARCHAR(36) NOT NULL;
