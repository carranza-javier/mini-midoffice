package com.jcarranza.minimidoffice.integration.port;

import java.time.LocalDate;

public class FlightSearchCriteria {

    private final String    originCode;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final int       lengthOfStay;
    private final int       passengerCount;
    private final String    currencyCode;

    private FlightSearchCriteria(Builder b) {
        this.originCode     = b.originCode;
        this.fromDate       = b.fromDate;
        this.toDate         = b.toDate;
        this.lengthOfStay   = b.lengthOfStay;
        this.passengerCount = b.passengerCount;
        this.currencyCode   = b.currencyCode;
    }

    public String    getOriginCode()     { return originCode; }
    public LocalDate getFromDate()       { return fromDate; }
    public LocalDate getToDate()         { return toDate; }
    public int       getLengthOfStay()   { return lengthOfStay; }
    public int       getPassengerCount() { return passengerCount; }
    public String    getCurrencyCode()   { return currencyCode; }

    public static Builder builder(String originCode) {
        return new Builder(originCode);
    }

    public static final class Builder {
        private final String originCode;
        private LocalDate fromDate;
        private LocalDate toDate;
        private int       lengthOfStay   = 7;
        private int       passengerCount = 1;
        private String    currencyCode   = "EUR";

        private Builder(String originCode) { this.originCode = originCode; }

        public Builder fromDate(LocalDate v)       { this.fromDate = v;       return this; }
        public Builder toDate(LocalDate v)         { this.toDate = v;         return this; }
        public Builder lengthOfStay(int v)         { this.lengthOfStay = v;   return this; }
        public Builder passengerCount(int v)       { this.passengerCount = v; return this; }
        public Builder currencyCode(String v)      { this.currencyCode = v;   return this; }

        public FlightSearchCriteria build()        { return new FlightSearchCriteria(this); }
    }
}
