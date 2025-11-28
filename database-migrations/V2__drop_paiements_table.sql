-- Migration: Drop paiements table
-- Run this script to remove the `paiements` table introduced by the older schema.
-- It is safe to run once; it will drop the table if it exists.

PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;
DROP TABLE IF EXISTS paiements;
COMMIT;
PRAGMA foreign_keys = ON;
