-- V3__add_leaver_date.sql
-- The date a person left (or will leave) the company. NULL means still employed.
-- Deliberately independent of person.active, which is the soft-delete flag.
ALTER TABLE person ADD COLUMN leaver_date DATE;
