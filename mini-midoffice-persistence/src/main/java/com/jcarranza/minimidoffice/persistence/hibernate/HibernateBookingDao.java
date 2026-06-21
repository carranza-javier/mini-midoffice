package com.jcarranza.minimidoffice.persistence.hibernate;

import com.jcarranza.minimidoffice.domain.enums.BookingStatus;
import com.jcarranza.minimidoffice.domain.model.Booking;
import com.jcarranza.minimidoffice.persistence.dao.BookingDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HibernateBookingDao implements BookingDao {

    @Autowired
    private SessionFactory sessionFactory;

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return Optional.ofNullable(session().get(Booking.class, id));
    }

    @Override
    public List<Booking> findByTravellerId(Long travellerId) {
        return session()
            .createQuery(
                "FROM Booking b WHERE b.traveller.id = :travellerId " +
                "ORDER BY b.searchDate DESC",
                Booking.class)
            .setParameter("travellerId", travellerId)
            .list();
    }

    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        return session()
            .createQuery(
                "FROM Booking b WHERE b.status = :status ORDER BY b.searchDate DESC",
                Booking.class)
            .setParameter("status", status)
            .list();
    }

    @Override
    public void save(Booking booking) {
        session().save(booking);
    }

    @Override
    public void update(Booking booking) {
        session().update(booking);
    }
}
