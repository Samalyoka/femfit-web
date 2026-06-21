-- FemFit — Migration v5: trainer availability status
-- PostgreSQL, UTF-8
-- Run AFTER migration_v4_account_type.sql has already been applied.
-- Safe to re-run: ALTER TABLE uses IF NOT EXISTS, idempotent by nature.
--
-- Run: psql -U postgres -d femfit -f migration_v5_trainer_availability.sql


-- ══════════════════════════════════════
--  1. NEW COLUMN
-- ══════════════════════════════════════
-- availability_status distinguishes a trainer who is temporarily away
-- (vacation, sick leave) from one whose account is simply enabled/disabled
-- (members.is_active). A trainer can be active in the system but
-- UNAVAILABLE for new assignments right now.
ALTER TABLE trainers
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';

-- Enforce allowed values (Postgres has no IF NOT EXISTS for constraints,
-- so guard with a DO block to keep this script safely re-runnable).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'trainers_availability_status_check'
    ) THEN
        ALTER TABLE trainers
            ADD CONSTRAINT trainers_availability_status_check
            CHECK (availability_status IN ('AVAILABLE', 'UNAVAILABLE'));
    END IF;
END $$;
