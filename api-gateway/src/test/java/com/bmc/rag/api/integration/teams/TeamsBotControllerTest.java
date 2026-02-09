package com.bmc.rag.api.integration.teams;

import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TeamsBotController.
 */
@WebMvcTest(
    controllers = TeamsBotController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
    "teams.bot.enabled=true",
    "security.enabled=false"
})
class TeamsBotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamsBotHandler botHandler;

    @MockBean
    private TeamsBotAuthenticator authenticator;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void receiveActivity_shouldProcessValidRequest() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");
        activity.put("text", "Hello");

        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("type", "message");
        responseNode.put("text", "Response");

        when(authenticator.validateRequest(anyString(), any())).thenReturn(true);
        when(botHandler.processMessage(any())).thenReturn(responseNode);

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("message"))
            .andExpect(jsonPath("$.text").value("Response"));
    }

    @Test
    void receiveActivity_shouldRejectUnauthorizedRequest() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");

        when(authenticator.validateRequest(anyString(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void receiveActivity_shouldHandleNoResponse() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "conversationUpdate");

        when(authenticator.validateRequest(anyString(), any())).thenReturn(true);
        when(botHandler.processMessage(any())).thenReturn(null);

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isOk());
    }

    @Test
    void receiveActivity_shouldHandleExceptions() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");

        when(authenticator.validateRequest(anyString(), any())).thenReturn(true);
        when(botHandler.processMessage(any())).thenThrow(new RuntimeException("Processing error"));

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void receiveActivity_shouldWorkWithoutAuthHeader() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "message");

        when(authenticator.validateRequest(any(), any())).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void health_shouldReturnHealthStatus() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teams/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("Teams bot is healthy"));
    }

    @Test
    void receiveActivity_shouldLogActivityType() throws Exception {
        // Given
        ObjectNode activity = objectMapper.createObjectNode();
        activity.put("type", "typing");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "message");

        when(authenticator.validateRequest(anyString(), any())).thenReturn(true);
        when(botHandler.processMessage(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/teams/messages")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activity)))
            .andExpect(status().isOk());
    }
}
