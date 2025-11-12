-- V1_2__create_catalog_items.sql
-- Creates catalog_items table in app schema for materials and services catalog

-- Create app schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS app;

-- Set search path to app schema for this migration
SET search_path TO app, public;

-- Create catalog_items table
CREATE TABLE IF NOT EXISTS app.catalog_items (
    id              uuid DEFAULT uuidv7() PRIMARY KEY,
    name            TEXT NOT NULL,
    category        TEXT NOT NULL CHECK (category IN ('MATERIAL', 'SERVICE')),
    price_cents     BIGINT NOT NULL CHECK (price_cents >= 0),
    status          TEXT NOT NULL DEFAULT 'PENDING_APPROVAL' CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'INACTIVE')),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for search performance
CREATE INDEX IF NOT EXISTS idx_catalog_items_name ON app.catalog_items(name);
CREATE INDEX IF NOT EXISTS idx_catalog_items_category ON app.catalog_items(category);
CREATE INDEX IF NOT EXISTS idx_catalog_items_status ON app.catalog_items(status);

-- Create GIN index for full-text search on name (optional, for better search performance)
-- This enables faster text search queries
CREATE INDEX IF NOT EXISTS idx_catalog_items_name_gin ON app.catalog_items USING gin(to_tsvector('english', name));

