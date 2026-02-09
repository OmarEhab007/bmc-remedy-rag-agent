package com.bmc.rag.api.controller;

import com.bmc.rag.agent.damee.GuidedServiceCreator;
import com.bmc.rag.agent.retrieval.SecureContentRetriever;
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
import static org.mockito.Mockito.*;
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

    @Test
    void chatCompletions_serviceError_returnsError() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content("Test question")
                    .build()
            ))
            .stream(false)
            .build();

        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Service unavailable"));

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
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("error")));
    }

    @Test
    void chatCompletions_invalidModel_stillProcessesRequest() throws Exception {
        // Given - invalid model ID is accepted (we don't validate model IDs)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("unknown-model")
            .messages(List.of(
                ChatMessage.builder()
                    .role("user")
                    .content("Test")
                    .build()
            ))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
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
            .andExpect(jsonPath("$.model").value("bmc-remedy-rag"));
    }

    @Test
    void chatCompletions_withSessionIdHeader_usesHeader() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("Test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chat(eq("my-session-123"), anyString(), any()))
            .thenReturn(serviceResponse);

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .header("X-Session-ID", "my-session-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_guidedFlowStreaming_returnsTextStream() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("1")))
            .stream(true)
            .build();

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("Select an option")
            .submitted(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_createIncidentIntent_callsToolServer() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Create incident for VPN not working")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "VPN not working");
        params.put("description", "Cannot connect");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_searchIncidentsIntent_callsToolServer() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Search for VPN issues")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", "VPN issues");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SEARCH_INCIDENTS, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_getIncidentIntent_returnsIncidentDetails() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Show me INC000123")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("incident_id", "INC000123");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.GET_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_confirmIntent_executesConfirmation() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.assistant("Action ID: `abc12345`"),
                ChatMessage.user("confirm")
            ))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_cancelIntent_executesCancellation() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("cancel")))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CANCEL, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_serviceRequestIntent_triggersGuidedFlow() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("I need a new laptop")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", "I need a new laptop");

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("What type of laptop?")
            .submitted(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SERVICE_REQUEST, params));
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("What type of laptop")));
    }

    @Test
    void chatCompletions_streamingExceedsMaxLength_returnsErrorSSE() throws Exception {
        // Given - create a message that exceeds 10000 characters for streaming
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longMessage.append("x");
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user(longMessage.toString())))
            .stream(true)
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_guidedFlowWithOptions_formatsOptions() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("1")))
            .stream(false)
            .build();

        List<GuidedServiceCreator.GuidedResponse.Option> options = List.of(
            new GuidedServiceCreator.GuidedResponse.Option("laptop", "Laptop"),
            new GuidedServiceCreator.GuidedResponse.Option("desktop", "Desktop")
        );

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("Select device type")
            .options(options)
            .submitted(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("**1.** Laptop")))
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("**2.** Desktop")));
    }

    @Test
    void chatCompletions_guidedFlowError_fallsBackToNormalProcessing() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Fallback response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Flow error"));
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("Fallback response"));
    }

    @Test
    void chatCompletions_createIncidentWithVagueReference_enrichesFromHistory() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("VPN authentication failed"),
                ChatMessage.user("Create incident with this issue")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this issue");
        params.put("description", "with this issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_serviceRequestWithNullResponse_fallsBackToRAG() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("I need help")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", "I need help");

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("How can I assist you?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SERVICE_REQUEST, params));
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(null);
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("How can I assist you?"));
    }

    @Test
    void chatCompletions_createIncidentWithBlankSummary_fallsBackToRAG() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Create incident")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Please provide more details")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("Please provide more details"));
    }

    @Test
    void chatCompletions_confirmWithNoActionId_returnsNoPendingActions() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("confirm")))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_extractActionIdFromHistory_findsActionId() throws Exception {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.assistant("**Action ID:** `abc12345`"),
                ChatMessage.user("yes, confirm")
            ))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void chatCompletions_anonymousSession_generatesAnonymousId() throws Exception {
        // Given - no user field and no session header
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("Test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chat(org.mockito.ArgumentMatchers.startsWith("anonymous-"), anyString(), any()))
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
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_blankUserField_usesAnonymousSession() throws Exception {
        // Given - user field is blank
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .user("   ")
            .messages(List.of(ChatMessage.user("Test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chat(org.mockito.ArgumentMatchers.startsWith("anonymous-"), anyString(), any()))
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
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_blankSessionHeader_usesAnonymousSession() throws Exception {
        // Given - session header is blank
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("Test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chat(org.mockito.ArgumentMatchers.startsWith("anonymous-"), anyString(), any()))
            .thenReturn(serviceResponse);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .header("X-Session-ID", "   ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_nullQuestion_handlesGracefully() throws Exception {
        // Given - request with only system message (no user message)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(
                ChatMessage.builder().role("system").content("system message").build()
            ))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then - null question skips length validation
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_guidedFlowNullResponse_fallsBackToIntent() throws Exception {
        // Given - guided flow returns null
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("test")))
            .stream(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Fallback response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(null);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("Fallback response"));
    }

    @Test
    void chatCompletions_extractActionId_noMatches_returnsNull() throws Exception {
        // Given - messages without action ID
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("hello"),
                ChatMessage.assistant("hi there"),
                ChatMessage.user("confirm")
            ))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_extractActionId_nullContent_skipsMessage() throws Exception {
        // Given - assistant message with null content
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.builder().role("assistant").content(null).build(),
                ChatMessage.user("confirm")
            ))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_extractActionId_userMessage_skips() throws Exception {
        // Given - action ID in user message (should be ignored)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("**Action ID:** `abc123`"),
                ChatMessage.user("confirm")
            ))
            .stream(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CONFIRM, Collections.emptyMap()));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_enrichCreateIncident_noEnrichmentNeeded() throws Exception {
        // Given - clear summary, no vague reference
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Create incident for printer not working")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "Printer not working");
        params.put("description", "Office printer offline");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_enrichCreateIncident_emptyMessages_noEnrichment() throws Exception {
        // Given - vague reference but empty message history
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Create incident with this issue")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this issue");
        params.put("description", "with this issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_enrichCreateIncident_noProblemInHistory() throws Exception {
        // Given - vague reference but no problem description in history
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Hello"),
                ChatMessage.assistant("Hi, how can I help?"),
                ChatMessage.user("Create incident with this issue")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_enrichCreateIncident_multipleProblems_usesLatest() throws Exception {
        // Given - multiple problem descriptions, should use most recent non-creation-request
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("My email is not working"),
                ChatMessage.user("Actually, VPN won't connect"),
                ChatMessage.user("Open a ticket for this problem")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this problem");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_vpnAuthentication() throws Exception {
        // Given - VPN authentication error
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("VPN authentication failed when trying to login"),
                ChatMessage.user("Create ticket for this problem")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "the problem");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_vpnConnection() throws Exception {
        // Given - VPN connection issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Can't connect to VPN"),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_workstationPerformance() throws Exception {
        // Given - workstation performance issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("My workstation is very slow and freezing"),
                ChatMessage.user("Log this issue")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "new ticket");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_emailIssue() throws Exception {
        // Given - email issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Email not working, getting error"),
                ChatMessage.user("Submit ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "new incident");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_networkConnectivity() throws Exception {
        // Given - network connectivity issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Network not working, can't connect"),
                ChatMessage.user("Raise incident")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "that issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_loginIssue() throws Exception {
        // Given - login/authentication issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Can't login, password not working"),
                ChatMessage.user("File ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "the issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_printerIssue() throws Exception {
        // Given - printer issue
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Printer not printing, paper jam"),
                ChatMessage.user("Create new incident")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "with this issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_applicationCrash() throws Exception {
        // Given - application crash
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Application keeps crashing when I open it"),
                ChatMessage.user("Log this")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this problem");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_firstSentence() throws Exception {
        // Given - description with clear first sentence
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("cannot access shared drive. Getting denied error. Need urgent help."),
                ChatMessage.user("Create ticket for this")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "for this");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_longFirstSentence() throws Exception {
        // Given - description with long first sentence (>80 chars)
        StringBuilder longSentence = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longSentence.append("x");
        }
        longSentence.append(". More text here.");

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user(longSentence.toString()),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_truncate80Chars() throws Exception {
        // Given - description without periods, over 80 chars
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 85; i++) {
            longText.append("x");
        }

        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user(longText.toString()),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "new ticket");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_searchIncidents_nullQuery_fallsBackToRAG() throws Exception {
        // Given - search intent with null query
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Search incidents")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", null);

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("What would you like to search for?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SEARCH_INCIDENTS, params));
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("What would you like to search for?"));
    }

    @Test
    void chatCompletions_getIncident_nullIncidentId_fallsBackToRAG() throws Exception {
        // Given - get incident intent with null incident ID
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Show incident")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("incident_id", null);

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Which incident?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.GET_INCIDENT, params));
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("Which incident?"));
    }

    @Test
    void chatCompletions_serviceRequest_blankQuery_fallsBackToRAG() throws Exception {
        // Given - service request with blank query
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("I need help")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", "   ");

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("How can I help?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SERVICE_REQUEST, params));
        when(ragAssistantService.chat(anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value("How can I help?"));
    }

    @Test
    void chatCompletions_serviceRequest_withOptions_formatsResponse() throws Exception {
        // Given - service request that returns options
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("Request new equipment")))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("query", "Request new equipment");

        List<GuidedServiceCreator.GuidedResponse.Option> options = List.of(
            new GuidedServiceCreator.GuidedResponse.Option("laptop", "Laptop"),
            new GuidedServiceCreator.GuidedResponse.Option("monitor", "Monitor"),
            new GuidedServiceCreator.GuidedResponse.Option("keyboard", "Keyboard")
        );

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("What type of equipment?")
            .options(options)
            .submitted(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.SERVICE_REQUEST, params));
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("**1.** Laptop")))
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("**2.** Monitor")))
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("**3.** Keyboard")));
    }

    @Test
    void chatCompletions_containsProblemDescription_arabicText() throws Exception {
        // Given - problem description with Arabic text
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("البريد الإلكتروني لا يعمل (email not working in Arabic)"),
                ChatMessage.user("Create ticket for this")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "this");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_isIncidentCreationRequest_variousPatterns() throws Exception {
        // Test various creation request patterns
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("My computer is broken"),
                ChatMessage.user("submit a new ticket for this issue")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "for this issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_streamingWithSources_formatsSourcesCitations() throws Exception {
        // Given - streaming request that should include source citations
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("What is VPN?")))
            .stream(true)
            .build();

        // Mock the streaming service to provide documents
        List<SecureContentRetriever.RetrievedDocument> documents = List.of(
            new SecureContentRetriever.RetrievedDocument(
                "INCIDENT",
                "INC000123",
                "RESOLUTION",
                "VPN troubleshooting steps",
                "VPN connection issue",
                "Network",
                "IT Support",
                0.85f
            )
        );

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        // Mock the streaming chat to call the completion handler with documents
        doAnswer(invocation -> {
            java.util.function.Consumer<String> tokenConsumer = invocation.getArgument(2);
            java.util.function.BiConsumer<List<SecureContentRetriever.RetrievedDocument>, Double> completionHandler = invocation.getArgument(3);

            // Simulate streaming tokens
            tokenConsumer.accept("VPN ");
            tokenConsumer.accept("stands ");
            tokenConsumer.accept("for ");
            tokenConsumer.accept("Virtual Private Network");

            // Call completion handler with documents
            completionHandler.accept(documents, 0.85);

            return null;
        }).when(ragAssistantService).chatWithStreaming(anyString(), anyString(), any(), any(), any());

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_streamingWithoutSources_noSourcesCitations() throws Exception {
        // Given - streaming request without sources
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("What is VPN?")))
            .stream(true)
            .build();

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        // Mock the streaming chat to call completion handler without documents
        doAnswer(invocation -> {
            java.util.function.Consumer<String> tokenConsumer = invocation.getArgument(2);
            java.util.function.BiConsumer<List<SecureContentRetriever.RetrievedDocument>, Double> completionHandler = invocation.getArgument(3);

            tokenConsumer.accept("Response");
            completionHandler.accept(null, 0.0);

            return null;
        }).when(ragAssistantService).chatWithStreaming(anyString(), anyString(), any(), any(), any());

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_streamingError_sendsErrorEvent() throws Exception {
        // Given - streaming request that encounters an error
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("What is VPN?")))
            .stream(true)
            .build();

        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(ToolIntentDetector.IntentResult.none());
        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        // Mock the streaming chat to throw an error
        doThrow(new RuntimeException("Service error"))
            .when(ragAssistantService).chatWithStreaming(anyString(), anyString(), any(), any(), any());

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_guidedFlowStreamingWithOptions_formatsOptions() throws Exception {
        // Given - guided flow with options in streaming mode
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(ChatMessage.user("1")))
            .stream(true)
            .build();

        List<GuidedServiceCreator.GuidedResponse.Option> options = List.of(
            new GuidedServiceCreator.GuidedResponse.Option("opt1", "Option 1"),
            new GuidedServiceCreator.GuidedResponse.Option("opt2", "Option 2")
        );

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("Choose an option")
            .options(options)
            .submitted(false)
            .build();

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("text/event-stream")));
    }

    @Test
    void chatCompletions_generateSummary_blankDescription_returnsDefault() throws Exception {
        // Given - empty/blank description
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user(""),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_performanceIssue() throws Exception {
        // Given - general performance issue (not workstation specific)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("System is very slow"),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_applicationIssueNocrash() throws Exception {
        // Given - application issue without crash/freeze
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Application is not working properly"),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_generateSummary_shortDescription() throws Exception {
        // Given - short description under 80 chars without period
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("my mouse is broken"),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_containsProblemDescription_variousKeywords() throws Exception {
        // Given - problem with various keywords (stuck, crash, freeze, etc.)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Application keeps getting stuck and freezing"),
                ChatMessage.user("Log ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_isVagueReference_variousPatterns() throws Exception {
        // Given - various vague reference patterns
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.user("Server timeout error"),
                ChatMessage.user("Create ticket with that problem")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "that problem");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_enrichFromHistory_assistantMessage_skipped() throws Exception {
        // Given - history with assistant messages (should be skipped)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.assistant("I can help with that"),
                ChatMessage.user("VPN error"),
                ChatMessage.user("Create ticket for the issue")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "the issue");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_containsProblemDescription_nullContent_returnsFalse() throws Exception {
        // Given - message with null content
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.builder().role("user").content(null).build(),
                ChatMessage.user("Create ticket")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_isIncidentCreationRequest_nullText_returnsFalse() throws Exception {
        // Given - enrichment with null text in history
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .messages(List.of(
                ChatMessage.builder().role("user").content(null).build(),
                ChatMessage.user("Create new incident")
            ))
            .stream(false)
            .build();

        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("summary", "");

        when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
        when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
            .thenReturn(new ToolIntentDetector.IntentResult(ToolIntentDetector.Intent.CREATE_INCIDENT, params));

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk());
    }

    @Test
    void chatCompletions_nonStreamingWithSources_doesNotFormatCitations() throws Exception {
        // Given - non-streaming request (sources are in the RAG response, not formatted separately)
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user("What is VPN?")))
            .stream(false)
            .build();

        List<String> sources = List.of("INC000123");

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("VPN stands for Virtual Private Network")
            .sources(sources)
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
            .andExpect(jsonPath("$.choices[0].message.content").value(containsString("Virtual Private Network")));
    }
}
