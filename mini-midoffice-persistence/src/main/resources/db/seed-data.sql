-- ============================================================
-- JCarranza Mini-Midoffice — Demo / Sample Data
-- ============================================================
-- PURPOSE  : Populates the database with realistic sample data
--            so the reports (by-destination, by-provider, by-month)
--            show varied, meaningful results in a demo environment.
--
-- IMPORTANT: This is NOT real booking data.
--            • Only SABRE rows would have come through a real GDS
--              integration (the live FlightCheck/BFM pipeline).
--            • AMADEUS and GALILEO rows are illustrative sample data
--              representing the multi-provider domain — the system
--              is designed to import bookings from Sabre, Amadeus,
--              and Galileo; these rows simulate that mix.
--
-- IDEMPOTENCY
--   This script assumes a fresh (empty) schema.  Running it against
--   a database that already has rows will produce duplicate bookings
--   for the same travellers.
--
--   To start clean, uncomment the two lines below before running:
--
-- TRUNCATE booking, traveller_profile RESTART IDENTITY CASCADE;
--
-- ============================================================

BEGIN;

-- ============================================================
-- 1. TRAVELLER PROFILES  (11 records)
-- ============================================================
-- ON CONFLICT DO NOTHING: safe to re-run if Ana Garcia already
-- exists from a previous smoke test.

INSERT INTO traveller_profile
    (first_name, last_name, email, company, passport_number, frequent_flyer_number, created_at)
VALUES
    ('Ana',       'Garcia',      'ana@corp.com',              'Iberia',                      'ES1234567', 'IB-889923', '2026-01-02 09:00:00'),
    ('Marco',     'Rossi',       'marco.rossi@skyteam.it',    'SkyTeam Travel',              'IT7654321', 'AZ-441207', '2026-01-02 09:10:00'),
    ('Sophie',    'Müller',      's.muller@lhcorporate.de',   'Lufthansa Corporate Travel',  'DE3456789', 'LH-557731', '2026-01-02 09:20:00'),
    ('James',     'O''Brien',    'j.obrien@dltbiz.com',       'Delta Business Travel',       'US9876543', 'DL-112043', '2026-01-02 09:30:00'),
    ('Priya',     'Sharma',      'p.sharma@infosys.com',      'Infosys Limited',             'IN2345678', 'BA-667894', '2026-01-02 09:40:00'),
    ('Carlos',    'Mendoza',     'c.mendoza@iberiagroup.es',  'Iberia Group',                'ES8901234', 'IB-334412', '2026-01-02 09:50:00'),
    ('Yuki',      'Tanaka',      'y.tanaka@anagroup.jp',      'ANA Holdings',                'JP5678901', 'NH-778821', '2026-01-02 10:00:00'),
    ('Emma',      'Wilson',      'e.wilson@bainconsulting.com','Bain & Company',             'US3456712', 'UA-990034', '2026-01-02 10:10:00'),
    ('Luca',      'Ferrari',     'l.ferrari@eni.com',         'Eni S.p.A.',                  'IT6789012', 'AZ-221156', '2026-01-02 10:20:00'),
    ('Rachel',    'Cohen',       'r.cohen@elalcorp.co.il',    'El Al Corporate Travel',      'IL1234890', 'LY-445567', '2026-01-02 10:30:00'),
    ('Ahmed',     'Al-Rashid',   'a.alrashid@emscorp.ae',     'Emirates Corporate Services', 'AE9012345', 'EK-883301', '2026-01-02 10:40:00')
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- 2. BOOKINGS  (51 records, all 12 months of 2026)
-- ============================================================
-- traveller_id resolved by email subquery so IDs are not hardcoded.
-- Providers: SABRE (real GDS), AMADEUS / GALILEO (sample multi-provider data).
-- Status: mostly RESERVED; four CANCELLED entries for realistic spread.
-- Prices: 200–4500 USD range; confirmed_price within 5 % of searched_price.
-- Destinations: GVA ZRH MAD BCN LHR CDG FRA JFK LAX MIA ORD DXB
-- ============================================================

