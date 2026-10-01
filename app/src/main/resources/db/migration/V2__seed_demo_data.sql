-- Demo data for development and the course demo.
--
-- Dates are relative to the day this migration runs: scheduled trips cover the
-- 14 days after it. Once they have gone by, reset the database and start the
-- application again to get fresh ones (docs/RUNBOOK.md).
--
-- Accounts, passwords stored as BCrypt hashes of strength 10:
--   admin@traingo.vn        Admin@123      ADMIN
--   an.nguyen@example.com   Customer@123   CUSTOMER
--   binh.tran@example.com   Customer@123   CUSTOMER
--   cuong.le@example.com    Customer@123   CUSTOMER
--
-- Train codes, timetables and prices are made up for the demo. They are not
-- the real Vietnam Railways timetable.

INSERT INTO users (full_name, email, password_hash, role, created_at) VALUES
    ('Quản trị viên', 'admin@traingo.vn',
        '$2a$10$oWyGo26KYB9mca5n4qzvXurGGFUEAlLfsitvOQt5l4v00VniQ2Vj.', 'ADMIN', TIMESTAMPADD(DAY, -30, LOCALTIMESTAMP)),
    ('Nguyễn Văn An', 'an.nguyen@example.com',
        '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -20, LOCALTIMESTAMP)),
    ('Trần Thị Bình', 'binh.tran@example.com',
        '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -15, LOCALTIMESTAMP)),
    ('Lê Hoàng Cường', 'cuong.le@example.com',
        '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -12, LOCALTIMESTAMP));

-- Quy Nhơn, Huế and Vinh have no route, so the station screen has stations it
-- can delete next to ones it must refuse.
INSERT INTO stations (code, name, city, address) VALUES
    ('SGN', 'Ga Sài Gòn',   'TP. Hồ Chí Minh', '1 Nguyễn Thông'),
    ('NTR', 'Ga Nha Trang', 'Khánh Hòa',       '17 Thái Nguyên'),
    ('QNH', 'Ga Quy Nhơn',  'Gia Lai',         NULL),
    ('DNG', 'Ga Đà Nẵng',   'Đà Nẵng',         '791 Hải Phòng'),
    ('HUE', 'Ga Huế',       'Huế',             '2 Bùi Thị Xuân'),
    ('VIN', 'Ga Vinh',      'Nghệ An',         NULL),
    ('HNI', 'Ga Hà Nội',    'Hà Nội',          '120 Lê Duẩn'),
    ('LCI', 'Ga Lào Cai',   'Lào Cai',         NULL);

-- The four popular routes of the landing page, both ways.
INSERT INTO routes (origin_station_id, destination_station_id, distance_km)
SELECT o.id, d.id, r.distance_km
FROM (
              SELECT 'SGN' AS origin_code, 'NTR' AS destination_code, 411 AS distance_km
    UNION ALL SELECT 'NTR', 'SGN', 411
    UNION ALL SELECT 'SGN', 'DNG', 935
    UNION ALL SELECT 'DNG', 'SGN', 935
    UNION ALL SELECT 'HNI', 'DNG', 791
    UNION ALL SELECT 'DNG', 'HNI', 791
    UNION ALL SELECT 'HNI', 'LCI', 296
    UNION ALL SELECT 'LCI', 'HNI', 296
) r
JOIN stations o ON o.code = r.origin_code
JOIN stations d ON d.code = r.destination_code;

-- One train per direction, as the railway numbers them.
INSERT INTO trains (code, name) VALUES
    ('SNT1', 'Sài Gòn - Nha Trang'),
    ('SNT2', 'Nha Trang - Sài Gòn'),
    ('SE22', 'Sài Gòn - Đà Nẵng'),
    ('SE21', 'Đà Nẵng - Sài Gòn'),
    ('SE19', 'Hà Nội - Đà Nẵng'),
    ('SE20', 'Đà Nẵng - Hà Nội'),
    ('SP1',  'Hà Nội - Lào Cai'),
    ('SP2',  'Lào Cai - Hà Nội');

-- Every train: two seat coaches of 32 (eight rows of four) and two sleeper
-- coaches of 16 (four cabins of four). Sleepers cost 200.000 dong more, the
-- example in SPEC.md section 21.
INSERT INTO coaches (train_id, coach_number, coach_type, capacity, price_modifier)
SELECT t.id, c.coach_number, c.coach_type, c.capacity, c.price_modifier
FROM trains t
CROSS JOIN (
              SELECT 1 AS coach_number, 'SEAT' AS coach_type, 32 AS capacity, 0 AS price_modifier
    UNION ALL SELECT 2, 'SEAT',    32, 0
    UNION ALL SELECT 3, 'SLEEPER', 16, 200000
    UNION ALL SELECT 4, 'SLEEPER', 16, 200000
) c;

-- Seats follow the coach, by the same rule the application uses when an admin
-- adds a coach: A01-A32 in a seat coach; B01-B16 in a sleeper, four beds per
-- cabin, so B01-B04 are cabin 1. The digits join yields the numbers 1 to 40.
INSERT INTO seats (coach_id, code, type, cabin_number)
SELECT c.id,
       CONCAT(CASE WHEN c.coach_type = 'SEAT' THEN 'A' ELSE 'B' END, LPAD(n.i, 2, '0')),
       CASE WHEN c.coach_type = 'SEAT' THEN 'SEAT' ELSE 'BED' END,
       CASE WHEN c.coach_type = 'SEAT' THEN NULL ELSE FLOOR((n.i - 1) / 4) + 1 END
FROM coaches c
JOIN (
    SELECT tens.d * 10 + units.d + 1 AS i
    FROM (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3) tens
    CROSS JOIN (
                  SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) units
) n ON n.i <= c.capacity;

-- One run a day for every train over the next 14 days. Each run is overnight
-- and shorter than 24 hours, so its arrival time falls on the next day.
INSERT INTO trips (train_id, route_id, departure_date, departure_time, arrival_time, base_price, status)
SELECT t.id, r.id, TIMESTAMPADD(DAY, days.n, CURRENT_DATE), s.departure_time, s.arrival_time, s.base_price, 'SCHEDULED'
FROM (
              SELECT 'SNT1' AS train_code, 'SGN' AS origin_code, 'NTR' AS destination_code,
                     TIME '20:30:00' AS departure_time, TIME '04:50:00' AS arrival_time, 350000 AS base_price
    UNION ALL SELECT 'SNT2', 'NTR', 'SGN', TIME '20:00:00', TIME '04:30:00', 350000
    UNION ALL SELECT 'SE22', 'SGN', 'DNG', TIME '10:45:00', TIME '04:40:00', 780000
    UNION ALL SELECT 'SE21', 'DNG', 'SGN', TIME '10:20:00', TIME '04:10:00', 780000
    UNION ALL SELECT 'SE19', 'HNI', 'DNG', TIME '20:05:00', TIME '11:20:00', 650000
    UNION ALL SELECT 'SE20', 'DNG', 'HNI', TIME '20:45:00', TIME '11:55:00', 650000
    UNION ALL SELECT 'SP1',  'HNI', 'LCI', TIME '21:35:00', TIME '05:55:00', 300000
    UNION ALL SELECT 'SP2',  'LCI', 'HNI', TIME '21:00:00', TIME '05:20:00', 300000
) s
JOIN trains t ON t.code = s.train_code
JOIN stations o ON o.code = s.origin_code
JOIN stations d ON d.code = s.destination_code
JOIN routes r ON r.origin_station_id = o.id AND r.destination_station_id = d.id
CROSS JOIN (
    SELECT tens.d * 10 + units.d AS n
    FROM (SELECT 0 AS d UNION ALL SELECT 1) tens
    CROSS JOIN (
                  SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ) units
) days
WHERE days.n BETWEEN 1 AND 14;

-- Two runs that already happened, so My Trips has a completed group to show.
INSERT INTO trips (train_id, route_id, departure_date, departure_time, arrival_time, base_price, status)
SELECT tr.train_id, tr.route_id, TIMESTAMPADD(DAY, p.day_offset, CURRENT_DATE),
       tr.departure_time, tr.arrival_time, tr.base_price, 'COMPLETED'
FROM (SELECT 'SNT1' AS train_code, -7 AS day_offset UNION ALL SELECT 'SE19', -3) p
JOIN trains t ON t.code = p.train_code
JOIN trips tr ON tr.train_id = t.id AND tr.departure_date = TIMESTAMPADD(DAY, 1, CURRENT_DATE);

-- One cancelled run, to show that a cancelled trip cannot be booked.
UPDATE trips SET status = 'CANCELLED'
WHERE departure_date = TIMESTAMPADD(DAY, 5, CURRENT_DATE)
  AND train_id = (SELECT id FROM trains WHERE code = 'SP1');

-- Every trip gets one row per seat of its train, all AVAILABLE, as the
-- application does when an admin creates a trip.
INSERT INTO trip_seats (trip_id, seat_id, status)
SELECT tr.id, s.id, 'AVAILABLE'
FROM trips tr
JOIN coaches c ON c.train_id = tr.train_id
JOIN seats s ON s.coach_id = c.id;

-- Four paid bookings: two upcoming, one on a completed run, one cancelled.
-- Codes are BK, the year and month of the seed, then a sequence number.
-- total_price is filled in from the passengers below.
INSERT INTO bookings (booking_code, customer_id, trip_id, total_price, booking_status, payment_status, paid_at, created_at)
SELECT CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), b.sequence_number),
       u.id, tr.id, 0, b.booking_status, 'PAID',
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP),
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP)
FROM (
              SELECT '001' AS sequence_number, 'an.nguyen@example.com' AS email, 'SNT1' AS train_code,
                     3 AS day_offset, 'CONFIRMED' AS booking_status, 2 AS created_days_ago
    UNION ALL SELECT '002', 'binh.tran@example.com', 'SE19',  5, 'CONFIRMED',  1
    UNION ALL SELECT '003', 'cuong.le@example.com',  'SNT1', -7, 'CONFIRMED', 10
    UNION ALL SELECT '004', 'an.nguyen@example.com', 'SE22',  6, 'CANCELLED',  4
) b
JOIN users u ON u.email = b.email
JOIN trains t ON t.code = b.train_code
JOIN trips tr ON tr.train_id = t.id AND tr.departure_date = TIMESTAMPADD(DAY, b.day_offset, CURRENT_DATE);

