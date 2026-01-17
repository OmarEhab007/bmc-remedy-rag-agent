-- V6__create_action_audit.sql
-- Flyway migration for Section 12: Agentic Operations audit trail

-- Create action audit table for tracking all agentic operations
CREATE TABLE IF NOT EXISTS action_audit (
    id BIGSERIAL PRIMARY KEY,

    -- Action identification
    action_id VARCHAR(8) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,

    -- Action details
    action_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    summary VARCHAR(255),
    record_id VARCHAR(50),
    error_message VARCHAR(1000),

    -- Optional request payload (JSON)
    request_payload TEXT,

    -- Timestamps
    staged_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Security context
    client_ip VARCHAR(45),
    user_agent VARCHAR(500),

    -- Constraints
    CONSTRAINT chk_action_type CHECK (action_type IN ('INCIDENT_CREATE', 'WORK_ORDER_CREATE', 'INCIDENT_UPDATE', 'WORK_ORDER_UPDATE')),
    CONSTRAINT chk_status CHECK (status IN ('STAGED', 'CONFIRMED', 'EXECUTED', 'CANCELLED', 'EXPIRED', 'FAILED'))
);

-- Create indexes for common query patterns
CREATE INDEX idx_action_audit_action_id ON action_audit(action_id);
CREATE INDEX idx_action_audit_user ON action_audit(user_id);
CREATE INDEX idx_action_audit_session ON action_audit(session_id);
CREATE INDEX idx_action_audit_status ON action_audit(status);
CREATE INDEX idx_action_audit_created ON action_audit(created_at DESC);
CREATE INDEX idx_action_audit_type_status ON action_audit(action_type, status);

-- Composite index for user activity queries
CREATE INDEX idx_action_audit_user_time ON action_audit(user_id, created_at DESC);

-- Comment the table
COMMENT ON TABLE action_audit IS 'Audit trail for agentic operations (ticket creation, updates)';
COMMENT ON COLUMN action_audit.action_id IS 'Unique 8-character action identifier for confirmation';
COMMENT ON COLUMN action_audit.session_id IS 'Session ID where action was initiated';
COMMENT ON COLUMN action_audit.user_id IS 'User who initiated the action';
COMMENT ON COLUMN action_audit.action_type IS 'Type of agentic action';
COMMENT ON COLUMN action_audit.status IS 'Current status of the action';
COMMENT ON COLUMN action_audit.summary IS 'Brief summary of the action (e.g., incident summary)';
COMMENT ON COLUMN action_audit.record_id IS 'ID of created record (e.g., INC000001234)';
COMMENT ON COLUMN action_audit.error_message IS 'Error message if action failed';
COMMENT ON COLUMN action_audit.request_payload IS 'JSON payload of the request (optional)';
COMMENT ON COLUMN action_audit.staged_at IS 'When the action was staged for confirmation';
COMMENT ON COLUMN action_audit.resolved_at IS 'When the action was confirmed/cancelled/expired';
COMMENT ON COLUMN action_audit.client_ip IS 'Client IP address for security audit';
COMMENT ON COLUMN action_audit.user_agent IS 'Client user agent for debugging';

-- Create function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_action_audit_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update updated_at
CREATE TRIGGER trigger_action_audit_updated
    BEFORE UPDATE ON action_audit
    FOR EACH ROW
    EXECUTE FUNCTION update_action_audit_timestamp();

-- Create view for action statistics (optional, useful for dashboards)
CREATE OR REPLACE VIEW action_audit_stats AS
SELECT
    DATE_TRUNC('day', created_at) as day,
    action_type,
    status,
    COUNT(*) as count
FROM action_audit
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY DATE_TRUNC('day', created_at), action_type, status
ORDER BY day DESC, action_type, status;

COMMENT ON VIEW action_audit_stats IS 'Daily statistics for agentic actions (last 30 days)';
