package com.jcarranza.minimidoffice.service;

import com.jcarranza.minimidoffice.service.dto.CreateProfileRequest;
import com.jcarranza.minimidoffice.service.dto.ProfileDTO;
import com.jcarranza.minimidoffice.service.dto.UpdateProfileRequest;

import java.util.List;

public interface ProfileService {

    ProfileDTO create(CreateProfileRequest request);

    ProfileDTO update(Long id, UpdateProfileRequest request);

    ProfileDTO findById(Long id);

    List<ProfileDTO> findAll(int offset, int limit);

    /** Searches by name, last name, email, or company (case-insensitive). */
    List<ProfileDTO> search(String term, int offset, int limit);

    void delete(Long id);
}
