package com.jcarranza.minimidoffice.integration.port;

import java.math.BigDecimal;
import java.util.List;

/** Result of GdsFlightCheckPort — real-time price and availability. */
public class FlightCheckResult {

    private final boolean          available;
    private final BigDecimal       totalPrice;
    private final BigDecimal       baseFare;
    private final BigDecimal       taxes;
    private final String           currencyCode;
    private final List<FareOption> additionalOffers;

    private FlightCheckResult(Builder b) {
        this.available       = b.available;
        this.totalPrice      = b.totalPrice;
        this.baseFare        = b.baseFare;
        this.taxes           = b.taxes;
        this.currencyCode    = b.currencyCode;
        this.additionalOffers = b.additionalOffers;
    }

    public boolean          isAvailable()        { return available; }
    public BigDecimal       getTotalPrice()       { return totalPrice; }
    public BigDecimal       getBaseFare()         { return baseFare; }
    public BigDecimal       getTaxes()            { return taxes; }
    public String           getCurrencyCode()     { return currencyCode; }
    public List<FareOption> getAdditionalOffers() { return additionalOffers; }

    public static final class Builder {
        private boolean          available       = false;
        private BigDecimal       totalPrice;
        private BigDecimal       baseFare;
        private BigDecimal       taxes;
        private String           currencyCode    = "EUR";
        private List<FareOption> additionalOffers = List.of();

        public Builder available(boolean v)              { this.available = v;        return this; }
        public Builder totalPrice(BigDecimal v)          { this.totalPrice = v;       return this; }
        public Builder baseFare(BigDecimal v)            { this.baseFare = v;         return this; }
        public Builder taxes(BigDecimal v)               { this.taxes = v;            return this; }
        public Builder currencyCode(String v)            { this.currencyCode = v;     return this; }
        public Builder additionalOffers(List<FareOption> v){ this.additionalOffers = v; return this; }

        public FlightCheckResult build()                 { return new FlightCheckResult(this); }
    }

    /** An alternative fare option returned by Sabre alongside the primary offer. */
    public static class FareOption {
        private final BigDecimal totalPrice;
        private final BigDecimal baseFare;
        private final BigDecimal taxes;
        private final String     currencyCode;

        public FareOption(BigDecimal totalPrice, BigDecimal baseFare,
                          BigDecimal taxes, String currencyCode) {
            this.totalPrice   = totalPrice;
            this.baseFare     = baseFare;
            this.taxes        = taxes;
            this.currencyCode = currencyCode;
        }

        public BigDecimal getTotalPrice()   { return totalPrice; }
        public BigDecimal getBaseFare()     { return baseFare; }
        public BigDecimal getTaxes()        { return taxes; }
        public String     getCurrencyCode() { return currencyCode; }
    }
}
