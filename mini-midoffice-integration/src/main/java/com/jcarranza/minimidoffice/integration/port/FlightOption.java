package com.jcarranza.minimidoffice.integration.port;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Result of GdsFlightSearchPort — an available flight with a reference price.
 * The flightKey may be opaque; it is persisted in Booking.flightKey and later
 * passed to GdsFlightCheckPort to revalidate price and availability.
 */
public class FlightOption {

    private String     flightKey;
    private String     originCode;
    private String     destinationCode;
    private LocalDate  departureDate;
    private String     departureTime;
    private String     arrivalTime;
    private String     airlineCode;
    private int        flightNumber;
    private int        durationMinutes;
    private BigDecimal totalPrice;
    private String     currencyCode;

    public String     getFlightKey()       { return flightKey; }
    public void       setFlightKey(String v)       { this.flightKey = v; }
    public String     getOriginCode()      { return originCode; }
    public void       setOriginCode(String v)      { this.originCode = v; }
    public String     getDestinationCode() { return destinationCode; }
    public void       setDestinationCode(String v) { this.destinationCode = v; }
    public LocalDate  getDepartureDate()   { return departureDate; }
    public void       setDepartureDate(LocalDate v){ this.departureDate = v; }
    public String     getDepartureTime()   { return departureTime; }
    public void       setDepartureTime(String v)   { this.departureTime = v; }
    public String     getArrivalTime()     { return arrivalTime; }
    public void       setArrivalTime(String v)     { this.arrivalTime = v; }
    public String     getAirlineCode()     { return airlineCode; }
    public void       setAirlineCode(String v)     { this.airlineCode = v; }
    public int        getFlightNumber()    { return flightNumber; }
    public void       setFlightNumber(int v)       { this.flightNumber = v; }
    public int        getDurationMinutes() { return durationMinutes; }
    public void       setDurationMinutes(int v)    { this.durationMinutes = v; }
    public BigDecimal getTotalPrice()      { return totalPrice; }
    public void       setTotalPrice(BigDecimal v)  { this.totalPrice = v; }
    public String     getCurrencyCode()    { return currencyCode; }
    public void       setCurrencyCode(String v)    { this.currencyCode = v; }
}
