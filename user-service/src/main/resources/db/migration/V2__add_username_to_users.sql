ALTER TABLE users
    ADD COLUMN username VARCHAR(50) NULL UNIQUE AFTER email;

CREATE UNIQUE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_deleted_username ON users (deleted, username);
