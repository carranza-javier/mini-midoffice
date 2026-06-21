package com.jcarranza.minimidoffice.service.dto;

import java.time.LocalDate;

public class FlightSearchRequest {

    private String    originCode;
    private String    destinationCode;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int       lengthOfStay  = 7;
    private int       passengerCount = 1;
    private String    currencyCode  = "EUR";

    public String    getOriginCode()               { return originCode; }
    public void      setOriginCode(String v)        { this.originCode = v; }

    public String    getDestinationCode()           { return destinationCode; }
    public void      setDestinationCode(String v)   { this.destinationCode = v; }

    public LocalDate getFromDate()                  { return fromDate; }
    public void      setFromDate(LocalDate v)        { this.fromDate = v; }

    public LocalDate getToDate()                    { return toDate; }
    public void      setToDate(LocalDate v)          { this.toDate = v; }

    public int       getLengthOfStay()              { return lengthOfStay; }
    public void      setLengthOfStay(int v)          { this.lengthOfStay = v; }

    public int       getPassengerCount()            { return passengerCount; }
    public void      setPassengerCount(int v)        { this.passengerCount = v; }

    public String    getCurrencyCode()              { return currencyCode; }
    public void      setCurrencyCode(String v)       { this.currencyCode = v; }
}
