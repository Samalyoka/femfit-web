-- FemFit — Migration v6: per-item revision requests on assignments
-- PostgreSQL, UTF-8
-- Run AFTER migration_v5_trainer_availability.sql has already been applied.
-- Safe to re-run: ALTER TABLE uses IF NOT EXISTS, idempotent by nature.
--
-- Run: psql -U postgres -d femfit -f migration_v6_assignment_item_revision.sql


-- ══════════════════════════════════════
--  1. NEW COLUMNS
-- ══════════════════════════════════════
-- Per-item revision flags let a client request a redo of specific parts
-- of their assignment (exercises, equipment, nutrition, schedule) rather
-- than the whole plan. assignments.status becomes REVISION_REQUESTED
-- when any of these flags is true; the trainer sees exactly which
-- item(s) need attention, plus an optional client comment.
ALTER TABLE assignments
    ADD COLUMN IF NOT EXISTS revision_exercises_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revision_equipment_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revision_nutrition_requested BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revision_schedule_requested  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS revision_comment              TEXT;
