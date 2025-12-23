-- Migration: add nickname column to group_members
-- Adds an optional nickname for group members

ALTER TABLE IF EXISTS group_members
  ADD COLUMN IF NOT EXISTS nickname VARCHAR(100);

-- Optional: set existing values to NULL (no-op) to be explicit
UPDATE group_members SET nickname = NULL WHERE nickname IS NULL;

SELECT 'Migration 002 applied: group_members.nickname added' AS status;
