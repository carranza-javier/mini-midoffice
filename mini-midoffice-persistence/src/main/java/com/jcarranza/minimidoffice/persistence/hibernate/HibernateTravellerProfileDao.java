package com.jcarranza.minimidoffice.persistence.hibernate;

import com.jcarranza.minimidoffice.domain.model.TravellerProfile;
import com.jcarranza.minimidoffice.persistence.dao.TravellerProfileDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class HibernateTravellerProfileDao implements TravellerProfileDao {

    @Autowired
    private SessionFactory sessionFactory;

    private Session session() {
        return sessionFactory.getCurrentSession();
    }

    @Override
    public Optional<TravellerProfile> findById(Long id) {
        return Optional.ofNullable(session().get(TravellerProfile.class, id));
    }

    @Override
    public List<TravellerProfile> findAll(int offset, int limit) {
        return session()
            .createQuery(
                "FROM TravellerProfile ORDER BY lastName, firstName",
                TravellerProfile.class)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .list();
    }

    @Override
    public List<TravellerProfile> search(String term, int offset, int limit) {
        String pattern = "%" + term.toLowerCase() + "%";
        return session()
            .createQuery(
                "FROM TravellerProfile p " +
                "WHERE LOWER(p.firstName) LIKE :term " +
                "   OR LOWER(p.lastName)  LIKE :term " +
                "   OR LOWER(p.email)     LIKE :term " +
                "   OR LOWER(p.company)   LIKE :term " +
                "ORDER BY p.lastName, p.firstName",
                TravellerProfile.class)
            .setParameter("term", pattern)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .list();
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = session()
            .createQuery(
                "SELECT COUNT(p) FROM TravellerProfile p WHERE p.email = :email",
                Long.class)
            .setParameter("email", email)
            .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public void save(TravellerProfile profile) {
        session().save(profile);
    }

    @Override
    public void update(TravellerProfile profile) {
        session().update(profile);
    }

    @Override
    public void delete(Long id) {
        TravellerProfile profile = session().get(TravellerProfile.class, id);
        if (profile != null) {
            session().delete(profile);
        }
    }
}
