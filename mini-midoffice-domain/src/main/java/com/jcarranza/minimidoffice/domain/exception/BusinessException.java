package com.jcarranza.minimidoffice.domain.exception;

public class BusinessException extends RuntimeException {

    private final int httpStatus;

    public BusinessException(String message) {
        super(message);
        this.httpStatus = 400;
    }

    /** Allows specifying 409 Conflict for state violations (duplicate email, already cancelled). */
    public BusinessException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = 400;
    }

    public int getHttpStatus() { return httpStatus; }
}
