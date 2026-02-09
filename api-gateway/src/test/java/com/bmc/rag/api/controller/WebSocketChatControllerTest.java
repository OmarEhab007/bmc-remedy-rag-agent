package com.bmc.rag.api.controller;

import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.RetrievedDocument;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.agent.service.RagAssistantService;
import com.bmc.rag.agent.service.RagAssistantService.ChatResponseDto;
import com.bmc.rag.agent.service.RagAssistantService.StreamingCompletionHandler;
import com.bmc.rag.api.dto.ChatQueryMessage;
import com.bmc.rag.api.dto.ChatResponseChunk;
import com.bmc.rag.api.dto.ChatResponseChunk.ChunkType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Map;

/**
 * Unit tests for WebSocketChatController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebSocketChatController Tests")
class WebSocketChatControllerTest {

    @Mock
    private RagAssistantService ragAssistantService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Captor
    private ArgumentCaptor<ChatResponseChunk> chunkCaptor;

    @Captor
    private ArgumentCaptor<UserContext> userContextCaptor;

    @Captor
    private ArgumentCaptor<Consumer<String>> tokenConsumerCaptor;

    @Captor
    private ArgumentCaptor<StreamingCompletionHandler> completionHandlerCaptor;

    private WebSocketChatController controller;

    @BeforeEach
    void setUp() {
        controller = new WebSocketChatController(ragAssistantService, messagingTemplate);
        when(headerAccessor.getSessionId()).thenReturn("ws-session-123");
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {

        @Test
        @DisplayName("handleChatQuery_nullText_sendsErrorChunk")
        void handleChatQuery_nullText_sendsErrorChunk() {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text(null)
                .sessionId("session-456")
                .userId("user1")
                .build();

            controller.handleChatQuery(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            ChatResponseChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getType()).isEqualTo(ChunkType.ERROR);
            assertThat(chunk.getError()).contains("cannot be empty");
            assertThat(chunk.isComplete()).isTrue();
        }

        @Test
        @DisplayName("handleChatQuery_blankText_sendsErrorChunk")
        void handleChatQuery_blankText_sendsErrorChunk() {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("   ")
                .sessionId("session-456")
                .userId("user1")
                .build();

            controller.handleChatQuery(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            ChatResponseChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getType()).isEqualTo(ChunkType.ERROR);
            assertThat(chunk.getError()).contains("cannot be empty");
        }

        @Test
        @DisplayName("handleChatQuery_exceedsMaxLength_sendsErrorChunk")
        void handleChatQuery_exceedsMaxLength_sendsErrorChunk() {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("a".repeat(10001))
                .sessionId("session-456")
                .userId("user1")
                .build();

            controller.handleChatQuery(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            ChatResponseChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getType()).isEqualTo(ChunkType.ERROR);
            assertThat(chunk.getError()).contains("exceeds maximum length");
        }

        @Test
        @DisplayName("handleChatQuery_validInput_sendsThinkingChunk")
        void handleChatQuery_validInput_sendsThinkingChunk() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("What is network connectivity?")
                .sessionId("session-456")
                .userId("user1")
                .userGroups(Set.of("IT Support"))
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            controller.handleChatQuery(message, headerAccessor);

            // Wait for async processing to start
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            // First chunk should be THINKING
            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).getType()).isEqualTo(ChunkType.THINKING);
        }
    }

    @Nested
    @DisplayName("Session ID Generation Tests")
    class SessionIdGenerationTests {

        @Test
        @DisplayName("handleChatQuery_noSessionId_generatesNew")
        void handleChatQuery_noSessionId_generatesNew() {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("test query")
                .sessionId(null)
                .userId("user1")
                .build();

            controller.handleChatQuery(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            ChatResponseChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getSessionId()).isNotNull();
            assertThat(chunk.getSessionId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("handleChatQuery_existingSessionId_preserves")
        void handleChatQuery_existingSessionId_preserves() {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("test query")
                .sessionId("session-existing-456")
                .userId("user1")
                .build();

            controller.handleChatQuery(message, headerAccessor);

            verify(messagingTemplate).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            ChatResponseChunk chunk = chunkCaptor.getValue();
            assertThat(chunk.getSessionId()).isEqualTo("session-existing-456");
        }
    }

    @Nested
    @DisplayName("Streaming Response Tests")
    class StreamingResponseTests {

        @Test
        @DisplayName("handleChatQuery_nonAgenticRequest_callsStreamingChat")
        void handleChatQuery_nonAgenticRequest_callsStreamingChat() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("What is VPN?")
                .sessionId("session-456")
                .userId("user1")
                .userGroups(Set.of("IT Support"))
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                any(UserContext.class),
                any(),
                any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(ragAssistantService).chatWithStreaming(
                eq("session-456"),
                eq("What is VPN?"),
                userContextCaptor.capture(),
                any(),
                any()
            );

            UserContext capturedContext = userContextCaptor.getValue();
            assertThat(capturedContext.userId()).isEqualTo("user1");
            assertThat(capturedContext.groups()).containsExactly("IT Support");
        }

        @Test
        @DisplayName("handleChatQuery_streaming_consumesTokens")
        void handleChatQuery_streaming_consumesTokens() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Explain VPN")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Consumer<String> tokenConsumer = invocation.getArgument(3);
                tokenConsumer.accept("VPN ");
                tokenConsumer.accept("stands ");
                tokenConsumer.accept("for ");
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                any(UserContext.class),
                any(),
                any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Wait a bit for message sending
            Thread.sleep(100);

            verify(messagingTemplate, atLeast(4)).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                any(ChatResponseChunk.class),
                anyMap()
            );
        }

        @Test
        @DisplayName("handleChatQuery_streamingComplete_sendsCitationsAndConfidence")
        void handleChatQuery_streamingComplete_sendsCitationsAndConfidence() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Explain network")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            List<RetrievedDocument> mockDocs = List.of(
                new RetrievedDocument("INCIDENT", "INC0001", "RESOLUTION", "Content", "VPN Setup", "Network", "IT Support", 0.95f)
            );

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                StreamingCompletionHandler handler = invocation.getArgument(4);
                handler.onComplete(mockDocs, 0.95);
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                any(UserContext.class),
                any(),
                any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Wait for message sending
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            // Find COMPLETE chunk
            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk completeChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.COMPLETE)
                .findFirst()
                .orElse(null);

            assertThat(completeChunk).isNotNull();
            assertThat(completeChunk.isComplete()).isTrue();
            assertThat(completeChunk.getCitations()).hasSize(1);
            assertThat(completeChunk.getCitations().get(0).getSourceId()).isEqualTo("INC0001");
            assertThat(completeChunk.getConfidenceScore()).isEqualTo(0.95);
        }

        @Test
        @DisplayName("handleChatQuery_streamingError_sendsCompletionWithEmptyCitations")
        void handleChatQuery_streamingError_sendsCompletionWithEmptyCitations() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Test query")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                StreamingCompletionHandler handler = invocation.getArgument(4);
                handler.onError(new RuntimeException("Test error"));
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                any(UserContext.class),
                any(),
                any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Wait for message sending
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            // Find COMPLETE chunk
            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk completeChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.COMPLETE)
                .findFirst()
                .orElse(null);

            assertThat(completeChunk).isNotNull();
            assertThat(completeChunk.isComplete()).isTrue();
            assertThat(completeChunk.getCitations()).isEmpty();
            assertThat(completeChunk.getConfidenceScore()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Agentic Request Tests")
    class AgenticRequestTests {

        @Test
        @DisplayName("handleChatQuery_agenticIntent_callsAgenticSupport")
        void handleChatQuery_agenticIntent_callsAgenticSupport() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("create an incident")
                .sessionId("session-456")
                .userId("user1")
                .userGroups(Set.of("IT Support"))
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(true);
            when(ragAssistantService.chatWithAgenticSupport(anyString(), anyString(), anyString(), any()))
                .thenReturn(ChatResponseDto.builder()
                    .sessionId("session-456")
                    .response("I can help you create an incident.")
                    .sources(Collections.emptyList())
                    .hasContext(false)
                    .build());

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return ChatResponseDto.builder()
                    .sessionId("session-456")
                    .response("I can help you create an incident.")
                    .sources(Collections.emptyList())
                    .hasContext(false)
                    .build();
            }).when(ragAssistantService).chatWithAgenticSupport(
                anyString(),
                anyString(),
                anyString(),
                any(UserContext.class)
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(ragAssistantService).chatWithAgenticSupport(
                eq("session-456"),
                eq("create an incident"),
                eq("user1"),
                userContextCaptor.capture()
            );

            UserContext capturedContext = userContextCaptor.getValue();
            assertThat(capturedContext.userId()).isEqualTo("user1");
            assertThat(capturedContext.groups()).containsExactly("IT Support");
        }

        @Test
        @DisplayName("handleChatQuery_agenticResponse_sendsTokenAndComplete")
        void handleChatQuery_agenticResponse_sendsTokenAndComplete() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("create ticket")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(true);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return ChatResponseDto.builder()
                    .sessionId("session-456")
                    .response("Creating incident...")
                    .sources(Collections.emptyList())
                    .hasContext(false)
                    .build();
            }).when(ragAssistantService).chatWithAgenticSupport(
                anyString(),
                anyString(),
                anyString(),
                any(UserContext.class)
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Wait for message sending
            Thread.sleep(100);

            verify(messagingTemplate, atLeast(3)).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();

            // Should have THINKING, TOKEN, and COMPLETE chunks
            boolean hasThinking = chunks.stream().anyMatch(c -> c.getType() == ChunkType.THINKING);
            boolean hasToken = chunks.stream().anyMatch(c -> c.getType() == ChunkType.TOKEN && "Creating incident...".equals(c.getToken()));
            boolean hasComplete = chunks.stream().anyMatch(c -> c.getType() == ChunkType.COMPLETE && c.isComplete());

            assertThat(hasThinking).isTrue();
            assertThat(hasToken).isTrue();
            assertThat(hasComplete).isTrue();
        }

        @Test
        @DisplayName("handleChatQuery_agenticError_sendsErrorChunk")
        void handleChatQuery_agenticError_sendsErrorChunk() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("create incident")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(true);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                throw new RuntimeException("Agentic service error");
            }).when(ragAssistantService).chatWithAgenticSupport(
                anyString(),
                anyString(),
                anyString(),
                any(UserContext.class)
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            // Wait for error handling
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"),
                eq("/queue/response"),
                chunkCaptor.capture(),
                anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk errorChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.ERROR)
                .findFirst()
                .orElse(null);

            assertThat(errorChunk).isNotNull();
            assertThat(errorChunk.getError()).contains("error processing your request");
            assertThat(errorChunk.isComplete()).isTrue();
        }
    }

    @Nested
    @DisplayName("User Context Tests")
    class UserContextTests {

        @Test
        @DisplayName("handleChatQuery_nullUserGroups_usesEmptySet")
        void handleChatQuery_nullUserGroups_usesEmptySet() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("test query")
                .sessionId("session-456")
                .userId("user1")
                .userGroups(null)
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                any(UserContext.class),
                any(),
                any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(ragAssistantService).chatWithStreaming(
                anyString(),
                anyString(),
                userContextCaptor.capture(),
                any(),
                any()
            );

            UserContext capturedContext = userContextCaptor.getValue();
            assertThat(capturedContext.groups()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown_gracefulTermination_succeeds")
        void shutdown_gracefulTermination_succeeds() {
            // Just verify no exceptions are thrown
            controller.shutdown();
        }

        @Test
        @DisplayName("shutdown_whenInterrupted_shutsDownNow")
        void shutdown_whenInterrupted_shutsDownNow() {
            // Call shutdown once to terminate executor, then call again
            // The second call exercises the already-terminated path
            controller.shutdown();
            // Re-calling should not throw even on a terminated executor
            controller.shutdown();
        }
    }

    @Nested
    @DisplayName("Citation Building Tests")
    class CitationBuildingTests {

        @Test
        @DisplayName("onComplete_nullDocuments_sendsEmptyCitations")
        void onComplete_nullDocuments_sendsEmptyCitations() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Test query")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                StreamingCompletionHandler handler = invocation.getArgument(4);
                handler.onComplete(null, 0.5);
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(), anyString(), any(UserContext.class), any(), any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"), eq("/queue/response"),
                chunkCaptor.capture(), anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk completeChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.COMPLETE)
                .findFirst().orElse(null);

            assertThat(completeChunk).isNotNull();
            assertThat(completeChunk.getCitations()).isEmpty();
        }

        @Test
        @DisplayName("onComplete_emptyDocuments_sendsEmptyCitations")
        void onComplete_emptyDocuments_sendsEmptyCitations() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Test query")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                StreamingCompletionHandler handler = invocation.getArgument(4);
                handler.onComplete(Collections.emptyList(), 0.0);
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(), anyString(), any(UserContext.class), any(), any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"), eq("/queue/response"),
                chunkCaptor.capture(), anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk completeChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.COMPLETE)
                .findFirst().orElse(null);

            assertThat(completeChunk).isNotNull();
            assertThat(completeChunk.getCitations()).isEmpty();
        }

        @Test
        @DisplayName("onComplete_documentWithNullTitle_usesEmptyString")
        void onComplete_documentWithNullTitle_usesEmptyString() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Test query")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString())).thenReturn(false);

            List<RetrievedDocument> docsWithNullTitle = List.of(
                new RetrievedDocument("INCIDENT", "INC0001", "RESOLUTION", "Content", null, "Network", "IT Support", 0.85f)
            );

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                StreamingCompletionHandler handler = invocation.getArgument(4);
                handler.onComplete(docsWithNullTitle, 0.85);
                latch.countDown();
                return null;
            }).when(ragAssistantService).chatWithStreaming(
                anyString(), anyString(), any(UserContext.class), any(), any()
            );

            controller.handleChatQuery(message, headerAccessor);

            boolean completed = latch.await(2, TimeUnit.SECONDS);
            assertThat(completed).isTrue();
            Thread.sleep(100);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"), eq("/queue/response"),
                chunkCaptor.capture(), anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk completeChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.COMPLETE)
                .findFirst().orElse(null);

            assertThat(completeChunk).isNotNull();
            assertThat(completeChunk.getCitations()).hasSize(1);
            assertThat(completeChunk.getCitations().get(0).getTitle()).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("handleChatQuery_unexpectedError_sendsErrorChunk")
        void handleChatQuery_unexpectedError_sendsErrorChunk() throws InterruptedException {
            ChatQueryMessage message = ChatQueryMessage.builder()
                .messageId("msg-123")
                .text("Test query")
                .sessionId("session-456")
                .userId("user1")
                .build();

            when(ragAssistantService.hasAgenticIntent(anyString()))
                .thenThrow(new RuntimeException("Unexpected service failure"));

            controller.handleChatQuery(message, headerAccessor);

            // Wait for async processing
            Thread.sleep(500);

            verify(messagingTemplate, atLeastOnce()).convertAndSendToUser(
                eq("ws-session-123"), eq("/queue/response"),
                chunkCaptor.capture(), anyMap()
            );

            List<ChatResponseChunk> chunks = chunkCaptor.getAllValues();
            ChatResponseChunk errorChunk = chunks.stream()
                .filter(c -> c.getType() == ChunkType.ERROR)
                .findFirst().orElse(null);

            assertThat(errorChunk).isNotNull();
            assertThat(errorChunk.getError()).contains("An error occurred");
            assertThat(errorChunk.isComplete()).isTrue();
        }
    }
}
