package com.bmc.rag.api.filter;

import com.bmc.rag.api.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for per-user rate limiting.
 * Applies different rate limits based on endpoint type.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting for health endpoints and static resources
        if (shouldSkipRateLimiting(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get user identifier
        String userId = getUserIdentifier(request);

        // Check rate limit based on endpoint type
        boolean rateLimited = checkRateLimit(path, userId);

        if (rateLimited) {
            log.warn("Rate limit exceeded for user {} on path {}", userId, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests. Please try again later.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipRateLimiting(String path) {
        return path.startsWith("/actuator/") ||
               path.startsWith("/api/v1/health") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/static/") ||
               path.startsWith("/ws/");  // WebSocket has its own rate limiting
    }

    private String getUserIdentifier(HttpServletRequest request) {
        // Try to get authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        // Fall back to session ID or IP address
        String sessionId = request.getSession(false) != null ?
            request.getSession().getId() : null;

        if (sessionId != null) {
            return "session:" + sessionId;
        }

        // Use IP address as last resort (consider X-Forwarded-For header)
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private boolean checkRateLimit(String path, String userId) {
        if (path.startsWith("/api/v1/chat") || path.startsWith("/api/chat")) {
            return rateLimitConfig.isRateLimitedChat(userId);
        }

        if (path.startsWith("/api/v1/search") || path.startsWith("/api/search")) {
            return rateLimitConfig.isRateLimitedSearch(userId);
        }

        if (path.startsWith("/api/admin") || path.startsWith("/api/v1/admin")) {
            return rateLimitConfig.isRateLimitedAdmin(userId);
        }

        if (path.startsWith("/api/v1/feedback") || path.startsWith("/api/feedback")) {
            return rateLimitConfig.isRateLimitedFeedback(userId);
        }

        // Default: use search rate limit for other API endpoints
        if (path.startsWith("/api/")) {
            return rateLimitConfig.isRateLimitedSearch(userId);
        }

        return false;
    }
}