-- Passengers, one seat each. ticket_price applies the price formula
-- (basePrice + priceModifier) so the seed cannot disagree with it.
INSERT INTO booking_passengers (booking_id, trip_seat_id, full_name, date_of_birth, identity_number, ticket_price)
SELECT b.id, ts.id, p.full_name, p.date_of_birth, p.identity_number, tr.base_price + c.price_modifier
FROM (
              SELECT '001' AS sequence_number, 1 AS coach_number, 'A01' AS seat_code,
                     'Nguyễn Văn An' AS full_name, DATE '1995-04-12' AS date_of_birth, '079095001234' AS identity_number
    UNION ALL SELECT '001', 1, 'A02', 'Nguyễn Thị Mai', DATE '1997-09-03', '079197004321'
    UNION ALL SELECT '002', 3, 'B01', 'Trần Thị Bình',  DATE '1990-01-25', '001190006789'
    UNION ALL SELECT '003', 1, 'A05', 'Lê Hoàng Cường', DATE '1988-11-30', '048088002468'
    UNION ALL SELECT '004', 2, 'A10', 'Nguyễn Văn An',  DATE '1995-04-12', '079095001234'
) p
JOIN bookings b
    ON b.booking_code = CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), p.sequence_number)
JOIN trips tr ON tr.id = b.trip_id
JOIN coaches c ON c.train_id = tr.train_id AND c.coach_number = p.coach_number
JOIN seats s ON s.coach_id = c.id AND s.code = p.seat_code
JOIN trip_seats ts ON ts.trip_id = tr.id AND ts.seat_id = s.id;

UPDATE bookings
SET total_price = (SELECT SUM(bp.ticket_price) FROM booking_passengers bp WHERE bp.booking_id = bookings.id);

-- Seats of confirmed bookings are BOOKED. The cancelled booking's seat went
-- back to AVAILABLE when it was cancelled, so it stays that way.
UPDATE trip_seats SET status = 'BOOKED'
WHERE id IN (
    SELECT bp.trip_seat_id
    FROM booking_passengers bp
    JOIN bookings b ON b.id = bp.booking_id
    WHERE b.booking_status = 'CONFIRMED'
);

-- One ticket per passenger, issued when the booking was paid. The code is the
-- booking code with TK in place of BK, then the passenger's position.
INSERT INTO tickets (ticket_code, booking_passenger_id, issued_at)
SELECT CONCAT('TK', SUBSTRING(b.booking_code, 3), '-',
              LPAD(ROW_NUMBER() OVER (PARTITION BY bp.booking_id ORDER BY bp.id), 2, '0')),
       bp.id, b.paid_at
FROM booking_passengers bp
JOIN bookings b ON b.id = bp.booking_id;
