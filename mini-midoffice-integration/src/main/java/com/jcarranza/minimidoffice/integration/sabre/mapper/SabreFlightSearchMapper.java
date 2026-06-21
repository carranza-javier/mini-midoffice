package com.jcarranza.minimidoffice.integration.sabre.mapper;

import com.jcarranza.minimidoffice.integration.port.FlightOption;
import com.jcarranza.minimidoffice.integration.port.FlightSearchCriteria;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchRequest;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchResponse;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchResponse.Flight;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchResponse.Journey;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightSearchResponse.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SabreFlightSearchMapper {

    private static final Logger log = LoggerFactory.getLogger(SabreFlightSearchMapper.class);

    public SabreFlightSearchRequest toSabreRequest(FlightSearchCriteria criteria) {
        SabreFlightSearchRequest req = new SabreFlightSearchRequest();

        req.setDepartureLocation(
            new SabreFlightSearchRequest.DepartureLocation("Airport", criteria.getOriginCode()));

        req.setDepartureDateRange(new SabreFlightSearchRequest.DepartureDateRange(
            criteria.getFromDate().toString(),
            criteria.getToDate().toString()));

        req.setLengthsOfStay(List.of(criteria.getLengthOfStay()));

        SabreFlightSearchRequest.ProcessingOptions opts = new SabreFlightSearchRequest.ProcessingOptions();
        opts.setPublicContentPointOfSaleCountry("US");
        opts.setReturnFullOffers(true);
        opts.setReturnLowestNonStopFare(true);
        opts.setReturnMode("Per Day");
        req.setProcessingOptions(opts);

        SabreFlightSearchRequest.Sources sources = new SabreFlightSearchRequest.Sources();
        sources.setProviders(List.of("Sabre"));
        sources.setDistributionModels(List.of("ATPCO"));
        req.setSources(sources);

        return req;
    }

    /**
     * Maps the BFM response to internal FlightOption objects.
     *
     * Actual resolution chain (verified against the Sabre cert response):
     *   Offer.journeyRefs[0]
     *     → Journey.id          → Journey.flightRefs[]
     *     → Flight.id           → Flight.(departure/arrival/airline/times/duration)
     *
     * Journey objects carry no route fields of their own — the route is read from
     * the first and last Flight referenced in flightRefs.
     */
    public List<FlightOption> toFlightOptions(SabreFlightSearchResponse response) {
        if (response == null
                || response.getOffers() == null
                || response.getOffers().isEmpty()) {
            log.warn("Sabre flightSearch returned no offers");
            return Collections.emptyList();
        }

        // Index: journeyId → Journey
        Map<String, Journey> journeyById = Collections.emptyMap();
        if (response.getJourneys() != null) {
            journeyById = response.getJourneys().stream()
                .filter(j -> j.getId() != null)
                .collect(Collectors.toMap(Journey::getId, Function.identity()));
        }

        // Index: flightId → Flight
        Map<String, Flight> flightById = Collections.emptyMap();
        if (response.getFlights() != null) {
            flightById = response.getFlights().stream()
                .filter(f -> f.getId() != null)
                .collect(Collectors.toMap(
                    Flight::getId,
                    Function.identity(),
                    (a, b) -> a   // keep first on duplicate id
                ));
        }

        log.debug("Resolving {} offers, {} journeys, {} flights",
            response.getOffers().size(), journeyById.size(), flightById.size());

        List<FlightOption> options = new ArrayList<>();
        int resolved = 0;

        for (Offer offer : response.getOffers()) {
            if (offer.getTotalPrice() == null) continue;

            FlightOption option = new FlightOption();
            option.setTotalPrice(new BigDecimal(offer.getTotalPrice().getAmount()));
            option.setCurrencyCode(offer.getTotalPrice().getCurrencyCode());

            if (offer.getJourneyRefs() != null && !offer.getJourneyRefs().isEmpty()) {
                Journey journey = journeyById.get(offer.getJourneyRefs().get(0));

                if (journey != null
                        && journey.getFlightRefs() != null
                        && !journey.getFlightRefs().isEmpty()) {

                    List<String> refs = journey.getFlightRefs();
                    Flight first = flightById.get(refs.get(0));
                    Flight last  = flightById.get(refs.get(refs.size() - 1));

                    if (first != null) {
                        option.setOriginCode(first.getDepartureAirportCode());
                        option.setDepartureTime(first.getDepartureTime());
                        option.setAirlineCode(first.getOperatingAirlineCode());
                        option.setFlightNumber(first.getOperatingFlightNumber());

                        if (first.getDepartureDate() != null) {
                            option.setDepartureDate(LocalDate.parse(first.getDepartureDate()));
                        }

                        // Total duration: sum all segments
                        int totalMins = refs.stream()
                            .map(flightById::get)
                            .filter(Objects::nonNull)
                            .mapToInt(Flight::getDurationInMinutes)
                            .sum();
                        option.setDurationMinutes(totalMins);

                        if (last != null) {
                            option.setDestinationCode(last.getArrivalAirportCode());
                            option.setArrivalTime(last.getArrivalTime());
                            option.setFlightKey(buildFlightKey(first, last));
                        }
                        resolved++;
                    } else {
                        log.debug("Flight {} not found in index for journey {}",
                            refs.get(0), journey.getId());
                    }
                }
            }
            options.add(option);
        }

        log.info("Resolved {}/{} offers with full flight details", resolved, options.size());
        return options;
    }

    /**
     * Builds the canonical flightKey:
     *   firstAirline|firstFlightNum|originAirport|destAirport|depDate|depTime|arrDate|arrTime|Y
     *
     * For connecting itineraries, origin/dest are the first and last airports.
     * Booking class is fixed to "Y" (economy) as an MVP simplification.
     */
    private String buildFlightKey(Flight first, Flight last) {
        return String.join("|",
            nvl(first.getOperatingAirlineCode()),
            String.valueOf(first.getOperatingFlightNumber()),
            nvl(first.getDepartureAirportCode()),
            nvl(last.getArrivalAirportCode()),
            nvl(first.getDepartureDate()),
            nvl(first.getDepartureTime()),
            nvl(last.getArrivalDate(), first.getDepartureDate()),
            nvl(last.getArrivalTime()),
            "Y"
        );
    }

    private String nvl(String s)             { return s != null ? s : ""; }
    private String nvl(String s, String def) { return s != null ? s : (def != null ? def : ""); }
}
