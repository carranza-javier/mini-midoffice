package com.jcarranza.minimidoffice.integration.sabre;

import com.jcarranza.minimidoffice.integration.port.FlightCheckRequest;
import com.jcarranza.minimidoffice.integration.port.FlightCheckResult;
import com.jcarranza.minimidoffice.integration.port.GdsFlightCheckPort;
import com.jcarranza.minimidoffice.integration.sabre.auth.SabreOAuthProvider;
import com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckRequest;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckResponse;
import com.jcarranza.minimidoffice.integration.sabre.mapper.SabreFlightCheckMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GdsFlightCheckPort implementation for Sabre.
 * Uses POST /v1/offers/flightCheck — always real-time, no cache.
 *
 * Reference flight validated in cert: LX 2020 ZRH→MAD (06:55→09:20).
 * Required PCC in cert: PC18. Configured in sabre.properties.
 *
 * Common errors:
 *   "No such flight (LX 2020)" → flight does not operate on that date in the cert dataset.
 *   "Itinerary could not be priced." → incorrect times, wrong PCC, or past date.
 */
public class SabreFlightCheckAdapter implements GdsFlightCheckPort {

    private static final Logger log       = LoggerFactory.getLogger(SabreFlightCheckAdapter.class);
    private static final String CHECK_PATH = "/v1/offers/flightCheck";

    private final SabreHttpClient         httpClient;
    private final SabreOAuthProvider      oAuthProvider;
    private final SabreFlightCheckMapper  mapper;

    public SabreFlightCheckAdapter(SabreHttpClient httpClient,
                                   SabreOAuthProvider oAuthProvider,
                                   SabreFlightCheckMapper mapper) {
        this.httpClient   = httpClient;
        this.oAuthProvider = oAuthProvider;
        this.mapper       = mapper;
    }

    @Override
    public FlightCheckResult check(FlightCheckRequest request) {
        log.info("Sabre flightCheck {}{}  {}/{} on {}",
            request.getAirlineCode(), request.getFlightNumber(),
            request.getOriginCode(), request.getDestinationCode(),
            request.getDepartureDate());

        String                   token        = oAuthProvider.getValidToken();
        SabreFlightCheckRequest  sabreRequest = mapper.toSabreRequest(request);
        SabreFlightCheckResponse response     = httpClient.post(
            CHECK_PATH, sabreRequest, SabreFlightCheckResponse.class, token);

        FlightCheckResult result = mapper.toCheckResult(response);
        log.info("Sabre flightCheck available={} price={} {}",
            result.isAvailable(), result.getTotalPrice(), result.getCurrencyCode());
        return result;
    }
}
