package com.bmc.rag.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CorsConfig.
 */
@DisplayName("CorsConfig Tests")
class CorsConfigTest {

    @Nested
    @DisplayName("Development CORS Configuration")
    class DevCorsConfigTests {

        @Test
        @DisplayName("developmentCorsConfigurationSource returns configured source")
        void developmentCorsConfigurationSource_returnsConfiguredSource() {
            CorsConfig config = new CorsConfig();

            CorsConfigurationSource source = config.developmentCorsConfigurationSource();

            assertThat(source).isNotNull();
            assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);

            // Verify the registered configuration for /api/** path
            CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/api/**");
            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowedOriginPatterns())
                .containsExactly("http://localhost:*", "http://127.0.0.1:*");
            assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
            assertThat(corsConfig.getAllowedHeaders())
                .contains("Content-Type", "Authorization", "X-Requested-With", "Accept", "X-Session-ID");
            assertThat(corsConfig.getExposedHeaders())
                .containsExactly("X-Total-Count", "X-Page-Number");
            assertThat(corsConfig.getAllowCredentials()).isTrue();
            assertThat(corsConfig.getMaxAge()).isEqualTo(600L);
        }

        @Test
        @DisplayName("developmentCorsConfigurationSource registers ws and v1 paths")
        void developmentCorsConfigurationSource_registersAllPaths() {
            CorsConfig config = new CorsConfig();

            CorsConfigurationSource source = config.developmentCorsConfigurationSource();
            var configs = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations();

            assertThat(configs).containsKeys("/api/**", "/ws/**", "/v1/**");
        }
    }

    @Nested
    @DisplayName("Production CORS Configuration")
    class ProdCorsConfigTests {

        @Test
        @DisplayName("productionCorsConfigurationSource with allowed origins configured")
        void productionCorsConfigurationSource_withAllowedOrigins() {
            CorsConfig config = new CorsConfig();
            ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example.com,https://portal.example.com");

            CorsConfigurationSource source = config.productionCorsConfigurationSource();

            assertThat(source).isNotNull();
            CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/api/**");
            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowedOrigins())
                .containsExactly("https://app.example.com", "https://portal.example.com");
            assertThat(corsConfig.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
            assertThat(corsConfig.getAllowedHeaders())
                .contains("Content-Type", "Authorization", "X-Session-ID");
            assertThat(corsConfig.getAllowCredentials()).isTrue();
            assertThat(corsConfig.getMaxAge()).isEqualTo(600L);
        }

        @Test
        @DisplayName("productionCorsConfigurationSource with blank origins uses empty list")
        void productionCorsConfigurationSource_blankOrigins_usesEmptyList() {
            CorsConfig config = new CorsConfig();
            ReflectionTestUtils.setField(config, "allowedOrigins", "");

            CorsConfigurationSource source = config.productionCorsConfigurationSource();

            CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/api/**");
            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("productionCorsConfigurationSource with null origins uses empty list")
        void productionCorsConfigurationSource_nullOrigins_usesEmptyList() {
            CorsConfig config = new CorsConfig();
            ReflectionTestUtils.setField(config, "allowedOrigins", null);

            CorsConfigurationSource source = config.productionCorsConfigurationSource();

            CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/api/**");
            assertThat(corsConfig).isNotNull();
            assertThat(corsConfig.getAllowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("productionCorsConfigurationSource registers api and v1 paths")
        void productionCorsConfigurationSource_registersAllPaths() {
            CorsConfig config = new CorsConfig();
            ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example.com");

            CorsConfigurationSource source = config.productionCorsConfigurationSource();
            var configs = ((UrlBasedCorsConfigurationSource) source).getCorsConfigurations();

            assertThat(configs).containsKeys("/api/**", "/v1/**");
        }

        @Test
        @DisplayName("productionCorsConfigurationSource exposes correct headers")
        void productionCorsConfigurationSource_exposesHeaders() {
            CorsConfig config = new CorsConfig();
            ReflectionTestUtils.setField(config, "allowedOrigins", "https://app.example.com");

            CorsConfigurationSource source = config.productionCorsConfigurationSource();

            CorsConfiguration corsConfig = ((UrlBasedCorsConfigurationSource) source)
                .getCorsConfigurations().get("/v1/**");
            assertThat(corsConfig.getExposedHeaders())
                .containsExactly("X-Total-Count", "X-Page-Number");
        }
    }

    @Nested
    @DisplayName("WebMvcConfigurer CORS Mappings")
    class CorsMappingsTests {

        @Test
        @DisplayName("addCorsMappings registers api and v1 paths without errors")
        void addCorsMappings_registersApiAndV1Paths() {
            CorsConfig config = new CorsConfig();
            CorsRegistry registry = new CorsRegistry();

            // addCorsMappings should not throw any exceptions
            config.addCorsMappings(registry);

            // Access protected method via reflection to validate internal state
            java.util.Map<String, CorsConfiguration> configs =
                (java.util.Map<String, CorsConfiguration>) org.springframework.test.util.ReflectionTestUtils
                    .invokeMethod(registry, "getCorsConfigurations");
            assertThat(configs).containsKeys("/api/**", "/v1/**");
        }

        @Test
        @DisplayName("addCorsMappings configures correct properties")
        void addCorsMappings_configuresCorrectProperties() {
            CorsConfig config = new CorsConfig();
            CorsRegistry registry = new CorsRegistry();

            config.addCorsMappings(registry);

            @SuppressWarnings("unchecked")
            java.util.Map<String, CorsConfiguration> configs =
                (java.util.Map<String, CorsConfiguration>) org.springframework.test.util.ReflectionTestUtils
                    .invokeMethod(registry, "getCorsConfigurations");
            CorsConfiguration apiConfig = configs.get("/api/**");
            assertThat(apiConfig).isNotNull();
            assertThat(apiConfig.getAllowedOriginPatterns())
                .containsExactly("http://localhost:*", "http://127.0.0.1:*");
            assertThat(apiConfig.getAllowCredentials()).isTrue();
        }
    }
}
