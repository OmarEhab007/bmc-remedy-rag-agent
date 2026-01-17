package com.bmc.rag.api.integration.teams;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Microsoft Teams bot integration.
 * These values are obtained from Azure Bot Service registration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "teams.bot")
public class TeamsBotConfig {

    /**
     * Microsoft App ID from Azure AD app registration.
     */
    private String appId;

    /**
     * Microsoft App Password (client secret) from Azure AD.
     */
    private String appPassword;

    /**
     * Azure AD Tenant ID for single-tenant apps (optional).
     */
    private String tenantId;

    /**
     * Whether the bot is enabled.
     */
    private boolean enabled = false;

    /**
     * Bot display name shown in Teams.
     */
    private String displayName = "BMC Remedy RAG Assistant";

    /**
     * Maximum message length before truncation.
     */
    private int maxMessageLength = 4000;

    /**
     * Whether to include source citations in responses.
     */
    private boolean includeCitations = true;

    /**
     * Whether to show confidence indicators in responses.
     */
    private boolean showConfidence = true;
}
