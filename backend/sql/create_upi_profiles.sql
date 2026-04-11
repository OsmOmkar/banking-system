-- ============================================================
-- JavaBank UPI Profiles Table
-- Run this in Supabase SQL Editor (or psql):
--   Dashboard → SQL Editor → paste & run
-- ============================================================

CREATE TABLE IF NOT EXISTS upi_profiles (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    account_id  INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    upi_id      VARCHAR(80) NOT NULL UNIQUE,   -- e.g. "omkar@javabank.com"
    pin_hash    VARCHAR(64) NOT NULL,           -- SHA-256 hex of 4-digit PIN
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for fast lookup by UPI ID (used for recipient lookup)
CREATE INDEX IF NOT EXISTS idx_upi_profiles_upi_id  ON upi_profiles(upi_id);
CREATE INDEX IF NOT EXISTS idx_upi_profiles_user_id ON upi_profiles(user_id);

-- Optional: trigger to keep updated_at current
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_upi_profiles_updated_at ON upi_profiles;
CREATE TRIGGER trg_upi_profiles_updated_at
    BEFORE UPDATE ON upi_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
