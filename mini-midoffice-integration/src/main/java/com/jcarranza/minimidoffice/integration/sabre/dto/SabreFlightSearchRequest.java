package com.jcarranza.minimidoffice.integration.sabre.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Mapea el body de POST /v1/offers/flightSearch. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SabreFlightSearchRequest {

    private DepartureLocation   departureLocation;
    private DepartureDateRange  departureDateRange;
    private List<Integer>       lengthsOfStay;
    private ProcessingOptions   processingOptions;
    private Sources             sources;

    public DepartureLocation  getDepartureLocation()  { return departureLocation; }
    public void setDepartureLocation(DepartureLocation v)  { this.departureLocation = v; }
    public DepartureDateRange getDepartureDateRange()  { return departureDateRange; }
    public void setDepartureDateRange(DepartureDateRange v){ this.departureDateRange = v; }
    public List<Integer>      getLengthsOfStay()       { return lengthsOfStay; }
    public void setLengthsOfStay(List<Integer> v)      { this.lengthsOfStay = v; }
    public ProcessingOptions  getProcessingOptions()   { return processingOptions; }
    public void setProcessingOptions(ProcessingOptions v){ this.processingOptions = v; }
    public Sources            getSources()             { return sources; }
    public void setSources(Sources v)                  { this.sources = v; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartureLocation {
        private String locationType;
        private String locationCode;

        public DepartureLocation() {}
        public DepartureLocation(String locationType, String locationCode) {
            this.locationType = locationType;
            this.locationCode = locationCode;
        }
        public String getLocationType() { return locationType; }
        public String getLocationCode() { return locationCode; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartureDateRange {
        private String fromDate;
        private String toDate;

        public DepartureDateRange() {}
        public DepartureDateRange(String fromDate, String toDate) {
            this.fromDate = fromDate;
            this.toDate   = toDate;
        }
        public String getFromDate() { return fromDate; }
        public String getToDate()   { return toDate; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProcessingOptions {
        private String  publicContentPointOfSaleCountry;
        private Boolean returnLowestNonStopFare;
        private Boolean returnFullOffers;
        private String  returnMode;

        public String  getPublicContentPointOfSaleCountry()  { return publicContentPointOfSaleCountry; }
        public void setPublicContentPointOfSaleCountry(String v){ this.publicContentPointOfSaleCountry = v; }
        public Boolean getReturnLowestNonStopFare()          { return returnLowestNonStopFare; }
        public void setReturnLowestNonStopFare(Boolean v)    { this.returnLowestNonStopFare = v; }
        public Boolean getReturnFullOffers()                 { return returnFullOffers; }
        public void setReturnFullOffers(Boolean v)           { this.returnFullOffers = v; }
        public String  getReturnMode()                       { return returnMode; }
        public void setReturnMode(String v)                  { this.returnMode = v; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Sources {
        private List<String> providers;
        private List<String> distributionModels;

        public List<String> getProviders()          { return providers; }
        public void setProviders(List<String> v)    { this.providers = v; }
        public List<String> getDistributionModels() { return distributionModels; }
        public void setDistributionModels(List<String> v){ this.distributionModels = v; }
    }
}
