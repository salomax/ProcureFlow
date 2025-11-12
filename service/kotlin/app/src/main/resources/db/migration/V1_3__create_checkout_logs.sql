-- V1_3__create_checkout_logs.sql
-- Creates checkout_logs table in app schema for checkout transaction logging

-- Set search path to app schema for this migration
SET search_path TO app, public;

-- Create checkout_logs table
CREATE TABLE IF NOT EXISTS app.checkout_logs (
    id                  uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id             uuid,
    items               JSONB NOT NULL,
    total_price_cents   BIGINT NOT NULL CHECK (total_price_cents >= 0),
    item_count          INTEGER NOT NULL CHECK (item_count >= 0),
    status              TEXT NOT NULL CHECK (status IN ('COMPLETED', 'FAILED')),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- Create indexes for querying
CREATE INDEX IF NOT EXISTS idx_checkout_logs_created_at ON app.checkout_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_checkout_logs_user_id ON app.checkout_logs(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_checkout_logs_status ON app.checkout_logs(status);

