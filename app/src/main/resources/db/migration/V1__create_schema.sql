-- TrainGo schema, version 1: the eleven tables of docs/product/domain-model.md.
--
-- This file is the source of truth for tables, keys and constraints. The JPA
-- entities map onto it and Hibernate only validates them
-- (spring.jpa.hibernate.ddl-auto=validate).
--
-- Never edit a migration that has already run on someone's database: Flyway
-- refuses to start when a checksum changes. Change the schema with a new file,
-- V3__..., V4__... and so on. See docs/decisions/0005-quan-ly-schema-bang-flyway.md.
--
-- Conventions:
--   * Money is BIGINT in dong (VND has no minor unit).
--   * Enums are VARCHAR plus a CHECK listing the allowed values.
--   * Foreign keys have no ON DELETE action: a referenced row cannot be
--     deleted, and the service layer must check and explain why first.
--   * Kept to syntax MySQL 8 and H2 in MySQL mode both accept.

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE stations (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    code    VARCHAR(10)  NOT NULL,
    name    VARCHAR(100) NOT NULL,
    city    VARCHAR(100),
    address VARCHAR(255),
    CONSTRAINT pk_stations PRIMARY KEY (id),
    CONSTRAINT uk_stations_code UNIQUE (code)
);

CREATE TABLE routes (
    id                     BIGINT NOT NULL AUTO_INCREMENT,
    origin_station_id      BIGINT NOT NULL,
    destination_station_id BIGINT NOT NULL,
    distance_km            INT    NOT NULL,
    CONSTRAINT pk_routes PRIMARY KEY (id),
    CONSTRAINT uk_routes_origin_destination UNIQUE (origin_station_id, destination_station_id),
    CONSTRAINT fk_routes_origin_station FOREIGN KEY (origin_station_id) REFERENCES stations (id),
    CONSTRAINT fk_routes_destination_station FOREIGN KEY (destination_station_id) REFERENCES stations (id),
    CONSTRAINT ck_routes_different_stations CHECK (origin_station_id <> destination_station_id),
    CONSTRAINT ck_routes_distance CHECK (distance_km > 0)
);

CREATE TABLE trains (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    code VARCHAR(10)  NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_trains PRIMARY KEY (id),
    CONSTRAINT uk_trains_code UNIQUE (code)
);

CREATE TABLE coaches (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    train_id       BIGINT      NOT NULL,
    coach_number   INT         NOT NULL,
    coach_type     VARCHAR(20) NOT NULL,
    capacity       INT         NOT NULL,
    price_modifier BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_coaches PRIMARY KEY (id),
    CONSTRAINT uk_coaches_train_number UNIQUE (train_id, coach_number),
    CONSTRAINT fk_coaches_train FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT ck_coaches_number CHECK (coach_number > 0),
    CONSTRAINT ck_coaches_type CHECK (coach_type IN ('SEAT', 'SLEEPER')),
    CONSTRAINT ck_coaches_capacity CHECK (capacity > 0),
    CONSTRAINT ck_coaches_price_modifier CHECK (price_modifier >= 0)
);

CREATE TABLE seats (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    coach_id     BIGINT      NOT NULL,
    code         VARCHAR(5)  NOT NULL,
    type         VARCHAR(10) NOT NULL,
    cabin_number INT,
    CONSTRAINT pk_seats PRIMARY KEY (id),
    CONSTRAINT uk_seats_coach_code UNIQUE (coach_id, code),
    CONSTRAINT fk_seats_coach FOREIGN KEY (coach_id) REFERENCES coaches (id),
    CONSTRAINT ck_seats_type CHECK (type IN ('SEAT', 'BED')),
    CONSTRAINT ck_seats_cabin_number CHECK (cabin_number IS NULL OR cabin_number > 0)
);

