package com.jcarranza.minimidoffice.persistence.dao;

import com.jcarranza.minimidoffice.domain.enums.BookingStatus;
import com.jcarranza.minimidoffice.domain.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingDao {

    Optional<Booking> findById(Long id);

    List<Booking> findByTravellerId(Long travellerId);

    List<Booking> findByStatus(BookingStatus status);

    void save(Booking booking);

    /** Re-attaches the detached entity and propagates the UPDATE on flush. */
    void update(Booking booking);
}
