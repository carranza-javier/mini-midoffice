package com.jcarranza.minimidoffice.service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FlightOptionDTO {

    private final String     flightKey;
    private final String     originCode;
    private final String     destinationCode;
    private final LocalDate  departureDate;
    private final String     departureTime;
    private final String     arrivalTime;
    private final String     airlineCode;
    private final int        flightNumber;
    private final int        durationMinutes;
    private final BigDecimal totalPrice;
    private final String     currencyCode;

    public FlightOptionDTO(String flightKey, String originCode, String destinationCode,
                           LocalDate departureDate, String departureTime, String arrivalTime,
                           String airlineCode, int flightNumber, int durationMinutes,
                           BigDecimal totalPrice, String currencyCode) {
        this.flightKey       = flightKey;
        this.originCode      = originCode;
        this.destinationCode = destinationCode;
        this.departureDate   = departureDate;
        this.departureTime   = departureTime;
        this.arrivalTime     = arrivalTime;
        this.airlineCode     = airlineCode;
        this.flightNumber    = flightNumber;
        this.durationMinutes = durationMinutes;
        this.totalPrice      = totalPrice;
        this.currencyCode    = currencyCode;
    }

    public String     getFlightKey()       { return flightKey; }
    public String     getOriginCode()      { return originCode; }
    public String     getDestinationCode() { return destinationCode; }
    public LocalDate  getDepartureDate()   { return departureDate; }
    public String     getDepartureTime()   { return departureTime; }
    public String     getArrivalTime()     { return arrivalTime; }
    public String     getAirlineCode()     { return airlineCode; }
    public int        getFlightNumber()    { return flightNumber; }
    public int        getDurationMinutes() { return durationMinutes; }
    public BigDecimal getTotalPrice()      { return totalPrice; }
    public String     getCurrencyCode()    { return currencyCode; }

    /** e.g. "LX 2020" */
    public String getFlightDisplay() {
        return airlineCode + " " + flightNumber;
    }
}
