package com.bmc.rag.api.filter;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to ensure ARServerUser ThreadLocal connections are properly cleaned up
 * after each HTTP request to prevent connection leaks.
 *
 * This filter runs last in the filter chain (lowest precedence) to ensure
 * all request processing is complete before cleanup.
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class ARContextCleanupFilter extends OncePerRequestFilter {

    private final ThreadLocalARContext arContext;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always cleanup ThreadLocal connection, even if request fails
            cleanupARContext(request);
        }
    }

    private void cleanupARContext(HttpServletRequest request) {
        try {
            if (arContext.isEnabled()) {
                arContext.closeContext();
                log.debug("Cleaned up AR context for request: {} {}",
                    request.getMethod(), request.getRequestURI());
            }
        } catch (Exception e) {
            log.warn("Error cleaning up AR context for request {} {}: {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip cleanup for non-API endpoints that don't use AR connections
        return path.startsWith("/actuator/") ||
               path.startsWith("/static/") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/ws/");  // WebSocket has its own lifecycle
    }
}
