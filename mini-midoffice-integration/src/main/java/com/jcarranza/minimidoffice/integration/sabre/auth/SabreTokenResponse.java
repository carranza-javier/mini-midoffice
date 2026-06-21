package com.jcarranza.minimidoffice.integration.sabre.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SabreTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    public String getAccessToken() { return accessToken; }
    public String getTokenType()   { return tokenType; }
    public long   getExpiresIn()   { return expiresIn; }
}
