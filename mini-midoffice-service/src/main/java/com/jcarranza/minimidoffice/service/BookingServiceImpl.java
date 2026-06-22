package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.domain.enums.BookingStatus;
import com.jcarranza.minimidoffice.domain.enums.GdsProvider;
import com.jcarranza.minimidoffice.domain.exception.BusinessException;
import com.jcarranza.minimidoffice.domain.model.Booking;
import com.jcarranza.minimidoffice.domain.model.TravellerProfile;
import com.jcarranza.minimidoffice.integration.port.FlightCheckRequest;
import com.jcarranza.minimidoffice.integration.port.FlightCheckResult;
import com.jcarranza.minimidoffice.integration.port.GdsFlightCheckPort;
import com.jcarranza.minimidoffice.persistence.dao.BookingDao;
import com.jcarranza.minimidoffice.persistence.dao.TravellerProfileDao;
import com.jcarranza.minimidoffice.service.dto.BookingDTO;
import com.jcarranza.minimidoffice.service.dto.BookingReserveRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Booking flow: real-time FlightCheck → persist RESERVED directly.
 *
 * Why TransactionTemplate instead of @Transactional on reserve():
 * - The FlightCheck HTTP call takes 2-5 s and must not hold a DB connection open.
 * - Phase 1 (verify traveller): short read transaction.
 * - Phase 2 (FlightCheck): no active transaction.
 * - Phase 3 (persist RESERVED): new REQUIRES_NEW transaction.
 */
