-- ============================================================
-- JCarranza Mini-Midoffice — Migration V2
-- Removes the SEARCHED status from the booking lifecycle.
-- The flow is now: FlightCheck directly → RESERVED.
-- ============================================================

-- Remove any existing SEARCHED records
DELETE FROM booking WHERE status = 'SEARCHED';

-- Update the CHECK constraint
ALTER TABLE booking DROP CONSTRAINT chk_booking_status;
ALTER TABLE booking ADD  CONSTRAINT chk_booking_status
    CHECK (status IN ('RESERVED', 'CANCELLED'));

-- Partial index on status='SEARCHED' is no longer needed
DROP INDEX IF EXISTS idx_booking_flightkey_traveller;
