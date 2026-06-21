package com.jcarranza.minimidoffice.integration.sabre.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Mapea el body de POST /v1/offers/flightCheck. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SabreFlightCheckRequest {

    private List<Journey>      journeys;
    private Fare               fare;
    private Retailing          retailing;
    private List<Traveler>     travelers;
    private ProcessingOptions  processingOptions;

    public List<Journey>     getJourneys()          { return journeys; }
    public void setJourneys(List<Journey> v)        { this.journeys = v; }
    public Fare              getFare()              { return fare; }
    public void setFare(Fare v)                     { this.fare = v; }
    public Retailing         getRetailing()         { return retailing; }
    public void setRetailing(Retailing v)           { this.retailing = v; }
    public List<Traveler>    getTravelers()         { return travelers; }
    public void setTravelers(List<Traveler> v)      { this.travelers = v; }
    public ProcessingOptions getProcessingOptions() { return processingOptions; }
    public void setProcessingOptions(ProcessingOptions v){ this.processingOptions = v; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Journey {
        private List<Flight> flights;
        public List<Flight> getFlights()      { return flights; }
        public void setFlights(List<Flight> v){ this.flights = v; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Flight {
        private String         departureAirportCode;
        private String         departureDate;          // YYYY-MM-DD
        private String         departureTime;          // HH:MM
        private String         arrivalAirportCode;
        private String         arrivalDate;
        private String         arrivalTime;
        private String         operatingAirlineCode;
        private int            operatingFlightNumber;
        private String         marketingAirlineCode;
        private int            marketingFlightNumber;
        private SegmentDetails segmentDetails;

        public String getDepartureAirportCode()    { return departureAirportCode; }
        public void setDepartureAirportCode(String v){ this.departureAirportCode = v; }
        public String getDepartureDate()           { return departureDate; }
        public void setDepartureDate(String v)     { this.departureDate = v; }
        public String getDepartureTime()           { return departureTime; }
        public void setDepartureTime(String v)     { this.departureTime = v; }
        public String getArrivalAirportCode()      { return arrivalAirportCode; }
        public void setArrivalAirportCode(String v){ this.arrivalAirportCode = v; }
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
        public SegmentDetails getSegmentDetails()  { return segmentDetails; }
        public void setSegmentDetails(SegmentDetails v){ this.segmentDetails = v; }
    }

    public static class SegmentDetails {
        private String bookingClassCode;
        public SegmentDetails() {}
        public SegmentDetails(String bookingClassCode){ this.bookingClassCode = bookingClassCode; }
        public String getBookingClassCode()        { return bookingClassCode; }
        public void setBookingClassCode(String v)  { this.bookingClassCode = v; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Fare {
        private String  currencyCode;
        private Cabin   cabin;
        private Boolean returnTaxBreakdown;

        public String  getCurrencyCode()              { return currencyCode; }
        public void setCurrencyCode(String v)         { this.currencyCode = v; }
        public Cabin   getCabin()                     { return cabin; }
        public void setCabin(Cabin v)                 { this.cabin = v; }
        public Boolean getReturnTaxBreakdown()        { return returnTaxBreakdown; }
        public void setReturnTaxBreakdown(Boolean v)  { this.returnTaxBreakdown = v; }
    }

    public static class Cabin {
        private String logic;
        private String name;
        public Cabin() {}
        public Cabin(String logic, String name){ this.logic = logic; this.name = name; }
        public String getLogic() { return logic; }
        public String getName()  { return name; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Retailing {
        private List<String>          returnOfferAttributes;
        private ReturnAdditionalOffers returnAdditionalOffers;

        public List<String> getReturnOfferAttributes()               { return returnOfferAttributes; }
        public void setReturnOfferAttributes(List<String> v)         { this.returnOfferAttributes = v; }
        public ReturnAdditionalOffers getReturnAdditionalOffers()    { return returnAdditionalOffers; }
        public void setReturnAdditionalOffers(ReturnAdditionalOffers v){ this.returnAdditionalOffers = v; }
    }

    public static class ReturnAdditionalOffers {
        private int numberOfAdditionalOffers;
        public ReturnAdditionalOffers() {}
        public ReturnAdditionalOffers(int n){ this.numberOfAdditionalOffers = n; }
        public int getNumberOfAdditionalOffers()    { return numberOfAdditionalOffers; }
        public void setNumberOfAdditionalOffers(int n){ this.numberOfAdditionalOffers = n; }
    }

    public static class Traveler {
        private String passengerTypeCode;
        public Traveler() {}
        public Traveler(String code){ this.passengerTypeCode = code; }
        public String getPassengerTypeCode()        { return passengerTypeCode; }
        public void setPassengerTypeCode(String v)  { this.passengerTypeCode = v; }
    }

    public static class ProcessingOptions {
        private String pseudoCityCode;
        public ProcessingOptions() {}
        public ProcessingOptions(String pcc){ this.pseudoCityCode = pcc; }
        public String getPseudoCityCode()        { return pseudoCityCode; }
        public void setPseudoCityCode(String v)  { this.pseudoCityCode = v; }
    }
}
