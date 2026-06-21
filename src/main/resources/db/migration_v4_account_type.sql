-- FemFit — Migration v4: account_type for discount logic
-- PostgreSQL, UTF-8
-- Run AFTER migration_v3_translations.sql has already been applied.
-- Safe to re-run: ALTER TABLE uses IF NOT EXISTS, idempotent by nature.
--
-- Run: psql -U postgres -d femfit -f migration_v4_account_type.sql


-- ══════════════════════════════════════
--  1. NEW COLUMN
-- ══════════════════════════════════════
-- account_type distinguishes REGULAR clients (discount grows automatically
-- with the number of completed training cycles) from CORPORATE clients
-- (flat discount tied to the account type itself, set by admin).
ALTER TABLE members
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(20) NOT NULL DEFAULT 'REGULAR';

-- Enforce allowed values (Postgres has no IF NOT EXISTS for constraints,
-- so guard with a DO block to keep this script safely re-runnable).
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'members_account_type_check'
        ) THEN
            ALTER TABLE members
                ADD CONSTRAINT members_account_type_check
                    CHECK (account_type IN ('REGULAR', 'CORPORATE'));
        END IF;
    END $$;