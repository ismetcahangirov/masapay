-- Local development database for masapay.
-- Creates the role and database the backend expects by default
-- (application.yml: jdbc:postgresql://localhost:5432/masapay, masapay/masapay).
-- The schema itself is NOT created here: Flyway applies
-- backend/src/main/resources/db/migration on the first backend start.
--
-- Run as the postgres superuser, connected to any database:
--   psql -U postgres -h localhost -f scripts/db/1-setup-db.sql
-- Safe to re-run.

SELECT 'CREATE ROLE masapay LOGIN PASSWORD ''masapay'''
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'masapay')
\gexec

SELECT 'CREATE DATABASE masapay OWNER masapay ENCODING ''UTF8'' TEMPLATE template0'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'masapay')
\gexec

\connect masapay

-- Flyway runs as masapay and creates every object in public.
GRANT ALL ON SCHEMA public TO masapay;
ALTER SCHEMA public OWNER TO masapay;
