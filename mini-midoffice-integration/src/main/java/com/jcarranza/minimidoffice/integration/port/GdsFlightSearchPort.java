package com.jcarranza.minimidoffice.integration.port;

import java.util.List;

/**
 * Anti-corruption port for exploratory flight search.
 *
 * Operates on results that may be cached by the GDS.
 * Results are NOT binding on price or availability.
 *
 * Current implementation: SabreFlightSearchAdapter (POST /v1/offers/flightSearch).
 * To switch to Amadeus/Galileo: provide a new implementation of this interface; the service layer is unchanged.
 */
public interface GdsFlightSearchPort {

    List<FlightOption> search(FlightSearchCriteria criteria);
}
