-- ==========================================================
-- JavaBank — Supabase SQL Migration
-- Run this in: Supabase Dashboard → SQL Editor → New Query
-- ==========================================================

-- ----------------------------------------------------------
-- Table: pending_transactions
-- Stores transactions that are HELD for user confirmation
-- when fraud is detected BEFORE the transaction completes.
-- The 5-minute timeout auto-cancels any unresponded entry.
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS pending_transactions (
    id                          SERIAL PRIMARY KEY,
    account_id                  INTEGER NOT NULL,
    account_number              VARCHAR(30),
    transaction_type            VARCHAR(20) NOT NULL,    -- WITHDRAWAL | TRANSFER
    amount                      DECIMAL(15,2) NOT NULL,
    to_account_number           VARCHAR(30),             -- for transfers
    description                 TEXT,
    ip_address                  VARCHAR(60),

    -- Status lifecycle: PENDING_CONFIRMATION → CONFIRMED | REJECTED | TIMEOUT
    status                      VARCHAR(30) DEFAULT 'PENDING_CONFIRMATION',

    -- Timestamps
    created_at                  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at                  TIMESTAMP WITH TIME ZONE,           -- NOW() + 5 min
    responded_at                TIMESTAMP WITH TIME ZONE,           -- when user replied

    -- Fraud info (why this was held)
    fraud_alert_type            VARCHAR(60),
    fraud_description           TEXT,
    severity                    VARCHAR(20),

    -- User contact info (cached for sending alerts)
    user_id                     INTEGER NOT NULL,
    user_email                  VARCHAR(120),
    user_phone                  VARCHAR(15),
    user_name                   VARCHAR(80),

    -- Balance snapshots (for audit trail)
    original_balance            DECIMAL(15,2) DEFAULT 0,
    recipient_original_balance  DECIMAL(15,2) DEFAULT 0,

    -- Foreign keys
    CONSTRAINT fk_pending_account FOREIGN KEY (account_id)
        REFERENCES accounts(id) ON DELETE CASCADE,
    CONSTRAINT fk_pending_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

-- Index for fast lookups: find all pending by user
CREATE INDEX IF NOT EXISTS idx_pending_user_status
    ON pending_transactions(user_id, status);

-- Index for timeout monitor: find expired pending transactions
CREATE INDEX IF NOT EXISTS idx_pending_expires
    ON pending_transactions(status, expires_at)
    WHERE status = 'PENDING_CONFIRMATION';

-- ==========================================================
-- Verification: run this after to confirm table was created
-- ==========================================================
SELECT 'pending_transactions table created successfully!' AS result;
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'pending_transactions'
ORDER BY ordinal_position;

-- ==========================================================
-- KYC Verification Columns for Users Table
-- Run this AFTER the pending_transactions block above
-- Adds email_verified, phone_verified, kyc_verified columns
-- ==========================================================

-- Add KYC verification columns to users table (IF NOT EXISTS guard)
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS kyc_verified   BOOLEAN DEFAULT FALSE;

-- Verification: confirm columns were added
SELECT 'KYC columns added successfully!' AS result;
SELECT column_name, data_type, column_default
FROM information_schema.columns
WHERE table_name = 'users'
  AND column_name IN ('email_verified', 'phone_verified', 'kyc_verified')
ORDER BY column_name;

