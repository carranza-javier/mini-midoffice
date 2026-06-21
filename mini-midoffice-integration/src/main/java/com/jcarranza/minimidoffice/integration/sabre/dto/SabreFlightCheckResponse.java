package com.jcarranza.minimidoffice.integration.sabre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Mapea la respuesta de POST /v1/offers/flightCheck. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SabreFlightCheckResponse {

    private List<Flight>     flights;
    private List<Offer>      offers;
    private List<SabreError> errors;

    public List<Flight>     getFlights()                  { return flights; }
    public void             setFlights(List<Flight> v)    { this.flights = v; }
    public List<Offer>      getOffers()                   { return offers; }
    public void             setOffers(List<Offer> v)      { this.offers = v; }
    public List<SabreError> getErrors()                   { return errors; }
    public void             setErrors(List<SabreError> v) { this.errors = v; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SabreError {
        private String category;
        private String type;
        private String description;

        public String getCategory()           { return category; }
        public void   setCategory(String v)   { this.category = v; }
        public String getType()               { return type; }
        public void   setType(String v)       { this.type = v; }
        public String getDescription()        { return description; }
        public void   setDescription(String v){ this.description = v; }

        @Override
        public String toString() { return category + " / " + type + " / " + description; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flight {
        private String departureAirportCode;
        private String departureTime;
        private String arrivalAirportCode;
        private String arrivalTime;
        private String operatingAirlineCode;
        private int    operatingFlightNumber;
        private String aircraftTypeCode;
        private int    durationInMinutes;

        public String getDepartureAirportCode()    { return departureAirportCode; }
        public void setDepartureAirportCode(String v){ this.departureAirportCode = v; }
        public String getDepartureTime()           { return departureTime; }
        public void setDepartureTime(String v)     { this.departureTime = v; }
        public String getArrivalAirportCode()      { return arrivalAirportCode; }
        public void setArrivalAirportCode(String v){ this.arrivalAirportCode = v; }
        public String getArrivalTime()             { return arrivalTime; }
        public void setArrivalTime(String v)       { this.arrivalTime = v; }
        public String getOperatingAirlineCode()    { return operatingAirlineCode; }
        public void setOperatingAirlineCode(String v){ this.operatingAirlineCode = v; }
        public int   getOperatingFlightNumber()    { return operatingFlightNumber; }
        public void setOperatingFlightNumber(int v){ this.operatingFlightNumber = v; }
        public String getAircraftTypeCode()        { return aircraftTypeCode; }
        public void setAircraftTypeCode(String v)  { this.aircraftTypeCode = v; }
        public int   getDurationInMinutes()        { return durationInMinutes; }
        public void setDurationInMinutes(int v)    { this.durationInMinutes = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offer {
        private Price       totalPrice;
        private List<Item>  items;

        public Price       getTotalPrice()      { return totalPrice; }
        public void setTotalPrice(Price v)      { this.totalPrice = v; }
        public List<Item>  getItems()           { return items; }
        public void setItems(List<Item> v)      { this.items = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private String amount;
        private String currencyCode;

        public String getAmount()         { return amount; }
        public void setAmount(String v)   { this.amount = v; }
        public String getCurrencyCode()   { return currencyCode; }
        public void setCurrencyCode(String v){ this.currencyCode = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private List<Fare> fares;

        public List<Fare> getFares()       { return fares; }
        public void setFares(List<Fare> v) { this.fares = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fare {
        private FareTotal fareTotal;

        public FareTotal getFareTotal()       { return fareTotal; }
        public void setFareTotal(FareTotal v) { this.fareTotal = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FareTotal {
        private String equivalentFare;
        private String taxAmount;
        private String amount;
        private String currencyCode;

        public String getEquivalentFare()       { return equivalentFare; }
        public void setEquivalentFare(String v) { this.equivalentFare = v; }
        public String getTaxAmount()            { return taxAmount; }
        public void setTaxAmount(String v)      { this.taxAmount = v; }
        public String getAmount()               { return amount; }
        public void setAmount(String v)         { this.amount = v; }
        public String getCurrencyCode()         { return currencyCode; }
        public void setCurrencyCode(String v)   { this.currencyCode = v; }
    }
}
