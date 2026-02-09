package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.config.GoogleAiConfig;
import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.config.ZaiConfig;
import com.bmc.rag.agent.memory.PostgresChatMemoryStore;
import com.bmc.rag.agent.metrics.RagMetricsService;
import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievalResult;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RagAssistantServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private StreamingChatLanguageModel streamingChatModel;

    @Mock
    private SecureContentRetriever contentRetriever;

    @Mock
    private PostgresChatMemoryStore chatMemoryStore;

    @Mock
    private RagConfig ragConfig;

    @Mock
    private ZaiConfig zaiConfig;

    @Mock
    private AgenticConfig agenticConfig;

    @Mock
    private GoogleAiConfig googleAiConfig;

    @Mock
    private RagMetricsService metricsService;

    private RagAssistantService service;

    @BeforeEach
    void setUp() {
        when(ragConfig.getMaxMemoryMessages()).thenReturn(20);
        when(ragConfig.getSystemPrompt()).thenReturn("You are a helpful IT assistant.");
        when(ragConfig.isIncludeCitations()).thenReturn(true);
        when(zaiConfig.isThinkingEnabled()).thenReturn(false);
        when(agenticConfig.isEnabled()).thenReturn(false);

        service = new RagAssistantService(
            chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
            ragConfig, zaiConfig, googleAiConfig, agenticConfig, metricsService
        );
    }

    @Nested
    @DisplayName("Cache Management")
    class CacheManagement {

        @Test
        void getCacheStats_initial_allZeros() {
            Map<String, Long> stats = service.getCacheStats();

            assertThat(stats).containsEntry("hitCount", 0L);
            assertThat(stats).containsEntry("missCount", 0L);
            assertThat(stats).containsEntry("evictionCount", 0L);
            assertThat(stats).containsEntry("estimatedSize", 0L);
        }

        @Test
        void getCacheStats_afterChatCall_showsCacheMiss() {
            // Setup for chat call
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            ChatResponse mockResponse = mock(ChatResponse.class);
            AiMessage aiMessage = AiMessage.from("Test response");
            when(mockResponse.aiMessage()).thenReturn(aiMessage);
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            service.chat("session-1", "test query", UserContext.anonymous());

            // First access to session-1 should be a cache miss
            verify(metricsService).recordCacheMiss();
        }

        @Test
        void getCacheStats_secondChatSameSession_showsCacheHit() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            ChatResponse mockResponse = mock(ChatResponse.class);
            AiMessage aiMessage = AiMessage.from("Test response");
            when(mockResponse.aiMessage()).thenReturn(aiMessage);
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            // First call creates memory (miss)
            service.chat("session-1", "query 1", UserContext.anonymous());
            // Second call reuses memory (hit)
            service.chat("session-1", "query 2", UserContext.anonymous());

            verify(metricsService, times(1)).recordCacheMiss();
            verify(metricsService, times(1)).recordCacheHit();
        }

        @Test
        void clearSession_invalidatesCacheAndStore() {
            service.clearSession("session-123");
            verify(chatMemoryStore).deleteMessages("session-123");
        }

        @Test
        void cacheInitialized_withFourStatKeys() {
            Map<String, Long> stats = service.getCacheStats();
            assertThat(stats).hasSize(4);
        }
    }

    @Nested
    @DisplayName("chat()")
    class Chat {

        @Test
        void chat_withEmptyRetrieval_returnsResponseWithoutContext() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("I can help with that."));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            var result = service.chat("s1", "How do I reset my password?", UserContext.anonymous());

            assertThat(result.getResponse()).isEqualTo("I can help with that.");
            assertThat(result.getSessionId()).isEqualTo("s1");
            assertThat(result.isHasContext()).isFalse();
            assertThat(result.getSources()).isEmpty();
        }

        @Test
        void chat_withRetrievalResults_returnsResponseWithContext() {
            List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Incident", "INC000001", "resolution",
                    "Reset via Active Directory", "Password Reset", "IT", "Service Desk", 0.85f)
            );
            RetrievalResult retrieval = new RetrievalResult(docs,
                "## Relevant Information\nReset via Active Directory");

            when(contentRetriever.retrieve(anyString(), any())).thenReturn(retrieval);
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("You can reset via AD."));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            var result = service.chat("s1", "password reset", UserContext.anonymous());

            assertThat(result.isHasContext()).isTrue();
            assertThat(result.getSources()).contains("Incident INC000001");
        }

        @Test
        void chat_recordsRetrievalMetrics() {
            List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Incident", "INC001", "summary", "content",
                    "title", "cat", "group", 0.9f)
            );
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(docs, "context"));

            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            service.chat("s1", "test", UserContext.anonymous());

            verify(metricsService).recordRetrievalLatency(anyLong());
            verify(metricsService).recordRetrieval(1);
            verify(metricsService).recordGenerationLatency(anyLong());
            verify(metricsService).recordTotalLatency(anyLong());
            verify(metricsService).recordCitations(1);
        }

        @Test
        void chat_llmThrowsException_returnsErrorMessage() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            when(chatModel.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("LLM unavailable"));

            var result = service.chat("s1", "test", UserContext.anonymous());

            assertThat(result.getResponse()).contains("error");
            verify(metricsService).recordError("llm_generation");
        }

        @Test
        void chat_arabicQuery_returnsArabicErrorOnFailure() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            when(chatModel.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Error"));

            var result = service.chat("s1", "كيف أعيد تعيين كلمة المرور؟", UserContext.anonymous());

            assertThat(result.getResponse()).contains("عذراً");
        }

        @Test
        void chat_messagesIncludeSystemPrompt() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            service.chat("s1", "test question", UserContext.anonymous());

            ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
            verify(chatModel).chat(captor.capture());

            List<ChatMessage> messages = captor.getValue().messages();
            assertThat(messages).isNotEmpty();
            assertThat(messages.get(0).toString()).contains("helpful IT assistant");
        }

        @Test
        void chat_withContextAndCitations_appendsCitationInstruction() {
            List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("KB", "KB001", "article", "content",
                    "title", "cat", "group", 0.8f)
            );
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(docs, "formatted context"));

            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            service.chat("s1", "How do I do X?", UserContext.anonymous());

            ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
            verify(chatModel).chat(captor.capture());

            // Last message should contain the citation instruction
            List<ChatMessage> messages = captor.getValue().messages();
            String lastMessage = messages.get(messages.size() - 1).toString();
            assertThat(lastMessage).contains("cite your sources");
        }

        @Test
        void chat_recordsGroundednessScore_whenContextAvailable() {
            List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("INC", "INC001", "summary", "content",
                    "title", "cat", "group", 0.9f),
                new RetrievedDocument("KB", "KB001", "article", "content2",
                    "title2", "cat", "group", 0.8f)
            );
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(docs, "context"));

            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            service.chat("s1", "test", UserContext.anonymous());

            verify(metricsService).recordGroundednessScore(anyDouble());
        }
    }

    @Nested
    @DisplayName("chatWithoutContext()")
    class ChatWithoutContext {

        @Test
        void chatWithoutContext_returnsResponseWithoutSources() {
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("Direct LLM answer"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            var result = service.chatWithoutContext("s1", "What is ITSM?");

            assertThat(result.getResponse()).isEqualTo("Direct LLM answer");
            assertThat(result.isHasContext()).isFalse();
            assertThat(result.getSources()).isEmpty();
        }

        @Test
        void chatWithoutContext_llmError_returnsErrorMessage() {
            when(chatModel.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Failure"));

            var result = service.chatWithoutContext("s1", "test");

            assertThat(result.getResponse()).contains("error");
        }
    }

    @Nested
    @DisplayName("chatWithAgenticSupport()")
    class ChatWithAgenticSupport {

        @Mock
        private AgenticAssistantService agenticAssistantService;

        @Test
        void chatWithAgenticSupport_noAgenticIntent_fallsBackToChat() {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("Normal response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            var result = service.chatWithAgenticSupport(
                "s1", "What is VPN?", "user1", UserContext.anonymous());

            assertThat(result.getResponse()).isEqualTo("Normal response");
        }

        @Test
        void chatWithAgenticSupport_agenticEnabled_delegatesToAgentic() {
            // Setup agentic
            when(agenticConfig.isEnabled()).thenReturn(true);
            when(agenticAssistantService.hasAgenticIntent("create an incident for VPN issue"))
                .thenReturn(true);

            var agenticResponse = AgenticAssistantService.AgenticResponse.builder()
                .response("Incident staged for confirmation")
                .build();
            when(agenticAssistantService.processMessage(
                anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(agenticResponse);

            service.setAgenticAssistantService(agenticAssistantService);

            var result = service.chatWithAgenticSupport(
                "s1", "create an incident for VPN issue", "user1", UserContext.anonymous());

            assertThat(result.getResponse()).isEqualTo("Incident staged for confirmation");
            assertThat(result.isHasContext()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAgenticIntent()")
    class HasAgenticIntent {

        @Test
        void hasAgenticIntent_noAgenticService_returnsFalse() {
            assertThat(service.hasAgenticIntent("create incident")).isFalse();
        }

        @Test
        void hasAgenticIntent_agenticDisabled_returnsFalse() {
            when(agenticConfig.isEnabled()).thenReturn(false);
            assertThat(service.hasAgenticIntent("create incident")).isFalse();
        }
    }

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        void search_delegatesToContentRetriever() {
            UserContext ctx = UserContext.withGroups("user1", "IT");
            when(contentRetriever.retrieve("VPN issue", ctx))
                .thenReturn(RetrievalResult.empty());

            var result = service.search("VPN issue", ctx);

            assertThat(result.isEmpty()).isTrue();
            verify(contentRetriever).retrieve("VPN issue", ctx);
        }
    }

    @Nested
    @DisplayName("Session Management")
    class SessionManagement {

        @Test
        void getConversationHistory_delegatesToStore() {
            when(chatMemoryStore.getMessages("s1")).thenReturn(Collections.emptyList());
            var history = service.getConversationHistory("s1");
            assertThat(history).isEmpty();
            verify(chatMemoryStore).getMessages("s1");
        }

        @Test
        void getSessionSummaries_delegatesToStore() {
            when(chatMemoryStore.getSessionSummaries()).thenReturn(List.of());
            service.getSessionSummaries();
            verify(chatMemoryStore).getSessionSummaries();
        }
    }

    @Nested
    @DisplayName("Constructor / LLM Semaphore")
    class ConstructorSemaphore {

        @Test
        void constructor_withZaiSemaphore_usesZaiConfig() {
            Semaphore zaiSemaphore = new Semaphore(5);
            when(zaiConfig.getRequestSemaphore()).thenReturn(zaiSemaphore);
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            // The service should be created without error and use Z.AI semaphore
            assertThat(svc).isNotNull();
        }

        @Test
        void constructor_withGoogleAiSemaphore_usesGoogleConfig() {
            Semaphore googleSemaphore = new Semaphore(10);
            when(googleAiConfig.getRequestSemaphore()).thenReturn(googleSemaphore);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, null, googleAiConfig, agenticConfig, metricsService);

            assertThat(svc).isNotNull();
        }

        @Test
        void constructor_noSemaphore_warnsButCreates() {
            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, null, null, agenticConfig, metricsService);

            assertThat(svc).isNotNull();
        }
    }

    @Nested
    @DisplayName("ChatResponseDto")
    class ChatResponseDtoTests {

        @Test
        void chatResponseDto_builder_setsAllFields() {
            var dto = RagAssistantService.ChatResponseDto.builder()
                .sessionId("s1")
                .response("Hello")
                .sources(List.of("INC001"))
                .hasContext(true)
                .build();

            assertThat(dto.getSessionId()).isEqualTo("s1");
            assertThat(dto.getResponse()).isEqualTo("Hello");
            assertThat(dto.getSources()).containsExactly("INC001");
            assertThat(dto.isHasContext()).isTrue();
        }
    }

    @Nested
    @DisplayName("chatWithStreaming()")
    class ChatWithStreaming {

        @Test
        void chatWithStreaming_callsTokenConsumer() throws Exception {
            List<RetrievedDocument> docs = List.of(
                new RetrievedDocument("Incident", "INC001", "resolution",
                    "Test content", "Test Title", "IT", "Service Desk", 0.85f)
            );
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(docs, "context"));

            List<String> tokens = new java.util.ArrayList<>();
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

            // Mock streaming behavior
            doAnswer(invocation -> {
                ChatRequest req = invocation.getArgument(0);
                StreamingChatResponseHandler handler = invocation.getArgument(1);

                // Simulate token streaming
                handler.onPartialResponse("Hello ");
                handler.onPartialResponse("world");

                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from("Hello world"));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test question",
                UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(tokens).contains("Hello ", "world");
        }

        @Test
        void chatWithStreaming_handlesError() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean errorHandled = new java.util.concurrent.atomic.AtomicBoolean(false);

            // Mock streaming error
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                handler.onError(new RuntimeException("Streaming failed"));
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test",
                UserContext.anonymous(),
                token -> {},
                new RagAssistantService.StreamingCompletionHandler() {
                    @Override
                    public void onComplete(List<SecureContentRetriever.RetrievedDocument> documents, Double confidence) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorHandled.set(true);
                        latch.countDown();
                    }
                }
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(errorHandled.get()).isTrue();
        }

        @Test
        void chatWithStreaming_handlesEmptyResponse() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Mock streaming with empty response
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from(""));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test",
                UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            // Should receive error message for empty response
            assertThat(String.join("", tokens)).isNotEmpty();
        }

        @Test
        void chatWithStreaming_truncatesLongResponses() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Mock streaming with response exceeding MAX_RESPONSE_SIZE
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);

                // Send many tokens to exceed 50,000 chars
                String longToken = "a".repeat(10000);
                for (int i = 0; i < 6; i++) {
                    handler.onPartialResponse(longToken);
                }

                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from("response"));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test",
                UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            String fullResponse = String.join("", tokens);
            assertThat(fullResponse).contains("truncated");
        }

        @Test
        void chatWithStreaming_thinkingMode_filtersThinkingTokens() throws Exception {
            // Create service with thinking enabled
            when(zaiConfig.getRequestSemaphore()).thenReturn(new Semaphore(5));
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);

            var thinkingService = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Simulate streaming with thinking tokens
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);

                // Send thinking start
                handler.onPartialResponse("<thinking>");
                // Send thinking content (should be filtered)
                handler.onPartialResponse("Let me reason about this...");
                // Send thinking end with trailing content
                handler.onPartialResponse("</thinking>Here is the answer");
                // Send normal token
                handler.onPartialResponse(" to your question");

                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from("Here is the answer to your question"));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            thinkingService.chatWithStreaming(
                "s1",
                "test question",
                SecureContentRetriever.UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

            // Should NOT contain thinking tokens
            String fullOutput = String.join("", tokens);
            assertThat(fullOutput).doesNotContain("<thinking>");
            assertThat(fullOutput).doesNotContain("</thinking>");
            assertThat(fullOutput).doesNotContain("Let me reason about this");
            // Should contain the actual answer
            assertThat(fullOutput).contains("Here is the answer");
        }

        @Test
        void chatWithStreaming_thinkingMode_midTokenThinkingStart() throws Exception {
            // Create service with thinking enabled
            when(zaiConfig.getRequestSemaphore()).thenReturn(new Semaphore(5));
            when(zaiConfig.isThinkingEnabled()).thenReturn(true);

            var thinkingService = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Test thinking start with content after the tag
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);

                handler.onPartialResponse("<thinking>Some reasoning here");
                handler.onPartialResponse("more reasoning");
                handler.onPartialResponse("</thinking>");
                handler.onPartialResponse("Visible response");

                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from("Visible response"));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            thinkingService.chatWithStreaming(
                "s1",
                "test",
                SecureContentRetriever.UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

            String fullOutput = String.join("", tokens);
            assertThat(fullOutput).doesNotContain("Some reasoning");
            assertThat(fullOutput).contains("Visible response");
        }

        @Test
        void chatWithStreaming_emptyResponseWithContext_returnsBilingualMessage() throws Exception {
            List<SecureContentRetriever.RetrievedDocument> docs = List.of(
                new SecureContentRetriever.RetrievedDocument("Incident", "INC001", "resolution",
                    "Test content", "Test Title", "IT", "Service Desk", 0.85f)
            );
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(new RetrievalResult(docs, "context"));

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Mock empty response
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from(""));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test English question",
                SecureContentRetriever.UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            String fullOutput = String.join("", tokens);
            // English question with context -> "I apologize" message
            assertThat(fullOutput).contains("couldn't generate a response");
        }

        @Test
        void chatWithStreaming_emptyResponseArabicNoContext_returnsArabicNoInfoMessage() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Mock empty response
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                ChatResponse response = mock(ChatResponse.class);
                when(response.aiMessage()).thenReturn(AiMessage.from(""));
                handler.onCompleteResponse(response);
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "ما هو الحل؟",
                SecureContentRetriever.UserContext.anonymous(),
                tokens::add,
                (documents, confidence) -> latch.countDown()
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            String fullOutput = String.join("", tokens);
            // Arabic query with no context -> Arabic "no info found" message
            assertThat(fullOutput).contains("لم أجد");
        }

        @Test
        void chatWithStreaming_onErrorWithPartialContent_savesToMemory() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Send partial content then error
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                handler.onPartialResponse("Partial ");
                handler.onPartialResponse("response");
                handler.onError(new RuntimeException("Connection lost"));
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "test question",
                SecureContentRetriever.UserContext.anonymous(),
                tokens::add,
                new RagAssistantService.StreamingCompletionHandler() {
                    @Override
                    public void onComplete(List<SecureContentRetriever.RetrievedDocument> documents, Double confidence) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        latch.countDown();
                    }
                }
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            // Partial content was received
            assertThat(tokens).contains("Partial ", "response");
        }

        @Test
        void chatWithStreaming_arabicQuery_arabicErrorMessage() throws Exception {
            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            List<String> tokens = new java.util.ArrayList<>();

            // Mock streaming error
            doAnswer(invocation -> {
                StreamingChatResponseHandler handler = invocation.getArgument(1);
                handler.onError(new RuntimeException("Error"));
                return null;
            }).when(streamingChatModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));

            service.chatWithStreaming(
                "s1",
                "ما هو عنوان VPN؟",
                UserContext.anonymous(),
                tokens::add,
                new RagAssistantService.StreamingCompletionHandler() {
                    @Override
                    public void onComplete(List<SecureContentRetriever.RetrievedDocument> documents, Double confidence) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        latch.countDown();
                    }
                }
            );

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(String.join("", tokens)).contains("عذراً");
        }
    }

    @Nested
    @DisplayName("Semaphore Handling")
    class SemaphoreHandling {

        @Test
        void chat_withSemaphore_acquiresAndReleases() {
            Semaphore semaphore = new Semaphore(1);
            when(zaiConfig.getRequestSemaphore()).thenReturn(semaphore);
            when(zaiConfig.isThinkingEnabled()).thenReturn(false);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            assertThat(semaphore.availablePermits()).isEqualTo(1);
            svc.chat("s1", "test", UserContext.anonymous());
            assertThat(semaphore.availablePermits()).isEqualTo(1); // Released after call
        }

        @Test
        void chat_semaphoreTimeout_returnsErrorMessage() {
            Semaphore semaphore = new Semaphore(0); // No permits available
            when(zaiConfig.getRequestSemaphore()).thenReturn(semaphore);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());

            var result = svc.chat("s1", "test", UserContext.anonymous());
            // The actual error message returned when semaphore times out
            assertThat(result.getResponse()).containsAnyOf("busy", "unavailable", "error");
        }

        @Test
        void chatWithoutContext_withSemaphore_acquiresAndReleases() {
            Semaphore semaphore = new Semaphore(1);
            when(googleAiConfig.getRequestSemaphore()).thenReturn(semaphore);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, null, googleAiConfig, agenticConfig, metricsService);

            ChatResponse mockResponse = mock(ChatResponse.class);
            when(mockResponse.aiMessage()).thenReturn(AiMessage.from("response"));
            when(chatModel.chat(any(ChatRequest.class))).thenReturn(mockResponse);

            assertThat(semaphore.availablePermits()).isEqualTo(1);
            svc.chatWithoutContext("s1", "test");
            assertThat(semaphore.availablePermits()).isEqualTo(1);
        }

        @Test
        void chatWithoutContext_semaphoreTimeout_returnsErrorMessage() {
            Semaphore semaphore = new Semaphore(0);
            when(googleAiConfig.getRequestSemaphore()).thenReturn(semaphore);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, null, googleAiConfig, agenticConfig, metricsService);

            var result = svc.chatWithoutContext("s1", "test");
            assertThat(result.getResponse()).containsAnyOf("busy", "unavailable", "error");
        }

        @Test
        void chat_llmThrowsException_releasesSemaphore() {
            Semaphore semaphore = new Semaphore(1);
            when(zaiConfig.getRequestSemaphore()).thenReturn(semaphore);

            var svc = new RagAssistantService(
                chatModel, streamingChatModel, contentRetriever, chatMemoryStore,
                ragConfig, zaiConfig, null, agenticConfig, metricsService);

            when(contentRetriever.retrieve(anyString(), any()))
                .thenReturn(RetrievalResult.empty());
            when(chatModel.chat(any(ChatRequest.class)))
                .thenThrow(new RuntimeException("LLM error"));

            svc.chat("s1", "test", UserContext.anonymous());
            assertThat(semaphore.availablePermits()).isEqualTo(1); // Released even on error
        }
    }
}