-- A trip has a departure date and two times, as in SPEC.md. An arrival time
-- earlier than the departure time means the next day.
CREATE TABLE trips (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    train_id       BIGINT      NOT NULL,
    route_id       BIGINT      NOT NULL,
    departure_date DATE        NOT NULL,
    departure_time TIME        NOT NULL,
    arrival_time   TIME        NOT NULL,
    base_price     BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT pk_trips PRIMARY KEY (id),
    CONSTRAINT fk_trips_train FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT ck_trips_base_price CHECK (base_price >= 0),
    CONSTRAINT ck_trips_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))
);

-- Trip search filters on route and date.
CREATE INDEX idx_trips_route_date ON trips (route_id, departure_date);

-- The unique key on (trip_id, seat_id) is the last line of defence against two
-- bookings sharing a seat. A HELD seat must say who holds it and until when.
CREATE TABLE trip_seats (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    trip_id         BIGINT      NOT NULL,
    seat_id         BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    held_by_user_id BIGINT,
    held_until      DATETIME,
    CONSTRAINT pk_trip_seats PRIMARY KEY (id),
    CONSTRAINT uk_trip_seats_trip_seat UNIQUE (trip_id, seat_id),
    CONSTRAINT fk_trip_seats_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT fk_trip_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT fk_trip_seats_held_by FOREIGN KEY (held_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_trip_seats_status CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED')),
    CONSTRAINT ck_trip_seats_hold CHECK (status <> 'HELD' OR (held_by_user_id IS NOT NULL AND held_until IS NOT NULL))
);

-- A booking is CONFIRMED only once it is PAID, and a PAID booking records when.
CREATE TABLE bookings (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    booking_code   VARCHAR(20) NOT NULL,
    customer_id    BIGINT      NOT NULL,
    trip_id        BIGINT      NOT NULL,
    total_price    BIGINT      NOT NULL,
    booking_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_at        DATETIME,
    created_at     DATETIME    NOT NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uk_bookings_code UNIQUE (booking_code),
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_trip FOREIGN KEY (trip_id) REFERENCES trips (id),
    CONSTRAINT ck_bookings_total_price CHECK (total_price >= 0),
    CONSTRAINT ck_bookings_booking_status CHECK (booking_status IN ('PENDING', 'CONFIRMED', 'CANCELLED')),
    CONSTRAINT ck_bookings_payment_status CHECK (payment_status IN ('UNPAID', 'PAID')),
    CONSTRAINT ck_bookings_confirmed_is_paid CHECK (booking_status <> 'CONFIRMED' OR payment_status = 'PAID'),
    CONSTRAINT ck_bookings_paid_at CHECK (payment_status <> 'PAID' OR paid_at IS NOT NULL)
);

-- A trip seat appears once per booking, not once overall: after a cancellation
-- the seat goes back on sale and a later booking may take it.
CREATE TABLE booking_passengers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    booking_id      BIGINT       NOT NULL,
    trip_seat_id    BIGINT       NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    date_of_birth   DATE         NOT NULL,
    identity_number VARCHAR(20)  NOT NULL,
    ticket_price    BIGINT       NOT NULL,
    CONSTRAINT pk_booking_passengers PRIMARY KEY (id),
    CONSTRAINT uk_booking_passengers_seat UNIQUE (booking_id, trip_seat_id),
    CONSTRAINT fk_booking_passengers_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_passengers_trip_seat FOREIGN KEY (trip_seat_id) REFERENCES trip_seats (id),
    CONSTRAINT ck_booking_passengers_ticket_price CHECK (ticket_price >= 0)
);

CREATE TABLE tickets (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    ticket_code          VARCHAR(20) NOT NULL,
    booking_passenger_id BIGINT      NOT NULL,
    issued_at            DATETIME    NOT NULL,
    CONSTRAINT pk_tickets PRIMARY KEY (id),
    CONSTRAINT uk_tickets_code UNIQUE (ticket_code),
    CONSTRAINT uk_tickets_passenger UNIQUE (booking_passenger_id),
    CONSTRAINT fk_tickets_booking_passenger FOREIGN KEY (booking_passenger_id) REFERENCES booking_passengers (id)
);
