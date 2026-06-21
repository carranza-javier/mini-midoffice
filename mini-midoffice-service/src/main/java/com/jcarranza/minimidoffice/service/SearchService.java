package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.service.dto.FlightOptionDTO;
import com.jcarranza.minimidoffice.service.dto.FlightSearchRequest;

import java.util.List;

public interface SearchService {

    /**
     * Searches for flights in the GDS and, if request.travellerId != null,
     * persists a Booking(SEARCHED) for each returned option.
     * Those bookings are the entry point for BookingService.reserve().
     */
    List<FlightOptionDTO> search(FlightSearchRequest request);
}
