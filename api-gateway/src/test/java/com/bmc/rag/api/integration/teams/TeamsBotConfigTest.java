package com.bmc.rag.api.integration.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TeamsBotConfig.
 * Tests configuration properties and default values.
 */
@DisplayName("TeamsBotConfig Tests")
class TeamsBotConfigTest {

    private TeamsBotConfig config;

    @BeforeEach
    void setUp() {
        config = new TeamsBotConfig();
    }

    @Test
    @DisplayName("defaultValues_shouldBeSet")
    void defaultValues_shouldBeSet() {
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getDisplayName()).isEqualTo("BMC Remedy RAG Assistant");
        assertThat(config.getMaxMessageLength()).isEqualTo(4000);
        assertThat(config.isIncludeCitations()).isTrue();
        assertThat(config.isShowConfidence()).isTrue();
    }

    @Test
    @DisplayName("setAppId_shouldUpdateValue")
    void setAppId_shouldUpdateValue() {
        config.setAppId("test-app-id");
        assertThat(config.getAppId()).isEqualTo("test-app-id");
    }

    @Test
    @DisplayName("setAppPassword_shouldUpdateValue")
    void setAppPassword_shouldUpdateValue() {
        config.setAppPassword("test-password");
        assertThat(config.getAppPassword()).isEqualTo("test-password");
    }

    @Test
    @DisplayName("setTenantId_shouldUpdateValue")
    void setTenantId_shouldUpdateValue() {
        config.setTenantId("test-tenant-id");
        assertThat(config.getTenantId()).isEqualTo("test-tenant-id");
    }

    @Test
    @DisplayName("setEnabled_shouldUpdateValue")
    void setEnabled_shouldUpdateValue() {
        config.setEnabled(true);
        assertThat(config.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("setDisplayName_shouldUpdateValue")
    void setDisplayName_shouldUpdateValue() {
        config.setDisplayName("Custom Bot Name");
        assertThat(config.getDisplayName()).isEqualTo("Custom Bot Name");
    }

    @Test
    @DisplayName("setMaxMessageLength_shouldUpdateValue")
    void setMaxMessageLength_shouldUpdateValue() {
        config.setMaxMessageLength(5000);
        assertThat(config.getMaxMessageLength()).isEqualTo(5000);
    }

    @Test
    @DisplayName("setIncludeCitations_shouldUpdateValue")
    void setIncludeCitations_shouldUpdateValue() {
        config.setIncludeCitations(false);
        assertThat(config.isIncludeCitations()).isFalse();
    }

    @Test
    @DisplayName("setShowConfidence_shouldUpdateValue")
    void setShowConfidence_shouldUpdateValue() {
        config.setShowConfidence(false);
        assertThat(config.isShowConfidence()).isFalse();
    }

    @Test
    @DisplayName("equalsAndHashCode_shouldWork")
    void equalsAndHashCode_shouldWork() {
        TeamsBotConfig config1 = new TeamsBotConfig();
        config1.setAppId("app-id");
        config1.setEnabled(true);

        TeamsBotConfig config2 = new TeamsBotConfig();
        config2.setAppId("app-id");
        config2.setEnabled(true);

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("toString_shouldContainFields")
    void toString_shouldContainFields() {
        config.setAppId("test-app-id");
        config.setEnabled(true);

        String toString = config.toString();

        assertThat(toString).contains("test-app-id");
        assertThat(toString).contains("true");
    }
}
