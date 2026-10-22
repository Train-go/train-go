-- TrainGo schema, version 5: one north-south line, and trips that belong to a train.
--
-- Recorded in docs/decisions/0011-chuyen-tao-tu-tau-noi-chang.md:
--   * Stations get their distance from Hà Nội along the line (km_mark), so every
--     list can show them north to south and a route's distance can be derived.
--   * A trip stores a full departure and arrival date-time. The old departure
--     date plus two times could not hold a run of 24 hours or more, such as
--     Hà Nội - Sài Gòn.
--   * The demo data is rebuilt around 8 stations on the main line, 16 routes,
--     3 trains and one train (SE1) that runs the line leg by leg.
--
-- This file deletes the demo data of V2 and V3 (every booking, ticket, trip,
-- route, train and station; users are kept). The project has no production
-- database, so nothing real is lost. MySQL only, like V4.

-- ---------------------------------------------------------------------------
-- Old demo data out, children before parents
-- ---------------------------------------------------------------------------

DELETE FROM tickets;
DELETE FROM bookings;
DELETE FROM trip_seats;
DELETE FROM trips;
DELETE FROM routes;
DELETE FROM trains;
DELETE FROM stations;

-- ---------------------------------------------------------------------------
-- Schema
-- ---------------------------------------------------------------------------

-- The table is empty here, so the column can be NOT NULL straight away.
ALTER TABLE stations
    ADD COLUMN km_mark INT NOT NULL,
    ADD CONSTRAINT uk_stations_km_mark UNIQUE (km_mark),
    ADD CONSTRAINT ck_stations_km_mark CHECK (km_mark >= 0);

ALTER TABLE trips
    ADD COLUMN departure_at DATETIME NOT NULL AFTER route_id,
    ADD COLUMN arrival_at   DATETIME NOT NULL AFTER departure_at,
    ADD CONSTRAINT ck_trips_arrival_after_departure CHECK (arrival_at > departure_at);

-- The new index goes first: fk_trips_route needs an index that starts with
-- route_id, and the old one serves it until this one exists.
-- Trip search filters on route and departure; the train's timetable (and the
-- rule that a new trip starts where the last one ended) reads by train.
CREATE INDEX idx_trips_route_departure ON trips (route_id, departure_at);
CREATE INDEX idx_trips_train_departure ON trips (train_id, departure_at);
DROP INDEX idx_trips_route_date ON trips;

ALTER TABLE trips
    DROP COLUMN departure_date,
    DROP COLUMN departure_time,
    DROP COLUMN arrival_time;

-- ---------------------------------------------------------------------------
-- Stations: the main line, north to south, km from Hà Nội
-- ---------------------------------------------------------------------------

-- Cities follow the provinces as merged on 1 July 2025 (Đồng Hới is in Quảng Trị).
INSERT INTO stations (code, name, city, address, km_mark) VALUES
    ('HNI', 'Ga Hà Nội',     'Hà Nội',          '120 Lê Duẩn',     0),
    ('THA', 'Ga Thanh Hóa',  'Thanh Hóa',       NULL,              175),
    ('VIN', 'Ga Vinh',       'Nghệ An',         NULL,              319),
    ('DHO', 'Ga Đồng Hới',   'Quảng Trị',       NULL,              522),
    ('HUE', 'Ga Huế',        'Huế',             '2 Bùi Thị Xuân',  688),
    ('DNG', 'Ga Đà Nẵng',    'Đà Nẵng',         '791 Hải Phòng',   791),
    ('NTR', 'Ga Nha Trang',  'Khánh Hòa',       '17 Thái Nguyên',  1315),
    ('SGN', 'Ga Sài Gòn',    'TP. Hồ Chí Minh', '1 Nguyễn Thông',  1726);

-- ---------------------------------------------------------------------------
-- Routes: each pair of neighbouring stations, plus Hà Nội - Sài Gòn, both ways
-- ---------------------------------------------------------------------------

-- The distance is the gap between the two km marks.
INSERT INTO routes (origin_station_id, destination_station_id, distance_km)
SELECT o.id, d.id, ABS(d.km_mark - o.km_mark)
FROM (
              SELECT 'HNI' AS a, 'THA' AS b
    UNION ALL SELECT 'THA', 'VIN'
    UNION ALL SELECT 'VIN', 'DHO'
    UNION ALL SELECT 'DHO', 'HUE'
    UNION ALL SELECT 'HUE', 'DNG'
    UNION ALL SELECT 'DNG', 'NTR'
    UNION ALL SELECT 'NTR', 'SGN'
    UNION ALL SELECT 'HNI', 'SGN'
) p
CROSS JOIN (SELECT 0 AS reversed UNION ALL SELECT 1) dir
JOIN stations o ON o.code = CASE dir.reversed WHEN 0 THEN p.a ELSE p.b END
JOIN stations d ON d.code = CASE dir.reversed WHEN 0 THEN p.b ELSE p.a END;

