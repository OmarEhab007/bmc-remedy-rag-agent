package com.bmc.rag.store.repository;

import com.bmc.rag.store.entity.EmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for embedding storage operations.
 */
@Repository
public interface EmbeddingRepository extends JpaRepository<EmbeddingEntity, UUID> {

    /**
     * Find embedding by chunk ID.
     */
    Optional<EmbeddingEntity> findByChunkId(String chunkId);

    /**
     * Find all embeddings for a source record.
     */
    List<EmbeddingEntity> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    /**
     * Find all embeddings by source type.
     */
    List<EmbeddingEntity> findBySourceType(String sourceType);

    /**
     * Find all embeddings by source type with pagination.
     */
    org.springframework.data.domain.Page<EmbeddingEntity> findBySourceType(String sourceType, org.springframework.data.domain.Pageable pageable);

    /**
     * Find all embeddings by chunk type.
     */
    List<EmbeddingEntity> findByChunkType(String chunkType);

    /**
     * Delete all embeddings for a source record.
     */
    @Modifying
    @Transactional
    void deleteBySourceTypeAndSourceId(String sourceType, String sourceId);

    /**
     * Delete all embeddings by source type.
     */
    @Modifying
    @Transactional
    void deleteBySourceType(String sourceType);

    /**
     * Count embeddings by source type.
     */
    long countBySourceType(String sourceType);

    /**
     * Count embeddings by chunk type.
     */
    long countByChunkType(String chunkType);

    /**
     * Check if embedding exists for chunk ID.
     */
    boolean existsByChunkId(String chunkId);

    /**
     * Get all unique source IDs for a source type.
     */
    @Query("SELECT DISTINCT e.sourceId FROM EmbeddingEntity e WHERE e.sourceType = :sourceType")
    List<String> findDistinctSourceIdsBySourceType(@Param("sourceType") String sourceType);

    /**
     * Native query to update embedding for an existing record.
     * Used by EmbeddingRefreshService to re-embed records with real semantic embeddings.
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE embedding_store SET embedding = cast(:embedding as vector), updated_at = NOW() WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);

    /**
     * Native query to insert embedding with vector.
     * Uses native SQL because JPA doesn't support pgvector type.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO embedding_store (
            id, chunk_id, embedding, text_segment, source_type, source_id,
            entry_id, chunk_type, sequence_number, metadata, created_at, updated_at
        ) VALUES (
            :id, :chunkId, cast(:embedding as vector), :textSegment, :sourceType, :sourceId,
            :entryId, :chunkType, :sequenceNumber, cast(:metadata as jsonb), NOW(), NOW()
        )
        ON CONFLICT (chunk_id) DO UPDATE SET
            embedding = cast(:embedding as vector),
            text_segment = :textSegment,
            metadata = cast(:metadata as jsonb),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertWithEmbedding(
        @Param("id") UUID id,
        @Param("chunkId") String chunkId,
        @Param("embedding") String embedding,  // "[0.1, 0.2, ...]" format
        @Param("textSegment") String textSegment,
        @Param("sourceType") String sourceType,
        @Param("sourceId") String sourceId,
        @Param("entryId") String entryId,
        @Param("chunkType") String chunkType,
        @Param("sequenceNumber") Integer sequenceNumber,
        @Param("metadata") String metadata  // JSON string
    );

    /**
     * Native query for semantic search with cosine similarity.
     */
    @Query(value = """
        SELECT
            e.id, e.chunk_id, e.text_segment, e.source_type, e.source_id,
            e.entry_id, e.chunk_type, e.sequence_number, e.metadata,
            e.created_at, e.updated_at,
            1 - (e.embedding <=> cast(:queryEmbedding as vector)) as score
        FROM embedding_store e
        WHERE 1 - (e.embedding <=> cast(:queryEmbedding as vector)) >= :minScore
        ORDER BY e.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Object[]> searchSimilar(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("maxResults") int maxResults,
        @Param("minScore") float minScore
    );

    /**
     * Native query for semantic search with ReBAC filtering.
     */
    @Query(value = """
        SELECT
            e.id, e.chunk_id, e.text_segment, e.source_type, e.source_id,
            e.entry_id, e.chunk_type, e.sequence_number, e.metadata,
            e.created_at, e.updated_at,
            1 - (e.embedding <=> cast(:queryEmbedding as vector)) as score
        FROM embedding_store e
        WHERE 1 - (e.embedding <=> cast(:queryEmbedding as vector)) >= :minScore
            AND (
                e.metadata->>'assigned_group' IS NULL
                OR e.metadata->>'assigned_group' = ANY(cast(:allowedGroups as text[]))
            )
        ORDER BY e.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Object[]> searchSimilarWithGroups(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("maxResults") int maxResults,
        @Param("minScore") float minScore,
        @Param("allowedGroups") String allowedGroups  // "{group1,group2}" format
    );

    /**
     * Native query for semantic search with source type filtering.
     */
    @Query(value = """
        SELECT
            e.id, e.chunk_id, e.text_segment, e.source_type, e.source_id,
            e.entry_id, e.chunk_type, e.sequence_number, e.metadata,
            e.created_at, e.updated_at,
            1 - (e.embedding <=> cast(:queryEmbedding as vector)) as score
        FROM embedding_store e
        WHERE 1 - (e.embedding <=> cast(:queryEmbedding as vector)) >= :minScore
            AND e.source_type = ANY(cast(:sourceTypes as text[]))
        ORDER BY e.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :maxResults
        """, nativeQuery = true)
    List<Object[]> searchSimilarBySourceTypes(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("maxResults") int maxResults,
        @Param("minScore") float minScore,
        @Param("sourceTypes") String sourceTypes  // "{Incident,WorkOrder}" format
    );
}
