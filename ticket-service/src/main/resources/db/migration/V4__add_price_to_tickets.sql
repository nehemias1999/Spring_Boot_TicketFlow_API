-- Improvement 3: Store the price paid at time of purchase
ALTER TABLE tickets ADD COLUMN price DECIMAL(12, 2) NULL;
