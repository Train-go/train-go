-- More demo data, on top of V2: eight more customers and sixteen bookings, so
-- the admin lists, the dashboard and the popular routes have something to show.
--
-- V2 is not edited because it may already have run on someone's database
-- (docs/decisions/0005-quan-ly-schema-bang-flyway.md).
--
-- Trips are picked from the ones V2 created, by train and by position (the
-- 3rd upcoming run of SNT1, and so on) instead of by "today plus N days", so
-- this file still finds them when it runs some days after V2. If every
-- upcoming trip of V2 has already gone by, the bookings that need one are
-- simply not inserted; reset the database to get fresh dates.
--
-- There is no PENDING booking on purpose: a pending booking holds its seats as
-- HELD for 5 minutes, and how an expired hold is released is still undecided
-- (docs/product/booking-flow.md). Every booking here is paid, CONFIRMED or
-- CANCELLED, like the four of V2.
--
-- Accounts: every new customer has the password Customer@123, the same BCrypt
-- hash as the customers of V2.
--
-- Booking codes continue V2: BK, the year and month, then 005 to 020.

INSERT INTO users (full_name, email, password_hash, role, created_at) VALUES
    ('Phạm Thu Dung',   'dung.pham@example.com', '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -40, LOCALTIMESTAMP)),
    ('Vũ Minh Đức',     'duc.vu@example.com',    '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -39, LOCALTIMESTAMP)),
    ('Hoàng Thị Lan',   'lan.hoang@example.com', '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -38, LOCALTIMESTAMP)),
    ('Đặng Quốc Huy',   'huy.dang@example.com',  '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -37, LOCALTIMESTAMP)),
    ('Bùi Ngọc Hà',     'ha.bui@example.com',    '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -36, LOCALTIMESTAMP)),
    ('Ngô Thanh Sơn',   'son.ngo@example.com',   '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -35, LOCALTIMESTAMP)),
    ('Đỗ Khánh Linh',   'linh.do@example.com',   '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -34, LOCALTIMESTAMP)),
    ('Lý Gia Bảo',      'bao.ly@example.com',    '$2a$10$hVNWsJd5dzJxn5/CArxMXeWihn5squ5IZetjpw7TSOlYrXeHBefQK', 'CUSTOMER', TIMESTAMPADD(DAY, -33, LOCALTIMESTAMP));

-- Eleven upcoming confirmed, two on completed runs, three cancelled. Every
-- booking is on its own trip, so no seat is sold twice.
INSERT INTO bookings (booking_code, customer_id, trip_id, total_price, booking_status, payment_status, paid_at, created_at)
SELECT CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), b.sequence_number),
       u.id, tr.id, 0, b.booking_status, 'PAID',
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP),
       TIMESTAMPADD(DAY, -b.created_days_ago, LOCALTIMESTAMP)
FROM (
              SELECT '005' AS sequence_number, 'duc.vu@example.com' AS email, 'SNT2' AS train_code,
                     'SCHEDULED' AS trip_status, 2 AS trip_position, 'CONFIRMED' AS booking_status, 3 AS created_days_ago
    UNION ALL SELECT '006', 'lan.hoang@example.com', 'SE22', 'SCHEDULED',  4, 'CONFIRMED',  2
    UNION ALL SELECT '007', 'huy.dang@example.com',  'SE21', 'SCHEDULED',  1, 'CONFIRMED',  6
    UNION ALL SELECT '008', 'ha.bui@example.com',    'SE20', 'SCHEDULED',  7, 'CONFIRMED',  1
    UNION ALL SELECT '009', 'dung.pham@example.com', 'SP2',  'SCHEDULED',  3, 'CONFIRMED',  5
    UNION ALL SELECT '010', 'son.ngo@example.com',   'SNT1', 'SCHEDULED',  8, 'CONFIRMED',  2
    UNION ALL SELECT '011', 'linh.do@example.com',   'SE19', 'SCHEDULED',  9, 'CONFIRMED',  4
    UNION ALL SELECT '012', 'bao.ly@example.com',    'SP1',  'SCHEDULED',  2, 'CONFIRMED',  1
    UNION ALL SELECT '013', 'an.nguyen@example.com', 'SE22', 'SCHEDULED', 10, 'CONFIRMED',  3
    UNION ALL SELECT '014', 'binh.tran@example.com', 'SNT2', 'SCHEDULED',  6, 'CONFIRMED',  2
    UNION ALL SELECT '015', 'cuong.le@example.com',  'SE21', 'SCHEDULED', 12, 'CONFIRMED',  1
    UNION ALL SELECT '016', 'duc.vu@example.com',    'SE19', 'COMPLETED',  1, 'CONFIRMED', 20
    UNION ALL SELECT '017', 'lan.hoang@example.com', 'SNT1', 'COMPLETED',  1, 'CONFIRMED', 25
    UNION ALL SELECT '018', 'huy.dang@example.com',  'SE22', 'SCHEDULED',  8, 'CANCELLED',  7
    UNION ALL SELECT '019', 'ha.bui@example.com',    'SP2',  'SCHEDULED',  5, 'CANCELLED',  5
    UNION ALL SELECT '020', 'son.ngo@example.com',   'SE20', 'SCHEDULED',  3, 'CANCELLED',  6
) b
JOIN users u ON u.email = b.email
JOIN trains t ON t.code = b.train_code
JOIN (
    SELECT id, train_id, status,
           ROW_NUMBER() OVER (PARTITION BY train_id, status ORDER BY departure_date, id) AS trip_rank
    FROM trips
    WHERE (status = 'SCHEDULED' AND departure_date >= CURRENT_DATE) OR status = 'COMPLETED'
) tr ON tr.train_id = t.id AND tr.status = b.trip_status AND tr.trip_rank = b.trip_position;

-- Passengers, one seat each, priced by basePrice + priceModifier like V2.
-- Coaches 1 and 2 are seat coaches (A01-A32), 3 and 4 are sleepers (B01-B16).
INSERT INTO booking_passengers (booking_id, trip_seat_id, full_name, date_of_birth, identity_number, ticket_price)
SELECT b.id, ts.id, p.full_name, p.date_of_birth, p.identity_number, tr.base_price + c.price_modifier
FROM (
              SELECT '005' AS sequence_number, 1 AS coach_number, 'A07' AS seat_code,
                     'Vũ Minh Đức' AS full_name, DATE '1992-06-18' AS date_of_birth, '001092012345' AS identity_number
    UNION ALL SELECT '005', 1, 'A08', 'Vũ Thu Hương',     DATE '1994-02-09', '001194023456'
    UNION ALL SELECT '006', 2, 'A15', 'Hoàng Thị Lan',    DATE '1989-12-01', '079189034567'
    UNION ALL SELECT '006', 2, 'A16', 'Hoàng Minh Khôi',  DATE '2015-05-20', '079215045678'
    UNION ALL SELECT '006', 2, 'A17', 'Hoàng Gia Hân',    DATE '2018-08-14', '079218056789'
    UNION ALL SELECT '007', 3, 'B05', 'Đặng Quốc Huy',    DATE '1985-03-22', '038085067890'
    UNION ALL SELECT '007', 3, 'B06', 'Đặng Thị Thu',     DATE '1987-10-05', '038187078901'
    UNION ALL SELECT '008', 1, 'A20', 'Bùi Ngọc Hà',      DATE '1996-07-30', '001196089012'
    UNION ALL SELECT '009', 4, 'B09', 'Phạm Thu Dung',    DATE '1993-11-11', '036193090123'
    UNION ALL SELECT '009', 4, 'B10', 'Phạm Quang Minh',  DATE '1991-04-27', '036091101234'
    UNION ALL SELECT '009', 4, 'B11', 'Phạm Thanh Tâm',   DATE '1965-01-15', '036065112345'
    UNION ALL SELECT '009', 4, 'B12', 'Lê Thị Nga',       DATE '1967-09-09', '036167123456'
    UNION ALL SELECT '010', 1, 'A25', 'Ngô Thanh Sơn',    DATE '1990-02-14', '052090134567'
    UNION ALL SELECT '010', 1, 'A26', 'Ngô Bảo Anh',      DATE '1992-08-08', '052192145678'
    UNION ALL SELECT '011', 2, 'A03', 'Đỗ Khánh Linh',    DATE '1998-12-25', '001198156789'
    UNION ALL SELECT '012', 3, 'B02', 'Lý Gia Bảo',       DATE '1994-05-05', '079094167890'
    UNION ALL SELECT '012', 3, 'B03', 'Trương Mỹ Linh',   DATE '1995-03-17', '079195178901'
    UNION ALL SELECT '013', 1, 'A12', 'Nguyễn Văn An',    DATE '1995-04-12', '079095001234'
    UNION ALL SELECT '014', 2, 'A21', 'Trần Thị Bình',    DATE '1990-01-25', '001190006789'
    UNION ALL SELECT '014', 2, 'A22', 'Trần Văn Hùng',    DATE '1962-06-30', '001062189012'
    UNION ALL SELECT '015', 1, 'A30', 'Lê Hoàng Cường',   DATE '1988-11-30', '048088002468'
    UNION ALL SELECT '015', 1, 'A31', 'Lê Thị Hoa',       DATE '1990-03-03', '048190190123'
    UNION ALL SELECT '015', 1, 'A32', 'Lê Hoàng Phúc',    DATE '2012-12-12', '048212201234'
    UNION ALL SELECT '016', 1, 'A18', 'Vũ Minh Đức',      DATE '1992-06-18', '001092012345'
    UNION ALL SELECT '017', 3, 'B13', 'Hoàng Thị Lan',    DATE '1989-12-01', '079189034567'
    UNION ALL SELECT '017', 3, 'B14', 'Hoàng Minh Khôi',  DATE '2015-05-20', '079215045678'
    UNION ALL SELECT '018', 2, 'A28', 'Đặng Quốc Huy',    DATE '1985-03-22', '038085067890'
    UNION ALL SELECT '019', 4, 'B15', 'Bùi Ngọc Hà',      DATE '1996-07-30', '001196089012'
    UNION ALL SELECT '019', 4, 'B16', 'Bùi Văn Long',     DATE '1960-10-10', '001060212345'
    UNION ALL SELECT '020', 1, 'A14', 'Ngô Thanh Sơn',    DATE '1990-02-14', '052090134567'
) p
JOIN bookings b
    ON b.booking_code = CONCAT('BK', EXTRACT(YEAR FROM CURRENT_DATE) * 100 + EXTRACT(MONTH FROM CURRENT_DATE), p.sequence_number)
JOIN trips tr ON tr.id = b.trip_id
JOIN coaches c ON c.train_id = tr.train_id AND c.coach_number = p.coach_number
JOIN seats s ON s.coach_id = c.id AND s.code = p.seat_code
JOIN trip_seats ts ON ts.trip_id = tr.id AND ts.seat_id = s.id;

-- Only the new bookings still have the placeholder total of 0.
UPDATE bookings
SET total_price = (SELECT SUM(bp.ticket_price) FROM booking_passengers bp WHERE bp.booking_id = bookings.id)
WHERE total_price = 0;

-- Seats of confirmed bookings are BOOKED. Seats of cancelled bookings stay
-- AVAILABLE, as they went back on sale when the booking was cancelled.
UPDATE trip_seats SET status = 'BOOKED'
WHERE status = 'AVAILABLE'
  AND id IN (
    SELECT bp.trip_seat_id
    FROM booking_passengers bp
    JOIN bookings b ON b.id = bp.booking_id
    WHERE b.booking_status = 'CONFIRMED'
);

-- One ticket per new passenger, with the code rule of V2: the booking code
-- with TK in place of BK, then the passenger's position.
INSERT INTO tickets (ticket_code, booking_passenger_id, issued_at)
SELECT CONCAT('TK', SUBSTRING(b.booking_code, 3), '-',
              LPAD(ROW_NUMBER() OVER (PARTITION BY bp.booking_id ORDER BY bp.id), 2, '0')),
       bp.id, b.paid_at
FROM booking_passengers bp
JOIN bookings b ON b.id = bp.booking_id
WHERE NOT EXISTS (SELECT 1 FROM tickets t WHERE t.booking_passenger_id = bp.id);
