package com.bmc.rag.store.repository;

import com.bmc.rag.store.entity.SyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository for sync state operations.
 */
@Repository
public interface SyncStateRepository extends JpaRepository<SyncStateEntity, Integer> {

    /**
     * Find sync state by source type.
     */
    Optional<SyncStateEntity> findBySourceType(String sourceType);

    /**
     * Update last sync timestamp.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SyncStateEntity s SET s.lastSyncTimestamp = :timestamp, s.lastSyncAt = CURRENT_TIMESTAMP, s.recordsSynced = :recordCount, s.status = 'completed', s.errorMessage = null WHERE s.sourceType = :sourceType")
    void updateSyncCompleted(
        @Param("sourceType") String sourceType,
        @Param("timestamp") Long timestamp,
        @Param("recordCount") Integer recordCount
    );

    /**
     * Mark sync as running.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SyncStateEntity s SET s.status = 'running', s.lastSyncAt = CURRENT_TIMESTAMP WHERE s.sourceType = :sourceType")
    void markSyncRunning(@Param("sourceType") String sourceType);

    /**
     * Mark sync as failed.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SyncStateEntity s SET s.status = 'failed', s.lastSyncAt = CURRENT_TIMESTAMP, s.errorMessage = :errorMessage WHERE s.sourceType = :sourceType")
    void markSyncFailed(
        @Param("sourceType") String sourceType,
        @Param("errorMessage") String errorMessage
    );

    /**
     * Get last sync timestamp for a source type.
     */
    @Query("SELECT s.lastSyncTimestamp FROM SyncStateEntity s WHERE s.sourceType = :sourceType")
    Optional<Long> getLastSyncTimestamp(@Param("sourceType") String sourceType);

    /**
     * Check if any sync is currently running.
     */
    @Query("SELECT COUNT(s) > 0 FROM SyncStateEntity s WHERE s.status = 'running'")
    boolean isAnySyncRunning();

    /**
     * Attempt to acquire a sync lock atomically using database-level UPDATE.
     * Returns 1 if lock acquired, 0 if lock was already held.
     * This prevents race conditions in the check-then-act pattern.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SyncStateEntity s SET s.status = 'running', s.lastSyncAt = CURRENT_TIMESTAMP " +
           "WHERE s.sourceType = :sourceType AND s.status != 'running'")
    int tryAcquireLock(@Param("sourceType") String sourceType);

    /**
     * Release sync lock by marking as completed or failed.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SyncStateEntity s SET s.status = 'idle', s.lastSyncAt = CURRENT_TIMESTAMP " +
           "WHERE s.sourceType = :sourceType AND s.status = 'running'")
    int releaseLock(@Param("sourceType") String sourceType);

    /**
     * Release stale locks that have been running longer than the timeout.
     * This handles cases where a sync process crashed without releasing its lock.
     *
     * @param timeoutMinutes Maximum time a lock can be held before being considered stale
     * @return Number of stale locks released
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE sync_state SET status = 'failed', " +
           "error_message = 'Lock timeout - sync process may have crashed', " +
           "last_sync_at = CURRENT_TIMESTAMP " +
           "WHERE status = 'running' AND last_sync_at < CURRENT_TIMESTAMP - (INTERVAL '1 minute' * :timeoutMinutes)",
           nativeQuery = true)
    int releaseStaleLocksNative(@Param("timeoutMinutes") int timeoutMinutes);

    /**
     * Check if a specific sync has a stale lock.
     *
     * @param sourceType The source type to check
     * @param timeoutMinutes Maximum time a lock can be held
     * @return true if the lock is stale
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM sync_state " +
           "WHERE source_type = :sourceType AND status = 'running' " +
           "AND last_sync_at < CURRENT_TIMESTAMP - (INTERVAL '1 minute' * :timeoutMinutes)",
           nativeQuery = true)
    boolean hasStalelock(@Param("sourceType") String sourceType, @Param("timeoutMinutes") int timeoutMinutes);
}
