-- Improvement 18: Change date column from VARCHAR to DATETIME
-- Allows proper temporal validation and sorting
ALTER TABLE events MODIFY COLUMN date DATETIME NOT NULL;