-- ---------------------------------------------------------------------------
-- Trains: no train is tied to a route; a trip picks both
-- ---------------------------------------------------------------------------

INSERT INTO trains (code, name) VALUES
    ('SE1', 'Thống Nhất 1'),
    ('SE2', 'Thống Nhất 2'),
    ('SE3', 'Thống Nhất 3');

-- ---------------------------------------------------------------------------
-- Trips: SE1 runs one leg a day, leaving at 08:00
-- ---------------------------------------------------------------------------

-- Seven legs south, then seven legs north: a 14-day loop in which every trip
-- starts at the station where the previous one ended. The timetable starts 4
-- days before the migration runs, so some runs are already COMPLETED, and
-- covers 18 days. Running time is 1 minute per km (60 km/h) rounded to 5
-- minutes; the base price is about 1.000 dong per km, rounded to 10.000.
-- Times and prices are made up for the demo, not the real timetable.
INSERT INTO trips (train_id, route_id, departure_at, arrival_at, base_price, status)
SELECT t.id, r.id,
       CAST(CURRENT_DATE AS DATETIME) + INTERVAL (days.n - 4) DAY + INTERVAL 8 HOUR,
       CAST(CURRENT_DATE AS DATETIME) + INTERVAL (days.n - 4) DAY + INTERVAL 8 HOUR
           + INTERVAL (ROUND(r.distance_km / 5) * 5) MINUTE,
       ROUND(r.distance_km / 10) * 10000,
       CASE WHEN days.n < 4 THEN 'COMPLETED' ELSE 'SCHEDULED' END
FROM (
    SELECT tens.d * 10 + units.d AS n
    FROM (SELECT 0 AS d UNION ALL SELECT 1) tens
    CROSS JOIN (
                  SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) units
) days
JOIN (
              SELECT 0 AS leg, 'HNI' AS origin_code, 'THA' AS destination_code
    UNION ALL SELECT 1,  'THA', 'VIN'
    UNION ALL SELECT 2,  'VIN', 'DHO'
    UNION ALL SELECT 3,  'DHO', 'HUE'
    UNION ALL SELECT 4,  'HUE', 'DNG'
    UNION ALL SELECT 5,  'DNG', 'NTR'
    UNION ALL SELECT 6,  'NTR', 'SGN'
    UNION ALL SELECT 7,  'SGN', 'NTR'
    UNION ALL SELECT 8,  'NTR', 'DNG'
    UNION ALL SELECT 9,  'DNG', 'HUE'
    UNION ALL SELECT 10, 'HUE', 'DHO'
    UNION ALL SELECT 11, 'DHO', 'VIN'
    UNION ALL SELECT 12, 'VIN', 'THA'
    UNION ALL SELECT 13, 'THA', 'HNI'
) loop_leg ON loop_leg.leg = days.n % 14
JOIN trains t ON t.code = 'SE1'
JOIN stations o ON o.code = loop_leg.origin_code
JOIN stations d ON d.code = loop_leg.destination_code
JOIN routes r ON r.origin_station_id = o.id AND r.destination_station_id = d.id
WHERE days.n < 18;

-- Every trip gets the fixed layout of docs/decisions/0009-bo-bang-toa-va-ghe.md,
-- all AVAILABLE, as the application does when an admin creates a trip:
-- coaches 1-2 hold seats A01-A32, coaches 3-4 hold beds B01-B16, four per
-- cabin, 200.000 dong more.
INSERT INTO trip_seats (trip_id, coach_number, coach_type, seat_code, cabin_number, price_modifier, status)
SELECT tr.id, c.coach_number, c.coach_type,
       CONCAT(CASE WHEN c.coach_type = 'SEAT' THEN 'A' ELSE 'B' END, LPAD(n.i, 2, '0')),
       CASE WHEN c.coach_type = 'SEAT' THEN NULL ELSE FLOOR((n.i - 1) / 4) + 1 END,
       c.price_modifier, 'AVAILABLE'
FROM trips tr
CROSS JOIN (
              SELECT 1 AS coach_number, 'SEAT' AS coach_type, 32 AS capacity, 0 AS price_modifier
    UNION ALL SELECT 2, 'SEAT',    32, 0
    UNION ALL SELECT 3, 'SLEEPER', 16, 200000
    UNION ALL SELECT 4, 'SLEEPER', 16, 200000
) c
JOIN (
    SELECT tens.d * 10 + units.d + 1 AS i
    FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) tens
    CROSS JOIN (
                  SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) units
) n ON n.i <= c.capacity;

-- ---------------------------------------------------------------------------
-- Bookings on SE1: two on completed runs, five upcoming, one cancelled
-- ---------------------------------------------------------------------------

