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
}
