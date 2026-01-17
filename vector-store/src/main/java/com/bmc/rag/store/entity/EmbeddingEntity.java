package com.bmc.rag.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for the embedding_store table.
 */
@Entity
@Table(name = "embedding_store")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chunk_id", nullable = false, unique = true)
    private String chunkId;

    // Note: The embedding column is handled separately via native queries
    // because JPA doesn't natively support pgvector type
    @Transient
    private float[] embedding;

    @Column(name = "text_segment", nullable = false, columnDefinition = "TEXT")
    private String textSegment;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false, length = 100)
    private String sourceId;

    @Column(name = "entry_id", length = 100)
    private String entryId;

    @Column(name = "chunk_type", length = 50)
    private String chunkType;

    @Column(name = "sequence_number")
    @Builder.Default
    private Integer sequenceNumber = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
