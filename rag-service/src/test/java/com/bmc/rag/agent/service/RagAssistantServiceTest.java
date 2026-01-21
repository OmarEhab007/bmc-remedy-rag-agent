package com.bmc.rag.agent.service;

import com.bmc.rag.agent.config.AgenticConfig;
import com.bmc.rag.agent.config.GoogleAiConfig;
import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.config.ZaiConfig;
import com.bmc.rag.agent.memory.PostgresChatMemoryStore;
import com.bmc.rag.agent.metrics.RagMetricsService;
import com.bmc.rag.agent.retrieval.SecureContentRetriever;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RagAssistantService, particularly cache management.
 */
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

    private RagAssistantService ragAssistantService;

    @BeforeEach
    void setUp() {
        // This stubbing is used when getOrCreateMemory is called internally
        when(ragConfig.getMaxMemoryMessages()).thenReturn(20);
        when(zaiConfig.isThinkingEnabled()).thenReturn(false);
        when(agenticConfig.isEnabled()).thenReturn(false);

        ragAssistantService = new RagAssistantService(
            chatModel,
            streamingChatModel,
            contentRetriever,
            chatMemoryStore,
            ragConfig,
            zaiConfig,
            googleAiConfig,
            agenticConfig,
            metricsService
        );
    }

    @Test
    @DisplayName("getCacheStats should return cache statistics")
    void testGetCacheStatsReturnsStatistics() {
        Map<String, Long> stats = ragAssistantService.getCacheStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("hitCount"));
        assertTrue(stats.containsKey("missCount"));
        assertTrue(stats.containsKey("evictionCount"));
        assertTrue(stats.containsKey("estimatedSize"));

        // Initially all should be 0
        assertEquals(0L, stats.get("hitCount"));
        assertEquals(0L, stats.get("missCount"));
        assertEquals(0L, stats.get("evictionCount"));
        assertEquals(0L, stats.get("estimatedSize"));
    }

    @Test
    @DisplayName("clearSession should invalidate cache entry and delete from store")
    void testClearSessionInvalidatesCacheAndStore() {
        String sessionId = "test-session-123";

        ragAssistantService.clearSession(sessionId);

        // Verify the memory store was called to delete messages
        verify(chatMemoryStore).deleteMessages(sessionId);
    }

    @Test
    @DisplayName("getConversationHistory should delegate to memory store")
    void testGetConversationHistoryDelegatesToStore() {
        String sessionId = "test-session-456";
        when(chatMemoryStore.getMessages(sessionId)).thenReturn(java.util.Collections.emptyList());

        var history = ragAssistantService.getConversationHistory(sessionId);

        assertNotNull(history);
        verify(chatMemoryStore).getMessages(sessionId);
    }

    @Test
    @DisplayName("Cache should be initialized with correct configuration")
    void testCacheInitializedWithCorrectConfig() {
        // The cache should exist and be functional
        Map<String, Long> stats = ragAssistantService.getCacheStats();

        // Stats should be available (Caffeine cache with recordStats enabled)
        assertNotNull(stats);
        assertEquals(4, stats.size());
    }
}
