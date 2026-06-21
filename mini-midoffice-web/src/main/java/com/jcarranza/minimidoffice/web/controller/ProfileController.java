package com.jcarranza.minimidoffice.web.controller;

import com.jcarranza.minimidoffice.service.ProfileService;
import com.jcarranza.minimidoffice.service.dto.CreateProfileRequest;
import com.jcarranza.minimidoffice.service.dto.ProfileDTO;
import com.jcarranza.minimidoffice.service.dto.UpdateProfileRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Traveller profile management.
 *
 * Base URL (with DispatcherServlet mapped to /api/*): /api/profiles
 *
 * GET  /api/profiles            — list (paginated, filterable with ?q=)
 * GET  /api/profiles/{id}       — profile by ID
 * POST /api/profiles            — create profile
 * PUT  /api/profiles/{id}       — update profile (non-null fields only)
 */
@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Lists profiles with optional pagination.
     * With ?q=text searches by name, last name, email, or company (case-insensitive).
     *
     * GET /api/profiles?offset=0&limit=20
     * GET /api/profiles?q=smith&offset=0&limit=20
     */
    @GetMapping
    public ResponseEntity<List<ProfileDTO>> list(
            @RequestParam(defaultValue = "0")  int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false)    String q) {

        List<ProfileDTO> result = (q != null && !q.isBlank())
            ? profileService.search(q.trim(), offset, limit)
            : profileService.findAll(offset, limit);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/profiles/{id}
     * 200 OK + ProfileDTO  |  400 si no existe (BusinessException lanzada por el servicio)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.findById(id));
    }

    /**
     * POST /api/profiles
     * Body: { "firstName":"Ana","lastName":"Roca","email":"ana@example.com" }
     * 201 Created + ProfileDTO  |  400 si faltan campos  |  409 si el email ya existe
     */
    @PostMapping
    public ResponseEntity<ProfileDTO> create(@RequestBody CreateProfileRequest request) {
        ProfileDTO created = profileService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/profiles/{id}
     * Body: fields to update (all optional). Only non-null fields are modified.
     * 200 OK + ProfileDTO  |  400 if not found  |  409 if the new email already exists
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProfileDTO> update(@PathVariable Long id,
                                              @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.update(id, request));
    }
}
