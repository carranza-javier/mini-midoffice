package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.service.dto.BookingDTO;
import com.jcarranza.minimidoffice.service.dto.BookingReserveRequest;

import java.util.List;

public interface BookingService {

    /**
     * Executes the full booking flow: FlightCheck → persist RESERVED.
     * 1. Reads the traveller (short read transaction, releases connection).
     * 2. Calls GdsFlightCheckPort outside any transaction.
     * 3. Validates availability and 5% price tolerance.
     * 4. Writes RESERVED in a new REQUIRES_NEW transaction.
     */
    BookingDTO reserve(BookingReserveRequest request);

    BookingDTO cancel(Long bookingId);

    BookingDTO findById(Long bookingId);

    List<BookingDTO> findByTraveller(Long travellerId);
}
