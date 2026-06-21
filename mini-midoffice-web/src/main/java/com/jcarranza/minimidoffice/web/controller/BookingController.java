package com.jcarranza.minimidoffice.web.controller;

import com.jcarranza.minimidoffice.service.BookingService;
import com.jcarranza.minimidoffice.service.dto.BookingDTO;
import com.jcarranza.minimidoffice.service.dto.BookingReserveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Booking lifecycle management: FlightCheck → RESERVED → CANCELLED.
 *
 * POST /api/bookings                  — reserve a flight (FlightCheck → RESERVED)
 * GET  /api/bookings?travellerId={id} — list bookings for a traveller
 * GET  /api/bookings/{id}             — get booking by ID
 * PUT  /api/bookings/{id}/cancel      — cancel booking (RESERVED → CANCELLED)
 *
 * POST /api/bookings flow:
 *   1. BookingService.reserve() verifies the traveller [short read TX]
 *   2. Calls GdsFlightCheckPort — no active transaction (DB connection released)
 *   3. Validates availability and 5% price tolerance
 *   4. Writes RESERVED in a REQUIRES_NEW transaction
 *   → FlightCheck is mandatory: no booking without real-time revalidation.
 */
@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     *
     * Body:
     * {
     *   "flightKey":     "LX|2020|ZRH|MAD|2026-07-17|06:55|2026-07-17|09:20|Y",
     *   "travellerId":   42,
     *   "searchedPrice": 162.34,
     *   "currencyCode":  "EUR"
     * }
     *
     * 201 Created + BookingDTO
     * 400 if the flight is no longer available or price changed more than 5%
     * 502/504 if Sabre fails during the Flight Check
     */
    @PostMapping
    public ResponseEntity<BookingDTO> reserve(@RequestBody BookingReserveRequest request) {
        BookingDTO dto = bookingService.reserve(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * GET /api/bookings?travellerId={id}
     * 200 OK + array de BookingDTO
     * 400 si el travellerId no existe
     */
    @GetMapping
    public ResponseEntity<List<BookingDTO>> findByTraveller(@RequestParam Long travellerId) {
        return ResponseEntity.ok(bookingService.findByTraveller(travellerId));
    }

    /**
     * GET /api/bookings/{id}
     * 200 OK + BookingDTO  |  400 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.findById(id));
    }

    /**
     * PUT /api/bookings/{id}/cancel
     * 200 OK + BookingDTO con status=CANCELLED
     * 409 si ya estaba cancelado
     * 400 si no existe
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(id));
    }
}
