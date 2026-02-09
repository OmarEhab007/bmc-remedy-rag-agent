package com.bmc.rag.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Servlet filter that assigns a unique correlation ID to every inbound request.
 * <p>
 * The correlation ID is stored in the SLF4J MDC so that every log statement
 * emitted while processing the request automatically includes it. The same
 * value is returned to the caller via the {@code X-Correlation-ID} response
 * header, making it straightforward to trace a request across client and
 * server logs.
 * <p>
 * Runs at {@link Ordered#HIGHEST_PRECEDENCE} to ensure the correlation ID
 * is available before any other filter or controller logic executes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String SESSION_ID_HEADER = "X-Session-Id";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String SESSION_ID_MDC_KEY = "sessionId";
    private static final String USER_ID_MDC_KEY = "userId";

    private static final int MAX_CORRELATION_ID_LENGTH = 64;
    private static final Pattern VALID_CORRELATION_ID_PATTERN = Pattern.compile("[a-zA-Z0-9\\-_.]+");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            } else {
                correlationId = sanitizeCorrelationId(correlationId);
            }
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Populate sessionId from request header (used by chat/agentic endpoints)
            String sessionId = request.getHeader(SESSION_ID_HEADER);
            if (sessionId != null && !sessionId.isBlank()) {
                MDC.put(SESSION_ID_MDC_KEY, sessionId);
            }

            // Populate userId from authenticated principal if available
            if (request.getUserPrincipal() != null) {
                MDC.put(USER_ID_MDC_KEY, request.getUserPrincipal().getName());
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY);
            MDC.remove(SESSION_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }

    /**
     * Sanitizes a client-provided correlation ID to prevent CRLF header injection.
     * <p>
     * Strips control characters, validates against allowed character set
     * {@code [a-zA-Z0-9\-_.]}, and enforces a maximum length of 64 characters.
     * Returns a generated UUID if the sanitized value is empty or the original
     * contained only invalid characters.
     */
    private String sanitizeCorrelationId(String correlationId) {
        // Strip control characters (including CR, LF, tab, etc.)
        String sanitized = correlationId.replaceAll("[\\p{Cntrl}]", "");

        // Truncate to max length
        if (sanitized.length() > MAX_CORRELATION_ID_LENGTH) {
            sanitized = sanitized.substring(0, MAX_CORRELATION_ID_LENGTH);
        }

        // Validate against allowed pattern
        if (sanitized.isEmpty() || !VALID_CORRELATION_ID_PATTERN.matcher(sanitized).matches()) {
            return UUID.randomUUID().toString();
        }

        return sanitized;
    }
}
