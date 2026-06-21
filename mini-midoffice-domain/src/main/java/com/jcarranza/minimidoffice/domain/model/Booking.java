package com.jcarranza.minimidoffice.domain.model;

import com.jcarranza.minimidoffice.domain.enums.BookingStatus;
import com.jcarranza.minimidoffice.domain.enums.GdsProvider;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "booking",
    indexes = {
        @Index(name = "idx_booking_traveller",      columnList = "traveller_id"),
        @Index(name = "idx_booking_status",         columnList = "status"),
        @Index(name = "idx_booking_origin_dest",    columnList = "origin, destination"),
        @Index(name = "idx_booking_departure_date", columnList = "departure_date"),
        @Index(name = "idx_booking_provider",       columnList = "provider")
    }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "booking_seq")
    @SequenceGenerator(
        name           = "booking_seq",
        sequenceName   = "booking_id_seq",
        allocationSize = 1
    )
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traveller_id", nullable = false)
    private TravellerProfile traveller;

    @NotNull
    @Size(max = 3)
    @Column(name = "origin", nullable = false, length = 3)
    private String origin;

    @NotNull
    @Size(max = 3)
    @Column(name = "destination", nullable = false, length = 3)
    private String destination;

    @Size(max = 200)
    @Column(name = "flight_key", length = 200)
    private String flightKey;

    /** Flight departure date — required for monthly reporting. */
    @Column(name = "departure_date")
    private LocalDate departureDate;

    /** Timestamp when the user executed the search. */
    @Column(name = "search_date", nullable = false)
    private LocalDateTime searchDate;

    /** Timestamp when the booking transitioned to RESERVED after the Flight Check. */
    @Column(name = "reservation_date")
    private LocalDateTime reservationDate;

    /** Price shown to the user during search (GdsFlightSearchPort — may be cached). */
    @Column(name = "searched_price", precision = 10, scale = 2)
    private BigDecimal searchedPrice;

    /** Price confirmed by GdsFlightCheckPort immediately before transitioning to RESERVED. */
    @Column(name = "confirmed_price", precision = 10, scale = 2)
    private BigDecimal confirmedPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private GdsProvider provider;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    public Booking() {}

    @PrePersist
    void onPrePersist() {
        if (this.searchDate == null) {
            this.searchDate = LocalDateTime.now();
        }
    }

    public Long             getId()                               { return id; }

    public TravellerProfile getTraveller()                        { return traveller; }
    public void             setTraveller(TravellerProfile v)      { this.traveller = v; }

    public String           getOrigin()                           { return origin; }
    public void             setOrigin(String origin)              { this.origin = origin; }

    public String           getDestination()                      { return destination; }
    public void             setDestination(String destination)    { this.destination = destination; }

    public String           getFlightKey()                        { return flightKey; }
    public void             setFlightKey(String flightKey)        { this.flightKey = flightKey; }

    public LocalDate        getDepartureDate()                    { return departureDate; }
    public void             setDepartureDate(LocalDate v)         { this.departureDate = v; }

    public LocalDateTime    getSearchDate()                       { return searchDate; }
    public void             setSearchDate(LocalDateTime v)        { this.searchDate = v; }

    public LocalDateTime    getReservationDate()                  { return reservationDate; }
    public void             setReservationDate(LocalDateTime v)   { this.reservationDate = v; }

    public BigDecimal       getSearchedPrice()                    { return searchedPrice; }
    public void             setSearchedPrice(BigDecimal v)        { this.searchedPrice = v; }

    public BigDecimal       getConfirmedPrice()                   { return confirmedPrice; }
    public void             setConfirmedPrice(BigDecimal v)       { this.confirmedPrice = v; }

    public GdsProvider      getProvider()                         { return provider; }
    public void             setProvider(GdsProvider provider)     { this.provider = provider; }

    public BookingStatus    getStatus()                           { return status; }
    public void             setStatus(BookingStatus status)       { this.status = status; }
}