-- ─── JANUARY ────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'marco.rossi@skyteam.it'),
 'JFK','GVA','LX|2|JFK|GVA|2026-01-08|19:25|2026-01-09|09:00|Y',
 '2026-01-08','2026-01-03 14:00:00','2026-01-03 14:05:00', 1250.00, 1262.50,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 's.muller@lhcorporate.de'),
 'FRA','JFK','LH|400|FRA|JFK|2026-01-15|10:00|2026-01-15|13:30|Y',
 '2026-01-15','2026-01-10 09:00:00','2026-01-10 09:04:00',  875.00,  879.90,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'j.obrien@dltbiz.com'),
 'ORD','LHR','UA|944|ORD|LHR|2026-01-22|17:30|2026-01-23|07:45|Y',
 '2026-01-22','2026-01-17 16:00:00','2026-01-17 16:03:00',  695.00,  695.00,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'p.sharma@infosys.com'),
 'LHR','DXB','EK|203|LHR|DXB|2026-01-28|10:00|2026-01-28|20:15|Y',
 '2026-01-28','2026-01-23 11:30:00','2026-01-23 11:33:00',  540.00,  543.80,'GALILEO', 'RESERVED');

-- ─── FEBRUARY ───────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'c.mendoza@iberiagroup.es'),
 'MAD','JFK','IB|6274|MAD|JFK|2026-02-05|11:00|2026-02-05|14:00|Y',
 '2026-02-05','2026-01-30 10:00:00','2026-01-30 10:02:00',  710.00,  714.30,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'ana@corp.com'),
 'JFK','BCN','IB|6275|JFK|BCN|2026-02-12|19:30|2026-02-13|10:00|Y',
 '2026-02-12','2026-02-07 14:00:00','2026-02-07 14:07:00',  680.00,  683.20,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'y.tanaka@anagroup.jp'),
 'CDG','LAX','AF|72|CDG|LAX|2026-02-19|10:30|2026-02-19|13:00|Y',
 '2026-02-19','2026-02-14 09:00:00','2026-02-14 09:05:00', 1050.00, 1058.40,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'e.wilson@bainconsulting.com'),
 'JFK','ZRH','LX|24|JFK|ZRH|2026-02-25|20:30|2026-02-26|11:00|Y',
 '2026-02-25','2026-02-20 12:00:00','2026-02-20 12:04:00', 1150.00, 1153.50,'SABRE',   'CANCELLED');

-- ─── MARCH ──────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'l.ferrari@eni.com'),
 'MIA','GVA','LX|51|MIA|GVA|2026-03-05|17:30|2026-03-06|08:00|Y',
 '2026-03-05','2026-02-28 15:00:00','2026-02-28 15:06:00', 1420.00, 1432.80,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'r.cohen@elalcorp.co.il'),
 'JFK','CDG','AF|11|JFK|CDG|2026-03-12|20:00|2026-03-13|09:45|Y',
 '2026-03-12','2026-03-07 10:00:00','2026-03-07 10:03:00',  750.00,  753.75,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'a.alrashid@emscorp.ae'),
 'DXB','LHR','EK|202|DXB|LHR|2026-03-19|08:00|2026-03-19|13:00|Y',
 '2026-03-19','2026-03-14 09:00:00','2026-03-14 09:02:00',  480.00,  481.00,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'marco.rossi@skyteam.it'),
 'JFK','FRA','LH|404|JFK|FRA|2026-03-26|19:00|2026-03-27|08:45|Y',
 '2026-03-26','2026-03-21 14:00:00','2026-03-21 14:08:00',  880.00,  885.60,'SABRE',   'RESERVED');

