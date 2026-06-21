package com.jcarranza.minimidoffice.domain.exception;

public class IntegrationException extends RuntimeException {

    private final String provider;
    private final int    httpStatus;

    public IntegrationException(String provider, int httpStatus, String message) {
        super("[" + provider + "] HTTP " + httpStatus + " — " + message);
        this.provider   = provider;
        this.httpStatus = httpStatus;
    }

    public IntegrationException(String provider, String message, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider   = provider;
        this.httpStatus = -1;
    }

    public String getProvider()  { return provider; }
    public int    getHttpStatus() { return httpStatus; }
}
