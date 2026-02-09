package com.bmc.rag.api.filter;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ARContextCleanupFilter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ARContextCleanupFilter Tests")
class ARContextCleanupFilterTest {

    @Mock
    private ThreadLocalARContext arContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ARContextCleanupFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ARContextCleanupFilter(arContext);
    }

    @Nested
    @DisplayName("doFilterInternal Tests")
    class DoFilterInternalTests {

        @Test
        @DisplayName("doFilterInternal_normalExecution_callsFilterChainAndCleanup")
        void doFilterInternal_normalExecution_callsFilterChainAndCleanup() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/chat");
            when(arContext.isEnabled()).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            InOrder inOrder = inOrder(filterChain, arContext);
            inOrder.verify(filterChain).doFilter(request, response);
            inOrder.verify(arContext).isEnabled();
            inOrder.verify(arContext).closeContext();
        }

        @Test
        @DisplayName("doFilterInternal_filterChainThrows_stillCleansUp")
        void doFilterInternal_filterChainThrows_stillCleansUp() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/chat");
            when(arContext.isEnabled()).thenReturn(true);
            doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessage("Test exception");

            verify(arContext).isEnabled();
            verify(arContext).closeContext();
        }

        @Test
        @DisplayName("doFilterInternal_arContextDisabled_doesNotCallCloseContext")
        void doFilterInternal_arContextDisabled_doesNotCallCloseContext() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/health");
            when(arContext.isEnabled()).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(arContext).isEnabled();
            verify(arContext, never()).closeContext();
        }

        @Test
        @DisplayName("doFilterInternal_cleanupThrowsException_logsWarningButDoesNotPropagate")
        void doFilterInternal_cleanupThrowsException_logsWarningButDoesNotPropagate() throws ServletException, IOException {
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/chat");
            when(arContext.isEnabled()).thenReturn(true);
            doThrow(new RuntimeException("Cleanup failed")).when(arContext).closeContext();

            // Should not throw exception - cleanup errors are logged
            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(arContext).closeContext();
        }
    }

    @Nested
    @DisplayName("shouldNotFilter Tests")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("shouldNotFilter_actuatorEndpoint_returnsTrue")
        void shouldNotFilter_actuatorEndpoint_returnsTrue() {
            when(request.getRequestURI()).thenReturn("/actuator/health");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("shouldNotFilter_staticResource_returnsTrue")
        void shouldNotFilter_staticResource_returnsTrue() {
            when(request.getRequestURI()).thenReturn("/static/css/style.css");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("shouldNotFilter_favicon_returnsTrue")
        void shouldNotFilter_favicon_returnsTrue() {
            when(request.getRequestURI()).thenReturn("/favicon.ico");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("shouldNotFilter_webSocketEndpoint_returnsTrue")
        void shouldNotFilter_webSocketEndpoint_returnsTrue() {
            when(request.getRequestURI()).thenReturn("/ws/chat");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("shouldNotFilter_apiEndpoint_returnsFalse")
        void shouldNotFilter_apiEndpoint_returnsFalse() {
            when(request.getRequestURI()).thenReturn("/api/v1/chat");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("shouldNotFilter_toolServerEndpoint_returnsFalse")
        void shouldNotFilter_toolServerEndpoint_returnsFalse() {
            when(request.getRequestURI()).thenReturn("/tool-server/incidents/search");

            boolean result = filter.shouldNotFilter(request);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("doFilterInternal_multipleRequests_eachCleanedUpIndependently")
        void doFilterInternal_multipleRequests_eachCleanedUpIndependently() throws ServletException, IOException {
            when(arContext.isEnabled()).thenReturn(true);

            // First request
            when(request.getMethod()).thenReturn("POST");
            when(request.getRequestURI()).thenReturn("/api/v1/chat");
            filter.doFilterInternal(request, response, filterChain);

            // Second request
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/search");
            filter.doFilterInternal(request, response, filterChain);

            // Verify cleanup called for each request
            verify(arContext, times(2)).isEnabled();
            verify(arContext, times(2)).closeContext();
        }
    }
}
