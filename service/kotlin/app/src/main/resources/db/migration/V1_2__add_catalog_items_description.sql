-- V1_2__add_catalog_items_description.sql
-- Adds description column to catalog_items table

-- Set search path to app schema for this migration
SET search_path TO app, public;

-- Add description column (nullable TEXT for long text)
ALTER TABLE app.catalog_items 
ADD COLUMN IF NOT EXISTS description TEXT;

