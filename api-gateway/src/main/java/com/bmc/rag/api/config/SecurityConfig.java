package com.bmc.rag.api.config;

import com.bmc.rag.api.filter.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the RAG API.
 * Implements OAuth2 Resource Server with JWT validation.
 *
 * For production, configure:
 * - spring.security.oauth2.resourceserver.jwt.issuer-uri (OIDC discovery)
 * - Or spring.security.oauth2.resourceserver.jwt.jwk-set-uri (direct JWKS)
 *
 * For development/testing, set security.enabled=false to disable authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Security configuration when security is ENABLED.
     * Requires JWT authentication for protected endpoints.
     */
    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securedFilterChain(HttpSecurity http) throws Exception {
        http
            // Add per-user rate limiting filter
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()

                // Static admin files (dashboards)
                .requestMatchers("/admin/**").permitAll()

                // Metrics endpoints for dashboards (read-only, safe to expose)
                .requestMatchers("/api/v1/metrics/**").permitAll()

                // OpenAI-compatible API endpoints for Open WebUI (dev mode - no auth required)
                // These endpoints allow Open WebUI to connect without authentication.
                // For production, consider adding API key validation in a filter.
                .requestMatchers("/v1/**").permitAll()

                // Admin endpoints require ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // WebSocket endpoints
                .requestMatchers("/ws/**").authenticated()

                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // Any other request
                .anyRequest().authenticated()
            )

            // Configure OAuth2 Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            // Apply CORS configuration
            .cors(cors -> {});

        return http.build();
    }

    /**
     * Security configuration when security is DISABLED.
     * Allows all requests without authentication - USE ONLY FOR DEVELOPMENT/TESTING.
     */
    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "false")
    public SecurityFilterChain disabledSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .cors(cors -> {});
        return http.build();
    }

    /**
     * JWT Authentication Converter that extracts roles from JWT claims.
     * Expects roles in "roles" or "realm_access.roles" claim.
     */
    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Convert claim "roles" to Spring Security authorities with ROLE_ prefix
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * JWT Decoder bean - only created when security is enabled and JWK Set URI is configured.
     */
    @Bean
    @ConditionalOnProperty(name = "security.enabled", havingValue = "true", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new IllegalStateException(
                "JWT configuration missing. Set spring.security.oauth2.resourceserver.jwt.jwk-set-uri " +
                "or spring.security.oauth2.resourceserver.jwt.issuer-uri in application.yml"
            );
        }
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
