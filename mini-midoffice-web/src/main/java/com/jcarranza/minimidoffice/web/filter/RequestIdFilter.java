package com.jcarranza.minimidoffice.web.filter;

import org.slf4j.MDC;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * Generates a unique requestId (UUID truncated to 8 hex chars) per HTTP request
 * and stores it in the SLF4J MDC under the key "requestId".
 *
 * With this ID in the MDC, every log line emitted while processing the request
 * (service, Sabre adapter, DAO) carries the same requestId.
 * Enables grep "a3f7b2c1" across all three log files to reconstruct the full
 * trace of a request that failed in production.
 *
 * The ID is removed from the MDC in the finally block — it never leaks to the
 * next thread (Tomcat reuses threads from its pool).
 */
public class RequestIdFilter implements Filter {

    private static final String MDC_KEY = "requestId";

    @Override
    public void init(FilterConfig config) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        MDC.put(MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public void destroy() {}
}