-- ─── APRIL ──────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 's.muller@lhcorporate.de'),
 'ZRH','JFK','LX|6|ZRH|JFK|2026-04-02|10:30|2026-04-02|14:00|Y',
 '2026-04-02','2026-03-28 11:00:00','2026-03-28 11:04:00',  920.00,  925.80,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'j.obrien@dltbiz.com'),
 'JFK','MIA','AA|1315|JFK|MIA|2026-04-09|07:00|2026-04-09|10:15|Y',
 '2026-04-09','2026-04-04 08:00:00','2026-04-04 08:02:00',  285.00,  285.00,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'p.sharma@infosys.com'),
 'LHR','JFK','BA|177|LHR|JFK|2026-04-16|11:00|2026-04-16|14:00|Y',
 '2026-04-16','2026-04-11 10:00:00','2026-04-11 10:05:00',  730.00,  733.70,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'c.mendoza@iberiagroup.es'),
 'BCN','ORD','VY|7851|BCN|ORD|2026-04-23|10:00|2026-04-23|13:30|Y',
 '2026-04-23','2026-04-18 12:00:00','2026-04-18 12:06:00',  960.00,  967.20,'AMADEUS', 'CANCELLED');

-- ─── MAY ────────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'ana@corp.com'),
 'JFK','LHR','BA|178|JFK|LHR|2026-05-01|21:00|2026-05-02|09:30|Y',
 '2026-05-01','2026-04-26 13:00:00','2026-04-26 13:03:00',  815.00,  820.10,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'y.tanaka@anagroup.jp'),
 'LAX','DXB','EK|216|LAX|DXB|2026-05-08|22:00|2026-05-09|14:00|Y',
 '2026-05-08','2026-05-03 10:00:00','2026-05-03 10:04:00', 1180.00, 1186.40,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'e.wilson@bainconsulting.com'),
 'GVA','JFK','LX|1|GVA|JFK|2026-05-15|10:00|2026-05-15|14:00|Y',
 '2026-05-15','2026-05-10 09:00:00','2026-05-10 09:07:00', 2350.00, 2362.00,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'l.ferrari@eni.com'),
 'FRA','MIA','LH|402|FRA|MIA|2026-05-22|10:30|2026-05-22|15:00|Y',
 '2026-05-22','2026-05-17 14:00:00','2026-05-17 14:05:00', 1050.00, 1055.25,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'r.cohen@elalcorp.co.il'),
 'JFK','MAD','IB|6251|JFK|MAD|2026-05-29|19:00|2026-05-30|09:30|Y',
 '2026-05-29','2026-05-24 11:00:00','2026-05-24 11:03:00',  695.00,  698.50,'SABRE',   'RESERVED');

-- ─── JUNE ───────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'a.alrashid@emscorp.ae'),
 'DXB','CDG','EK|75|DXB|CDG|2026-06-04|09:00|2026-06-04|14:00|Y',
 '2026-06-04','2026-05-30 09:00:00','2026-05-30 09:02:00',  620.00,  623.10,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'marco.rossi@skyteam.it'),
 'JFK','GVA','LX|2|JFK|GVA|2026-06-11|19:25|2026-06-12|09:00|Y',
 '2026-06-11','2026-06-06 14:00:00','2026-06-06 14:06:00', 1340.00, 1349.40,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 's.muller@lhcorporate.de'),
 'LHR','ORD','UA|935|LHR|ORD|2026-06-18|11:00|2026-06-18|14:00|Y',
 '2026-06-18','2026-06-13 10:00:00','2026-06-13 10:04:00',  730.00,  733.70,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'j.obrien@dltbiz.com'),
 'MIA','GVA','LX|51|MIA|GVA|2026-06-25|17:30|2026-06-26|08:00|Y',
 '2026-06-25','2026-06-20 16:00:00','2026-06-20 16:08:00', 1680.00, 1696.80,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'p.sharma@infosys.com'),
 'JFK','ZRH','LX|24|JFK|ZRH|2026-06-30|20:30|2026-07-01|11:00|Y',
 '2026-06-30','2026-06-25 11:00:00','2026-06-25 11:05:00', 1020.00, 1025.10,'SABRE',   'CANCELLED');

