package com.bmc.rag.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration with profile-based security.
 * - Development: Allows localhost origins
 * - Production: Restricts to configured allowed origins only
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    /**
     * Development CORS configuration - allows localhost.
     */
    @Bean
    @Profile("dev")
    public CorsConfigurationSource developmentCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Restrict allowed headers to specific values
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With", "Accept"));
        configuration.setExposedHeaders(List.of("X-Total-Count", "X-Page-Number"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(600L); // 10 minutes

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }

    /**
     * Production CORS configuration - restricts to configured origins.
     */
    @Bean
    @Profile("!dev")
    public CorsConfigurationSource productionCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // In production, only allow explicitly configured origins
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            configuration.setAllowedOrigins(origins);
        } else {
            // No wildcard - require explicit configuration
            configuration.setAllowedOrigins(List.of());
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Strict header whitelist
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        configuration.setExposedHeaders(List.of("X-Total-Count", "X-Page-Number"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(600L); // 10 minutes

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // WebMvcConfigurer CORS mapping for additional flexibility
        // The CorsConfigurationSource bean takes precedence when Spring Security is enabled
        registry.addMapping("/api/**")
            .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept")
            .allowCredentials(true)
            .maxAge(600);
    }
}
