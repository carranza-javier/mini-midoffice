package com.jcarranza.minimidoffice.domain.model;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "traveller_profile",
    indexes = {
        @Index(name = "idx_traveller_email",   columnList = "email"),
        @Index(name = "idx_traveller_company",  columnList = "company"),
        @Index(name = "idx_traveller_name",     columnList = "last_name, first_name")
    }
)
public class TravellerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "traveller_profile_seq")
    @SequenceGenerator(
        name           = "traveller_profile_seq",
        sequenceName   = "traveller_profile_id_seq",
        allocationSize = 1
    )
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Size(max = 200)
    @Column(name = "company", length = 200)
    private String company;

    @Size(max = 50)
    @Column(name = "passport_number", length = 50)
    private String passportNumber;

    @Size(max = 50)
    @Column(name = "frequent_flyer_number", length = 50)
    private String frequentFlyerNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TravellerProfile() {}

    @PrePersist
    void onPrePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long          getId()                           { return id; }

    public String        getFirstName()                    { return firstName; }
    public void          setFirstName(String firstName)    { this.firstName = firstName; }

    public String        getLastName()                     { return lastName; }
    public void          setLastName(String lastName)      { this.lastName = lastName; }

    public String        getEmail()                        { return email; }
    public void          setEmail(String email)            { this.email = email; }

    public String        getCompany()                      { return company; }
    public void          setCompany(String company)        { this.company = company; }

    public String        getPassportNumber()               { return passportNumber; }
    public void          setPassportNumber(String v)       { this.passportNumber = v; }

    public String        getFrequentFlyerNumber()          { return frequentFlyerNumber; }
    public void          setFrequentFlyerNumber(String v)  { this.frequentFlyerNumber = v; }

    public LocalDateTime getCreatedAt()                    { return createdAt; }
}
