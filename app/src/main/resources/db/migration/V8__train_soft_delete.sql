-- Trains are soft deleted (Phi, 26/10/2026): the row stays, so trips, bookings
-- and tickets keep their train, and the admin can restore it later.
ALTER TABLE trains ADD COLUMN is_delete BOOLEAN NOT NULL DEFAULT FALSE;

-- A deleted train's code may be given to a new train. Only trains that are not
-- deleted need distinct codes: active_code is NULL for a deleted train, and a
-- unique index accepts any number of NULLs.
ALTER TABLE trains
    ADD COLUMN active_code VARCHAR(10) GENERATED ALWAYS AS (IF(is_delete, NULL, code)) STORED,
    DROP INDEX uk_trains_code,
    ADD CONSTRAINT uk_trains_active_code UNIQUE (active_code);