@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log         = LoggerFactory.getLogger(BookingServiceImpl.class);
    private static final Logger businessLog = LoggerFactory.getLogger("com.jcarranza.minimidoffice.business");

    // Default 100.0 is intentional for the Sabre cert sandbox: BFM flight search and FlightCheck
    // draw prices from different unsynced datasets, producing a typical 3-4x difference that would
    // always fail a tight tolerance. In production (live Sabre inventory), both APIs share the same
    // pricing source — override with booking.priceTolerance=1.05 for a 5% production tolerance.
    private final BigDecimal PRICE_TOLERANCE;

    private final BookingDao                 bookingDao;
    private final TravellerProfileDao        profileDao;
    private final GdsFlightCheckPort         flightCheckPort;
    private final PlatformTransactionManager transactionManager;
    private final String                     pseudoCityCode;

    private final TransactionTemplate readTemplate;
    private final TransactionTemplate writeTemplate;

    public BookingServiceImpl(BookingDao bookingDao,
                               TravellerProfileDao profileDao,
                               GdsFlightCheckPort flightCheckPort,
                               PlatformTransactionManager transactionManager,
                               @Value("${sabre.pseudoCityCode:PC18}") String pseudoCityCode,
                               @Value("${booking.priceTolerance:100.0}") BigDecimal priceTolerance) {
        this.bookingDao         = bookingDao;
        this.profileDao         = profileDao;
        this.flightCheckPort    = flightCheckPort;
        this.transactionManager = transactionManager;
        this.pseudoCityCode     = pseudoCityCode;
        this.PRICE_TOLERANCE    = priceTolerance;

        this.readTemplate = new TransactionTemplate(transactionManager);
        this.readTemplate.setReadOnly(true);

        this.writeTemplate = new TransactionTemplate(transactionManager);
        this.writeTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public BookingDTO reserve(BookingReserveRequest request) {
        validateReserveRequest(request);

        String currency = request.getCurrencyCode() != null ? request.getCurrencyCode() : "EUR";

        // Parse flightKey early — fail fast before touching DB or GDS if the format is invalid
        FlightCheckRequest checkRequest = FlightCheckRequest.fromFlightKey(
            request.getFlightKey(), currency, pseudoCityCode);

        // ── Phase 1: Verify traveller exists ── short read transaction ──
        readTemplate.execute(status -> {
            profileDao.findById(request.getTravellerId())
                .orElseThrow(() -> new BusinessException(
                    "Traveller not found: " + request.getTravellerId()));
            return null;
        });
        // Read TX has committed here. DB connection is released.

        // ── Phase 2: FlightCheck ── no active transaction, no DB connection held ──
        businessLog.info("FLIGHT_CHECK_START travellerId={} flightKey={}",
            request.getTravellerId(), request.getFlightKey());
        FlightCheckResult checkResult = flightCheckPort.check(checkRequest);

        // ── Phase 3: Business validations ── outside any transaction ──
        if (!checkResult.isAvailable()) {
            throw new BusinessException(
                "Flight is no longer available: " + request.getFlightKey());
        }
        if (checkResult.getTotalPrice() != null && request.getSearchedPrice() != null) {
            BigDecimal maxAcceptable = request.getSearchedPrice().multiply(PRICE_TOLERANCE);
            if (checkResult.getTotalPrice().compareTo(maxAcceptable) > 0) {
                throw new BusinessException(String.format(
                    "Price exceeds tolerance. Searched: %s %s, confirmed: %s %s",
                    request.getSearchedPrice(), currency,
                    checkResult.getTotalPrice(), checkResult.getCurrencyCode()));
            }
        }

        // ── Phase 4: Persist RESERVED ── new REQUIRES_NEW transaction ──
        final BigDecimal confirmedPrice = checkResult.getTotalPrice();
        final String     confirmedCcy   = checkResult.getCurrencyCode();
        return writeTemplate.execute(status -> {
            TravellerProfile traveller = profileDao.findById(request.getTravellerId())
                .orElseThrow(() -> new BusinessException(
                    "Traveller not found: " + request.getTravellerId()));
            Booking booking = new Booking();
            booking.setTraveller(traveller);
            booking.setOrigin(checkRequest.getOriginCode());
            booking.setDestination(checkRequest.getDestinationCode());
            booking.setFlightKey(request.getFlightKey());
            booking.setDepartureDate(checkRequest.getDepartureDate());
            booking.setSearchedPrice(request.getSearchedPrice());
            booking.setConfirmedPrice(confirmedPrice);
            booking.setProvider(GdsProvider.SABRE);
            booking.setStatus(BookingStatus.RESERVED);
            booking.setReservationDate(LocalDateTime.now());
            bookingDao.save(booking);
            businessLog.info("BOOKING_RESERVED bookingId={} travellerId={} flight={} confirmedPrice={} {}",
                booking.getId(), request.getTravellerId(), request.getFlightKey(), confirmedPrice, confirmedCcy);
            return toDTO(booking);
        });
    }

    @Override
    @Transactional
    public BookingDTO cancel(Long bookingId) {
        Booking booking = bookingDao.findById(bookingId)
            .orElseThrow(() -> new BusinessException("Booking not found: " + bookingId));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Booking is already cancelled: " + bookingId, 409);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        businessLog.info("BOOKING_CANCELLED bookingId={} travellerId={}",
            bookingId, booking.getTraveller() != null ? booking.getTraveller().getId() : "?");
        return toDTO(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDTO findById(Long bookingId) {
        return bookingDao.findById(bookingId)
            .map(this::toDTO)
            .orElseThrow(() -> new BusinessException("Booking not found: " + bookingId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDTO> findByTraveller(Long travellerId) {
        profileDao.findById(travellerId)
            .orElseThrow(() -> new BusinessException("Traveller not found: " + travellerId));
        return bookingDao.findByTravellerId(travellerId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private BookingDTO toDTO(Booking b) {
        TravellerProfile t = b.getTraveller();
        String fullName = t != null ? t.getFirstName() + " " + t.getLastName() : null;
        return new BookingDTO(
            b.getId(),
            t != null ? t.getId() : null,
            fullName,
            b.getOrigin(),
            b.getDestination(),
            b.getFlightKey(),
            b.getDepartureDate(),
            b.getSearchDate(),
            b.getReservationDate(),
            b.getSearchedPrice(),
            b.getConfirmedPrice(),
            b.getProvider()  != null ? b.getProvider().name()  : null,
            b.getStatus()    != null ? b.getStatus().name()    : null
        );
    }

    private static void validateReserveRequest(BookingReserveRequest req) {
        if (req.getFlightKey() == null || req.getFlightKey().isBlank()) {
            throw new BusinessException("flightKey is required");
        }
        if (req.getTravellerId() == null) {
            throw new BusinessException("travellerId is required");
        }
    }
}
