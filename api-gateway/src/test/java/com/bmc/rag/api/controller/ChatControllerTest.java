package com.bmc.rag.api.controller;

import com.bmc.rag.agent.damee.GuidedServiceCreator;
import com.bmc.rag.agent.memory.PostgresChatMemoryStore;
import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievalResult;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.api.dto.ChatRequest;
import com.bmc.rag.api.dto.SearchRequest;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ChatController.
 */
@WebMvcTest(
    controllers = ChatController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagAssistantService ragAssistantService;

    @MockBean
    private SecureContentRetriever contentRetriever;

    @MockBean
    private GuidedServiceCreator guidedServiceCreator;

    // Dependencies required by auto-detected @Component filters (ARContextCleanupFilter, RateLimitFilter)
    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void chat_validRequest_returnsResponse() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("What is VPN?")
            .userId("test-user")
            .userGroups(Set.of("Network Support"))
            .skipContext(false)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("VPN stands for Virtual Private Network...")
            .sources(List.of("INC000123", "INC000124"))
            .hasContext(true)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(
            eq("test-session"),
            eq("What is VPN?"),
            eq("test-user"),
            any()
        )).thenReturn(serviceResponse);

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("test-session"))
            .andExpect(jsonPath("$.response").value(containsString("Virtual Private Network")))
            .andExpect(jsonPath("$.sources").isArray())
            .andExpect(jsonPath("$.sources[0]").value("INC000123"))
            .andExpect(jsonPath("$.hasContext").value(true))
            .andExpect(jsonPath("$.timestamp").isNumber());

        verify(ragAssistantService).chatWithAgenticSupport(
            eq("test-session"),
            eq("What is VPN?"),
            eq("test-user"),
            any()
        );
    }

    @Test
    void chat_withoutSessionId_generatesSessionId() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .question("Test question")
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Test response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").exists())
            .andExpect(jsonPath("$.sessionId").isString());
    }

    @Test
    void chat_exceedsMaxLength_throwsException() throws Exception {
        // Given - create a question that exceeds 10000 characters
        StringBuilder longQuestion = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longQuestion.append("x");
        }

        ChatRequest request = ChatRequest.builder()
            .question(longQuestion.toString())
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chat_skipContext_callsChatWithoutContext() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("Hello")
            .skipContext(true)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Hello! How can I help you?")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithoutContext("test-session", "Hello"))
            .thenReturn(serviceResponse);

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasContext").value(false));

        verify(ragAssistantService).chatWithoutContext("test-session", "Hello");
    }

    @Test
    void search_validRequest_returnsResults() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .query("VPN authentication")
            .userId("test-user")
            .userGroups(Set.of("Network Support"))
            .build();

        // RetrievedDocument record: (sourceType, sourceId, chunkType, content, title, category, assignedGroup, score)
        List<RetrievedDocument> mockDocuments = List.of(
            new RetrievedDocument(
                "Incident",
                "INC000123",
                "RESOLUTION",
                "VPN authentication failed for user",
                "VPN auth error",
                "Network",
                "Network Support",
                0.95f
            ),
            new RetrievedDocument(
                "Incident",
                "INC000124",
                "DESCRIPTION",
                "User cannot connect to network",
                "Network problem",
                "Network",
                "Network Support",
                0.85f
            )
        );

        // RetrievalResult record: (documents, formattedContext)
        RetrievalResult retrievalResult = new RetrievalResult(mockDocuments, "Formatted context here");

        when(contentRetriever.retrieve(eq("VPN authentication"), any()))
            .thenReturn(retrievalResult);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCount").value(2))
            .andExpect(jsonPath("$.results").isArray())
            .andExpect(jsonPath("$.results[0].sourceId").value("INC000123"))
            .andExpect(jsonPath("$.results[0].sourceType").value("Incident"))
            .andExpect(jsonPath("$.results[0].score").value(0.95))
            .andExpect(jsonPath("$.results[1].sourceId").value("INC000124"));
    }

    @Test
    void search_exceedsMaxLength_throwsException() throws Exception {
        // Given
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 10001; i++) {
            longQuery.append("x");
        }

        SearchRequest request = SearchRequest.builder()
            .query(longQuery.toString())
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getSessions_returnsSessionList() throws Exception {
        // Given - SessionInfo is from PostgresChatMemoryStore
        List<PostgresChatMemoryStore.SessionInfo> mockSessions = List.of(
            new PostgresChatMemoryStore.SessionInfo(
                "session-1",
                "VPN discussion",
                5,
                System.currentTimeMillis()
            ),
            new PostgresChatMemoryStore.SessionInfo(
                "session-2",
                "Network issue",
                3,
                System.currentTimeMillis() - 3600000
            )
        );

        when(ragAssistantService.getSessionSummaries()).thenReturn(mockSessions);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/sessions")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].sessionId").value("session-1"))
            .andExpect(jsonPath("$[0].title").value("VPN discussion"))
            .andExpect(jsonPath("$[0].messageCount").value(5))
            .andExpect(jsonPath("$[1].sessionId").value("session-2"));
    }

    @Test
    void clearSession_validSessionId_returnsSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/chat/sessions/test-session")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("cleared"))
            .andExpect(jsonPath("$.sessionId").value("test-session"));

        verify(ragAssistantService).clearSession("test-session");
    }

    @Test
    void getHistory_validSessionId_returnsMessages() throws Exception {
        // Given
        List<ChatMessage> mockMessages = List.of(
            new SystemMessage("You are a helpful assistant"),
            new UserMessage("What is VPN?"),
            new AiMessage("VPN stands for Virtual Private Network")
        );

        when(ragAssistantService.getConversationHistory("test-session"))
            .thenReturn(mockMessages);

        // When & Then
        mockMvc.perform(get("/api/v1/chat/sessions/test-session/history")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("test-session"))
            .andExpect(jsonPath("$.messageCount").value(3))
            .andExpect(jsonPath("$.messages").isArray())
            .andExpect(jsonPath("$.messages[0].type").value("SYSTEM"))
            .andExpect(jsonPath("$.messages[1].type").value("USER"))
            .andExpect(jsonPath("$.messages[1].content").value("What is VPN?"))
            .andExpect(jsonPath("$.messages[2].type").value("AI"))
            .andExpect(jsonPath("$.messages[2].content").value(containsString("Virtual Private Network")));
    }

    @Test
    void chat_withGuidedFlow_returnsGuidedResponse() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("1")
            .userId("test-user")
            .build();

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("Please provide more details about your issue.")
            .submitted(false)
            .options(List.of(
                new GuidedServiceCreator.GuidedResponse.Option("1", "VPN issue"),
                new GuidedServiceCreator.GuidedResponse.Option("2", "Email issue")
            ))
            .build();

        when(guidedServiceCreator.hasActiveFlow("test-session")).thenReturn(true);
        when(guidedServiceCreator.processMessage("test-session", "test-user", "1"))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(containsString("Please provide more details")))
            .andExpect(jsonPath("$.response").value(containsString("**1.**")))
            .andExpect(jsonPath("$.response").value(containsString("**2.**")));
    }

    @Test
    void chat_emptyQuestion_returnsBadRequest() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chat_serviceError_returnsInternalServerError() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .question("Test question")
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("Service error"));

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void search_emptyQuery_returnsBadRequest() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .query("")
            .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getSessions_emptyList_returnsEmptyArray() throws Exception {
        // Given
        when(ragAssistantService.getSessionSummaries()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/chat/sessions")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void chat_withGuidedFlowError_fallsBackToNormalProcessing() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("What is VPN?")
            .userId("test-user")
            .build();

        when(guidedServiceCreator.hasActiveFlow("test-session")).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Guided flow error"));

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("VPN response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("VPN response"));
    }

    @Test
    void chat_guidedFlowReturnsNull_fallsBackToNormalProcessing() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("What is VPN?")
            .userId("test-user")
            .build();

        when(guidedServiceCreator.hasActiveFlow("test-session")).thenReturn(true);
        when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
            .thenReturn(null);

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("VPN response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("VPN response"));
    }

    @Test
    void chat_nullUserGroups_usesEmptySet() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("Test question")
            .userId("test-user")
            .userGroups(null)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Test response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Test response"));

        verify(ragAssistantService).chatWithAgenticSupport(
            eq("test-session"),
            eq("Test question"),
            eq("test-user"),
            any()
        );
    }

    @Test
    void search_nullUserGroups_usesEmptySet() throws Exception {
        // Given
        SearchRequest request = SearchRequest.builder()
            .query("VPN")
            .userId("test-user")
            .userGroups(null)
            .build();

        RetrievalResult retrievalResult = new RetrievalResult(Collections.emptyList(), "");

        when(contentRetriever.retrieve(eq("VPN"), any()))
            .thenReturn(retrievalResult);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCount").value(0));
    }

    @Test
    void search_longButValidQuery_succeeds() throws Exception {
        // Given - create a long query
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            longQuery.append("x");
        }

        SearchRequest request = SearchRequest.builder()
            .query(longQuery.toString())
            .userId("test-user")
            .build();

        RetrievalResult retrievalResult = new RetrievalResult(Collections.emptyList(), "");

        when(contentRetriever.retrieve(anyString(), any()))
            .thenReturn(retrievalResult);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then - should not fail, query gets truncated in logs
        mockMvc.perform(post("/api/v1/chat/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultCount").value(0));
    }

    @Test
    void chat_withNullUserId_generatesSessionBasedId() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("Test question")
            .userId(null)
            .build();

        RagAssistantService.ChatResponseDto serviceResponse = RagAssistantService.ChatResponseDto.builder()
            .response("Test response")
            .sources(Collections.emptyList())
            .hasContext(false)
            .build();

        when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
            .thenReturn(serviceResponse);

        when(guidedServiceCreator.hasActiveFlow(anyString()))
            .thenReturn(false);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Test response"));

        verify(ragAssistantService).chatWithAgenticSupport(
            eq("test-session"),
            eq("Test question"),
            eq("session:test-session"),
            any()
        );
    }

    @Test
    void chat_guidedFlowWithEmptyOptions_formatsCorrectly() throws Exception {
        // Given
        ChatRequest request = ChatRequest.builder()
            .sessionId("test-session")
            .question("1")
            .userId("test-user")
            .build();

        GuidedServiceCreator.GuidedResponse guidedResponse = GuidedServiceCreator.GuidedResponse.builder()
            .message("Please provide more details.")
            .submitted(false)
            .options(Collections.emptyList())
            .build();

        when(guidedServiceCreator.hasActiveFlow("test-session")).thenReturn(true);
        when(guidedServiceCreator.processMessage("test-session", "test-user", "1"))
            .thenReturn(guidedResponse);

        String requestJson = objectMapper.writeValueAsString(request);

        // When & Then
        mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value("Please provide more details."));
    }
}
