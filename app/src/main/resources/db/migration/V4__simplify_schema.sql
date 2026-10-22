-- TrainGo schema, version 4: 11 tables become 8.
--
-- Three changes, each with its own record in docs/decisions/:
--   * 0008: `tickets` held one row per `booking_passengers` row, so a ticket was
--     just two columns of its passenger. They merge, and the table is called
--     `tickets`.
--   * 0009: every train has the same coach layout, so `coaches` and `seats` only
--     held constants. Each trip_seats row carries its own coach number, coach
--     type, seat code, cabin and price modifier, and the two tables are dropped.
--     seat_type is not kept: a SLEEPER coach holds beds, a SEAT coach holds seats.
--   * 0010: the seat hold is not built. A seat is claimed only inside the payment
--     transaction (AVAILABLE -> BOOKED), so trip_seats loses held_by_user_id,
--     held_until and the HELD status, and a ticket is created together with its
--     paid booking, so ticket_code and issued_at are always set.
--
-- Nothing in the V1-V3 demo data is HELD or lacks a ticket, so the statements
-- below fail loudly rather than lose data if that is not true on some machine.
-- MySQL only: it renames foreign keys and checks by dropping and re-adding them.

-- ---------------------------------------------------------------------------
-- tickets: booking_passengers + tickets
-- ---------------------------------------------------------------------------

ALTER TABLE booking_passengers
    ADD COLUMN ticket_code VARCHAR(20) NULL,
    ADD COLUMN issued_at   DATETIME    NULL;

UPDATE booking_passengers bp
JOIN tickets t ON t.booking_passenger_id = bp.id
SET bp.ticket_code = t.ticket_code,
    bp.issued_at   = t.issued_at;

-- Frees the names uk_tickets_code and fk_tickets_* for the renamed table.
DROP TABLE tickets;

ALTER TABLE booking_passengers
    MODIFY COLUMN ticket_code VARCHAR(20) NOT NULL,
    MODIFY COLUMN issued_at   DATETIME    NOT NULL;

RENAME TABLE booking_passengers TO tickets;

-- Constraint names follow the table name (see V1). A primary key is always
-- called PRIMARY in MySQL, so pk_booking_passengers has nothing to rename.
ALTER TABLE tickets
    DROP FOREIGN KEY fk_booking_passengers_booking,
    DROP FOREIGN KEY fk_booking_passengers_trip_seat,
    DROP CHECK ck_booking_passengers_ticket_price,
    RENAME INDEX uk_booking_passengers_seat TO uk_tickets_seat,
    RENAME INDEX fk_booking_passengers_trip_seat TO fk_tickets_trip_seat;

ALTER TABLE tickets
    ADD CONSTRAINT uk_tickets_code UNIQUE (ticket_code),
    ADD CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    ADD CONSTRAINT fk_tickets_trip_seat FOREIGN KEY (trip_seat_id) REFERENCES trip_seats (id),
    ADD CONSTRAINT ck_tickets_ticket_price CHECK (ticket_price >= 0);

-- ---------------------------------------------------------------------------
-- trip_seats: coaches + seats move in, the hold moves out
-- ---------------------------------------------------------------------------

UPDATE trip_seats SET status = 'AVAILABLE' WHERE status = 'HELD';

ALTER TABLE trip_seats
    ADD COLUMN coach_number   INT         NULL,
    ADD COLUMN coach_type     VARCHAR(20) NULL,
    ADD COLUMN seat_code      VARCHAR(5)  NULL,
    ADD COLUMN cabin_number   INT         NULL,
    ADD COLUMN price_modifier BIGINT      NULL;

UPDATE trip_seats ts
JOIN seats s   ON s.id = ts.seat_id
JOIN coaches c ON c.id = s.coach_id
SET ts.coach_number   = c.coach_number,
    ts.coach_type     = c.coach_type,
    ts.seat_code      = s.code,
    ts.cabin_number   = s.cabin_number,
    ts.price_modifier = c.price_modifier;

ALTER TABLE trip_seats
    MODIFY COLUMN coach_number   INT         NOT NULL,
    MODIFY COLUMN coach_type     VARCHAR(20) NOT NULL,
    MODIFY COLUMN seat_code      VARCHAR(5)  NOT NULL,
    MODIFY COLUMN price_modifier BIGINT      NOT NULL DEFAULT 0;

-- The new unique key goes first: fk_trip_seats_trip needs an index that starts
-- with trip_id, and the old key is the one serving it until this one exists.
ALTER TABLE trip_seats
    ADD CONSTRAINT uk_trip_seats_coach_seat UNIQUE (trip_id, coach_number, seat_code),
    ADD CONSTRAINT ck_trip_seats_coach_number CHECK (coach_number > 0),
    ADD CONSTRAINT ck_trip_seats_coach_type CHECK (coach_type IN ('SEAT', 'SLEEPER')),
    ADD CONSTRAINT ck_trip_seats_cabin_number CHECK (cabin_number IS NULL OR cabin_number > 0),
    ADD CONSTRAINT ck_trip_seats_price_modifier CHECK (price_modifier >= 0);

ALTER TABLE trip_seats DROP FOREIGN KEY fk_trip_seats_seat;
ALTER TABLE trip_seats DROP INDEX uk_trip_seats_trip_seat;
ALTER TABLE trip_seats DROP COLUMN seat_id;

DROP TABLE seats;
DROP TABLE coaches;

-- No hold: the HELD status and the columns that went with it are gone.
ALTER TABLE trip_seats DROP CHECK ck_trip_seats_hold;
ALTER TABLE trip_seats DROP FOREIGN KEY fk_trip_seats_held_by;
ALTER TABLE trip_seats
    DROP COLUMN held_by_user_id,
    DROP COLUMN held_until,
    DROP CHECK ck_trip_seats_status;
ALTER TABLE trip_seats
    ADD CONSTRAINT ck_trip_seats_status CHECK (status IN ('AVAILABLE', 'BOOKED'));
