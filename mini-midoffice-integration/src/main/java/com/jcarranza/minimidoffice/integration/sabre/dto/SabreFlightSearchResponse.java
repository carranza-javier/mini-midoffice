package com.jcarranza.minimidoffice.integration.sabre.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Mapea la respuesta de POST /v1/offers/flightSearch (returnFullOffers: true). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SabreFlightSearchResponse {

    private String          timestamp;
    private List<Flight>    flights;
    private List<Journey>   journeys;
    private List<Offer>     offers;

    public String        getTimestamp() { return timestamp; }
    public void setTimestamp(String v)  { this.timestamp = v; }
    public List<Flight>  getFlights()   { return flights; }
    public void setFlights(List<Flight> v)  { this.flights = v; }
    public List<Journey> getJourneys()  { return journeys; }
    public void setJourneys(List<Journey> v){ this.journeys = v; }
    public List<Offer>   getOffers()    { return offers; }
    public void setOffers(List<Offer> v){ this.offers = v; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Flight {
        private String id;
        private String departureAirportCode;
        private String arrivalAirportCode;
        private String departureDate;
        private String departureTime;
        private String arrivalDate;
        private String arrivalTime;
        private String operatingAirlineCode;
        private int    operatingFlightNumber;
        private String marketingAirlineCode;
        private int    marketingFlightNumber;
        private int    durationInMinutes;
        private String aircraftTypeCode;

        public String getId()                      { return id; }
        public void setId(String v)                { this.id = v; }
        public String getDepartureAirportCode()    { return departureAirportCode; }
        public void setDepartureAirportCode(String v){ this.departureAirportCode = v; }
        public String getArrivalAirportCode()      { return arrivalAirportCode; }
        public void setArrivalAirportCode(String v){ this.arrivalAirportCode = v; }
        public String getDepartureDate()           { return departureDate; }
        public void setDepartureDate(String v)     { this.departureDate = v; }
        public String getDepartureTime()           { return departureTime; }
        public void setDepartureTime(String v)     { this.departureTime = v; }
        public String getArrivalDate()             { return arrivalDate; }
        public void setArrivalDate(String v)       { this.arrivalDate = v; }
        public String getArrivalTime()             { return arrivalTime; }
        public void setArrivalTime(String v)       { this.arrivalTime = v; }
        public String getOperatingAirlineCode()    { return operatingAirlineCode; }
        public void setOperatingAirlineCode(String v){ this.operatingAirlineCode = v; }
        public int   getOperatingFlightNumber()    { return operatingFlightNumber; }
        public void setOperatingFlightNumber(int v){ this.operatingFlightNumber = v; }
        public String getMarketingAirlineCode()    { return marketingAirlineCode; }
        public void setMarketingAirlineCode(String v){ this.marketingAirlineCode = v; }
        public int   getMarketingFlightNumber()    { return marketingFlightNumber; }
        public void setMarketingFlightNumber(int v){ this.marketingFlightNumber = v; }
        public int   getDurationInMinutes()        { return durationInMinutes; }
        public void setDurationInMinutes(int v)    { this.durationInMinutes = v; }
        public String getAircraftTypeCode()        { return aircraftTypeCode; }
        public void setAircraftTypeCode(String v)  { this.aircraftTypeCode = v; }
    }

    /**
     * A journey groups one or more flight segments (flightRefs) forming one leg of the
     * itinerary (outbound=0, return=1). Route and dates must be read from the referenced
     * Flight objects, not from the journey itself.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Journey {
        private String       id;
        private int          requestedJourneyIndex;
        private List<String> flightRefs;

        public String       getId()                      { return id; }
        public void         setId(String v)              { this.id = v; }
        public int          getRequestedJourneyIndex()   { return requestedJourneyIndex; }
        public void         setRequestedJourneyIndex(int v){ this.requestedJourneyIndex = v; }
        public List<String> getFlightRefs()              { return flightRefs; }
        public void         setFlightRefs(List<String> v){ this.flightRefs = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offer {
        private String       type;
        private String       id;
        private Price        totalPrice;
        private List<String> journeyRefs;
        private List<Item>   items;

        public String       getType()       { return type; }
        public void setType(String v)       { this.type = v; }
        public String       getId()         { return id; }
        public void setId(String v)         { this.id = v; }
        public Price        getTotalPrice() { return totalPrice; }
        public void setTotalPrice(Price v)  { this.totalPrice = v; }
        public List<String> getJourneyRefs(){ return journeyRefs; }
        public void setJourneyRefs(List<String> v){ this.journeyRefs = v; }
        public List<Item>   getItems()      { return items; }
        public void setItems(List<Item> v)  { this.items = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private String amount;
        private String currencyCode;

        public String getAmount()       { return amount; }
        public void setAmount(String v) { this.amount = v; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String v){ this.currencyCode = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private List<Fare> fares;

        public List<Fare> getFares()      { return fares; }
        public void setFares(List<Fare> v){ this.fares = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Fare {
        private FareTotal fareTotal;

        public FareTotal getFareTotal()      { return fareTotal; }
        public void setFareTotal(FareTotal v){ this.fareTotal = v; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FareTotal {
        private String equivalentFare;
        private String taxAmount;
        private String amount;
        private String currencyCode;

        public String getEquivalentFare()        { return equivalentFare; }
        public void setEquivalentFare(String v)  { this.equivalentFare = v; }
        public String getTaxAmount()             { return taxAmount; }
        public void setTaxAmount(String v)       { this.taxAmount = v; }
        public String getAmount()                { return amount; }
        public void setAmount(String v)          { this.amount = v; }
        public String getCurrencyCode()          { return currencyCode; }
        public void setCurrencyCode(String v)    { this.currencyCode = v; }
    }
}
