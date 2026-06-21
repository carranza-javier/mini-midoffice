package com.jcarranza.minimidoffice.integration.port;

import java.time.LocalDate;

/**
 * Input for GdsFlightCheckPort — identifies a specific, known flight.
 * Built by parsing the flightKey stored in Booking, or directly
 * from the flight data selected by the user.
 *
 * flightKey format:
 *   airlineCode|flightNumber|origin|destination|departureDate|departureTime|arrivalDate|arrivalTime|bookingClass
 * Example:
 *   LX|2020|ZRH|MAD|2026-07-17|06:55|2026-07-17|09:20|Y
 */
public class FlightCheckRequest {

    private final String    airlineCode;
    private final int       flightNumber;
    private final String    originCode;
    private final String    destinationCode;
    private final LocalDate departureDate;
    private final String    departureTime;
    private final LocalDate arrivalDate;
    private final String    arrivalTime;
    private final String    bookingClass;
    private final String    currencyCode;
    private final String    pseudoCityCode;
    private final int       passengerCount;

    private FlightCheckRequest(Builder b) {
        this.airlineCode     = b.airlineCode;
        this.flightNumber    = b.flightNumber;
        this.originCode      = b.originCode;
        this.destinationCode = b.destinationCode;
        this.departureDate   = b.departureDate;
        this.departureTime   = b.departureTime;
        this.arrivalDate     = b.arrivalDate;
        this.arrivalTime     = b.arrivalTime;
        this.bookingClass    = b.bookingClass;
        this.currencyCode    = b.currencyCode;
        this.pseudoCityCode  = b.pseudoCityCode;
        this.passengerCount  = b.passengerCount;
    }

    /** Parses the flightKey stored in Booking. */
    public static FlightCheckRequest fromFlightKey(String flightKey, String currencyCode, String pseudoCityCode) {
        String[] parts = flightKey.split("\\|");
        if (parts.length != 9) {
            throw new IllegalArgumentException("Invalid flightKey format: " + flightKey);
        }
        return new Builder()
            .airlineCode(parts[0])
            .flightNumber(Integer.parseInt(parts[1]))
            .originCode(parts[2])
            .destinationCode(parts[3])
            .departureDate(LocalDate.parse(parts[4]))
            .departureTime(parts[5])
            .arrivalDate(LocalDate.parse(parts[6]))
            .arrivalTime(parts[7])
            .bookingClass(parts[8])
            .currencyCode(currencyCode)
            .pseudoCityCode(pseudoCityCode)
            .build();
    }

    public String    getAirlineCode()     { return airlineCode; }
    public int       getFlightNumber()    { return flightNumber; }
    public String    getOriginCode()      { return originCode; }
    public String    getDestinationCode() { return destinationCode; }
    public LocalDate getDepartureDate()   { return departureDate; }
    public String    getDepartureTime()   { return departureTime; }
    public LocalDate getArrivalDate()     { return arrivalDate; }
    public String    getArrivalTime()     { return arrivalTime; }
    public String    getBookingClass()    { return bookingClass; }
    public String    getCurrencyCode()    { return currencyCode; }
    public String    getPseudoCityCode()  { return pseudoCityCode; }
    public int       getPassengerCount()  { return passengerCount; }

    public static final class Builder {
        private String    airlineCode;
        private int       flightNumber;
        private String    originCode;
        private String    destinationCode;
        private LocalDate departureDate;
        private String    departureTime;
        private LocalDate arrivalDate;
        private String    arrivalTime;
        private String    bookingClass   = "Y";
        private String    currencyCode   = "EUR";
        private String    pseudoCityCode = "PC18";
        private int       passengerCount = 1;

        public Builder airlineCode(String v)     { this.airlineCode = v;     return this; }
        public Builder flightNumber(int v)       { this.flightNumber = v;    return this; }
        public Builder originCode(String v)      { this.originCode = v;      return this; }
        public Builder destinationCode(String v) { this.destinationCode = v; return this; }
        public Builder departureDate(LocalDate v){ this.departureDate = v;   return this; }
        public Builder departureTime(String v)   { this.departureTime = v;   return this; }
        public Builder arrivalDate(LocalDate v)  { this.arrivalDate = v;     return this; }
        public Builder arrivalTime(String v)     { this.arrivalTime = v;     return this; }
        public Builder bookingClass(String v)    { this.bookingClass = v;    return this; }
        public Builder currencyCode(String v)    { this.currencyCode = v;    return this; }
        public Builder pseudoCityCode(String v)  { this.pseudoCityCode = v;  return this; }
        public Builder passengerCount(int v)     { this.passengerCount = v;  return this; }

        public FlightCheckRequest build()        { return new FlightCheckRequest(this); }
    }
}
