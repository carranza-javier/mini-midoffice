package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.domain.exception.BusinessException;
import com.jcarranza.minimidoffice.integration.port.FlightOption;
import com.jcarranza.minimidoffice.integration.port.FlightSearchCriteria;
import com.jcarranza.minimidoffice.integration.port.GdsFlightSearchPort;
import com.jcarranza.minimidoffice.service.dto.FlightOptionDTO;
import com.jcarranza.minimidoffice.service.dto.FlightSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log         = LoggerFactory.getLogger(SearchServiceImpl.class);
    private static final Logger businessLog = LoggerFactory.getLogger("com.jcarranza.minimidoffice.business");

    private final GdsFlightSearchPort flightSearchPort;

    public SearchServiceImpl(GdsFlightSearchPort flightSearchPort) {
        this.flightSearchPort = flightSearchPort;
    }

    @Override
    public List<FlightOptionDTO> search(FlightSearchRequest request) {
        if (request.getOriginCode() == null || request.getOriginCode().isBlank()) {
            throw new BusinessException("originCode is required");
        }

        FlightSearchCriteria criteria = buildCriteria(request);
        businessLog.info("FLIGHT_SEARCH origin={} dest={} from={} to={} los={}d pax={}",
            criteria.getOriginCode(),
            request.getDestinationCode() != null ? request.getDestinationCode() : "*",
            criteria.getFromDate(), criteria.getToDate(),
            criteria.getLengthOfStay(), criteria.getPassengerCount());

        List<FlightOption> options = flightSearchPort.search(criteria);

        if (request.getDestinationCode() != null && !request.getDestinationCode().isBlank()) {
            String dest = request.getDestinationCode().toUpperCase();
            options = options.stream()
                .filter(o -> dest.equals(o.getDestinationCode()))
                .collect(Collectors.toList());
        }

        businessLog.info("FLIGHT_SEARCH_RESULT origin={} dest={} results={}",
            criteria.getOriginCode(),
            request.getDestinationCode() != null ? request.getDestinationCode() : "*",
            options.size());

        return options.stream().map(this::toDTO).collect(Collectors.toList());
    }

    private FlightSearchCriteria buildCriteria(FlightSearchRequest req) {
        return FlightSearchCriteria.builder(req.getOriginCode())
            .fromDate(req.getFromDate())
            .toDate(req.getToDate())
            .lengthOfStay(req.getLengthOfStay())
            .passengerCount(req.getPassengerCount())
            .currencyCode(req.getCurrencyCode())
            .build();
    }

    private FlightOptionDTO toDTO(FlightOption opt) {
        return new FlightOptionDTO(
            opt.getFlightKey(),
            opt.getOriginCode(),
            opt.getDestinationCode(),
            opt.getDepartureDate(),
            opt.getDepartureTime(),
            opt.getArrivalTime(),
            opt.getAirlineCode(),
            opt.getFlightNumber(),
            opt.getDurationMinutes(),
            opt.getTotalPrice(),
            opt.getCurrencyCode()
        );
    }
}
