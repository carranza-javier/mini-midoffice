package com.jcarranza.minimidoffice.web.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Logs every request to /api/* with method, URI, HTTP status, and duration.
 * Useful for integration diagnostics and 2nd-level support (Phases 8/9).
 *
 * Ejemplo de log:
 *   [REQUEST] POST /api/bookings → 201 (847 ms)
 *   [REQUEST] GET  /api/flights/search?origin=JFK → 200 (1203 ms)
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void init(FilterConfig config) {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req   = (HttpServletRequest)  servletRequest;
        HttpServletResponse resp  = (HttpServletResponse) servletResponse;

        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, resp);
        } finally {
            long ms      = System.currentTimeMillis() - start;
            String query = req.getQueryString() != null ? "?" + req.getQueryString() : "";
            log.info("[REQUEST] {} {}{} → {} ({} ms)",
                req.getMethod(), req.getRequestURI(), query, resp.getStatus(), ms);
        }
    }

    @Override
    public void destroy() {}
}