-- ─── JULY ───────────────────────────────────────────────────
-- Note: one booking already in DB from smoke test (Ana, JFK→GVA, 2026-07-18, id=1).
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'c.mendoza@iberiagroup.es'),
 'JFK','LHR','BA|178|JFK|LHR|2026-07-03|21:00|2026-07-04|09:30|Y',
 '2026-07-03','2026-06-28 12:00:00','2026-06-28 12:03:00',  890.00,  894.50,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'y.tanaka@anagroup.jp'),
 'LAX','FRA','LH|456|LAX|FRA|2026-07-10|17:00|2026-07-11|12:00|Y',
 '2026-07-10','2026-07-05 10:00:00','2026-07-05 10:04:00', 1250.00, 1258.75,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'e.wilson@bainconsulting.com'),
 'CDG','JFK','AF|22|CDG|JFK|2026-07-17|11:30|2026-07-17|14:00|Y',
 '2026-07-17','2026-07-12 09:00:00','2026-07-12 09:05:00',  760.00,  765.00,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'l.ferrari@eni.com'),
 'ORD','BCN','IB|7851|ORD|BCN|2026-07-24|18:00|2026-07-25|10:30|Y',
 '2026-07-24','2026-07-19 14:00:00','2026-07-19 14:07:00',  850.00,  855.25,'GALILEO', 'RESERVED');

-- ─── AUGUST ─────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'r.cohen@elalcorp.co.il'),
 'JFK','MAD','IB|6251|JFK|MAD|2026-08-06|19:00|2026-08-07|09:30|Y',
 '2026-08-06','2026-08-01 10:00:00','2026-08-01 10:04:00',  715.00,  720.15,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'a.alrashid@emscorp.ae'),
 'DXB','LHR','EK|202|DXB|LHR|2026-08-13|08:00|2026-08-13|13:00|Y',
 '2026-08-13','2026-08-08 09:00:00','2026-08-08 09:02:00',  510.00,  512.55,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'marco.rossi@skyteam.it'),
 'ZRH','MIA','LX|97|ZRH|MIA|2026-08-20|11:00|2026-08-20|15:30|Y',
 '2026-08-20','2026-08-15 14:00:00','2026-08-15 14:08:00', 1380.00, 1390.00,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 's.muller@lhcorporate.de'),
 'JFK','GVA','LX|2|JFK|GVA|2026-08-27|19:25|2026-08-28|09:00|Y',
 '2026-08-27','2026-08-22 11:00:00','2026-08-22 11:05:00', 2890.00, 2907.50,'SABRE',   'RESERVED');

-- ─── SEPTEMBER ──────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'j.obrien@dltbiz.com'),
 'ORD','CDG','AF|625|ORD|CDG|2026-09-03|19:00|2026-09-04|09:45|Y',
 '2026-09-03','2026-08-29 16:00:00','2026-08-29 16:04:00',  750.00,  754.00,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'p.sharma@infosys.com'),
 'LHR','DXB','EK|203|LHR|DXB|2026-09-10|10:00|2026-09-10|20:15|Y',
 '2026-09-10','2026-09-05 10:00:00','2026-09-05 10:03:00',  490.00,  492.50,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'c.mendoza@iberiagroup.es'),
 'MAD','LAX','IB|6167|MAD|LAX|2026-09-17|11:00|2026-09-17|14:00|Y',
 '2026-09-17','2026-09-12 12:00:00','2026-09-12 12:05:00', 1050.00, 1055.25,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'ana@corp.com'),
 'JFK','FRA','LH|404|JFK|FRA|2026-09-24|19:00|2026-09-25|08:45|Y',
 '2026-09-24','2026-09-19 14:00:00','2026-09-19 14:03:00',  920.00,  925.00,'SABRE',   'RESERVED');

