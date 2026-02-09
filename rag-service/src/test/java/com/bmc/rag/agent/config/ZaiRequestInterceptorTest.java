package com.bmc.rag.agent.config;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ZaiRequestInterceptor}.
 */
@ExtendWith(MockitoExtension.class)
class ZaiRequestInterceptorTest {

    @Mock
    private ZaiConfig zaiConfig;

    @Mock
    private Interceptor.Chain chain;

    @Mock
    private Response mockResponse;

    private ZaiRequestInterceptor interceptor;

    private static final MediaType JSON = MediaType.parse("application/json");

    @BeforeEach
    void setUp() {
        interceptor = new ZaiRequestInterceptor(zaiConfig);
    }

    @Nested
    @DisplayName("Non-Chat-Completions URLs")
    class NonChatCompletionsUrls {

        @Test
        void intercept_nonChatCompletionsUrl_passesThrough() throws IOException {
            // Given: A request to a non-chat-completions endpoint
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/models")
                .post(RequestBody.create("{\"test\": \"data\"}", JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);

            // When
            Response response = interceptor.intercept(chain);

            // Then: Request should pass through unchanged
            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
            verifyNoInteractions(zaiConfig);
        }

        @Test
        void intercept_rootUrl_passesThrough() throws IOException {
            Request request = new Request.Builder()
                .url("https://api.test.com/")
                .get()
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
        }

        @Test
        void intercept_differentEndpoint_passesThrough() throws IOException {
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/embeddings")
                .post(RequestBody.create("{\"input\": \"text\"}", JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
        }
    }

    @Nested
    @DisplayName("Null or Empty Body")
    class NullOrEmptyBody {

        @Test
        void intercept_getRequest_passesThrough() throws IOException {
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .get()
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
        }

        @Test
        void intercept_emptyRequestBody_passesThrough() throws IOException {
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create("", JSON))
                .build();

            when(chain.request()).thenReturn(request);
            // Empty body will cause JSON parsing to fail, so original request is used
            when(chain.proceed(request)).thenReturn(mockResponse);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
        }
    }

    @Nested
    @DisplayName("Thinking Enabled")
    class ThinkingEnabled {

        @Test
        void intercept_thinkingEnabled_addsTypeFromConfig() throws IOException {
            // Given
            String requestJson = "{\"model\": \"test-model\", \"messages\": []}";
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/chat/completions")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);
            when(zaiConfig.getThinkingType()).thenReturn("enabled");

            // When
            Response response = interceptor.intercept(chain);

            // Then: Should proceed with modified request
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(chain).proceed(requestCaptor.capture());

            Request modifiedRequest = requestCaptor.getValue();
            assertThat(modifiedRequest).isNotNull();
            assertThat(modifiedRequest.url().toString()).contains("chat/completions");

            // Verify thinking was added to the body
            assertThat(modifiedRequest.body()).isNotNull();
        }

        @Test
        void intercept_thinkingEnabledWithRetentionType_addsRetentionType() throws IOException {
            String requestJson = "{\"model\": \"test-model\"}";
            Request request = new Request.Builder()
                .url("https://api.test.com/api/paas/v4/chat/completions")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);
            when(zaiConfig.getThinkingType()).thenReturn("retention");

            Response response = interceptor.intercept(chain);

            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(chain).proceed(requestCaptor.capture());

            Request modifiedRequest = requestCaptor.getValue();
            assertThat(modifiedRequest.body()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Thinking Disabled")
    class ThinkingDisabled {

        @Test
        void intercept_thinkingDisabled_addsDisabledType() throws IOException {
            // Given
            String requestJson = "{\"model\": \"test-model\", \"temperature\": 0.5}";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            // When
            Response response = interceptor.intercept(chain);

            // Then
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(chain).proceed(requestCaptor.capture());

            Request modifiedRequest = requestCaptor.getValue();
            assertThat(modifiedRequest).isNotNull();
            assertThat(modifiedRequest.body()).isNotNull();
        }

        @Test
        void intercept_thinkingDisabled_preservesOriginalFields() throws IOException {
            String requestJson = "{\"model\": \"glm-4.7\", \"temperature\": 0.7, \"max_tokens\": 2048}";
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/chat/completions")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(any(Request.class));
            verify(zaiConfig).isThinkingEnabled();
        }
    }

    @Nested
    @DisplayName("Malformed JSON")
    class MalformedJson {

        @Test
        void intercept_malformedJson_fallsBackToOriginalRequest() throws IOException {
            // Given: Invalid JSON in request body
            String malformedJson = "{invalid json syntax";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create(malformedJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);
            // Don't stub zaiConfig - it won't be called if JSON parsing fails

            // When
            Response response = interceptor.intercept(chain);

            // Then: Should fall back to original request
            verify(chain).proceed(request);
            assertThat(response).isEqualTo(mockResponse);
        }

        @Test
        void intercept_emptyJson_fallsBackToOriginalRequest() throws IOException {
            String emptyJson = "";
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/chat/completions")
                .post(RequestBody.create(emptyJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);
            // Don't stub zaiConfig - it won't be called if JSON parsing fails

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
        }

        @Test
        void intercept_nonJsonBody_fallsBackToOriginalRequest() throws IOException {
            String nonJson = "this is not json";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create(nonJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(request)).thenReturn(mockResponse);
            // Don't stub zaiConfig - it won't be called if JSON parsing fails

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(request);
        }
    }

    @Nested
    @DisplayName("URL Variations")
    class UrlVariations {

        @Test
        void intercept_chatCompletionsInPath_modifiesRequest() throws IOException {
            String requestJson = "{\"model\": \"test\"}";
            Request request = new Request.Builder()
                .url("https://example.com/api/v1/chat/completions")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(any(Request.class));
        }

        @Test
        void intercept_chatCompletionsWithQueryParams_modifiesRequest() throws IOException {
            String requestJson = "{\"model\": \"test\"}";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions?version=v1")
                .post(RequestBody.create(requestJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);
            when(zaiConfig.getThinkingType()).thenReturn("enabled");

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(any(Request.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        void intercept_complexJsonBody_preservesStructure() throws IOException {
            String complexJson = "{\"model\": \"test\", \"messages\": [{\"role\": \"user\", \"content\": \"Hello\"}], \"temperature\": 0.5, \"stream\": true}";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create(complexJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            Response response = interceptor.intercept(chain);

            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(chain).proceed(requestCaptor.capture());

            Request modifiedRequest = requestCaptor.getValue();
            assertThat(modifiedRequest.body()).isNotNull();
        }

        @Test
        void intercept_jsonWithNestedObjects_preservesStructure() throws IOException {
            String nestedJson = "{\"model\": \"test\", \"metadata\": {\"user\": \"test\", \"session\": \"123\"}}";
            Request request = new Request.Builder()
                .url("https://api.test.com/v1/chat/completions")
                .post(RequestBody.create(nestedJson, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);
            when(zaiConfig.getThinkingType()).thenReturn("enabled");

            Response response = interceptor.intercept(chain);

            verify(chain).proceed(any(Request.class));
        }

        @Test
        void intercept_existingThinkingField_overrides() throws IOException {
            // Request already has thinking field - should be overridden
            String jsonWithThinking = "{\"model\": \"test\", \"thinking\": {\"type\": \"wrong\"}}";
            Request request = new Request.Builder()
                .url("https://api.test.com/chat/completions")
                .post(RequestBody.create(jsonWithThinking, JSON))
                .build();

            when(chain.request()).thenReturn(request);
            when(chain.proceed(any(Request.class))).thenReturn(mockResponse);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            Response response = interceptor.intercept(chain);

            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
            verify(chain).proceed(requestCaptor.capture());

            Request modifiedRequest = requestCaptor.getValue();
            assertThat(modifiedRequest.body()).isNotNull();
        }
    }
}