-- Codes start again at 001 because every older booking is gone: BK, the year
-- and month, then the sequence. A booking is made before its trip leaves.
-- total_price is filled in from the tickets below.
INSERT INTO bookings (booking_code, customer_id, trip_id, total_price, booking_status, payment_status, paid_at, created_at)
SELECT CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), b.sequence_number),
       u.id, tr.id, 0, b.booking_status, 'PAID',
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP),
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP)
FROM (
              SELECT '001' AS sequence_number, 'an.nguyen@example.com' AS email, -3 AS day_offset,
                     'CONFIRMED' AS booking_status, 6 AS created_days_ago
    UNION ALL SELECT '002', 'binh.tran@example.com',  -1, 'CONFIRMED', 4
    UNION ALL SELECT '003', 'cuong.le@example.com',    1, 'CONFIRMED', 2
    UNION ALL SELECT '004', 'dung.pham@example.com',   2, 'CONFIRMED', 3
    UNION ALL SELECT '005', 'duc.vu@example.com',      3, 'CONFIRMED', 1
    UNION ALL SELECT '006', 'lan.hoang@example.com',   5, 'CANCELLED', 2
    UNION ALL SELECT '007', 'huy.dang@example.com',    6, 'CONFIRMED', 1
    UNION ALL SELECT '008', 'ha.bui@example.com',      9, 'CONFIRMED', 0
) b
JOIN users u ON u.email = b.email
JOIN trains t ON t.code = 'SE1'
JOIN trips tr ON tr.train_id = t.id
    AND DATE(tr.departure_at) = TIMESTAMPADD(DAY, b.day_offset, CURRENT_DATE);

-- One ticket per passenger, priced by basePrice + priceModifier so the seed
-- cannot disagree with the formula. The code is the booking code with TK in
-- place of BK, then the passenger's position; it is issued when paid.
INSERT INTO tickets (booking_id, trip_seat_id, full_name, date_of_birth, identity_number, ticket_price, ticket_code, issued_at)
SELECT b.id, ts.id, p.full_name, p.date_of_birth, p.identity_number, tr.base_price + ts.price_modifier,
       CONCAT('TK', SUBSTRING(b.booking_code, 3), '-', LPAD(p.position, 2, '0')), b.paid_at
FROM (
              SELECT '001' AS sequence_number, 1 AS position, 1 AS coach_number, 'A01' AS seat_code,
                     'Nguyễn Văn An' AS full_name, DATE '1995-04-12' AS date_of_birth, '079095001234' AS identity_number
    UNION ALL SELECT '001', 2, 1, 'A02', 'Nguyễn Thị Mai',   DATE '1997-09-03', '079197004321'
    UNION ALL SELECT '002', 1, 3, 'B01', 'Trần Thị Bình',    DATE '1990-01-25', '001190006789'
    UNION ALL SELECT '003', 1, 1, 'A05', 'Lê Hoàng Cường',   DATE '1988-11-30', '048088002468'
    UNION ALL SELECT '004', 1, 4, 'B09', 'Phạm Thu Dung',    DATE '1993-11-11', '036193090123'
    UNION ALL SELECT '004', 2, 4, 'B10', 'Phạm Quang Minh',  DATE '1991-04-27', '036091101234'
    UNION ALL SELECT '005', 1, 1, 'A07', 'Vũ Minh Đức',      DATE '1992-06-18', '001092012345'
    UNION ALL SELECT '005', 2, 1, 'A08', 'Vũ Thu Hương',     DATE '1994-02-09', '001194023456'
    UNION ALL SELECT '006', 1, 2, 'A10', 'Hoàng Thị Lan',    DATE '1989-12-01', '079189034567'
    UNION ALL SELECT '007', 1, 2, 'A15', 'Đặng Quốc Huy',    DATE '1985-03-22', '038085067890'
    UNION ALL SELECT '008', 1, 1, 'A20', 'Bùi Ngọc Hà',      DATE '1996-07-30', '001196089012'
) p
JOIN bookings b
    ON b.booking_code = CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), p.sequence_number)
JOIN trips tr ON tr.id = b.trip_id
JOIN trip_seats ts ON ts.trip_id = tr.id AND ts.coach_number = p.coach_number AND ts.seat_code = p.seat_code;

UPDATE bookings
SET total_price = (SELECT SUM(t.ticket_price) FROM tickets t WHERE t.booking_id = bookings.id);

-- Seats of confirmed bookings are BOOKED. The cancelled booking's seat went
-- back on sale when it was cancelled, so it stays AVAILABLE.
UPDATE trip_seats SET status = 'BOOKED'
WHERE id IN (
    SELECT t.trip_seat_id
    FROM tickets t
    JOIN bookings b ON b.id = t.booking_id
    WHERE b.booking_status = 'CONFIRMED'
);
