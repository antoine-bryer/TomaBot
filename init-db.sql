-- init-db.sql
-- This script runs when PostgreSQL container starts for the first time

-- Ensure database exists (already created by POSTGRES_DB env var)
-- But we can set additional settings here

-- Set timezone to UTC
SET timezone = 'UTC';

-- Enable pg_stat_statements for query performance monitoring (optional)
-- Requires restart: ALTER SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log message
DO $init$
BEGIN
    RAISE NOTICE '🍅 TomaBot database initialized successfully!';
END $init$;