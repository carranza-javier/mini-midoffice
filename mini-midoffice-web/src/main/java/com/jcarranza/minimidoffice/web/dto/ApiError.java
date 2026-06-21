package com.jcarranza.minimidoffice.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** Standard JSON body for all API error responses. */
public class ApiError {

    private final int           status;
    private final String        error;
    private final String        message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;
    private final String        path;

    public ApiError(int status, String error, String message, String path) {
        this.status    = status;
        this.error     = error;
        this.message   = message;
        this.timestamp = LocalDateTime.now();
        this.path      = path;
    }

    public int           getStatus()    { return status; }
    public String        getError()     { return error; }
    public String        getMessage()   { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String        getPath()      { return path; }
}
