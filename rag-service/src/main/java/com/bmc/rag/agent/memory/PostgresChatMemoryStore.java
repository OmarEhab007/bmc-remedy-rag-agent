package com.bmc.rag.agent.memory;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.*;

/**
 * PostgreSQL-backed ChatMemoryStore for persistent conversation history.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sessionId = memoryId.toString();

        List<ChatMessage> messages = jdbcTemplate.query(
            "SELECT message_type, content, metadata, created_at " +
            "FROM chat_memory " +
            "WHERE session_id = ? " +
            "ORDER BY created_at ASC",
            (rs, rowNum) -> {
                String type = rs.getString("message_type");
                String content = rs.getString("content");
                return createMessage(type, content);
            },
            sessionId
        );

        log.debug("Retrieved {} messages for session {}", messages.size(), sessionId);
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String sessionId = memoryId.toString();

        // Delete existing messages
        jdbcTemplate.update("DELETE FROM chat_memory WHERE session_id = ?", sessionId);

        // Insert new messages
        if (messages != null && !messages.isEmpty()) {
            for (ChatMessage message : messages) {
                String type = getMessageType(message);
                String content = getMessageContent(message);

                jdbcTemplate.update(
                    "INSERT INTO chat_memory (id, session_id, message_type, content, created_at) " +
                    "VALUES (?, ?, ?, ?, ?)",
                    UUID.randomUUID(),
                    sessionId,
                    type,
                    content,
                    new Timestamp(System.currentTimeMillis())
                );
            }
        }

        log.debug("Updated {} messages for session {}", messages != null ? messages.size() : 0, sessionId);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sessionId = memoryId.toString();
        int deleted = jdbcTemplate.update("DELETE FROM chat_memory WHERE session_id = ?", sessionId);
        log.debug("Deleted {} messages for session {}", deleted, sessionId);
    }

    /**
     * Get message count for a session.
     */
    public int getMessageCount(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_memory WHERE session_id = ?",
            Integer.class,
            sessionId
        );
        return count != null ? count : 0;
    }

    /**
     * Delete old sessions (retention policy).
     *
     * @param olderThanDays Delete sessions older than this many days
     * @return Number of deleted messages
     */
    public int deleteOldSessions(int olderThanDays) {
        // Validate input to prevent negative values
        if (olderThanDays < 0) {
            throw new IllegalArgumentException("olderThanDays must be non-negative");
        }

        // Use parameterized query with INTERVAL arithmetic to prevent SQL injection
        int deleted = jdbcTemplate.update(
            "DELETE FROM chat_memory WHERE created_at < NOW() - (? * INTERVAL '1 day')",
            olderThanDays
        );
        log.info("Deleted {} old chat messages (older than {} days)", deleted, olderThanDays);
        return deleted;
    }

    /**
     * Get all active session IDs, ordered by most recent activity.
     */
    public List<String> getActiveSessions() {
        return jdbcTemplate.queryForList(
            "SELECT session_id FROM chat_memory GROUP BY session_id ORDER BY MAX(created_at) DESC",
            String.class
        );
    }

    /**
     * Create a ChatMessage from type and content.
     */
    private ChatMessage createMessage(String type, String content) {
        return switch (type) {
            case "USER" -> UserMessage.from(content);
            case "AI" -> AiMessage.from(content);
            case "SYSTEM" -> SystemMessage.from(content);
            default -> throw new IllegalArgumentException("Unknown message type: " + type);
        };
    }

    /**
     * Get message type string from ChatMessage.
     */
    private String getMessageType(ChatMessage message) {
        if (message instanceof UserMessage) {
            return "USER";
        } else if (message instanceof AiMessage) {
            return "AI";
        } else if (message instanceof SystemMessage) {
            return "SYSTEM";
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
        }
    }

    /**
     * Get message content from ChatMessage.
     */
    private String getMessageContent(ChatMessage message) {
        if (message instanceof UserMessage userMsg) {
            return userMsg.singleText();
        } else if (message instanceof AiMessage aiMsg) {
            return aiMsg.text();
        } else if (message instanceof SystemMessage sysMsg) {
            return sysMsg.text();
        } else {
            return message.toString();
        }
    }
}
