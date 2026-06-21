package com.jcarranza.minimidoffice.integration.port;

/**
 * Anti-corruption port for revalidating a specific flight before booking.
 *
 * Always operates in real time, with NO GDS cache.
 * Called by BookingService immediately before transitioning to RESERVED.
 * If the result indicates !available or a price outside tolerance, the service
 * throws BusinessException and the transition does not occur.
 *
 * Current implementation: SabreFlightCheckAdapter (POST /v1/offers/flightCheck).
 */
public interface GdsFlightCheckPort {

    FlightCheckResult check(FlightCheckRequest request);
}
