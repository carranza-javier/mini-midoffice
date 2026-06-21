package com.jcarranza.minimidoffice.web.advice;

import com.jcarranza.minimidoffice.domain.exception.BusinessException;
import com.jcarranza.minimidoffice.domain.exception.IntegrationException;
import com.jcarranza.minimidoffice.web.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;

/**
 * Translates domain and integration exceptions to HTTP responses with a standard JSON body.
 *
 * Mapping:
 *   BusinessException(400)  → 400 Bad Request    (validation, "not found", "not available")
 *   BusinessException(409)  → 409 Conflict       (duplicate email, already cancelled)
 *   IntegrationException(-1)→ 504 Gateway Timeout (I/O error / timeout calling GDS)
 *   IntegrationException(N) → 502 Bad Gateway    (HTTP error from GDS)
 *   DataIntegrityViolation  → 409 Conflict       (FK restrict in DB, email unique at DB level)
 *   invalid param           → 400 Bad Request
 *   Exception               → 500 Internal Server Error
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log      = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    // Unhandled 500s go to errors.log (routed in logback.xml by logger name)
    private static final Logger errorLog = LoggerFactory.getLogger("com.jcarranza.minimidoffice.errors");

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        int status = ex.getHttpStatus();
        log.warn("Business rule violation [{}] on {} {}: {}",
            status, req.getMethod(), req.getRequestURI(), ex.getMessage());
        return error(status, ex.getMessage(), req);
    }

    @ExceptionHandler(IntegrationException.class)
    public ResponseEntity<ApiError> handleIntegration(IntegrationException ex, HttpServletRequest req) {
        // httpStatus == -1 indicates a network error/timeout (GDS never sent an HTTP response)
        int status = (ex.getHttpStatus() == -1) ? 504 : 502;
        log.error("GDS integration error [{}] on {} {}: {}",
            status, req.getMethod(), req.getRequestURI(), ex.getMessage());
        return error(status, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                         HttpServletRequest req) {
        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation on {} {}: {}", req.getMethod(), req.getRequestURI(), detail);
        return error(409, "The operation conflicts with existing data: " + detail, req);
    }

    @ExceptionHandler({ MissingServletRequestParameterException.class,
                        HttpMessageNotReadableException.class,
                        MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ApiError> handleBadInput(Exception ex, HttpServletRequest req) {
        log.debug("Bad input on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return error(400, ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Logged to errors.log (via com.jcarranza.minimidoffice.errors logger) AND to the class logger.
        // The errors.log appender is routed in logback.xml: only ERROR level, full stack trace.
        String location = req.getMethod() + " " + req.getRequestURI();
        log.error("Unexpected error on {}", location, ex);
        errorLog.error("UNHANDLED_EXCEPTION on {}: {}", location, ex.getMessage(), ex);
        return error(500, "An unexpected error occurred. If the problem persists, contact support.", req);
    }

    private ResponseEntity<ApiError> error(int status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status)
            .body(new ApiError(status, httpStatusLabel(status), message, req.getRequestURI()));
    }

    private static String httpStatusLabel(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 409 -> "Conflict";
            case 502 -> "Bad Gateway";
            case 504 -> "Gateway Timeout";
            default  -> "Internal Server Error";
        };
    }
}
