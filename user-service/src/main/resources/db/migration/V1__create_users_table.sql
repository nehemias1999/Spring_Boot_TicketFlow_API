CREATE TABLE IF NOT EXISTS users (
    id         VARCHAR(36)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME,
    PRIMARY KEY (id)
);

CREATE UNIQUE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_deleted_email ON users (deleted, email);
