package com.jcarranza.minimidoffice.service.dto;

import java.math.BigDecimal;

public class BookingReserveRequest {

    private String     flightKey;
    private Long       travellerId;
    /** Reference price from the search — used to validate the 5% price tolerance. */
    private BigDecimal searchedPrice;
    private String     currencyCode = "EUR";

    public String     getFlightKey()              { return flightKey; }
    public void       setFlightKey(String v)       { this.flightKey = v; }

    public Long       getTravellerId()             { return travellerId; }
    public void       setTravellerId(Long v)        { this.travellerId = v; }

    public BigDecimal getSearchedPrice()           { return searchedPrice; }
    public void       setSearchedPrice(BigDecimal v){ this.searchedPrice = v; }

    public String     getCurrencyCode()            { return currencyCode; }
    public void       setCurrencyCode(String v)     { this.currencyCode = v; }
}
