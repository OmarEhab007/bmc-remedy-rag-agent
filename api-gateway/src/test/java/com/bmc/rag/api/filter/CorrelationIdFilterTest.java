package com.bmc.rag.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.MDC;

import java.io.IOException;
import java.security.Principal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CorrelationIdFilter.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Principal principal;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Nested
    @DisplayName("Correlation ID Generation Tests")
    class CorrelationIdGenerationTests {

        @Test
        @DisplayName("doFilterInternal_noCorrelationIdInHeader_generatesNewId")
        void doFilterInternal_noCorrelationIdInHeader_generatesNewId() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            assertThat(correlationId).isNotNull();
            assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("doFilterInternal_existingCorrelationIdInHeader_usesExisting")
        void doFilterInternal_existingCorrelationIdInHeader_usesExisting() throws ServletException, IOException {
            String existingId = "test-correlation-id-123";
            when(request.getHeader("X-Correlation-ID")).thenReturn(existingId);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader("X-Correlation-ID", existingId);
        }

        @Test
        @DisplayName("doFilterInternal_blankCorrelationIdInHeader_generatesNewId")
        void doFilterInternal_blankCorrelationIdInHeader_generatesNewId() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("   ");

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            assertThat(correlationId).isNotBlank();
            assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }

    @Nested
    @DisplayName("MDC Population Tests")
    class MdcPopulationTests {

        @Test
        @DisplayName("doFilterInternal_withCorrelationId_addsMdcEntry")
        void doFilterInternal_withCorrelationId_addsMdcEntry() throws ServletException, IOException {
            String correlationId = "test-id-123";
            when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);

            doAnswer(invocation -> {
                // Check MDC inside filter chain execution
                assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            // MDC should be cleaned up after execution
            assertThat(MDC.get("correlationId")).isNull();
        }

        @Test
        @DisplayName("doFilterInternal_withSessionId_addsMdcEntry")
        void doFilterInternal_withSessionId_addsMdcEntry() throws ServletException, IOException {
            String sessionId = "session-123";
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getHeader("X-Session-Id")).thenReturn(sessionId);

            doAnswer(invocation -> {
                assertThat(MDC.get("sessionId")).isEqualTo(sessionId);
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("sessionId")).isNull();
        }

        @Test
        @DisplayName("doFilterInternal_withAuthenticatedUser_addsMdcEntry")
        void doFilterInternal_withAuthenticatedUser_addsMdcEntry() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getUserPrincipal()).thenReturn(principal);
            when(principal.getName()).thenReturn("user@example.com");

            doAnswer(invocation -> {
                assertThat(MDC.get("userId")).isEqualTo("user@example.com");
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("doFilterInternal_noSessionId_doesNotAddSessionMdc")
        void doFilterInternal_noSessionId_doesNotAddSessionMdc() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getHeader("X-Session-Id")).thenReturn(null);

            doAnswer(invocation -> {
                assertThat(MDC.get("sessionId")).isNull();
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);
        }

        @Test
        @DisplayName("doFilterInternal_blankSessionId_doesNotAddSessionMdc")
        void doFilterInternal_blankSessionId_doesNotAddSessionMdc() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getHeader("X-Session-Id")).thenReturn("   ");

            doAnswer(invocation -> {
                assertThat(MDC.get("sessionId")).isNull();
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);
        }

        @Test
        @DisplayName("doFilterInternal_noAuthenticatedUser_doesNotAddUserMdc")
        void doFilterInternal_noAuthenticatedUser_doesNotAddUserMdc() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getUserPrincipal()).thenReturn(null);

            doAnswer(invocation -> {
                assertThat(MDC.get("userId")).isNull();
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);
        }
    }

    @Nested
    @DisplayName("MDC Cleanup Tests")
    class MdcCleanupTests {

        @Test
        @DisplayName("doFilterInternal_afterExecution_removesMdcEntries")
        void doFilterInternal_afterExecution_removesMdcEntries() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getHeader("X-Session-Id")).thenReturn("session-123");
            when(request.getUserPrincipal()).thenReturn(principal);
            when(principal.getName()).thenReturn("user@example.com");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("correlationId")).isNull();
            assertThat(MDC.get("sessionId")).isNull();
            assertThat(MDC.get("userId")).isNull();
        }

        @Test
        @DisplayName("doFilterInternal_filterChainThrows_stillCleansMdc")
        void doFilterInternal_filterChainThrows_stillCleansMdc() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");
            when(request.getHeader("X-Session-Id")).thenReturn("session-123");
            doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ServletException.class);

            // MDC should still be cleaned up
            assertThat(MDC.get("correlationId")).isNull();
            assertThat(MDC.get("sessionId")).isNull();
        }
    }

    @Nested
    @DisplayName("Correlation ID Sanitization Tests")
    class CorrelationIdSanitizationTests {

        @Test
        @DisplayName("doFilterInternal_crlfInjectionAttempt_generatesNewUuid")
        void doFilterInternal_crlfInjectionAttempt_generatesNewUuid() throws ServletException, IOException {
            // CRLF injection: attacker tries to inject a new header via \r\n
            when(request.getHeader("X-Correlation-ID")).thenReturn("evil\r\nX-Injected: true");

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            // Control chars stripped leaves "evilX-Injected: true" which has invalid chars (space, colon)
            // so a UUID should be generated
            assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("doFilterInternal_specialCharsInCorrelationId_generatesNewUuid")
        void doFilterInternal_specialCharsInCorrelationId_generatesNewUuid() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("id with spaces & symbols!");

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("doFilterInternal_tooLongCorrelationId_truncatesTo64Chars")
        void doFilterInternal_tooLongCorrelationId_truncatesTo64Chars() throws ServletException, IOException {
            String longId = "a".repeat(100);
            when(request.getHeader("X-Correlation-ID")).thenReturn(longId);

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            assertThat(correlationId).hasSize(64);
            assertThat(correlationId).isEqualTo("a".repeat(64));
        }

        @Test
        @DisplayName("doFilterInternal_validCorrelationIdWithAllowedChars_passesThrough")
        void doFilterInternal_validCorrelationIdWithAllowedChars_passesThrough() throws ServletException, IOException {
            String validId = "req-123_test.abc";
            when(request.getHeader("X-Correlation-ID")).thenReturn(validId);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setHeader("X-Correlation-ID", validId);
        }

        @Test
        @DisplayName("doFilterInternal_onlyControlChars_generatesNewUuid")
        void doFilterInternal_onlyControlChars_generatesNewUuid() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("\r\n\t");

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(response).setHeader(eq("X-Correlation-ID"), captor.capture());

            String correlationId = captor.getValue();
            assertThat(correlationId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }

    @Nested
    @DisplayName("Filter Chain Execution Tests")
    class FilterChainExecutionTests {

        @Test
        @DisplayName("doFilterInternal_normalExecution_callsFilterChain")
        void doFilterInternal_normalExecution_callsFilterChain() throws ServletException, IOException {
            when(request.getHeader("X-Correlation-ID")).thenReturn("corr-123");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("doFilterInternal_allHeadersPresent_populatesAllMdcAndCallsFilterChain")
        void doFilterInternal_allHeadersPresent_populatesAllMdcAndCallsFilterChain() throws ServletException, IOException {
            String correlationId = "corr-123";
            String sessionId = "session-456";
            String userId = "user@example.com";

            when(request.getHeader("X-Correlation-ID")).thenReturn(correlationId);
            when(request.getHeader("X-Session-Id")).thenReturn(sessionId);
            when(request.getUserPrincipal()).thenReturn(principal);
            when(principal.getName()).thenReturn(userId);

            doAnswer(invocation -> {
                assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
                assertThat(MDC.get("sessionId")).isEqualTo(sessionId);
                assertThat(MDC.get("userId")).isEqualTo(userId);
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response).setHeader("X-Correlation-ID", correlationId);
        }
    }
}
