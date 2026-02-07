-- V11: Arabic Search Enhancements
-- Adds support for Arabic full-text search and bilingual hybrid search
-- Part of the Damee AI Assistant Test & Enhancement Strategy

-- ============================================================================
-- 1. Add Arabic text search column with 'simple' configuration
-- PostgreSQL's 'simple' config works better for Arabic than language-specific
-- ============================================================================

ALTER TABLE embedding_store ADD COLUMN IF NOT EXISTS text_search_arabic tsvector;

-- Create trigger to auto-update Arabic text search vector
CREATE OR REPLACE FUNCTION embedding_store_arabic_search_trigger()
RETURNS trigger AS $$
BEGIN
    -- Use 'simple' configuration for Arabic (no stemming, preserves word forms)
    NEW.text_search_arabic := to_tsvector('simple', COALESCE(NEW.text_segment, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS embedding_store_arabic_search_update ON embedding_store;

CREATE TRIGGER embedding_store_arabic_search_update
    BEFORE INSERT OR UPDATE ON embedding_store
    FOR EACH ROW
    EXECUTE FUNCTION embedding_store_arabic_search_trigger();

-- Populate existing records with Arabic text search vectors
UPDATE embedding_store
SET text_search_arabic = to_tsvector('simple', COALESCE(text_segment, ''))
WHERE text_search_arabic IS NULL;

-- Create GIN index for Arabic full-text search
CREATE INDEX IF NOT EXISTS idx_embedding_store_arabic_search
ON embedding_store USING GIN(text_search_arabic);

-- ============================================================================
-- 2. Create bilingual hybrid search function
-- Combines English and Arabic text search with vector similarity
-- ============================================================================

CREATE OR REPLACE FUNCTION bilingual_hybrid_search(
    query_text TEXT,
    query_embedding TEXT,
    max_results INT DEFAULT 10,
    min_score FLOAT DEFAULT 0.5,
    vector_weight FLOAT DEFAULT 0.6,
    english_text_weight FLOAT DEFAULT 0.25,
    arabic_text_weight FLOAT DEFAULT 0.15,
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
    english_text_score FLOAT,
    arabic_text_score FLOAT,
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
        LIMIT max_results * 3
    ),
    english_text_results AS (
        -- English full-text search
        SELECT
            e.id,
            ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_vector @@ plainto_tsquery('english', query_text)
        LIMIT max_results * 3
    ),
    arabic_text_results AS (
        -- Arabic full-text search (using simple config)
        SELECT
            e.id,
            ts_rank_cd(e.text_search_arabic, plainto_tsquery('simple', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_arabic, plainto_tsquery('simple', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_arabic @@ plainto_tsquery('simple', query_text)
        LIMIT max_results * 3
    ),
    combined AS (
        -- Reciprocal Rank Fusion across all three result sets
        SELECT
            COALESCE(v.id, en.id, ar.id) as id,
            COALESCE(v.score, 0)::float as vector_score,
            COALESCE(en.score, 0)::float as english_text_score,
            COALESCE(ar.score, 0)::float as arabic_text_score,
            (
                vector_weight * COALESCE(1.0 / (k_rrf + v.rank), 0) +
                english_text_weight * COALESCE(1.0 / (k_rrf + en.rank), 0) +
                arabic_text_weight * COALESCE(1.0 / (k_rrf + ar.rank), 0)
            )::float as hybrid_score
        FROM vector_results v
        FULL OUTER JOIN english_text_results en ON v.id = en.id
        FULL OUTER JOIN arabic_text_results ar ON COALESCE(v.id, en.id) = ar.id
    )
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
        c.vector_score,
        c.english_text_score,
        c.arabic_text_score,
        c.hybrid_score
    FROM combined c
    JOIN embedding_store e ON e.id = c.id
    ORDER BY c.hybrid_score DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 3. Create bilingual hybrid search with ReBAC filtering
-- ============================================================================

CREATE OR REPLACE FUNCTION bilingual_hybrid_search_with_groups(
    query_text TEXT,
    query_embedding TEXT,
    allowed_groups TEXT[],
    max_results INT DEFAULT 10,
    min_score FLOAT DEFAULT 0.5,
    vector_weight FLOAT DEFAULT 0.6,
    english_text_weight FLOAT DEFAULT 0.25,
    arabic_text_weight FLOAT DEFAULT 0.15,
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
    english_text_score FLOAT,
    arabic_text_score FLOAT,
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
        LIMIT max_results * 3
    ),
    english_text_results AS (
        SELECT
            e.id,
            ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_vector, plainto_tsquery('english', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_vector @@ plainto_tsquery('english', query_text)
            AND (
                e.metadata->>'assigned_group' IS NULL
                OR e.metadata->>'assigned_group' = ANY(allowed_groups)
            )
        LIMIT max_results * 3
    ),
    arabic_text_results AS (
        SELECT
            e.id,
            ts_rank_cd(e.text_search_arabic, plainto_tsquery('simple', query_text))::float as score,
            ROW_NUMBER() OVER (ORDER BY ts_rank_cd(e.text_search_arabic, plainto_tsquery('simple', query_text)) DESC) as rank
        FROM embedding_store e
        WHERE e.text_search_arabic @@ plainto_tsquery('simple', query_text)
            AND (
                e.metadata->>'assigned_group' IS NULL
                OR e.metadata->>'assigned_group' = ANY(allowed_groups)
            )
        LIMIT max_results * 3
    ),
    combined AS (
        SELECT
            COALESCE(v.id, en.id, ar.id) as id,
            COALESCE(v.score, 0)::float as vector_score,
            COALESCE(en.score, 0)::float as english_text_score,
            COALESCE(ar.score, 0)::float as arabic_text_score,
            (
                vector_weight * COALESCE(1.0 / (k_rrf + v.rank), 0) +
                english_text_weight * COALESCE(1.0 / (k_rrf + en.rank), 0) +
                arabic_text_weight * COALESCE(1.0 / (k_rrf + ar.rank), 0)
            )::float as hybrid_score
        FROM vector_results v
        FULL OUTER JOIN english_text_results en ON v.id = en.id
        FULL OUTER JOIN arabic_text_results ar ON COALESCE(v.id, en.id) = ar.id
    )
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
        c.vector_score,
        c.english_text_score,
        c.arabic_text_score,
        c.hybrid_score
    FROM combined c
    JOIN embedding_store e ON e.id = c.id
    ORDER BY c.hybrid_score DESC
    LIMIT max_results;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 4. Add metadata column for language detection
-- ============================================================================

-- Add language column to track content language
ALTER TABLE embedding_store ADD COLUMN IF NOT EXISTS detected_language VARCHAR(10) DEFAULT 'en';

-- Create index for language-filtered queries
CREATE INDEX IF NOT EXISTS idx_embedding_store_language
ON embedding_store(detected_language);

-- ============================================================================
-- 5. Add comments for documentation
-- ============================================================================

COMMENT ON FUNCTION bilingual_hybrid_search IS
'Bilingual hybrid search combining vector similarity with English and Arabic full-text search using Reciprocal Rank Fusion (RRF).
Default weights: vector=0.6, english=0.25, arabic=0.15';

COMMENT ON FUNCTION bilingual_hybrid_search_with_groups IS
'Bilingual hybrid search with ReBAC group filtering for secure multi-tenant access control';

COMMENT ON COLUMN embedding_store.text_search_arabic IS
'Arabic full-text search vector using simple configuration (no stemming)';

COMMENT ON COLUMN embedding_store.detected_language IS
'Detected language of the text segment (en, ar, mixed)';
