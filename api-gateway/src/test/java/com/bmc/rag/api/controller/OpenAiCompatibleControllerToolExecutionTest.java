package com.bmc.rag.api.controller;

import com.bmc.rag.agent.damee.GuidedServiceCreator;
import com.bmc.rag.agent.damee.GuidedServiceCreator.GuidedResponse;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.api.dto.openai.*;
import com.bmc.rag.api.service.ToolIntentDetector;
import com.bmc.rag.api.service.ToolIntentDetector.Intent;
import com.bmc.rag.api.service.ToolIntentDetector.IntentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenAiCompatibleController tool execution methods.
 * Uses MockedConstruction to intercept RestTemplate instantiation inside
 * the controller's private executeToolDirectly methods.
 */
@ExtendWith(MockitoExtension.class)
class OpenAiCompatibleControllerToolExecutionTest {

    private OpenAiCompatibleController controller;

    @Mock
    private RagAssistantService ragAssistantService;

    @Mock
    private ToolIntentDetector toolIntentDetector;

    @Mock
    private GuidedServiceCreator guidedServiceCreator;

    @Mock
    private HttpServletResponse httpServletResponse;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new OpenAiCompatibleController(
            ragAssistantService, objectMapper, toolIntentDetector, guidedServiceCreator
        );
        ReflectionTestUtils.setField(controller, "toolServerBaseUrl", "http://localhost:8080");
    }

    // Helper to build a basic non-streaming request
    private ChatCompletionRequest buildRequest(String userMessage) {
        return ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(List.of(ChatMessage.user(userMessage)))
            .stream(false)
            .build();
    }

    // Helper to build a request with conversation history
    private ChatCompletionRequest buildRequestWithHistory(List<ChatMessage> messages) {
        return ChatCompletionRequest.builder()
            .model("bmc-remedy-rag")
            .messages(messages)
            .stream(false)
            .build();
    }

    // ---------------------------------------------------------------
    // executeCreateIncident tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeCreateIncident")
    class ExecuteCreateIncidentTests {

        @Test
        void createIncident_stagedStatus_returnsConfirmationMessage() {
            // Given
            Map<String, String> params = new HashMap<>();
            params.put("summary", "VPN not working");
            params.put("description", "Cannot connect to VPN");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident for VPN not working");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "STAGED");
                        response.put("preview", "Summary: VPN not working\nDescription: Cannot connect to VPN");
                        response.put("actionId", "abc123def456");
                        response.put("expiresAt", "2025-01-01T12:05:00Z");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                // When
                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                // Then
                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident Staged for Confirmation");
                assertThat(content).contains("abc123def456");
                assertThat(content).contains("confirm");
                assertThat(content).contains("cancel");
            }
        }

        @Test
        void createIncident_duplicateWarning_returnsDuplicateMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "VPN issue");
            params.put("description", "VPN problem");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident for VPN issue");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "DUPLICATE_WARNING");
                        response.put("preview", "Similar: INC000999 - VPN issue");
                        response.put("actionId", "dup789");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Potential Duplicates Found");
                assertThat(content).contains("dup789");
            }
        }

        @Test
        void createIncident_createdStatus_returnsSuccessMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Printer jam");
            params.put("description", "Office printer has paper jam");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident for printer jam");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "CREATED");
                        response.put("incidentNumber", "INC000456");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Success");
                assertThat(content).contains("INC000456");
            }
        }

        @Test
        void createIncident_rateLimitedStatus_returnsRateLimitMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Test incident");
            params.put("description", "Test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "RATE_LIMITED");
                        response.put("message", "Too many requests");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Rate Limit Exceeded");
                assertThat(content).contains("Too many requests");
            }
        }

        @Test
        void createIncident_unknownStatus_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Test incident");
            params.put("description", "Test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "UNKNOWN_STATUS");
                        response.put("message", "Something unexpected");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Error");
                assertThat(content).contains("Something unexpected");
            }
        }

        @Test
        void createIncident_nullResponse_returnsNoResponseError() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Test incident");
            params.put("description", "Test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(null);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No response from incident creation service");
            }
        }

        @Test
        void createIncident_nullDescription_useSummaryAsDescription() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Test incident");
            params.put("description", null);

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "CREATED");
                        response.put("incidentNumber", "INC000789");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("INC000789");
            }
        }

        @Test
        void createIncident_restTemplateException_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "Test incident");
            params.put("description", "Test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Create incident");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenThrow(new RuntimeException("Connection refused"));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed to stage incident");
                assertThat(content).contains("Connection refused");
            }
        }
    }

    // ---------------------------------------------------------------
    // executeSearchIncidents tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeSearchIncidents")
    class ExecuteSearchIncidentsTests {

        @Test
        void searchIncidents_withResults_returnsFormattedList() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "VPN issues");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for VPN issues");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        List<Map<String, Object>> results = new ArrayList<>();
                        Map<String, Object> incident1 = new LinkedHashMap<>();
                        incident1.put("id", "INC000111");
                        incident1.put("title", "VPN connection drops");
                        incident1.put("status", "Resolved");
                        incident1.put("score", 0.95);
                        results.add(incident1);

                        Map<String, Object> incident2 = new LinkedHashMap<>();
                        incident2.put("id", "INC000222");
                        incident2.put("title", "VPN timeout");
                        incident2.put("status", "Open");
                        incident2.put("score", 0.82);
                        results.add(incident2);

                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("results", results);

                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Search Results");
                assertThat(content).contains("INC000111");
                assertThat(content).contains("VPN connection drops");
                assertThat(content).contains("INC000222");
                assertThat(content).contains("95%");
                assertThat(content).contains("82%");
                assertThat(content).contains("2");
            }
        }

        @Test
        void searchIncidents_noResults_returnsNoIncidentsMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "nonexistent issue");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for nonexistent issue");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("results", Collections.emptyList());

                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No incidents found");
            }
        }

        @Test
        void searchIncidents_nullResults_returnsNoIncidentsMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("results", null);

                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No incidents found");
            }
        }

        @Test
        void searchIncidents_nullResponse_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(null);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No response from search service");
            }
        }

        @Test
        void searchIncidents_restTemplateException_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenThrow(new RuntimeException("Service unavailable"));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed to search incidents");
            }
        }

        @Test
        void searchIncidents_resultWithNullFields_handlesGracefully() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "test");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SEARCH_INCIDENTS, params));

            ChatCompletionRequest request = buildRequest("Search for test");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        List<Map<String, Object>> results = new ArrayList<>();
                        Map<String, Object> incident = new LinkedHashMap<>();
                        incident.put("id", "INC000333");
                        incident.put("title", null);
                        incident.put("status", null);
                        incident.put("score", null);
                        results.add(incident);

                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("results", results);

                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("INC000333");
                assertThat(content).contains("N/A");
            }
        }
    }

    // ---------------------------------------------------------------
    // executeGetIncident tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeGetIncident")
    class ExecuteGetIncidentTests {

        @Test
        void getIncident_found_returnsFormattedDetails() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC000123");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC000123");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> incident = new LinkedHashMap<>();
                        incident.put("found", true);
                        incident.put("incidentNumber", "INC000123");
                        incident.put("summary", "VPN connection issue");
                        incident.put("description", "Cannot connect to corporate VPN");
                        incident.put("status", "Resolved");
                        incident.put("priorityLabel", "High");
                        incident.put("impactLabel", "3-Moderate");
                        incident.put("urgencyLabel", "2-High");
                        incident.put("assignedGroup", "Network Team");
                        incident.put("assignedTo", "John Doe");
                        incident.put("resolution", "Restarted VPN gateway");
                        incident.put("categoryPath", "Network/VPN");
                        incident.put("submitter", "Jane Smith");
                        incident.put("createDate", "2025-01-01T10:00:00Z");
                        incident.put("lastModifiedDate", "2025-01-02T15:00:00Z");

                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenReturn(incident);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("INC000123");
                assertThat(content).contains("VPN connection issue");
                assertThat(content).contains("Resolved");
                assertThat(content).contains("Network Team");
                assertThat(content).contains("Restarted VPN gateway");
                assertThat(content).contains("Jane Smith");
                assertThat(content).contains("Description");
                assertThat(content).contains("Resolution");
            }
        }

        @Test
        void getIncident_notFoundFlag_returnsNotFoundMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC999999");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC999999");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> incident = new LinkedHashMap<>();
                        incident.put("found", false);
                        incident.put("errorMessage", "Incident INC999999 not found");

                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenReturn(incident);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident not found");
                assertThat(content).contains("INC999999 not found");
            }
        }

        @Test
        void getIncident_notFoundFlag_nullErrorMessage_usesIncidentId() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC999999");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC999999");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> incident = new LinkedHashMap<>();
                        incident.put("found", false);
                        incident.put("errorMessage", null);

                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenReturn(incident);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident not found");
                assertThat(content).contains("INC999999");
            }
        }

        @Test
        void getIncident_nullResponse_returnsNotFoundMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC000555");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC000555");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenReturn(null);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident not found");
            }
        }

        @Test
        void getIncident_httpNotFoundException_returnsNotFoundMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC000777");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC000777");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenThrow(HttpClientErrorException.create(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Not Found",
                                org.springframework.http.HttpHeaders.EMPTY,
                                new byte[0],
                                null));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident not found");
                assertThat(content).contains("INC000777");
            }
        }

        @Test
        void getIncident_genericException_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC000888");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC000888");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenThrow(new RuntimeException("Connection timed out"));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed to retrieve incident");
                assertThat(content).contains("Connection timed out");
            }
        }

        @Test
        void getIncident_nullFields_showsNAValues() {
            Map<String, String> params = new HashMap<>();
            params.put("incident_id", "INC000444");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.GET_INCIDENT, params));

            ChatCompletionRequest request = buildRequest("Show INC000444");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> incident = new LinkedHashMap<>();
                        incident.put("found", true);
                        incident.put("incidentNumber", null);
                        incident.put("summary", null);
                        incident.put("description", null);
                        incident.put("status", null);
                        incident.put("priorityLabel", null);
                        incident.put("impactLabel", null);
                        incident.put("urgencyLabel", null);
                        incident.put("assignedGroup", null);
                        incident.put("assignedTo", null);
                        incident.put("resolution", null);
                        incident.put("categoryPath", null);
                        incident.put("submitter", null);
                        incident.put("createDate", null);
                        incident.put("lastModifiedDate", null);

                        when(mock.getForObject(anyString(), eq(Map.class)))
                            .thenReturn(incident);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                // When incidentNumber is null, should fallback to using the incident_id from params
                assertThat(content).contains("INC000444");
                assertThat(content).contains("N/A");
                // Should NOT contain description/resolution sections (they are null/blank)
                assertThat(content).doesNotContain("**Description:**");
                assertThat(content).doesNotContain("**Resolution:**");
            }
        }
    }

    // ---------------------------------------------------------------
    // executeConfirmAction tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeConfirmAction")
    class ExecuteConfirmActionTests {

        @Test
        void confirmAction_withActionIdFromHistory_executedStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `abc12345def`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "EXECUTED");
                        response.put("recordId", "INC000999");
                        response.put("recordType", "Incident");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Success");
                assertThat(content).contains("Incident");
                assertThat(content).contains("INC000999");
            }
        }

        @Test
        void confirmAction_withActionIdFromHistory_expiredStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `e0e1ed123a`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "EXPIRED");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Action Expired");
            }
        }

        @Test
        void confirmAction_withActionIdFromHistory_failedStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `fa11ed1230`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "FAILED");
                        response.put("message", "AR System error ARERR 93");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed");
                assertThat(content).contains("ARERR 93");
            }
        }

        @Test
        void confirmAction_withActionIdFromHistory_notFoundStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `00f0a0d10`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "NOT_FOUND");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Action Not Found");
            }
        }

        @Test
        void confirmAction_withActionIdFromHistory_unknownStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `a0b0c01100`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "PROCESSING");
                        response.put("message", "Still being processed");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("PROCESSING");
                assertThat(content).contains("Still being processed");
            }
        }

        @Test
        void confirmAction_withActionIdFromHistory_nullResponse() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `00110e5010`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(null);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No response from confirmation service");
            }
        }

        @Test
        void confirmAction_noActionIdFromHistory_lookupPendingActions() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequest("confirm");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        // First call: lookup pending actions
                        List<Map<String, Object>> pendingActions = new ArrayList<>();
                        Map<String, Object> action = new LinkedHashMap<>();
                        action.put("actionId", "pending123");
                        pendingActions.add(action);

                        when(mock.getForObject(contains("/actions/pending"), eq(List.class)))
                            .thenReturn(pendingActions);

                        // Second call: confirm the action
                        Map<String, Object> confirmResponse = new LinkedHashMap<>();
                        confirmResponse.put("status", "EXECUTED");
                        confirmResponse.put("recordId", "INC000111");
                        confirmResponse.put("recordType", "Incident");
                        when(mock.postForObject(contains("/actions/confirm"), any(), eq(Map.class)))
                            .thenReturn(confirmResponse);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Success");
                assertThat(content).contains("INC000111");
            }
        }

        @Test
        void confirmAction_noActionIdFromHistory_noPendingActions() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequest("confirm");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.getForObject(contains("/actions/pending"), eq(List.class)))
                            .thenReturn(Collections.emptyList());
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No pending actions");
            }
        }

        @Test
        void confirmAction_exception_returnsErrorMessage() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `e00ac10001`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenThrow(new RuntimeException("Service down"));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed to confirm action");
                assertThat(content).contains("Service down");
            }
        }

        @Test
        void confirmAction_executedWithDefaultRecordType() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CONFIRM, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `00ec00e000`"),
                ChatMessage.user("confirm")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "EXECUTED");
                        response.put("recordId", "REQ000001");
                        // No recordType - should default to "Record"
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Success");
                assertThat(content).contains("Record");
                assertThat(content).contains("REQ000001");
            }
        }
    }

    // ---------------------------------------------------------------
    // executeCancelAction tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeCancelAction")
    class ExecuteCancelActionTests {

        @Test
        void cancelAction_withActionIdFromHistory_cancelledStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `ca0ce12300`"),
                ChatMessage.user("cancel")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "CANCELLED");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Action Cancelled");
            }
        }

        @Test
        void cancelAction_withActionIdFromHistory_notFoundStatus() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `c0f404000`"),
                ChatMessage.user("cancel")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "NOT_FOUND");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Action Not Found");
            }
        }

        @Test
        void cancelAction_unknownStatus_returnsGenericMessage() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `a0b5a01100`"),
                ChatMessage.user("cancel")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "PENDING");
                        response.put("message", "Action is still pending");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("PENDING");
                assertThat(content).contains("Action is still pending");
            }
        }

        @Test
        void cancelAction_nullResponse_returnsErrorMessage() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `00110e5990`"),
                ChatMessage.user("cancel")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(null);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No response from cancellation service");
            }
        }

        @Test
        void cancelAction_noActionIdFromHistory_lookupPendingActions() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequest("cancel");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        // Lookup pending actions
                        List<Map<String, Object>> pendingActions = new ArrayList<>();
                        Map<String, Object> action = new LinkedHashMap<>();
                        action.put("actionId", "pendCancel1");
                        pendingActions.add(action);

                        when(mock.getForObject(contains("/actions/pending"), eq(List.class)))
                            .thenReturn(pendingActions);

                        // Cancel the action
                        Map<String, Object> cancelResponse = new LinkedHashMap<>();
                        cancelResponse.put("status", "CANCELLED");
                        when(mock.postForObject(contains("/actions/cancel"), any(), eq(Map.class)))
                            .thenReturn(cancelResponse);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Action Cancelled");
            }
        }

        @Test
        void cancelAction_noActionIdFromHistory_noPendingActions() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequest("cancel");

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.getForObject(contains("/actions/pending"), eq(List.class)))
                            .thenReturn(Collections.emptyList());
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("No pending actions");
            }
        }

        @Test
        void cancelAction_exception_returnsErrorMessage() {
            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CANCEL, Collections.emptyMap()));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.assistant("**Action ID:** `e00ac01230`"),
                ChatMessage.user("cancel")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenThrow(new RuntimeException("Network error"));
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Failed to cancel action");
                assertThat(content).contains("Network error");
            }
        }
    }

    // ---------------------------------------------------------------
    // Service Request execution tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("executeServiceRequest")
    class ExecuteServiceRequestTests {

        @Test
        void serviceRequest_processMessageThrows_returnsErrorMessage() {
            Map<String, String> params = new HashMap<>();
            params.put("query", "I need a laptop");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.SERVICE_REQUEST, params));
            when(guidedServiceCreator.processMessage(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Service catalog unavailable"));

            ChatCompletionRequest request = buildRequest("I need a laptop");

            Object result = controller.chatCompletions(null, null, request, httpServletResponse);

            assertThat(result).isInstanceOf(ResponseEntity.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
            String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
            assertThat(content).contains("Failed to process service request");
            assertThat(content).contains("Service catalog unavailable");
        }
    }

    // ---------------------------------------------------------------
    // Shutdown tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("shutdown")
    class ShutdownTests {

        @Test
        void shutdown_executesGracefully() {
            // Just verify shutdown does not throw
            controller.shutdown();
        }
    }

    // ---------------------------------------------------------------
    // Rate limit fallback tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("rateLimitFallback")
    class RateLimitFallbackTests {

        @Test
        void rateLimitFallback_nonStreaming_returns429() {
            ChatCompletionRequest request = buildRequest("test");

            Object result = ReflectionTestUtils.invokeMethod(
                controller, "chatCompletionRateLimitFallback",
                null, null, request, httpServletResponse,
                mock(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
            );

            assertThat(result).isInstanceOf(ResponseEntity.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) result;
            assertThat(responseEntity.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        void rateLimitFallback_streaming_returnsSseEmitter() {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("bmc-remedy-rag")
                .messages(List.of(ChatMessage.user("test")))
                .stream(true)
                .build();

            Object result = ReflectionTestUtils.invokeMethod(
                controller, "chatCompletionRateLimitFallback",
                null, null, request, httpServletResponse,
                mock(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
            );

            assertThat(result).isInstanceOf(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class);
            verify(httpServletResponse).setContentType("text/event-stream");
        }

        @Test
        void rateLimitFallback_nullRequest_returns429() {
            Object result = ReflectionTestUtils.invokeMethod(
                controller, "chatCompletionRateLimitFallback",
                null, null, null, httpServletResponse,
                mock(io.github.resilience4j.ratelimiter.RequestNotPermitted.class)
            );

            assertThat(result).isInstanceOf(ResponseEntity.class);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = (ResponseEntity<Map<String, Object>>) result;
            assertThat(responseEntity.getStatusCode().value()).isEqualTo(429);
        }
    }

    // ---------------------------------------------------------------
    // createToolCallResponse tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createToolCallResponse")
    class CreateToolCallResponseTests {

        @Test
        void createToolCallResponse_createIncident_returnsToolCall() {
            Map<String, String> params = Map.of("summary", "Test", "description", "Test desc");
            IntentResult intent = new IntentResult(Intent.CREATE_INCIDENT, params);

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNotNull();
            assertThat(result.getChoices()).hasSize(1);
            assertThat(result.getChoices().get(0).getMessage().getToolCalls().get(0)
                .getFunction().getName()).isEqualTo("create_incident");
        }

        @Test
        void createToolCallResponse_createIncident_blankSummary_returnsNull() {
            Map<String, String> params = Map.of("summary", "  ", "description", "desc");
            IntentResult intent = new IntentResult(Intent.CREATE_INCIDENT, params);

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNull();
        }

        @Test
        void createToolCallResponse_searchIncidents_returnsToolCall() {
            Map<String, String> params = Map.of("query", "VPN");
            IntentResult intent = new IntentResult(Intent.SEARCH_INCIDENTS, params);

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNotNull();
            assertThat(result.getChoices().get(0).getMessage().getToolCalls().get(0)
                .getFunction().getName()).isEqualTo("search_incidents");
        }

        @Test
        void createToolCallResponse_searchIncidents_blankQuery_returnsNull() {
            Map<String, String> params = Map.of("query", "");
            IntentResult intent = new IntentResult(Intent.SEARCH_INCIDENTS, params);

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNull();
        }

        @Test
        void createToolCallResponse_confirm_returnsToolCall() {
            IntentResult intent = new IntentResult(Intent.CONFIRM, Collections.emptyMap());

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNotNull();
            assertThat(result.getChoices().get(0).getMessage().getToolCalls().get(0)
                .getFunction().getName()).isEqualTo("confirm_action");
        }

        @Test
        void createToolCallResponse_cancel_returnsToolCall() {
            IntentResult intent = new IntentResult(Intent.CANCEL, Collections.emptyMap());

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNotNull();
            assertThat(result.getChoices().get(0).getMessage().getToolCalls().get(0)
                .getFunction().getName()).isEqualTo("confirm_action");
        }

        @Test
        void createToolCallResponse_none_returnsNull() {
            IntentResult intent = new IntentResult(Intent.NONE, Collections.emptyMap());

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            assertThat(result).isNull();
        }

        @Test
        void createToolCallResponse_getIncident_returnsNull() {
            Map<String, String> params = Map.of("incident_id", "INC123");
            IntentResult intent = new IntentResult(Intent.GET_INCIDENT, params);

            ToolCallResponse result = ReflectionTestUtils.invokeMethod(
                controller, "createToolCallResponse", intent
            );

            // GET_INCIDENT falls through to default case which returns null
            assertThat(result).isNull();
        }
    }

    // ---------------------------------------------------------------
    // formatSourcesCitations tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("formatSourcesCitations")
    class FormatSourcesCitationsTests {

        @Test
        void formatSourcesCitations_withDocuments_returnsFormattedReferences() {
            List<com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument> docs = List.of(
                new com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument(
                    "INCIDENT", "INC000123", "RESOLUTION", "content",
                    "title", "IT", "Support", 0.85f
                ),
                new com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument(
                    "INCIDENT", "INC000456", "NOTES", "other content",
                    "other title", "Network", "NOC", 0.72f
                )
            );

            String result = ReflectionTestUtils.invokeMethod(
                controller, "formatSourcesCitations", docs
            );

            assertThat(result).contains("References");
            assertThat(result).contains("INC000123");
            assertThat(result).contains("85%");
            assertThat(result).contains("INC000456");
            assertThat(result).contains("72%");
        }

        @Test
        void formatSourcesCitations_emptyDocuments_returnsEmptyString() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "formatSourcesCitations", Collections.emptyList()
            );

            assertThat(result).isEmpty();
        }

        @Test
        void formatSourcesCitations_nullDocuments_returnsEmptyString() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "formatSourcesCitations", (List<?>) null
            );

            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // generateSummary tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("generateSummary")
    class GenerateSummaryTests {

        @Test
        void generateSummary_nullDescription_returnsDefault() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", (String) null
            );
            assertThat(result).isEqualTo("New incident");
        }

        @Test
        void generateSummary_blankDescription_returnsDefault() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "   "
            );
            assertThat(result).isEqualTo("New incident");
        }

        @Test
        void generateSummary_workstationSlow_returnsWorkstationPerformance() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "My workstation is very slow"
            );
            assertThat(result).isEqualTo("Workstation performance issue");
        }

        @Test
        void generateSummary_computerSlow_returnsWorkstationPerformance() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "The computer has bad performance"
            );
            assertThat(result).isEqualTo("Workstation performance issue");
        }

        @Test
        void generateSummary_pcPerformance_returnsWorkstationPerformance() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "PC is slow and unresponsive"
            );
            assertThat(result).isEqualTo("Workstation performance issue");
        }

        @Test
        void generateSummary_printerIssue_returnsPrinterIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "The printer is jammed"
            );
            assertThat(result).isEqualTo("Printer issue");
        }

        @Test
        void generateSummary_printingIssue_returnsPrinterIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Printing is not working"
            );
            assertThat(result).isEqualTo("Printer issue");
        }

        @Test
        void generateSummary_vpnAuth_returnsVpnAuth() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "VPN authentication fails"
            );
            assertThat(result).isEqualTo("VPN authentication error");
        }

        @Test
        void generateSummary_vpnConnect_returnsVpnConnection() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Cannot connect to VPN"
            );
            assertThat(result).isEqualTo("VPN connection issue");
        }

        @Test
        void generateSummary_emailNotWorking_returnsEmailIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Email not working properly"
            );
            assertThat(result).isEqualTo("Email issue");
        }

        @Test
        void generateSummary_networkNotWorking_returnsNetworkIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Network not working"
            );
            assertThat(result).isEqualTo("Network connectivity issue");
        }

        @Test
        void generateSummary_passwordIssue_returnsLoginIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Password reset not working"
            );
            assertThat(result).isEqualTo("Login/authentication issue");
        }

        @Test
        void generateSummary_applicationCrash_returnsAppCrash() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Application keeps crashing"
            );
            assertThat(result).isEqualTo("Application crash/freeze");
        }

        @Test
        void generateSummary_applicationIssue_returnsAppIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Application showing wrong data"
            );
            assertThat(result).isEqualTo("Application issue");
        }

        @Test
        void generateSummary_generalSlowness_returnsPerformanceIssue() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "Everything is slow today"
            );
            assertThat(result).isEqualTo("Performance issue");
        }

        @Test
        void generateSummary_firstSentence_capitalized() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "cannot access files. Need help urgently."
            );
            assertThat(result).isEqualTo("Cannot access files");
        }

        @Test
        void generateSummary_longText_truncatedTo80() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) sb.append("a");
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", sb.toString()
            );
            assertThat(result).hasSize(80);
            // First letter capitalized
            assertThat(result.charAt(0)).isEqualTo('A');
        }

        @Test
        void generateSummary_shortText_capitalized() {
            String result = ReflectionTestUtils.invokeMethod(
                controller, "generateSummary", "some random text"
            );
            assertThat(result).isEqualTo("Some random text");
        }
    }

    // ---------------------------------------------------------------
    // isVagueReference tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("isVagueReference")
    class IsVagueReferenceTests {

        @Test
        void isVagueReference_null_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isVagueReference", (String) null
            );
            assertThat(result).isTrue();
        }

        @Test
        void isVagueReference_blank_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isVagueReference", "   "
            );
            assertThat(result).isTrue();
        }

        @Test
        void isVagueReference_theIssue_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isVagueReference", "the issue"
            );
            assertThat(result).isTrue();
        }

        @Test
        void isVagueReference_thisProblem_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isVagueReference", "this problem"
            );
            assertThat(result).isTrue();
        }

        @Test
        void isVagueReference_specificText_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isVagueReference", "VPN not working"
            );
            assertThat(result).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // containsProblemDescription tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("containsProblemDescription")
    class ContainsProblemDescriptionTests {

        @Test
        void containsProblemDescription_null_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "containsProblemDescription", (String) null
            );
            assertThat(result).isFalse();
        }

        @Test
        void containsProblemDescription_blank_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "containsProblemDescription", "   "
            );
            assertThat(result).isFalse();
        }

        @Test
        void containsProblemDescription_withProblemKeyword_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "containsProblemDescription", "Something is broken"
            );
            assertThat(result).isTrue();
        }

        @Test
        void containsProblemDescription_noProblemKeyword_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "containsProblemDescription", "Hello there"
            );
            assertThat(result).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // isIncidentCreationRequest tests (via reflection)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("isIncidentCreationRequest")
    class IsIncidentCreationRequestTests {

        @Test
        void isIncidentCreationRequest_null_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isIncidentCreationRequest", (String) null
            );
            assertThat(result).isFalse();
        }

        @Test
        void isIncidentCreationRequest_createIncident_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isIncidentCreationRequest", "Create an incident"
            );
            assertThat(result).isTrue();
        }

        @Test
        void isIncidentCreationRequest_withThisIssue_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isIncidentCreationRequest", "with this issue"
            );
            assertThat(result).isTrue();
        }

        @Test
        void isIncidentCreationRequest_forThisProblem_returnsTrue() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isIncidentCreationRequest", "for this problem"
            );
            assertThat(result).isTrue();
        }

        @Test
        void isIncidentCreationRequest_normalText_returnsFalse() {
            Boolean result = ReflectionTestUtils.invokeMethod(
                controller, "isIncidentCreationRequest", "My printer is jammed"
            );
            assertThat(result).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // enrichCreateIncidentFromHistory via integration flow
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("enrichCreateIncidentFromHistory integration")
    class EnrichCreateIncidentTests {

        @Test
        void enrichFromHistory_workstationIssue_generatesWorkstationSummary() {
            // The key here: the last user message is the creation request (which gets skipped
            // by isIncidentCreationRequest), and the second-to-last user message has the
            // workstation problem description.
            Map<String, String> params = new HashMap<>();
            params.put("summary", "this issue");
            params.put("description", "with this issue");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.user("My workstation is very slow"),
                ChatMessage.user("Open an incident for this issue")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "STAGED");
                        response.put("preview", "Workstation performance issue");
                        response.put("actionId", "aaa111bbb222");
                        response.put("expiresAt", "2025-01-01T12:05:00Z");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("Incident Staged");
            }
        }

        @Test
        void enrichFromHistory_printerIssue_generatesPrinterSummary() {
            Map<String, String> params = new HashMap<>();
            params.put("summary", "the problem");
            params.put("description", "the problem");

            when(guidedServiceCreator.hasActiveFlow(anyString())).thenReturn(false);
            when(toolIntentDetector.detectIntent(anyString(), anyBoolean()))
                .thenReturn(new IntentResult(Intent.CREATE_INCIDENT, params));

            ChatCompletionRequest request = buildRequestWithHistory(List.of(
                ChatMessage.user("The printer on floor 3 is jammed"),
                ChatMessage.user("Create a ticket for this problem")
            ));

            try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                    (mock, context) -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("status", "CREATED");
                        response.put("incidentNumber", "INC000777");
                        when(mock.postForObject(anyString(), any(), eq(Map.class)))
                            .thenReturn(response);
                    })) {

                Object result = controller.chatCompletions(null, null, request, httpServletResponse);

                assertThat(result).isInstanceOf(ResponseEntity.class);
                @SuppressWarnings("unchecked")
                ResponseEntity<ChatCompletionResponse> responseEntity = (ResponseEntity<ChatCompletionResponse>) result;
                String content = responseEntity.getBody().getChoices().get(0).getMessage().getContent();
                assertThat(content).contains("INC000777");
            }
        }
    }
}
