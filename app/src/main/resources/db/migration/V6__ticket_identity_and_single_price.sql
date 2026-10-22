-- Decisions dated 2026-10-21: travellers provide a name and identity number;
-- all seats on a trip share its base price. Existing ticket prices stay frozen.
ALTER TABLE tickets DROP COLUMN date_of_birth;

ALTER TABLE trip_seats DROP CHECK ck_trip_seats_price_modifier;
ALTER TABLE trip_seats DROP COLUMN price_modifier;
