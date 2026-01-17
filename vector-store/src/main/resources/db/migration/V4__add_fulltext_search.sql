-- V4: Add Full-Text Search Support for Hybrid Search (P2.1)
-- Adds ts_vector column and GIN index for BM25-style keyword search

-- Add ts_vector column for full-text search
ALTER TABLE embedding_store ADD COLUMN IF NOT EXISTS text_search_vector tsvector;

-- Create trigger to auto-update ts_vector on insert/update
CREATE OR REPLACE FUNCTION embedding_store_text_search_trigger()
RETURNS trigger AS $$
BEGIN
    NEW.text_search_vector := to_tsvector('english', COALESCE(NEW.text_segment, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS embedding_store_text_search_update ON embedding_store;

CREATE TRIGGER embedding_store_text_search_update
    BEFORE INSERT OR UPDATE ON embedding_store
    FOR EACH ROW
    EXECUTE FUNCTION embedding_store_text_search_trigger();

-- Populate existing records
UPDATE embedding_store
SET text_search_vector = to_tsvector('english', COALESCE(text_segment, ''))
WHERE text_search_vector IS NULL;

-- Create GIN index for fast full-text search
CREATE INDEX IF NOT EXISTS idx_embedding_store_text_search
ON embedding_store USING GIN(text_search_vector);

-- Create function for hybrid search with Reciprocal Rank Fusion (RRF)
CREATE OR REPLACE FUNCTION hybrid_search(
    query_text TEXT,
    query_embedding TEXT,  -- Vector as string "[0.1, 0.2, ...]"
    max_results INT DEFAULT 10,
    min_score FLOAT DEFAULT 0.3,
    vector_weight FLOAT DEFAULT 0.7,
    text_weight FLOAT DEFAULT 0.3,
    k_rrf INT DEFAULT 60  -- RRF constant (higher = more weight to top results)
)
RETURNS TABLE (
    id UUID,
    chunk_id VARCHAR,
    text_segment TEXT,
    source_type VARCHAR,
    source_id VARCHAR,
    entry_id VARCHAR,
    chunk_type VARCHAR,
    sequence_number INT,
    metadata JSONB,
    vector_score FLOAT,
    text_score FLOAT,
    hybrid_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        -- Vector similarity search
        SELECT
            e.id,
            e.chunk_id,
            e.text_segment,
            e.source_type,
            e.source_id,
            e.entry_id,
            e.chunk_type,
            e.sequence_number,
            e.metadata,
            (1 - (e.embedding <=> cast(query_embedding as vector)))::float as score,
            ROW_NUMBER() OVER (ORDER BY e.embedding <=> cast(query_embedding as vector)) as rank
        FROM embedding_store e
        WHERE 1 - (e.embedding <=> cast(query_embedding as vector)) >= min_score
        LIMIT max_results * 2
    ),
    text_results AS (
        -- Full-text search using ts_rank
        SELECT
            e.id,
            e.chunk_id,
            e.text_segment,
            e.source_type,
            e.source_id,
            e.entry_id,
            e.chunk_type,
            e.sequence_number,
            e.metadata,
            ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_vector @@ plainto_tsquery('english', query_text)
        LIMIT max_results * 2
    ),
    combined AS (
        -- Reciprocal Rank Fusion
        SELECT
            COALESCE(v.id, t.id) as id,
            COALESCE(v.chunk_id, t.chunk_id) as chunk_id,
            COALESCE(v.text_segment, t.text_segment) as text_segment,
            COALESCE(v.source_type, t.source_type) as source_type,
            COALESCE(v.source_id, t.source_id) as source_id,
            COALESCE(v.entry_id, t.entry_id) as entry_id,
            COALESCE(v.chunk_type, t.chunk_type) as chunk_type,
            COALESCE(v.sequence_number, t.sequence_number) as sequence_number,
            COALESCE(v.metadata, t.metadata) as metadata,
            COALESCE(v.score, 0)::float as vector_score,
            COALESCE(t.score, 0)::float as text_score,
            (
                vector_weight * COALESCE(1.0 / (k_rrf + v.rank), 0) +
                text_weight * COALESCE(1.0 / (k_rrf + t.rank), 0)
            )::float as hybrid_score
        FROM vector_results v
        FULL OUTER JOIN text_results t ON v.id = t.id
    )
    SELECT
        c.id,
        c.chunk_id,
        c.text_segment,
        c.source_type,
        c.source_id,
        c.entry_id,
        c.chunk_type,
        c.sequence_number,
        c.metadata,
        c.vector_score,
        c.text_score,
        c.hybrid_score
    FROM combined c
    ORDER BY c.hybrid_score DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- Create function for hybrid search with ReBAC filtering
-- Note: Parameters without defaults must come before parameters with defaults
CREATE OR REPLACE FUNCTION hybrid_search_with_groups(
    query_text TEXT,
    query_embedding TEXT,
    allowed_groups TEXT[],
    max_results INT DEFAULT 10,
    min_score FLOAT DEFAULT 0.3,
    vector_weight FLOAT DEFAULT 0.7,
    text_weight FLOAT DEFAULT 0.3,
    k_rrf INT DEFAULT 60
)
RETURNS TABLE (
    id UUID,
    chunk_id VARCHAR,
    text_segment TEXT,
    source_type VARCHAR,
    source_id VARCHAR,
    entry_id VARCHAR,
    chunk_type VARCHAR,
    sequence_number INT,
    metadata JSONB,
    vector_score FLOAT,
    text_score FLOAT,
    hybrid_score FLOAT
) AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        SELECT
            e.id,
            e.chunk_id,
            e.text_segment,
            e.source_type,
            e.source_id,
            e.entry_id,
            e.chunk_type,
            e.sequence_number,
            e.metadata,
            (1 - (e.embedding <=> cast(query_embedding as vector)))::float as score,
            ROW_NUMBER() OVER (ORDER BY e.embedding <=> cast(query_embedding as vector)) as rank
        FROM embedding_store e
        WHERE 1 - (e.embedding <=> cast(query_embedding as vector)) >= min_score
            AND (
                e.metadata->>'assigned_group' IS NULL
                OR e.metadata->>'assigned_group' = ANY(allowed_groups)
            )
        LIMIT max_results * 2
    ),
    text_results AS (
        SELECT
            e.id,
            e.chunk_id,
            e.text_segment,
            e.source_type,
            e.source_id,
            e.entry_id,
            e.chunk_type,
            e.sequence_number,
            e.metadata,
            ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_vector @@ plainto_tsquery('english', query_text)
            AND (
                e.metadata->>'assigned_group' IS NULL
                OR e.metadata->>'assigned_group' = ANY(allowed_groups)
            )
        LIMIT max_results * 2
    ),
    combined AS (
        SELECT
            COALESCE(v.id, t.id) as id,
            COALESCE(v.chunk_id, t.chunk_id) as chunk_id,
            COALESCE(v.text_segment, t.text_segment) as text_segment,
            COALESCE(v.source_type, t.source_type) as source_type,
            COALESCE(v.source_id, t.source_id) as source_id,
            COALESCE(v.entry_id, t.entry_id) as entry_id,
            COALESCE(v.chunk_type, t.chunk_type) as chunk_type,
            COALESCE(v.sequence_number, t.sequence_number) as sequence_number,
            COALESCE(v.metadata, t.metadata) as metadata,
            COALESCE(v.score, 0)::float as vector_score,
            COALESCE(t.score, 0)::float as text_score,
            (
                vector_weight * COALESCE(1.0 / (k_rrf + v.rank), 0) +
                text_weight * COALESCE(1.0 / (k_rrf + t.rank), 0)
            )::float as hybrid_score
        FROM vector_results v
        FULL OUTER JOIN text_results t ON v.id = t.id
    )
    SELECT
        c.id,
        c.chunk_id,
        c.text_segment,
        c.source_type,
        c.source_id,
        c.entry_id,
        c.chunk_type,
        c.sequence_number,
        c.metadata,
        c.vector_score,
        c.text_score,
        c.hybrid_score
    FROM combined c
    ORDER BY c.hybrid_score DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION hybrid_search IS 'Hybrid search combining vector similarity and full-text search using Reciprocal Rank Fusion';
COMMENT ON FUNCTION hybrid_search_with_groups IS 'Hybrid search with ReBAC group filtering';
