package com.jcarranza.minimidoffice.web.controller;

import com.jcarranza.minimidoffice.service.SearchService;
import com.jcarranza.minimidoffice.service.dto.FlightOptionDTO;
import com.jcarranza.minimidoffice.service.dto.FlightSearchRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Flight search via GDS.
 *
 * GET /api/flights/search — searches for flight options in Sabre.
 *
 * Note on cert environment: Flight Search only returns data for US origins (JFK, LAX, etc.).
 * European origins (ZRH, MAD) return an empty array — limitation of the Sabre cert dataset.
 * See docs/03-sabre-integration.md §11.
 */
@RestController
@RequestMapping("/flights")
public class FlightController {

    private final SearchService searchService;

    public FlightController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * GET /api/flights/search
     *
     * Parameters:
     *   origin         (required)  — IATA origin code, e.g.: JFK
     *   destination    (optional)  — IATA destination code; if omitted, open-jaw search
     *   fromDate       (optional)  — first departure date (ISO: yyyy-MM-dd)
     *   toDate         (optional)  — last departure date (ISO: yyyy-MM-dd)
     *   lengthOfStay   (optional)  — trip duration in days (default: 7)
     *   passengerCount (optional)  — number of passengers (default: 1)
     *   currencyCode   (optional)  — currency (default: EUR)
     * Response: 200 OK + FlightOptionDTO array
     *           400 if origin is not provided
     *           502/504 if Sabre fails
     */
    @GetMapping("/search")
    public ResponseEntity<List<FlightOptionDTO>> search(
            @RequestParam                                                     String    origin,
            @RequestParam(required = false)                                   String    destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "7")                                 int       lengthOfStay,
            @RequestParam(defaultValue = "1")                                 int       passengerCount,
            @RequestParam(defaultValue = "EUR")                               String    currencyCode) {

        FlightSearchRequest req = new FlightSearchRequest();
        req.setOriginCode(origin.toUpperCase());
        if (destination != null && !destination.isBlank()) req.setDestinationCode(destination.toUpperCase());
        req.setFromDate(fromDate);
        req.setToDate(toDate);
        req.setLengthOfStay(lengthOfStay);
        req.setPassengerCount(passengerCount);
        req.setCurrencyCode(currencyCode.toUpperCase());

        return ResponseEntity.ok(searchService.search(req));
    }
}
