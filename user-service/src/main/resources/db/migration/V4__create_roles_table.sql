-- -------------------------------------------------------
-- V4: Introduce a dedicated roles table.
--
-- Previously, role names were stored as plain VARCHAR
-- strings in users.role and role_permissions.role with
-- no referential integrity.  This migration:
--   1. Creates the roles table as the single source of
--      truth for valid role names.
--   2. Seeds the four existing roles.
--   3. Adds FK constraints from both users.role and
--      role_permissions.role to roles.name.
-- -------------------------------------------------------

CREATE TABLE IF NOT EXISTS roles (
    name        VARCHAR(20)  NOT NULL,
    description VARCHAR(200),
    PRIMARY KEY (name)
);

-- Seed roles to match the existing UserRole enum values
INSERT INTO roles (name, description) VALUES
    ('USER',      'Default role. Can purchase and manage own tickets, view events.'),
    ('SELLER',    'Can create and manage own events; view tickets of own events.'),
    ('MODERATOR', 'Read-only access to events and tickets; can reassign USER/SELLER roles.'),
    ('ADMIN',     'Full access across the entire platform.');

-- Add FK: users.role → roles.name
ALTER TABLE users
    ADD CONSTRAINT fk_users_role
    FOREIGN KEY (role) REFERENCES roles (name);

-- Add FK: role_permissions.role → roles.name
ALTER TABLE role_permissions
    ADD CONSTRAINT fk_rp_role
    FOREIGN KEY (role) REFERENCES roles (name);
