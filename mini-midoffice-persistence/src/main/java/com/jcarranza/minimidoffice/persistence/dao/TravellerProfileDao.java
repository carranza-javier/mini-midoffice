package com.jcarranza.minimidoffice.persistence.dao;

import com.jcarranza.minimidoffice.domain.model.TravellerProfile;

import java.util.List;
import java.util.Optional;

public interface TravellerProfileDao {

    Optional<TravellerProfile> findById(Long id);

    List<TravellerProfile> findAll(int offset, int limit);

    /** Searches by firstName, lastName, email, or company (LIKE case-insensitive). */
    List<TravellerProfile> search(String term, int offset, int limit);

    boolean existsByEmail(String email);

    void save(TravellerProfile profile);

    /** Re-attaches the detached entity and propagates the UPDATE on flush. */
    void update(TravellerProfile profile);

    void delete(Long id);
}
