-- V1_5__refactor_checkout_items_to_relation.sql
-- Refactors checkout_logs.items from JSONB to a proper N-to-N relationship table
-- Creates checkout_log_items junction table with foreign keys to checkout_logs and catalog_items

-- Set search path to app schema for this migration
SET search_path TO app, public;

-- Create checkout_log_items junction table
CREATE TABLE IF NOT EXISTS app.checkout_log_items (
    id                  uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    checkout_log_id     uuid NOT NULL,
    catalog_item_id     uuid NOT NULL,
    name                TEXT NOT NULL,
    price_cents         BIGINT NOT NULL CHECK (price_cents >= 0),
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    
    -- Foreign keys
    CONSTRAINT fk_checkout_log_items_checkout_log 
        FOREIGN KEY (checkout_log_id) 
        REFERENCES app.checkout_logs(id) 
        ON DELETE CASCADE,
    
    CONSTRAINT fk_checkout_log_items_catalog_item 
        FOREIGN KEY (catalog_item_id) 
        REFERENCES app.catalog_items(id) 
        ON DELETE RESTRICT
);

-- Create indexes for querying
CREATE INDEX IF NOT EXISTS idx_checkout_log_items_checkout_log_id 
    ON app.checkout_log_items(checkout_log_id);
CREATE INDEX IF NOT EXISTS idx_checkout_log_items_catalog_item_id 
    ON app.checkout_log_items(catalog_item_id);

-- Drop the JSONB column from checkout_logs
ALTER TABLE app.checkout_logs DROP COLUMN IF EXISTS items;

