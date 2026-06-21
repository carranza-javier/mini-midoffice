package com.jcarranza.minimidoffice.service.dto;

public class CreateProfileRequest {

    private String firstName;
    private String lastName;
    private String email;
    private String company;
    private String passportNumber;
    private String frequentFlyerNumber;

    public String getFirstName()            { return firstName; }
    public void   setFirstName(String v)    { this.firstName = v; }

    public String getLastName()             { return lastName; }
    public void   setLastName(String v)     { this.lastName = v; }

    public String getEmail()                { return email; }
    public void   setEmail(String v)        { this.email = v; }

    public String getCompany()              { return company; }
    public void   setCompany(String v)      { this.company = v; }

    public String getPassportNumber()       { return passportNumber; }
    public void   setPassportNumber(String v){ this.passportNumber = v; }

    public String getFrequentFlyerNumber()  { return frequentFlyerNumber; }
    public void   setFrequentFlyerNumber(String v){ this.frequentFlyerNumber = v; }
}
