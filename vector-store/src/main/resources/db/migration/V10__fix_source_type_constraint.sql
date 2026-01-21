-- =============================================================================
-- V10: Fix source_type constraint to include DameeService
-- =============================================================================
-- The original V1 constraint was too restrictive and blocked DameeService records

-- Drop the existing constraint
ALTER TABLE embedding_store DROP CONSTRAINT IF EXISTS valid_source_type;

-- Add updated constraint with DameeService included
ALTER TABLE embedding_store ADD CONSTRAINT valid_source_type CHECK (
    source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest', 'DameeService')
);

-- Also ensure sync_state can track DameeService (if not already done)
ALTER TABLE sync_state DROP CONSTRAINT IF EXISTS valid_sync_source;
ALTER TABLE sync_state ADD CONSTRAINT valid_sync_source CHECK (
    source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest', 'DameeService')
);

COMMENT ON CONSTRAINT valid_source_type ON embedding_store IS
    'Restricts source_type to known ITSM record types including Damee services';
