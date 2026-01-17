package com.bmc.rag.store.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for the sync_state table.
 * Tracks incremental sync state for each source type.
 */
@Entity
@Table(name = "sync_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "source_type", nullable = false, unique = true, length = 50)
    private String sourceType;

    @Column(name = "last_sync_timestamp", nullable = false)
    @Builder.Default
    private Long lastSyncTimestamp = 0L;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "records_synced")
    @Builder.Default
    private Integer recordsSynced = 0;

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "completed";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Update sync state after successful sync.
     */
    public void markSyncCompleted(long newTimestamp, int recordCount) {
        this.lastSyncTimestamp = newTimestamp;
        this.lastSyncAt = Instant.now();
        this.recordsSynced = recordCount;
        this.status = "completed";
        this.errorMessage = null;
    }

    /**
     * Mark sync as running.
     */
    public void markSyncRunning() {
        this.status = "running";
        this.lastSyncAt = Instant.now();
    }

    /**
     * Mark sync as failed.
     */
    public void markSyncFailed(String error) {
        this.status = "failed";
        this.lastSyncAt = Instant.now();
        this.errorMessage = error;
    }
}
