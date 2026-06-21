package com.jcarranza.minimidoffice.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookingDTO {

    private final Long          id;
    private final Long          travellerId;
    private final String        travellerFullName;
    private final String        origin;
    private final String        destination;
    private final String        flightKey;
    private final LocalDate     departureDate;
    private final LocalDateTime searchDate;
    private final LocalDateTime reservationDate;
    private final BigDecimal    searchedPrice;
    private final BigDecimal    confirmedPrice;
    private final String        provider;
    private final String        status;

    public BookingDTO(Long id, Long travellerId, String travellerFullName,
                      String origin, String destination, String flightKey,
                      LocalDate departureDate, LocalDateTime searchDate,
                      LocalDateTime reservationDate, BigDecimal searchedPrice,
                      BigDecimal confirmedPrice, String provider, String status) {
        this.id               = id;
        this.travellerId      = travellerId;
        this.travellerFullName = travellerFullName;
        this.origin           = origin;
        this.destination      = destination;
        this.flightKey        = flightKey;
        this.departureDate    = departureDate;
        this.searchDate       = searchDate;
        this.reservationDate  = reservationDate;
        this.searchedPrice    = searchedPrice;
        this.confirmedPrice   = confirmedPrice;
        this.provider         = provider;
        this.status           = status;
    }

    public Long          getId()               { return id; }
    public Long          getTravellerId()      { return travellerId; }
    public String        getTravellerFullName() { return travellerFullName; }
    public String        getOrigin()           { return origin; }
    public String        getDestination()      { return destination; }
    public String        getFlightKey()        { return flightKey; }
    public LocalDate     getDepartureDate()    { return departureDate; }
    public LocalDateTime getSearchDate()       { return searchDate; }
    public LocalDateTime getReservationDate()  { return reservationDate; }
    public BigDecimal    getSearchedPrice()    { return searchedPrice; }
    public BigDecimal    getConfirmedPrice()   { return confirmedPrice; }
    public String        getProvider()         { return provider; }
    public String        getStatus()           { return status; }
}
