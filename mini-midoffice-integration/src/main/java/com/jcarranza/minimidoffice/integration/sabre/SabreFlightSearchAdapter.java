package com.jcarranza.minimidoffice.integration.sabre;

import com.jcarranza.minimidoffice.integration.port.FlightOption;
import com.jcarranza.minimidoffice.integration.port.FlightSearchCriteria;
import com.jcarranza.minimidoffice.integration.port.GdsFlightSearchPort;
import com.jcarranza.minimidoffice.integration.sabre.auth.SabreOAuthProvider;
import com.jcarranza.minimidoffice.integration.sabre.client.SabreHttpClient;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchRequest;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchResponse;
import com.jcarranza.minimidoffice.integration.sabre.mapper.SabreFlightSearchMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * GdsFlightSearchPort implementation for Sabre.
 * Uses POST /v1/offers/flightSearch; results may be cached by Sabre.
 *
 * CERT NOTE: in the certification environment with DEVCENTER credentials,
 * this endpoint only returns results for US origins (JFK confirmed: ~1 430 offers).
 * European origins such as ZRH return an empty response. This is a cert environment
 * limitation, not a code bug.
 */
public class SabreFlightSearchAdapter implements GdsFlightSearchPort {

    private static final Logger log        = LoggerFactory.getLogger(SabreFlightSearchAdapter.class);
    private static final String SEARCH_PATH = "/v1/offers/flightSearch";

    private final SabreHttpClient          httpClient;
    private final SabreOAuthProvider       oAuthProvider;
    private final SabreFlightSearchMapper  mapper;

    public SabreFlightSearchAdapter(SabreHttpClient httpClient,
                                    SabreOAuthProvider oAuthProvider,
                                    SabreFlightSearchMapper mapper) {
        this.httpClient   = httpClient;
        this.oAuthProvider = oAuthProvider;
        this.mapper       = mapper;
    }

    @Override
    public List<FlightOption> search(FlightSearchCriteria criteria) {
        log.info("Sabre flightSearch origin={} from={} to={} los={}d",
            criteria.getOriginCode(), criteria.getFromDate(),
            criteria.getToDate(), criteria.getLengthOfStay());

        String                     token   = oAuthProvider.getValidToken();
        SabreFlightSearchRequest   request = mapper.toSabreRequest(criteria);
        SabreFlightSearchResponse  response = httpClient.post(
            SEARCH_PATH, request, SabreFlightSearchResponse.class, token);

        List<FlightOption> options = mapper.toFlightOptions(response);
        log.info("Sabre flightSearch → {} options returned", options.size());
        return options;
    }
}
