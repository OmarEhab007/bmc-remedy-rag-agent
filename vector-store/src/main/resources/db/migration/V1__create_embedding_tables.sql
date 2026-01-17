-- BMC Remedy RAG Agent - Vector Store Schema
-- PostgreSQL + pgvector

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- Embedding Store Table
-- Stores text chunks and their vector embeddings for semantic search
-- =============================================================================
CREATE TABLE embedding_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Chunk identification
    chunk_id VARCHAR(255) NOT NULL UNIQUE,

    -- Vector embedding (384 dimensions for all-minilm-l6-v2)
    embedding vector(384) NOT NULL,

    -- Text content
    text_segment TEXT NOT NULL,

    -- Source record information
    source_type VARCHAR(50) NOT NULL,  -- Incident, WorkOrder, KnowledgeArticle, ChangeRequest
    source_id VARCHAR(100) NOT NULL,   -- Business ID (INC000..., WO000..., etc.)
    entry_id VARCHAR(100),             -- Remedy entry ID
    chunk_type VARCHAR(50),            -- SUMMARY, DESCRIPTION, RESOLUTION, etc.
    sequence_number INTEGER DEFAULT 0,

    -- Metadata for filtering and ReBAC
    metadata JSONB NOT NULL DEFAULT '{}',

    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Index hints
    CONSTRAINT valid_source_type CHECK (
        source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest')
    )
);

-- HNSW index for fast approximate nearest neighbor search
-- Using cosine distance (vector_cosine_ops) as recommended for text embeddings
CREATE INDEX idx_embedding_hnsw ON embedding_store
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Index for source record lookup (for updates/deletes)
CREATE INDEX idx_embedding_source ON embedding_store (source_type, source_id);

-- Index for chunk type filtering
CREATE INDEX idx_embedding_chunk_type ON embedding_store (chunk_type);

-- GIN index for JSONB metadata queries (e.g., filtering by assigned_group)
CREATE INDEX idx_embedding_metadata ON embedding_store USING GIN (metadata);

-- Index for assigned_group ReBAC filtering (commonly used)
CREATE INDEX idx_embedding_assigned_group ON embedding_store ((metadata->>'assigned_group'));

-- =============================================================================
-- Sync State Table
-- Tracks incremental sync state for CDC (Change Data Capture)
-- =============================================================================
CREATE TABLE sync_state (
    id SERIAL PRIMARY KEY,
    source_type VARCHAR(50) NOT NULL UNIQUE,
    last_sync_timestamp BIGINT NOT NULL DEFAULT 0,  -- Unix epoch seconds
    last_sync_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    records_synced INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'completed',
    error_message TEXT,

    CONSTRAINT valid_sync_source CHECK (
        source_type IN ('Incident', 'WorkOrder', 'KnowledgeArticle', 'ChangeRequest')
    )
);

-- Initialize sync state for each source type
INSERT INTO sync_state (source_type, last_sync_timestamp) VALUES
    ('Incident', 0),
    ('WorkOrder', 0),
    ('KnowledgeArticle', 0),
    ('ChangeRequest', 0);

-- =============================================================================
-- Chat Memory Store Table
-- Persistent storage for conversation history
-- =============================================================================
CREATE TABLE chat_memory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(20) NOT NULL,  -- USER, AI, SYSTEM
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_message_type CHECK (
        message_type IN ('USER', 'AI', 'SYSTEM')
    )
);

-- Index for session lookup
CREATE INDEX idx_chat_memory_session ON chat_memory (session_id, created_at);

-- Cleanup old sessions (retention policy can be adjusted)
CREATE INDEX idx_chat_memory_created ON chat_memory (created_at);

-- =============================================================================
-- Ingestion Job Table
-- Tracks ingestion job status and progress
-- =============================================================================
CREATE TABLE ingestion_job (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_type VARCHAR(50) NOT NULL,  -- FULL, INCREMENTAL, SINGLE_RECORD
    source_type VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'pending',  -- pending, running, completed, failed
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_processed INTEGER DEFAULT 0,
    chunks_created INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_job_status CHECK (
        status IN ('pending', 'running', 'completed', 'failed', 'cancelled')
    )
);

-- Index for finding running jobs
CREATE INDEX idx_ingestion_job_status ON ingestion_job (status, created_at);

-- =============================================================================
-- Functions
-- =============================================================================

-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for embedding_store
CREATE TRIGGER update_embedding_store_updated_at
    BEFORE UPDATE ON embedding_store
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function for semantic search with ReBAC filtering
CREATE OR REPLACE FUNCTION search_embeddings(
    query_embedding vector(384),
    max_results INTEGER DEFAULT 5,
    min_score FLOAT DEFAULT 0.7,
    allowed_groups TEXT[] DEFAULT NULL,
    source_types TEXT[] DEFAULT NULL,
    chunk_types TEXT[] DEFAULT NULL
)
RETURNS TABLE (
    id UUID,
    chunk_id VARCHAR,
    text_segment TEXT,
    source_type VARCHAR,
    source_id VARCHAR,
    chunk_type VARCHAR,
    metadata JSONB,
    score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        e.id,
        e.chunk_id,
        e.text_segment,
        e.source_type,
        e.source_id,
        e.chunk_type,
        e.metadata,
        1 - (e.embedding <=> query_embedding) AS score
    FROM embedding_store e
    WHERE
        -- ReBAC: Filter by allowed groups
        (allowed_groups IS NULL OR
         e.metadata->>'assigned_group' = ANY(allowed_groups) OR
         e.metadata->>'assigned_group' IS NULL)
        -- Filter by source type
        AND (source_types IS NULL OR e.source_type = ANY(source_types))
        -- Filter by chunk type
        AND (chunk_types IS NULL OR e.chunk_type = ANY(chunk_types))
        -- Minimum similarity score
        AND (1 - (e.embedding <=> query_embedding)) >= min_score
    ORDER BY e.embedding <=> query_embedding
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- Views
-- =============================================================================

-- View for embedding statistics
CREATE VIEW embedding_stats AS
SELECT
    source_type,
    chunk_type,
    COUNT(*) as chunk_count,
    AVG(LENGTH(text_segment)) as avg_text_length,
    MIN(created_at) as oldest_chunk,
    MAX(updated_at) as newest_chunk
FROM embedding_store
GROUP BY source_type, chunk_type
ORDER BY source_type, chunk_type;

-- View for sync status
CREATE VIEW sync_status AS
SELECT
    source_type,
    last_sync_timestamp,
    to_timestamp(last_sync_timestamp) as last_sync_time,
    last_sync_at,
    records_synced,
    status,
    error_message
FROM sync_state
ORDER BY source_type;
