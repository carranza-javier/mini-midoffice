package com.jcarranza.minimidoffice.integration.sabre.mapper;

import com.jcarranza.minimidoffice.integration.port.FlightCheckRequest;
import com.jcarranza.minimidoffice.integration.port.FlightCheckResult;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckRequest;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckResponse;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckResponse.FareTotal;
import com.jcarranza.minimidoffice.integration.sabre.dto.SabreFlightCheckResponse.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SabreFlightCheckMapper {

    private static final Logger log = LoggerFactory.getLogger(SabreFlightCheckMapper.class);

    public SabreFlightCheckRequest toSabreRequest(FlightCheckRequest req) {
        SabreFlightCheckRequest sabre = new SabreFlightCheckRequest();

        // Journey → Flight
        SabreFlightCheckRequest.Flight flight = new SabreFlightCheckRequest.Flight();
        flight.setDepartureAirportCode(req.getOriginCode());
        flight.setDepartureDate(req.getDepartureDate().toString());
        flight.setDepartureTime(req.getDepartureTime());
        flight.setArrivalAirportCode(req.getDestinationCode());
        flight.setArrivalDate(req.getArrivalDate().toString());
        flight.setArrivalTime(req.getArrivalTime());
        flight.setOperatingAirlineCode(req.getAirlineCode());
        flight.setOperatingFlightNumber(req.getFlightNumber());
        flight.setMarketingAirlineCode(req.getAirlineCode());
        flight.setMarketingFlightNumber(req.getFlightNumber());
        flight.setSegmentDetails(new SabreFlightCheckRequest.SegmentDetails(req.getBookingClass()));

        SabreFlightCheckRequest.Journey journey = new SabreFlightCheckRequest.Journey();
        journey.setFlights(List.of(flight));
        sabre.setJourneys(List.of(journey));

        // Fare
        SabreFlightCheckRequest.Fare fare = new SabreFlightCheckRequest.Fare();
        fare.setCurrencyCode(req.getCurrencyCode());
        fare.setCabin(new SabreFlightCheckRequest.Cabin("Jump Cabin", "Economy"));
        fare.setReturnTaxBreakdown(true);
        sabre.setFare(fare);

        // Retailing — request up to 3 additional fare options
        SabreFlightCheckRequest.Retailing retailing = new SabreFlightCheckRequest.Retailing();
        retailing.setReturnOfferAttributes(List.of("Baggage"));
        retailing.setReturnAdditionalOffers(new SabreFlightCheckRequest.ReturnAdditionalOffers(3));
        sabre.setRetailing(retailing);

        // Travelers
        List<SabreFlightCheckRequest.Traveler> travelers = new ArrayList<>();
        for (int i = 0; i < req.getPassengerCount(); i++) {
            travelers.add(new SabreFlightCheckRequest.Traveler("ADT"));
        }
        sabre.setTravelers(travelers);

        // Processing options — PCC required by Sabre cert: PC18
        sabre.setProcessingOptions(new SabreFlightCheckRequest.ProcessingOptions(req.getPseudoCityCode()));

        return sabre;
    }

    /**
     * Maps the Sabre response to an internal FlightCheckResult.
     *
     * The first offer is the primary confirmed price.
     * Additional offers (index 1+) are stored in additionalOffers.
     *
     * If errors is non-empty or offers is null/empty → result.available = false.
     */
    public FlightCheckResult toCheckResult(SabreFlightCheckResponse response) {
        // Detect error response.
        // IMPORTANT: Sabre errors are structured objects {category, type, description},
        // NOT plain strings. This was the real bug that caused a deserialization exception
        // and 504 responses until the SabreError inner class was added. We log all three
        // fields separately to make diagnosis straightforward.
        if (hasErrors(response)) {
            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                response.getErrors().forEach(e ->
                    log.warn("SABRE_BUSINESS_ERROR category={} type={} description={}",
                        e.getCategory(), e.getType(), e.getDescription())
                );
            } else {
                log.warn("SABRE_BUSINESS_ERROR response=null or errors=null");
            }
            return new FlightCheckResult.Builder().available(false).build();
        }

        if (response.getOffers() == null || response.getOffers().isEmpty()) {
            log.warn("SABRE_NO_OFFERS flightCheck returned empty offers array");
            return new FlightCheckResult.Builder().available(false).build();
        }

        // Primary offer
        Offer primaryOffer = response.getOffers().get(0);
        FareTotal mainFare  = extractFirstFareTotal(primaryOffer);

        FlightCheckResult.Builder builder = new FlightCheckResult.Builder()
            .available(true);

        if (mainFare != null) {
            builder.totalPrice(parseBigDecimal(mainFare.getAmount()))
                   .baseFare(parseBigDecimal(mainFare.getEquivalentFare()))
                   .taxes(parseBigDecimal(mainFare.getTaxAmount()))
                   .currencyCode(mainFare.getCurrencyCode());
        } else if (primaryOffer.getTotalPrice() != null) {
            builder.totalPrice(parseBigDecimal(primaryOffer.getTotalPrice().getAmount()))
                   .currencyCode(primaryOffer.getTotalPrice().getCurrencyCode());
        }

        // Additional offers
        if (response.getOffers().size() > 1) {
            List<FlightCheckResult.FareOption> extras = new ArrayList<>();
            for (int i = 1; i < response.getOffers().size(); i++) {
                Offer    extra = response.getOffers().get(i);
                FareTotal ft   = extractFirstFareTotal(extra);
                if (ft != null) {
                    extras.add(new FlightCheckResult.FareOption(
                        parseBigDecimal(ft.getAmount()),
                        parseBigDecimal(ft.getEquivalentFare()),
                        parseBigDecimal(ft.getTaxAmount()),
                        ft.getCurrencyCode()
                    ));
                }
            }
            builder.additionalOffers(extras);
        }

        return builder.build();
    }

    private boolean hasErrors(SabreFlightCheckResponse r) {
        return r == null
            || (r.getErrors() != null && !r.getErrors().isEmpty());
    }

    private FareTotal extractFirstFareTotal(Offer offer) {
        if (offer.getItems() == null || offer.getItems().isEmpty()) return null;
        var item = offer.getItems().get(0);
        if (item.getFares() == null || item.getFares().isEmpty()) return null;
        return item.getFares().get(0).getFareTotal();
    }

    private BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse price '{}' as BigDecimal", s);
            return null;
        }
    }
}
