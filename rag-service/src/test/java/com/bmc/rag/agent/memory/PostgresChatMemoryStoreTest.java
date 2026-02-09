package com.bmc.rag.agent.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PostgresChatMemoryStore, particularly the SQL injection fix.
 */
@ExtendWith(MockitoExtension.class)
class PostgresChatMemoryStoreTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PostgresChatMemoryStore chatMemoryStore;

    @BeforeEach
    void setUp() {
        chatMemoryStore = new PostgresChatMemoryStore(jdbcTemplate);
    }

    @Test
    @DisplayName("deleteOldSessions should reject negative days")
    void testDeleteOldSessionsRejectsNegativeDays() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> chatMemoryStore.deleteOldSessions(-1)
        );

        assertEquals("olderThanDays must be non-negative", exception.getMessage());
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("deleteOldSessions should accept zero days")
    void testDeleteOldSessionsAcceptsZeroDays() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(5);

        int deleted = chatMemoryStore.deleteOldSessions(0);

        assertEquals(5, deleted);
        verify(jdbcTemplate).update(anyString(), eq(0));
    }

    @Test
    @DisplayName("deleteOldSessions should use parameterized query")
    void testDeleteOldSessionsUsesParameterizedQuery() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(10);

        chatMemoryStore.deleteOldSessions(30);

        // Verify the query uses a parameter placeholder instead of string concatenation
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> paramCaptor = ArgumentCaptor.forClass(Object.class);

        verify(jdbcTemplate).update(sqlCaptor.capture(), paramCaptor.capture());

        String sql = sqlCaptor.getValue();
        // The SQL should contain a ? parameter placeholder, not the literal value
        assertTrue(sql.contains("?"), "SQL should use parameterized query");
        assertFalse(sql.contains("30"), "SQL should not contain literal value");

        // Verify the parameter was passed correctly
        assertEquals(30, paramCaptor.getValue());
    }

    @Test
    @DisplayName("deleteOldSessions should handle large values safely")
    void testDeleteOldSessionsHandlesLargeValues() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(0);

        // Should not throw for large values - they're parameterized
        int deleted = chatMemoryStore.deleteOldSessions(Integer.MAX_VALUE);

        assertEquals(0, deleted);
        verify(jdbcTemplate).update(anyString(), eq(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("deleteMessages should use parameterized query")
    void testDeleteMessagesUsesParameterizedQuery() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(1);

        chatMemoryStore.deleteMessages("session-123");

        verify(jdbcTemplate).update(
            eq("DELETE FROM chat_memory WHERE session_id = ?"),
            eq("session-123")
        );
    }

    @Test
    @DisplayName("getMessageCount should use parameterized query")
    void testGetMessageCountUsesParameterizedQuery() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object.class)))
            .thenReturn(5);

        int count = chatMemoryStore.getMessageCount("session-456");

        assertEquals(5, count);
        verify(jdbcTemplate).queryForObject(
            eq("SELECT COUNT(*) FROM chat_memory WHERE session_id = ?"),
            eq(Integer.class),
            eq("session-456")
        );
    }

    @Test
    @DisplayName("getMessageCount should return 0 for null result")
    void testGetMessageCountReturnsZeroForNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object.class)))
            .thenReturn(null);

        int count = chatMemoryStore.getMessageCount("session-789");

        assertEquals(0, count);
    }

    @Test
    @DisplayName("getMessages should return empty list when no messages found")
    void testGetMessagesReturnsEmptyList() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object.class)))
            .thenReturn(java.util.Collections.emptyList());

        var messages = chatMemoryStore.getMessages("session-empty");

        assertTrue(messages.isEmpty());
    }

    @Test
    @DisplayName("getMessages should retrieve user messages")
    void testGetMessagesRetrievesUserMessages() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object.class)))
            .thenAnswer(invocation -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = invocation.getArgument(1);
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("message_type")).thenReturn("USER");
                when(rs.getString("content")).thenReturn("Hello");
                return java.util.List.of(mapper.mapRow(rs, 1));
            });

        var messages = chatMemoryStore.getMessages("session-1");

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof dev.langchain4j.data.message.UserMessage);
    }

    @Test
    @DisplayName("getMessages should retrieve AI messages")
    void testGetMessagesRetrievesAiMessages() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object.class)))
            .thenAnswer(invocation -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = invocation.getArgument(1);
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("message_type")).thenReturn("AI");
                when(rs.getString("content")).thenReturn("Hi there");
                return java.util.List.of(mapper.mapRow(rs, 1));
            });

        var messages = chatMemoryStore.getMessages("session-1");

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof dev.langchain4j.data.message.AiMessage);
    }

    @Test
    @DisplayName("getMessages should retrieve system messages")
    void testGetMessagesRetrievesSystemMessages() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object.class)))
            .thenAnswer(invocation -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = invocation.getArgument(1);
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("message_type")).thenReturn("SYSTEM");
                when(rs.getString("content")).thenReturn("System prompt");
                return java.util.List.of(mapper.mapRow(rs, 1));
            });

        var messages = chatMemoryStore.getMessages("session-1");

        assertEquals(1, messages.size());
        assertTrue(messages.get(0) instanceof dev.langchain4j.data.message.SystemMessage);
    }

    @Test
    @DisplayName("updateMessages should delete and insert")
    void testUpdateMessagesDeletesAndInserts() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(1);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        java.util.List<dev.langchain4j.data.message.ChatMessage> messages = java.util.List.of(
            dev.langchain4j.data.message.UserMessage.from("test")
        );

        chatMemoryStore.updateMessages("session-1", messages);

        // Verify delete was called
        verify(jdbcTemplate).update("DELETE FROM chat_memory WHERE session_id = ?", "session-1");
        // Verify insert was called
        verify(jdbcTemplate, atLeastOnce()).update(
            contains("INSERT INTO chat_memory"),
            any(), eq("session-1"), eq("USER"), eq("test"), any()
        );
    }

    @Test
    @DisplayName("updateMessages should handle null messages")
    void testUpdateMessagesHandlesNull() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(0);

        chatMemoryStore.updateMessages("session-1", null);

        verify(jdbcTemplate).update("DELETE FROM chat_memory WHERE session_id = ?", "session-1");
        verify(jdbcTemplate, never()).update(contains("INSERT"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("updateMessages should handle empty list")
    void testUpdateMessagesHandlesEmptyList() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(0);

        chatMemoryStore.updateMessages("session-1", java.util.Collections.emptyList());

        verify(jdbcTemplate).update("DELETE FROM chat_memory WHERE session_id = ?", "session-1");
        verify(jdbcTemplate, never()).update(contains("INSERT"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("addMessage should insert single message")
    void testAddMessageInsertsSingleMessage() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        chatMemoryStore.addMessage("session-1", "user1", dev.langchain4j.data.message.UserMessage.from("hello"));

        verify(jdbcTemplate).update(
            contains("INSERT INTO chat_memory"),
            any(), eq("session-1"), eq("user1"), eq("USER"), eq("hello"), any()
        );
    }

    @Test
    @DisplayName("getActiveSessions should return session IDs")
    void testGetActiveSessionsReturnsIds() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
            .thenReturn(java.util.List.of("session-1", "session-2"));

        var sessions = chatMemoryStore.getActiveSessions();

        assertEquals(2, sessions.size());
        assertTrue(sessions.contains("session-1"));
        assertTrue(sessions.contains("session-2"));
    }

    @Test
    @DisplayName("getSessionsForUser should use parameterized query")
    void testGetSessionsForUserUsesParameterizedQuery() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object.class)))
            .thenReturn(java.util.List.of("session-1"));

        var sessions = chatMemoryStore.getSessionsForUser("user123");

        assertEquals(1, sessions.size());
        verify(jdbcTemplate).queryForList(
            contains("WHERE user_id = ?"),
            eq(String.class),
            eq("user123")
        );
    }

    @Test
    @DisplayName("setSessionUser should update user_id")
    void testSetSessionUserUpdates() {
        when(jdbcTemplate.update(
            eq("UPDATE chat_memory SET user_id = ? WHERE session_id = ?"),
            eq("user456"),
            eq("session-1")
        )).thenReturn(1);

        chatMemoryStore.setSessionUser("session-1", "user456");

        verify(jdbcTemplate).update(
            eq("UPDATE chat_memory SET user_id = ? WHERE session_id = ?"),
            eq("user456"),
            eq("session-1")
        );
    }

    @Test
    @DisplayName("getSessionSummaries should return SessionInfo records")
    void testGetSessionSummariesReturnsInfo() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
            .thenAnswer(invocation -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = invocation.getArgument(1);
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("session_id")).thenReturn("session-1");
                when(rs.getString("title")).thenReturn("Test title");
                when(rs.getInt("message_count")).thenReturn(5);
                when(rs.getLong("last_updated_millis")).thenReturn(1234567890L);
                return java.util.List.of(mapper.mapRow(rs, 1));
            });

        var summaries = chatMemoryStore.getSessionSummaries();

        assertEquals(1, summaries.size());
        var info = summaries.get(0);
        assertEquals("session-1", info.sessionId());
        assertEquals("Test title", info.title());
        assertEquals(5, info.messageCount());
        assertEquals(1234567890L, info.lastUpdated());
    }

    @Test
    @DisplayName("SessionInfo record should be created correctly")
    void testSessionInfoRecordCreation() {
        var info = new PostgresChatMemoryStore.SessionInfo("s1", "title", 10, 999999L);

        assertEquals("s1", info.sessionId());
        assertEquals("title", info.title());
        assertEquals(10, info.messageCount());
        assertEquals(999999L, info.lastUpdated());
    }

    @Test
    @DisplayName("getMessages should throw on unknown message type")
    void testGetMessagesThrowsOnUnknownType() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object.class)))
            .thenAnswer(invocation -> {
                org.springframework.jdbc.core.RowMapper<?> mapper = invocation.getArgument(1);
                java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
                when(rs.getString("message_type")).thenReturn("UNKNOWN_TYPE");
                when(rs.getString("content")).thenReturn("test");

                // The mapper will throw when called with unknown type
                assertThrows(IllegalArgumentException.class, () -> mapper.mapRow(rs, 1));
                return java.util.Collections.emptyList();
            });

        chatMemoryStore.getMessages("session-1");
    }

    @Test
    @DisplayName("updateMessages should handle unsupported message type")
    void testUpdateMessagesHandlesUnsupportedType() {
        when(jdbcTemplate.update(anyString(), any(Object.class))).thenReturn(0);

        // Create a custom unsupported message type using ToolExecutionResultMessage
        // which is a valid ChatMessage type but not handled by the store
        dev.langchain4j.data.message.ChatMessage unsupportedMessage =
            dev.langchain4j.data.message.ToolExecutionResultMessage.from(
                null,
                "tool-1",
                "result"
            );

        java.util.List<dev.langchain4j.data.message.ChatMessage> messages = java.util.List.of(unsupportedMessage);

        // Should throw IllegalArgumentException when trying to get message type
        assertThrows(IllegalArgumentException.class, () -> {
            chatMemoryStore.updateMessages("session-1", messages);
        });
    }

    @Test
    @DisplayName("addMessage should handle all message types")
    void testAddMessageHandlesAllTypes() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);

        // Test UserMessage
        chatMemoryStore.addMessage("s1", "u1", dev.langchain4j.data.message.UserMessage.from("user text"));
        verify(jdbcTemplate).update(contains("INSERT"), any(), eq("s1"), eq("u1"), eq("USER"), eq("user text"), any());

        // Test AiMessage
        chatMemoryStore.addMessage("s2", "u2", dev.langchain4j.data.message.AiMessage.from("ai text"));
        verify(jdbcTemplate).update(contains("INSERT"), any(), eq("s2"), eq("u2"), eq("AI"), eq("ai text"), any());

        // Test SystemMessage
        chatMemoryStore.addMessage("s3", "u3", dev.langchain4j.data.message.SystemMessage.from("system text"));
        verify(jdbcTemplate).update(contains("INSERT"), any(), eq("s3"), eq("u3"), eq("SYSTEM"), eq("system text"), any());
    }
}
