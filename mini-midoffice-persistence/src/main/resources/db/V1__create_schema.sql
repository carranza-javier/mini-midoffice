-- ============================================================
-- JCarranza Mini-Midoffice — Schema V1
-- PostgreSQL 14+
-- Run once against the miniumbrella database.
-- ============================================================

-- ------------------------------------------------------------
-- Sequences
-- allocationSize=1 in Hibernate → INCREMENT BY 1 here as well.
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS traveller_profile_id_seq
    START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS booking_id_seq
    START WITH 1 INCREMENT BY 1 NO CYCLE;

-- ============================================================
-- TRAVELLER_PROFILE
-- ============================================================
CREATE TABLE traveller_profile (
    id                    BIGINT        NOT NULL DEFAULT nextval('traveller_profile_id_seq'),
    first_name            VARCHAR(100)  NOT NULL,
    last_name             VARCHAR(100)  NOT NULL,
    email                 VARCHAR(255)  NOT NULL,
    company               VARCHAR(200),
    passport_number       VARCHAR(50),
    frequent_flyer_number VARCHAR(50),
    created_at            TIMESTAMP     NOT NULL,

    CONSTRAINT pk_traveller_profile PRIMARY KEY (id),
    CONSTRAINT uq_traveller_email   UNIQUE      (email)
);

CREATE INDEX idx_traveller_email   ON traveller_profile (email);
CREATE INDEX idx_traveller_company ON traveller_profile (company);
CREATE INDEX idx_traveller_name    ON traveller_profile (last_name, first_name);

COMMENT ON TABLE  traveller_profile IS
    'Traveller profiles managed by the agency.';
COMMENT ON COLUMN traveller_profile.frequent_flyer_number IS
    'Frequent flyer number (any airline).';
COMMENT ON COLUMN traveller_profile.created_at IS
    'Profile creation timestamp — managed by @PrePersist, never updatable.';

-- ============================================================
-- BOOKING
-- ============================================================
CREATE TABLE booking (
    id               BIGINT        NOT NULL DEFAULT nextval('booking_id_seq'),
    traveller_id     BIGINT        NOT NULL,
    origin           VARCHAR(3)    NOT NULL,
    destination      VARCHAR(3)    NOT NULL,
    flight_key       VARCHAR(200),
    departure_date   DATE,
    search_date      TIMESTAMP     NOT NULL,
    reservation_date TIMESTAMP,
    searched_price   NUMERIC(10,2),
    confirmed_price  NUMERIC(10,2),
    provider         VARCHAR(20)   NOT NULL,
    status           VARCHAR(20)   NOT NULL,

    CONSTRAINT pk_booking           PRIMARY KEY (id),
    CONSTRAINT fk_booking_traveller FOREIGN KEY (traveller_id)
                                    REFERENCES  traveller_profile (id)
                                    ON DELETE RESTRICT,
    CONSTRAINT chk_booking_status   CHECK (status   IN ('RESERVED','CANCELLED')),
    CONSTRAINT chk_booking_provider CHECK (provider IN ('SABRE','AMADEUS','GALILEO'))
);

CREATE INDEX idx_booking_traveller      ON booking (traveller_id);
CREATE INDEX idx_booking_status         ON booking (status);
CREATE INDEX idx_booking_origin_dest    ON booking (origin, destination);
CREATE INDEX idx_booking_departure_date ON booking (departure_date);
CREATE INDEX idx_booking_provider       ON booking (provider);

COMMENT ON TABLE  booking IS
    'Records all lifecycle stages of a booking (searched → reserved → cancelled).';
COMMENT ON COLUMN booking.flight_key IS
    'Opaque flight key: airline|number|date|cabin. Example: IB3456|2026-07-15|Y';
COMMENT ON COLUMN booking.departure_date IS
    'Flight departure date — required for monthly travel reporting.';
COMMENT ON COLUMN booking.searched_price IS
    'Price shown to the user during search (GdsFlightSearchPort, may be cached).';
COMMENT ON COLUMN booking.confirmed_price IS
    'Price confirmed by GdsFlightCheckPort in real time before transitioning to RESERVED.';
COMMENT ON COLUMN booking.reservation_date IS
    'Timestamp when the booking transitioned to RESERVED after a successful Flight Check.';
