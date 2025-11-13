-- V1_4__add_fulltext_search_indexes.sql
-- Adds full-text search indexes for catalog items with Portuguese language support
-- This enables efficient full-text search on name and description fields

-- Set search path to app schema for this migration
SET search_path TO app, public;

-- Drop the existing English language index if it exists
DROP INDEX IF EXISTS app.idx_catalog_items_name_gin;

-- Create GIN index for full-text search on name with Portuguese language
-- This enables faster text search queries and handles plural forms
CREATE INDEX IF NOT EXISTS idx_catalog_items_name_gin_pt 
ON app.catalog_items 
USING gin(to_tsvector('portuguese', name));

-- Create GIN index for full-text search on description with Portuguese language
-- This enables faster text search on description field
CREATE INDEX IF NOT EXISTS idx_catalog_items_description_gin_pt 
ON app.catalog_items 
USING gin(to_tsvector('portuguese', COALESCE(description, '')));

-- Create combined GIN index for full-text search on name + description
-- This optimizes queries that search both fields simultaneously
CREATE INDEX IF NOT EXISTS idx_catalog_items_name_description_gin_pt 
ON app.catalog_items 
USING gin(to_tsvector('portuguese', COALESCE(name, '') || ' ' || COALESCE(description, '')));

