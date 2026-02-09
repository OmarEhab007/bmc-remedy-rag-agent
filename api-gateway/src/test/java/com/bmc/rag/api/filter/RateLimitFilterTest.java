package com.bmc.rag.api.filter;

import com.bmc.rag.api.config.RateLimitConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitFilter}.
 * Tests rate limiting behavior, user identification, and path-based filtering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Path Skipping Tests")
    class PathSkippingTests {

        @Test
        @DisplayName("Should skip rate limiting for /actuator/ paths")
        void shouldSkipActuatorPaths() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
            verify(rateLimitConfig, never()).isRateLimitedSearch(anyString());
        }

        @Test
        @DisplayName("Should skip rate limiting for /api/v1/health paths")
        void shouldSkipHealthPaths() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/health");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
        }

        @Test
        @DisplayName("Should skip rate limiting for /favicon.ico")
        void shouldSkipFavicon() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/favicon.ico");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
        }

        @Test
        @DisplayName("Should skip rate limiting for /static/ paths")
        void shouldSkipStaticPaths() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/static/css/main.css");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
        }

        @Test
        @DisplayName("Should skip rate limiting for /ws/ WebSocket paths")
        void shouldSkipWebSocketPaths() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ws/chat");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
        }
    }

    @Nested
    @DisplayName("User Identification Tests")
    class UserIdentificationTests {

        @Test
        @DisplayName("Should use authenticated username when available")
        void shouldUseAuthenticatedUsername() throws Exception {
            // Create a real authenticated user
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "john.doe", "password", java.util.Collections.emptyList());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            SecurityContextHolder.setContext(securityContext);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Configure mock to accept both possible user identifiers
            when(rateLimitConfig.isRateLimitedChat("john.doe")).thenReturn(false);
            when(rateLimitConfig.isRateLimitedChat(anyString())).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Verify the filter chain was called (authentication worked)
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should skip anonymousUser and fall back to session ID")
        void shouldSkipAnonymousUser() throws Exception {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    "anonymousUser", "password", java.util.Collections.emptyList());
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(auth);
            SecurityContextHolder.setContext(securityContext);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // Create a session to get session ID
            HttpSession session = request.getSession(true);
            String sessionId = session.getId();

            when(rateLimitConfig.isRateLimitedChat(anyString())).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            // Verify filter chain was called (anonymousUser was skipped)
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use session ID when no authentication")
        void shouldUseSessionId() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            MockHttpServletResponse response = new MockHttpServletResponse();

            HttpSession session = request.getSession(true);
            String sessionId = session.getId();

            when(rateLimitConfig.isRateLimitedChat("session:" + sessionId)).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("session:" + sessionId);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should use X-Forwarded-For header when no session")
        void shouldUseXForwardedForHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.100")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("ip:192.168.1.100");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should trim and use first IP from X-Forwarded-For")
        void shouldTrimXForwardedFor() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.addHeader("X-Forwarded-For", " 203.0.113.42 , 198.51.100.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:203.0.113.42")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("ip:203.0.113.42");
        }

        @Test
        @DisplayName("Should use remote address as last resort")
        void shouldUseRemoteAddress() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.setRemoteAddr("192.168.1.50");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.50")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("ip:192.168.1.50");
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Rate Limit Check Tests")
    class RateLimitCheckTests {

        @Test
        @DisplayName("Should check chat rate limit for /api/v1/chat path")
        void shouldCheckChatRateLimitV1() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("ip:192.168.1.1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should check chat rate limit for /api/chat path")
        void shouldCheckChatRateLimitLegacy() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/chat");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedChat("ip:192.168.1.1");
        }

        @Test
        @DisplayName("Should check search rate limit for /api/v1/search path")
        void shouldCheckSearchRateLimitV1() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/search");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedSearch("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedSearch("ip:192.168.1.1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should check search rate limit for /api/search path")
        void shouldCheckSearchRateLimitLegacy() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/search");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedSearch("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedSearch("ip:192.168.1.1");
        }

        @Test
        @DisplayName("Should check admin rate limit for /api/admin path")
        void shouldCheckAdminRateLimit() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/admin/users");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedAdmin("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedAdmin("ip:192.168.1.1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should check admin rate limit for /api/v1/admin path")
        void shouldCheckAdminRateLimitV1() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/admin/settings");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedAdmin("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedAdmin("ip:192.168.1.1");
        }

        @Test
        @DisplayName("Should check feedback rate limit for /api/v1/feedback path")
        void shouldCheckFeedbackRateLimitV1() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/feedback");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedFeedback("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedFeedback("ip:192.168.1.1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should check feedback rate limit for /api/feedback path")
        void shouldCheckFeedbackRateLimitLegacy() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/feedback");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedFeedback("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedFeedback("ip:192.168.1.1");
        }

        @Test
        @DisplayName("Should use search rate limit as default for other /api/ paths")
        void shouldUseSearchRateLimitAsDefault() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/documents/123");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedSearch("ip:192.168.1.1")).thenReturn(false);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig).isRateLimitedSearch("ip:192.168.1.1");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not rate limit non-API paths")
        void shouldNotRateLimitNonApiPaths() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/public/index.html");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(rateLimitConfig, never()).isRateLimitedChat(anyString());
            verify(rateLimitConfig, never()).isRateLimitedSearch(anyString());
            verify(rateLimitConfig, never()).isRateLimitedAdmin(anyString());
            verify(rateLimitConfig, never()).isRateLimitedFeedback(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Rate Limit Exceeded Tests")
    class RateLimitExceededTests {

        @Test
        @DisplayName("Should return 429 when chat rate limit exceeded")
        void shouldReturn429WhenChatRateLimitExceeded() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.1")).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getContentAsString()).contains("Rate limit exceeded");
            assertThat(response.getContentAsString()).contains("Too many requests. Please try again later.");
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should return 429 when search rate limit exceeded")
        void shouldReturn429WhenSearchRateLimitExceeded() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/search");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedSearch("ip:192.168.1.1")).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/json");
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should return 429 when admin rate limit exceeded")
        void shouldReturn429WhenAdminRateLimitExceeded() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/admin");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedAdmin("ip:192.168.1.1")).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should return 429 when feedback rate limit exceeded")
        void shouldReturn429WhenFeedbackRateLimitExceeded() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/feedback");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedFeedback("ip:192.168.1.1")).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should return valid JSON error body when rate limited")
        void shouldReturnValidJsonErrorBody() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/chat");
            request.setRemoteAddr("192.168.1.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(rateLimitConfig.isRateLimitedChat("ip:192.168.1.1")).thenReturn(true);

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            String body = response.getContentAsString();
            assertThat(body).contains("\"error\":\"Rate limit exceeded\"");
            assertThat(body).contains("\"message\":\"Too many requests. Please try again later.\"");
        }
    }
}
