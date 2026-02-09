package com.bmc.rag.connector.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RemedyConnectionConfig}.
 * Tests default values and configuration properties.
 */
@DisplayName("RemedyConnectionConfig")
class RemedyConnectionConfigTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("defaultValues_newInstance_hasCorrectDefaults")
        void defaultValues_newInstance_hasCorrectDefaults() {
            // Given & When
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // Then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getServer()).isEqualTo("localhost");
            assertThat(config.getPort()).isEqualTo(7100);
            assertThat(config.getUsername()).isEqualTo("Demo");
            assertThat(config.getPassword()).isEqualTo("");
            assertThat(config.getSocketTimeout()).isEqualTo(60000);
            assertThat(config.getChunkSize()).isEqualTo(500);
            assertThat(config.getMaxRetrieve()).isEqualTo(2000);
            assertThat(config.getLocale()).isEqualTo("en_US");
            assertThat(config.isSslEnabled()).isFalse();
            assertThat(config.getRetryAttempts()).isEqualTo(3);
            assertThat(config.getRetryDelayMs()).isEqualTo(5000);
            assertThat(config.getPoolSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("defaultAuthString_newInstance_isNull")
        void defaultAuthString_newInstance_isNull() {
            // Given & When
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // Then
            assertThat(config.getAuthString()).isNull();
        }
    }

    @Nested
    @DisplayName("Enabled Property")
    class EnabledProperty {

        @Test
        @DisplayName("setEnabled_withTrue_enablesConnection")
        void setEnabled_withTrue_enablesConnection() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setEnabled(true);

            // Then
            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("setEnabled_withFalse_disablesConnection")
        void setEnabled_withFalse_disablesConnection() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setEnabled(false);

            // Then
            assertThat(config.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Server Configuration")
    class ServerConfiguration {

        @Test
        @DisplayName("setServer_withCustomHost_updatesServer")
        void setServer_withCustomHost_updatesServer() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setServer("remedy.example.com");

            // Then
            assertThat(config.getServer()).isEqualTo("remedy.example.com");
        }

        @Test
        @DisplayName("setPort_withCustomPort_updatesPort")
        void setPort_withCustomPort_updatesPort() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setPort(8080);

            // Then
            assertThat(config.getPort()).isEqualTo(8080);
        }

        @Test
        @DisplayName("setUsername_withCustomUsername_updatesUsername")
        void setUsername_withCustomUsername_updatesUsername() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setUsername("admin");

            // Then
            assertThat(config.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("setPassword_withPassword_updatesPassword")
        void setPassword_withPassword_updatesPassword() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setPassword("secret123");

            // Then
            assertThat(config.getPassword()).isEqualTo("secret123");
        }
    }

    @Nested
    @DisplayName("Timeout Configuration")
    class TimeoutConfiguration {

        @Test
        @DisplayName("setSocketTimeout_withCustomTimeout_updatesTimeout")
        void setSocketTimeout_withCustomTimeout_updatesTimeout() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setSocketTimeout(120000);

            // Then
            assertThat(config.getSocketTimeout()).isEqualTo(120000);
        }

        @Test
        @DisplayName("setChunkSize_withCustomSize_updatesChunkSize")
        void setChunkSize_withCustomSize_updatesChunkSize() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setChunkSize(100);

            // Then
            assertThat(config.getChunkSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("setMaxRetrieve_withCustomMax_updatesMaxRetrieve")
        void setMaxRetrieve_withCustomMax_updatesMaxRetrieve() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setMaxRetrieve(5000);

            // Then
            assertThat(config.getMaxRetrieve()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("Authentication Configuration")
    class AuthenticationConfiguration {

        @Test
        @DisplayName("setAuthString_withCustomAuthString_updatesAuthString")
        void setAuthString_withCustomAuthString_updatesAuthString() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setAuthString("custom-auth-token");

            // Then
            assertThat(config.getAuthString()).isEqualTo("custom-auth-token");
        }

        @Test
        @DisplayName("setLocale_withCustomLocale_updatesLocale")
        void setLocale_withCustomLocale_updatesLocale() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setLocale("fr_FR");

            // Then
            assertThat(config.getLocale()).isEqualTo("fr_FR");
        }
    }

    @Nested
    @DisplayName("SSL Configuration")
    class SslConfiguration {

        @Test
        @DisplayName("setSslEnabled_withTrue_enablesSsl")
        void setSslEnabled_withTrue_enablesSsl() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setSslEnabled(true);

            // Then
            assertThat(config.isSslEnabled()).isTrue();
        }

        @Test
        @DisplayName("setSslEnabled_withFalse_disablesSsl")
        void setSslEnabled_withFalse_disablesSsl() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setSslEnabled(false);

            // Then
            assertThat(config.isSslEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Retry Configuration")
    class RetryConfiguration {

        @Test
        @DisplayName("setRetryAttempts_withCustomAttempts_updatesRetryAttempts")
        void setRetryAttempts_withCustomAttempts_updatesRetryAttempts() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setRetryAttempts(5);

            // Then
            assertThat(config.getRetryAttempts()).isEqualTo(5);
        }

        @Test
        @DisplayName("setRetryDelayMs_withCustomDelay_updatesRetryDelay")
        void setRetryDelayMs_withCustomDelay_updatesRetryDelay() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setRetryDelayMs(10000);

            // Then
            assertThat(config.getRetryDelayMs()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("Pool Configuration")
    class PoolConfiguration {

        @Test
        @DisplayName("setPoolSize_withCustomSize_updatesPoolSize")
        void setPoolSize_withCustomSize_updatesPoolSize() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setPoolSize(10);

            // Then
            assertThat(config.getPoolSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Production Scenarios")
    class ProductionScenarios {

        @Test
        @DisplayName("productionConfig_withTypicalSettings_hasCorrectValues")
        void productionConfig_withTypicalSettings_hasCorrectValues() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When - Simulate production configuration
            config.setEnabled(true);
            config.setServer("remedy-prod.company.com");
            config.setPort(7100);
            config.setUsername("remedy-rag-agent");
            config.setPassword("productionPassword123");
            config.setSocketTimeout(90000);
            config.setChunkSize(250);
            config.setMaxRetrieve(2000);
            config.setSslEnabled(true);
            config.setRetryAttempts(5);
            config.setRetryDelayMs(10000);
            config.setPoolSize(10);
            config.setLocale("en_US");

            // Then
            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getServer()).isEqualTo("remedy-prod.company.com");
            assertThat(config.getPort()).isEqualTo(7100);
            assertThat(config.getUsername()).isEqualTo("remedy-rag-agent");
            assertThat(config.getPassword()).isEqualTo("productionPassword123");
            assertThat(config.getSocketTimeout()).isEqualTo(90000);
            assertThat(config.getChunkSize()).isEqualTo(250);
            assertThat(config.getMaxRetrieve()).isEqualTo(2000);
            assertThat(config.isSslEnabled()).isTrue();
            assertThat(config.getRetryAttempts()).isEqualTo(5);
            assertThat(config.getRetryDelayMs()).isEqualTo(10000);
            assertThat(config.getPoolSize()).isEqualTo(10);
            assertThat(config.getLocale()).isEqualTo("en_US");
        }

        @Test
        @DisplayName("testConfig_withDisabledConnection_canBeUsedForTesting")
        void testConfig_withDisabledConnection_canBeUsedForTesting() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When - Simulate test configuration with disabled connection
            config.setEnabled(false);

            // Then
            assertThat(config.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("slowNetworkConfig_withIncreasedTimeouts_hasCorrectSettings")
        void slowNetworkConfig_withIncreasedTimeouts_hasCorrectSettings() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When - Simulate slow network configuration
            config.setSocketTimeout(180000); // 3 minutes
            config.setChunkSize(100); // Smaller chunks
            config.setRetryAttempts(5);
            config.setRetryDelayMs(15000); // 15 seconds

            // Then
            assertThat(config.getSocketTimeout()).isEqualTo(180000);
            assertThat(config.getChunkSize()).isEqualTo(100);
            assertThat(config.getRetryAttempts()).isEqualTo(5);
            assertThat(config.getRetryDelayMs()).isEqualTo(15000);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("emptyPassword_withEmptyString_isAllowed")
        void emptyPassword_withEmptyString_isAllowed() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setPassword("");

            // Then
            assertThat(config.getPassword()).isEqualTo("");
        }

        @Test
        @DisplayName("nullAuthString_afterSetToNull_isNull")
        void nullAuthString_afterSetToNull_isNull() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();
            config.setAuthString("test");

            // When
            config.setAuthString(null);

            // Then
            assertThat(config.getAuthString()).isNull();
        }

        @Test
        @DisplayName("minimalPoolSize_withSize1_isAllowed")
        void minimalPoolSize_withSize1_isAllowed() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setPoolSize(1);

            // Then
            assertThat(config.getPoolSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("largeChunkSize_with1000_isAllowed")
        void largeChunkSize_with1000_isAllowed() {
            // Given
            RemedyConnectionConfig config = new RemedyConnectionConfig();

            // When
            config.setChunkSize(1000);

            // Then
            assertThat(config.getChunkSize()).isEqualTo(1000);
        }
    }
}
