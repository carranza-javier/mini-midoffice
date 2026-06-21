package com.jcarranza.minimidoffice.service.dto;

import java.time.LocalDateTime;

public class ProfileDTO {

    private final Long          id;
    private final String        firstName;
    private final String        lastName;
    private final String        email;
    private final String        company;
    private final String        passportNumber;
    private final String        frequentFlyerNumber;
    private final LocalDateTime createdAt;

    public ProfileDTO(Long id, String firstName, String lastName, String email,
                      String company, String passportNumber, String frequentFlyerNumber,
                      LocalDateTime createdAt) {
        this.id                  = id;
        this.firstName           = firstName;
        this.lastName            = lastName;
        this.email               = email;
        this.company             = company;
        this.passportNumber      = passportNumber;
        this.frequentFlyerNumber = frequentFlyerNumber;
        this.createdAt           = createdAt;
    }

    public Long          getId()                  { return id; }
    public String        getFirstName()            { return firstName; }
    public String        getLastName()             { return lastName; }
    public String        getFullName()             { return firstName + " " + lastName; }
    public String        getEmail()                { return email; }
    public String        getCompany()              { return company; }
    public String        getPassportNumber()       { return passportNumber; }
    public String        getFrequentFlyerNumber()  { return frequentFlyerNumber; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
}
