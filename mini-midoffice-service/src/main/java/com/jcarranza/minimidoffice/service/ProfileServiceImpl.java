package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.domain.exception.BusinessException;
import com.jcarranza.minimidoffice.domain.model.TravellerProfile;
import com.jcarranza.minimidoffice.persistence.dao.TravellerProfileDao;
import com.jcarranza.minimidoffice.service.dto.CreateProfileRequest;
import com.jcarranza.minimidoffice.service.dto.ProfileDTO;
import com.jcarranza.minimidoffice.service.dto.UpdateProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log         = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private static final Logger businessLog = LoggerFactory.getLogger("com.jcarranza.minimidoffice.business");

    private final TravellerProfileDao profileDao;

    public ProfileServiceImpl(TravellerProfileDao profileDao) {
        this.profileDao = profileDao;
    }

    @Override
    @Transactional
    public ProfileDTO create(CreateProfileRequest request) {
        validateRequired(request.getFirstName(), "firstName");
        validateRequired(request.getLastName(),  "lastName");
        validateRequired(request.getEmail(),     "email");

        if (profileDao.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail(), 409);
        }

        TravellerProfile profile = new TravellerProfile();
        profile.setFirstName(request.getFirstName().trim());
        profile.setLastName(request.getLastName().trim());
        profile.setEmail(request.getEmail().trim().toLowerCase());
        profile.setCompany(request.getCompany());
        profile.setPassportNumber(request.getPassportNumber());
        profile.setFrequentFlyerNumber(request.getFrequentFlyerNumber());

        profileDao.save(profile);
        businessLog.info("PROFILE_CREATED travellerId={} email={} company={}",
            profile.getId(), profile.getEmail(), profile.getCompany());
        return toDTO(profile);
    }

    @Override
    @Transactional
    public ProfileDTO update(Long id, UpdateProfileRequest request) {
        TravellerProfile profile = profileDao.findById(id)
            .orElseThrow(() -> new BusinessException("Traveller not found: " + id));

        if (request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(profile.getEmail())
                && profileDao.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail(), 409);
        }

        if (request.getFirstName()           != null) profile.setFirstName(request.getFirstName().trim());
        if (request.getLastName()            != null) profile.setLastName(request.getLastName().trim());
        if (request.getEmail()               != null) profile.setEmail(request.getEmail().trim().toLowerCase());
        if (request.getCompany()             != null) profile.setCompany(request.getCompany());
        if (request.getPassportNumber()      != null) profile.setPassportNumber(request.getPassportNumber());
        if (request.getFrequentFlyerNumber() != null) profile.setFrequentFlyerNumber(request.getFrequentFlyerNumber());

        // Hibernate dirty checking persists the changes on commit — no explicit update() needed.
        businessLog.info("PROFILE_UPDATED travellerId={}", id);
        return toDTO(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO findById(Long id) {
        return profileDao.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new BusinessException("Traveller not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileDTO> findAll(int offset, int limit) {
        return profileDao.findAll(offset, limit).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileDTO> search(String term, int offset, int limit) {
        return profileDao.search(term, offset, limit).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        profileDao.findById(id)
            .orElseThrow(() -> new BusinessException("Traveller not found: " + id));
        profileDao.delete(id);
        businessLog.info("PROFILE_DELETED travellerId={}", id);
    }

    private ProfileDTO toDTO(TravellerProfile p) {
        return new ProfileDTO(
            p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(),
            p.getCompany(), p.getPassportNumber(), p.getFrequentFlyerNumber(),
            p.getCreatedAt()
        );
    }

    private static void validateRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("Field required: " + field);
        }
    }
}
