CREATE TABLE IF NOT EXISTS permissions (
    name VARCHAR(50) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role       VARCHAR(20) NOT NULL,
    permission VARCHAR(50) NOT NULL,
    PRIMARY KEY (role, permission),
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission) REFERENCES permissions (name)
);

-- Seed all permissions
INSERT INTO permissions (name) VALUES
    ('EVENT_VIEW'),
    ('EVENT_CREATE'),
    ('EVENT_UPDATE'),
    ('EVENT_DELETE'),
    ('TICKET_VIEW'),
    ('TICKET_CREATE'),
    ('TICKET_CANCEL'),
    ('TICKET_DELETE'),
    ('USER_VIEW'),
    ('USER_ROLE_UPDATE');

-- ADMIN: all permissions
INSERT INTO role_permissions (role, permission) VALUES
    ('ADMIN', 'EVENT_VIEW'),
    ('ADMIN', 'EVENT_CREATE'),
    ('ADMIN', 'EVENT_UPDATE'),
    ('ADMIN', 'EVENT_DELETE'),
    ('ADMIN', 'TICKET_VIEW'),
    ('ADMIN', 'TICKET_CREATE'),
    ('ADMIN', 'TICKET_CANCEL'),
    ('ADMIN', 'TICKET_DELETE'),
    ('ADMIN', 'USER_VIEW'),
    ('ADMIN', 'USER_ROLE_UPDATE');

-- MODERATOR: read-only on events/tickets + role management
INSERT INTO role_permissions (role, permission) VALUES
    ('MODERATOR', 'EVENT_VIEW'),
    ('MODERATOR', 'TICKET_VIEW'),
    ('MODERATOR', 'USER_VIEW'),
    ('MODERATOR', 'USER_ROLE_UPDATE');

-- SELLER: full own-event management + read tickets of own events
INSERT INTO role_permissions (role, permission) VALUES
    ('SELLER', 'EVENT_VIEW'),
    ('SELLER', 'EVENT_CREATE'),
    ('SELLER', 'EVENT_UPDATE'),
    ('SELLER', 'EVENT_DELETE'),
    ('SELLER', 'TICKET_VIEW');

-- USER: default role — buy and manage own tickets, view events
INSERT INTO role_permissions (role, permission) VALUES
    ('USER', 'EVENT_VIEW'),
    ('USER', 'TICKET_VIEW'),
    ('USER', 'TICKET_CREATE'),
    ('USER', 'TICKET_CANCEL'),
    ('USER', 'TICKET_DELETE');
