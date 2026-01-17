-- V3: Create feedback table for storing user feedback on AI responses
-- This supports the P3.4 Feedback Mechanism enhancement

CREATE TABLE IF NOT EXISTS feedback (
    id VARCHAR(36) PRIMARY KEY,
    message_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    feedback_type VARCHAR(10) NOT NULL CHECK (feedback_type IN ('positive', 'negative')),
    feedback_text TEXT,
    user_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Index for querying by session
CREATE INDEX idx_feedback_session_id ON feedback(session_id);

-- Index for querying by message
CREATE INDEX idx_feedback_message_id ON feedback(message_id);

-- Index for filtering by type
CREATE INDEX idx_feedback_type ON feedback(feedback_type);

-- Index for time-based queries
CREATE INDEX idx_feedback_created_at ON feedback(created_at DESC);

-- Composite index for stats queries
CREATE INDEX idx_feedback_session_type ON feedback(session_id, feedback_type);

COMMENT ON TABLE feedback IS 'User feedback on AI responses for quality tracking and model improvement';
COMMENT ON COLUMN feedback.id IS 'Unique identifier for the feedback entry';
COMMENT ON COLUMN feedback.message_id IS 'ID of the AI message that received feedback';
COMMENT ON COLUMN feedback.session_id IS 'Session ID where the message was generated';
COMMENT ON COLUMN feedback.feedback_type IS 'Type of feedback: positive or negative';
COMMENT ON COLUMN feedback.feedback_text IS 'Optional detailed feedback text for negative feedback';
COMMENT ON COLUMN feedback.user_id IS 'Optional user ID for tracking';
COMMENT ON COLUMN feedback.created_at IS 'Timestamp when feedback was submitted';
