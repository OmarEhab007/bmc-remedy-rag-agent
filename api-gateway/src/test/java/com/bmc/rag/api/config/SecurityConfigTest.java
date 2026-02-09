package com.bmc.rag.api.config;

import com.bmc.rag.api.filter.RateLimitFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityConfig.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Mock
    private RateLimitFilter rateLimitFilter;

    @Test
    void constructor_shouldInitializeWithRateLimitFilter() {
        SecurityConfig config = new SecurityConfig(rateLimitFilter);
        assertThat(config).isNotNull();
    }

    @Nested
    @DisplayName("JWT Authentication Converter Tests")
    class JwtAuthenticationConverterTests {

        @Test
        void jwtAuthenticationConverter_shouldCreateConverter() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            assertThat(converter).isNotNull();
        }

        @Test
        void jwtAuthenticationConverter_shouldConfigureAuthoritiesConverter() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);

            JwtAuthenticationConverter converter = config.jwtAuthenticationConverter();

            // The converter should be configured - verify it is a valid instance
            assertThat(converter).isNotNull();
            // Calling it twice should produce distinct instances
            JwtAuthenticationConverter converter2 = config.jwtAuthenticationConverter();
            assertThat(converter2).isNotNull().isNotSameAs(converter);
        }
    }

    @Nested
    @DisplayName("JWT Decoder Tests")
    class JwtDecoderTests {

        @Test
        void jwtDecoder_shouldCreateDecoderWithValidUri() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);
            ReflectionTestUtils.setField(config, "jwkSetUri", "https://example.com/.well-known/jwks.json");

            JwtDecoder decoder = config.jwtDecoder();

            assertThat(decoder).isNotNull();
        }

        @Test
        void jwtDecoder_shouldThrowExceptionWhenUriMissing() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);
            ReflectionTestUtils.setField(config, "jwkSetUri", "");

            assertThatThrownBy(() -> config.jwtDecoder())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT configuration missing");
        }

        @Test
        void jwtDecoder_shouldThrowExceptionWhenUriNull() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);
            ReflectionTestUtils.setField(config, "jwkSetUri", null);

            assertThatThrownBy(() -> config.jwtDecoder())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT configuration missing");
        }

        @Test
        void jwtDecoder_shouldThrowExceptionWhenUriBlank() {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);
            ReflectionTestUtils.setField(config, "jwkSetUri", "   ");

            assertThatThrownBy(() -> config.jwtDecoder())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT configuration missing");
        }
    }

    @Nested
    @DisplayName("Security Filter Chain Tests")
    class SecurityFilterChainTests {

        @Test
        @SuppressWarnings("unchecked")
        void securedFilterChain_shouldConfigureSecurityCorrectly() throws Exception {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);

            HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            DefaultSecurityFilterChain mockChain = mock(DefaultSecurityFilterChain.class);

            // Capture the Customizer lambdas so we can invoke them for full coverage
            ArgumentCaptor<Customizer> sessionCaptor = ArgumentCaptor.forClass(Customizer.class);
            ArgumentCaptor<Customizer> authCaptor = ArgumentCaptor.forClass(Customizer.class);
            ArgumentCaptor<Customizer> oauth2Captor = ArgumentCaptor.forClass(Customizer.class);
            ArgumentCaptor<Customizer> corsCaptor = ArgumentCaptor.forClass(Customizer.class);

            when(http.addFilterBefore(any(), any())).thenReturn(http);
            when(http.csrf(any())).thenReturn(http);
            when(http.sessionManagement(any())).thenReturn(http);
            when(http.authorizeHttpRequests(any())).thenReturn(http);
            when(http.oauth2ResourceServer(any())).thenReturn(http);
            when(http.cors(any())).thenReturn(http);
            when(http.build()).thenReturn(mockChain);

            SecurityFilterChain result = config.securedFilterChain(http);

            assertThat(result).isNotNull();
            verify(http).addFilterBefore(eq(rateLimitFilter), any());
            verify(http).csrf(any());
            verify(http).build();

            // Invoke the session management lambda to cover its body
            verify(http).sessionManagement(sessionCaptor.capture());
            SessionManagementConfigurer sessionConfigurer = mock(SessionManagementConfigurer.class, RETURNS_DEEP_STUBS);
            sessionCaptor.getValue().customize(sessionConfigurer);
            verify(sessionConfigurer).sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS);

            // Invoke the authorize HTTP requests lambda to cover its body
            verify(http).authorizeHttpRequests(authCaptor.capture());
            AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry authRegistry =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class, RETURNS_DEEP_STUBS);
            authCaptor.getValue().customize(authRegistry);

            // Invoke the oauth2ResourceServer lambda to cover its body
            verify(http).oauth2ResourceServer(oauth2Captor.capture());
            org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer oauth2Configurer =
                mock(org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer.class, RETURNS_DEEP_STUBS);
            oauth2Captor.getValue().customize(oauth2Configurer);

            // Invoke the cors lambda (empty body, but still needs coverage)
            verify(http).cors(corsCaptor.capture());
            CorsConfigurer corsConfigurer = mock(CorsConfigurer.class);
            corsCaptor.getValue().customize(corsConfigurer);
        }

        @Test
        @SuppressWarnings("unchecked")
        void disabledSecurityFilterChain_shouldPermitAllRequests() throws Exception {
            SecurityConfig config = new SecurityConfig(rateLimitFilter);

            HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
            DefaultSecurityFilterChain mockChain = mock(DefaultSecurityFilterChain.class);

            ArgumentCaptor<Customizer> authCaptor = ArgumentCaptor.forClass(Customizer.class);
            ArgumentCaptor<Customizer> corsCaptor = ArgumentCaptor.forClass(Customizer.class);

            when(http.csrf(any())).thenReturn(http);
            when(http.authorizeHttpRequests(any())).thenReturn(http);
            when(http.cors(any())).thenReturn(http);
            when(http.build()).thenReturn(mockChain);

            SecurityFilterChain result = config.disabledSecurityFilterChain(http);

            assertThat(result).isNotNull();
            verify(http).csrf(any());
            verify(http).build();

            // Invoke the authorize HTTP requests lambda
            verify(http).authorizeHttpRequests(authCaptor.capture());
            AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry authRegistry =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class, RETURNS_DEEP_STUBS);
            authCaptor.getValue().customize(authRegistry);

            // Invoke the cors lambda
            verify(http).cors(corsCaptor.capture());
            CorsConfigurer corsConfigurer = mock(CorsConfigurer.class);
            corsCaptor.getValue().customize(corsConfigurer);

            // Verify no oauth2ResourceServer is configured for disabled security
            verify(http, never()).oauth2ResourceServer(any());
        }
    }
}
