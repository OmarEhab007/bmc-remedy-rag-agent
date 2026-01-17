-- V5: HNSW Index Tuning for Scale (P2.5)
-- Optimizes HNSW parameters for 1M+ embeddings

-- Drop old index if exists
DROP INDEX IF EXISTS idx_embedding_store_embedding;

-- Create optimized HNSW index with better parameters for scale
-- m=24 (connections per layer, higher = better recall, more memory)
-- ef_construction=200 (build-time quality, higher = better index, slower build)
CREATE INDEX idx_embedding_store_embedding_hnsw ON embedding_store
USING hnsw (embedding vector_cosine_ops)
WITH (m = 24, ef_construction = 200);

-- Add configuration table for runtime HNSW parameters
CREATE TABLE IF NOT EXISTS vector_search_config (
    config_key VARCHAR(50) PRIMARY KEY,
    config_value INTEGER NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Insert default configuration
INSERT INTO vector_search_config (config_key, config_value, description) VALUES
    ('ef_search', 100, 'HNSW ef_search parameter - higher = better recall, slower search (default: 40)')
ON CONFLICT (config_key) DO NOTHING;

-- Create function to set ef_search at query time
CREATE OR REPLACE FUNCTION set_ef_search(ef_value INTEGER)
RETURNS void AS $$
BEGIN
    EXECUTE format('SET hnsw.ef_search = %s', ef_value);
END;
$$ LANGUAGE plpgsql;

-- Create function to get optimal ef_search based on desired recall level
CREATE OR REPLACE FUNCTION get_optimal_ef_search(recall_level VARCHAR DEFAULT 'high')
RETURNS INTEGER AS $$
BEGIN
    RETURN CASE recall_level
        WHEN 'low' THEN 40      -- Fastest, ~90% recall
        WHEN 'medium' THEN 64   -- Balanced, ~95% recall
        WHEN 'high' THEN 100    -- Better recall, slower
        WHEN 'max' THEN 200     -- Maximum recall, slowest
        ELSE 100
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Create index usage statistics view
-- Note: pg_stat_user_indexes uses relname for table name and indexrelname for index name
CREATE OR REPLACE VIEW embedding_index_stats AS
SELECT
    schemaname,
    relname as tablename,
    indexrelname as indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    idx_scan as number_of_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE relname = 'embedding_store';

-- Add comment
COMMENT ON INDEX idx_embedding_store_embedding_hnsw IS 'Optimized HNSW index for semantic search at scale (m=24, ef_construction=200)';
