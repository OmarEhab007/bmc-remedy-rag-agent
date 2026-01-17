package com.bmc.rag.agent.retrieval;

import com.bmc.rag.agent.config.RagConfig;
import com.bmc.rag.agent.security.ReBACFilter;
import com.bmc.rag.agent.retrieval.SecureContentRetriever.UserContext;
import com.bmc.rag.store.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SecureContentRetriever query validation and security.
 */
@ExtendWith(MockitoExtension.class)
class SecureContentRetrieverTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ReBACFilter rebacFilter;

    @Mock
    private RagConfig ragConfig;

    private SecureContentRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new SecureContentRetriever(vectorStoreService, rebacFilter, ragConfig);
    }

    @Test
    @DisplayName("Null query should throw IllegalArgumentException")
    void testNullQueryThrowsException() {
        UserContext userContext = new UserContext("user1", Set.of("Service Desk"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> retriever.retrieve(null, userContext)
        );

        assertEquals("Query cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Empty query should throw IllegalArgumentException")
    void testEmptyQueryThrowsException() {
        UserContext userContext = new UserContext("user1", Set.of("Service Desk"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> retriever.retrieve("", userContext)
        );

        assertEquals("Query cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Blank query should throw IllegalArgumentException")
    void testBlankQueryThrowsException() {
        UserContext userContext = new UserContext("user1", Set.of("Service Desk"));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> retriever.retrieve("   ", userContext)
        );

        assertEquals("Query cannot be null or empty", exception.getMessage());
    }

    @Test
    @DisplayName("Query exceeding max length should throw IllegalArgumentException")
    void testQueryExceedingMaxLengthThrowsException() {
        UserContext userContext = new UserContext("user1", Set.of("Service Desk"));

        // Create a query longer than 10000 characters
        String longQuery = "a".repeat(10001);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> retriever.retrieve(longQuery, userContext)
        );

        assertTrue(exception.getMessage().contains("exceeds maximum length"));
    }

    @Test
    @DisplayName("Query at max length should be accepted")
    void testQueryAtMaxLengthIsAccepted() {
        when(ragConfig.getMaxResults()).thenReturn(5);
        when(ragConfig.getMinScore()).thenReturn(0.5f);
        when(ragConfig.isRebacEnabled()).thenReturn(false);
        when(vectorStoreService.search(anyString(), anyInt(), anyFloat()))
            .thenReturn(Collections.emptyList());

        UserContext userContext = new UserContext("user1", Set.of("Service Desk"));

        // Create a query exactly at 10000 characters
        String maxQuery = "a".repeat(10000);

        // Should not throw - will return empty result
        var result = retriever.retrieve(maxQuery, userContext);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("UserContext.anonymous() should return context without groups")
    void testAnonymousUserContext() {
        UserContext anonymous = UserContext.anonymous();

        assertNull(anonymous.userId());
        assertTrue(anonymous.groups().isEmpty());
        assertFalse(anonymous.hasGroups());
    }

    @Test
    @DisplayName("UserContext.withGroups() should create context with groups")
    void testUserContextWithGroups() {
        UserContext context = UserContext.withGroups("user1", "Group A", "Group B");

        assertEquals("user1", context.userId());
        assertEquals(2, context.groups().size());
        assertTrue(context.hasGroups());
        assertTrue(context.groups().contains("Group A"));
        assertTrue(context.groups().contains("Group B"));
    }

    @Test
    @DisplayName("getGroupsAsList() should return list from set")
    void testGetGroupsAsList() {
        UserContext context = new UserContext("user1", Set.of("Group A", "Group B"));

        var groupsList = context.getGroupsAsList();

        assertEquals(2, groupsList.size());
        assertTrue(groupsList.contains("Group A"));
        assertTrue(groupsList.contains("Group B"));
    }
}
