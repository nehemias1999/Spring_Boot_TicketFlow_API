-- Creates the three application databases if they don't already exist.
-- This script runs once when the MySQL container is first initialized.
CREATE DATABASE IF NOT EXISTS ticketflow_events   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ticketflow_tickets  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ticketflow_users    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