-- ─── OCTOBER ────────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'y.tanaka@anagroup.jp'),
 'JFK','BCN','IB|6275|JFK|BCN|2026-10-01|19:30|2026-10-02|10:00|Y',
 '2026-10-01','2026-09-26 10:00:00','2026-09-26 10:05:00',  680.00,  683.70,'SABRE',   'CANCELLED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'e.wilson@bainconsulting.com'),
 'GVA','ORD','LX|33|GVA|ORD|2026-10-08|11:00|2026-10-08|14:30|Y',
 '2026-10-08','2026-10-03 09:00:00','2026-10-03 09:07:00', 1150.00, 1158.20,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'l.ferrari@eni.com'),
 'MIA','LHR','BA|2036|MIA|LHR|2026-10-15|18:00|2026-10-16|07:30|Y',
 '2026-10-15','2026-10-10 14:00:00','2026-10-10 14:06:00',  780.00,  784.70,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'r.cohen@elalcorp.co.il'),
 'CDG','JFK','AF|22|CDG|JFK|2026-10-22|11:30|2026-10-22|14:00|Y',
 '2026-10-22','2026-10-17 10:00:00','2026-10-17 10:03:00',  695.00,  698.50,'GALILEO', 'RESERVED');

-- ─── NOVEMBER ───────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'a.alrashid@emscorp.ae'),
 'DXB','FRA','EK|48|DXB|FRA|2026-11-05|09:30|2026-11-05|14:30|Y',
 '2026-11-05','2026-10-31 09:00:00','2026-10-31 09:02:00',  430.00,  432.00,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'marco.rossi@skyteam.it'),
 'JFK','ZRH','LX|24|JFK|ZRH|2026-11-12|20:30|2026-11-13|11:00|Y',
 '2026-11-12','2026-11-07 14:00:00','2026-11-07 14:08:00', 1180.00, 1188.80,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 's.muller@lhcorporate.de'),
 'FRA','DXB','EK|49|FRA|DXB|2026-11-19|10:00|2026-11-19|16:30|Y',
 '2026-11-19','2026-11-14 10:00:00','2026-11-14 10:04:00',  540.00,  542.70,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'j.obrien@dltbiz.com'),
 'JFK','LHR','BA|178|JFK|LHR|2026-11-26|21:00|2026-11-27|09:30|Y',
 '2026-11-26','2026-11-21 16:00:00','2026-11-21 16:05:00',  760.00,  764.50,'AMADEUS', 'RESERVED');

-- ─── DECEMBER ───────────────────────────────────────────────
INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'p.sharma@infosys.com'),
 'LAX','GVA','LX|39|LAX|GVA|2026-12-03|18:00|2026-12-04|14:30|Y',
 '2026-12-03','2026-11-28 10:00:00','2026-11-28 10:06:00', 2450.00, 2464.50,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'c.mendoza@iberiagroup.es'),
 'MAD','JFK','IB|6274|MAD|JFK|2026-12-10|11:00|2026-12-10|14:00|Y',
 '2026-12-10','2026-12-05 12:00:00','2026-12-05 12:03:00',  810.00,  814.90,'SABRE',   'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'ana@corp.com'),
 'JFK','CDG','AF|11|JFK|CDG|2026-12-17|20:00|2026-12-18|09:45|Y',
 '2026-12-17','2026-12-12 14:00:00','2026-12-12 14:04:00',  720.00,  724.90,'AMADEUS', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'y.tanaka@anagroup.jp'),
 'FRA','LAX','LH|458|FRA|LAX|2026-12-21|11:00|2026-12-21|14:00|Y',
 '2026-12-21','2026-12-16 10:00:00','2026-12-16 10:03:00', 1320.00, 1329.00,'GALILEO', 'RESERVED');

INSERT INTO booking (traveller_id, origin, destination, flight_key, departure_date, search_date, reservation_date, searched_price, confirmed_price, provider, status) VALUES
((SELECT id FROM traveller_profile WHERE email = 'e.wilson@bainconsulting.com'),
 'LHR','ORD','UA|935|LHR|ORD|2026-12-28|11:00|2026-12-28|14:00|Y',
 '2026-12-28','2026-12-23 09:00:00','2026-12-23 09:05:00',  740.00,  745.50,'SABRE',   'RESERVED');

COMMIT;
