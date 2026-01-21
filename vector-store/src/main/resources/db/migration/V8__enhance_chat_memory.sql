-- =============================================================================
-- V8: Enhance Chat Memory with User ID and Retention Support
-- =============================================================================

-- Add user_id column for user-specific conversation history
ALTER TABLE chat_memory ADD COLUMN IF NOT EXISTS user_id VARCHAR(128);

-- Create index for user-based queries
CREATE INDEX IF NOT EXISTS idx_chat_memory_user ON chat_memory (user_id, created_at DESC);

-- Add composite index for session + user queries
CREATE INDEX IF NOT EXISTS idx_chat_memory_session_user ON chat_memory (session_id, user_id);

-- Create a separate table for chat history (OpenAI-compatible format)
CREATE TABLE IF NOT EXISTS chat_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128),
    message_type VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',  -- OpenAI: user, assistant, system
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    token_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    CONSTRAINT valid_role CHECK (role IN ('user', 'assistant', 'system', 'tool'))
);

-- Indexes for chat_history
CREATE INDEX IF NOT EXISTS idx_chat_history_session ON chat_history (session_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_history_user ON chat_history (user_id, created_at DESC);

-- Function to clean up old chat history (retention policy)
CREATE OR REPLACE FUNCTION cleanup_old_chat_history(retention_days INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER := 0;
    temp_count INTEGER := 0;
BEGIN
    DELETE FROM chat_history
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;

    DELETE FROM chat_memory
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;
    GET DIAGNOSTICS temp_count = ROW_COUNT;
    deleted_count := deleted_count + temp_count;

    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE chat_history IS 'Persistent conversation history with OpenAI-compatible role format';
COMMENT ON COLUMN chat_history.role IS 'OpenAI message role: user, assistant, system, or tool';
COMMENT ON COLUMN chat_history.token_count IS 'Estimated token count for context window management';
