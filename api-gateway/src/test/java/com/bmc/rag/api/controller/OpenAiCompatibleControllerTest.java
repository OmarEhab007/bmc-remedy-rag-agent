package com.bmc.rag.api.controller;

import com.bmc.rag.agent.damee.GuidedServiceCreator;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.api.dto.openai.*;
import com.bmc.rag.api.service.ToolIntentDetector;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OpenAiCompatibleController.
 */
@WebMvcTest(
    controllers = OpenAiCompatibleController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class OpenAiCompatibleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagAssistantService ragAssistantService;

    @MockBean
    private ToolIntentDetector toolIntentDetector;

    @MockBean
    private GuidedServiceCreator guidedServiceCreator;

    // Dependencies required by auto-detected @Component filters (ARContextCleanupFilter, RateLimitFilter)
    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void listModels_returnsModelList() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/models")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value("bmc-remedy-rag"))
            .andExpect(jsonPath("$.data[0].object").value("model"))
            .andExpect(jsonPath("$.data[0].owned_by").value("bmc-rag-system"));
    }

    @Test
    void getModel_validId_returnsModel() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/models/bmc-remedy-rag")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("bmc-remedy-rag"))
            .andExpect(jsonPath("$.object").value("model"))
            .andExpect(jsonPath("$.owned_by").value("bmc-rag-system"))
            .andExpect(jsonPath("$.created").isNumber());
    }

    @Test
    void getModel_invalidId_returns404() throws Exception {
        // When & Then
        mockMvc.perform(get("/v1/models/unknown-model")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.message").value(containsString("does not exist")))
            .andExpect(jsonPath("$.error.type").value("invalid_request_error"))
            .andExpect(jsonPath("$.error.code").value("model_not_found"));
    }

    @Test
    void chatCompletions_exceedsMaxLength_returnsBadRequest() throws Exception {
        // Given - create a message that exceeds 10000 characters
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longMessage.append("x");
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content(longMessage.toString())
                    .build()
            ))
            .stream(false)
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.message").value(containsString("exceeds maximum length")))
            .andExpect(jsonPath("$.error.type").value("invalid_request_error"))
            .andExpect(jsonPath("$.error.code").value("context_length_exceeded"));
    }

    @Test
    void chatCompletions_validNonStreamingRequest_returnsResponse() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content("What is VPN?")
                    .build()
            ))
            .stream(false)
            .build();

        // Mock service response
        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("VPN stands for Virtual Private Network...")
            .sources(Collections.emptyList())
            .hasContext(true)
            .build();

        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.object").value("chat.completion"))
            .andExpect(jsonPath("$.model").value("bmc-remedy-rag"))
            .andExpect(jsonPath("$.choices").isArray())
            .andExpect(jsonPath("$.choices[0].message.role").value("assistant"))
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("Virtual Private Network")))
            .andExpect(jsonPath("$.usage.prompt_tokens").exists())
            .andExpect(jsonPath("$.usage.completion_tokens").exists())
            .andExpect(jsonPath("$.usage.total_tokens").exists());
    }

    @Test
    void chatCompletions_withUserField_generatesStableSessionId() throws Exception {
        // Given
        String userId = "test-user-123";

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .user(userId)
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content("Test question")
                    .build()
            ))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Test response")
            .sources(Collections.emptyList())
            .hasContext(true)
            .build();

        when(ragAssistantService.chat(eq("user-" + userId), anyString(), any()))
            .thenReturn(serviceResponse);

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_streamingRequest_returnsTextEventStream() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content("What is VPN?")
                    .build()
            ))
            .stream(true)
            .build();

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")))
            .andExpect(header().string("Cache-Control", "no-cache"))
            .andExpect(header().string("Connection", "keep-alive"));
    }

    @Test
    void chatCompletions_emptyMessages_returnsBadRequest() throws Exception {
        // Given - request with empty messages list, which violates @NotEmpty validation
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(Collections.emptyList())
            .stream(false)
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then - @NotEmpty on messages field rejects empty list with 400
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }
}
