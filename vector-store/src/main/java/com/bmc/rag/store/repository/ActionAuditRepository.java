package com.bmc.rag.store.repository;

import com.bmc.rag.store.entity.ActionAuditEntity;
import com.bmc.rag.store.entity.ActionAuditEntity.ActionStatus;
import com.bmc.rag.store.entity.ActionAuditEntity.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for action audit entities.
 * Provides querying capabilities for audit trail analysis.
 */
@Repository
public interface ActionAuditRepository extends JpaRepository<ActionAuditEntity, Long> {

    /**
     * Find an audit entry by action ID.
     */
    Optional<ActionAuditEntity> findByActionId(String actionId);

    /**
     * Find all audit entries for a session.
     */
    List<ActionAuditEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    /**
     * Find all audit entries for a user.
     */
    Page<ActionAuditEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Find audit entries by status.
     */
    Page<ActionAuditEntity> findByStatusOrderByCreatedAtDesc(ActionStatus status, Pageable pageable);

    /**
     * Find audit entries by action type.
     */
    Page<ActionAuditEntity> findByActionTypeOrderByCreatedAtDesc(ActionType actionType, Pageable pageable);

    /**
     * Find audit entries within a time range.
     */
    @Query("SELECT a FROM ActionAuditEntity a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<ActionAuditEntity> findByTimeRange(
        @Param("start") Instant start,
        @Param("end") Instant end,
        Pageable pageable
    );

    /**
     * Count actions by user within a time period.
     */
    @Query("SELECT COUNT(a) FROM ActionAuditEntity a WHERE a.userId = :userId AND a.createdAt > :since AND a.status IN ('EXECUTED', 'CONFIRMED')")
    long countUserActionsInPeriod(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Count actions by status within a time period.
     */
    @Query("SELECT a.status, COUNT(a) FROM ActionAuditEntity a WHERE a.createdAt > :since GROUP BY a.status")
    List<Object[]> countByStatusSince(@Param("since") Instant since);

    /**
     * Count actions by type within a time period.
     */
    @Query("SELECT a.actionType, COUNT(a) FROM ActionAuditEntity a WHERE a.createdAt > :since GROUP BY a.actionType")
    List<Object[]> countByTypeSince(@Param("since") Instant since);

    /**
     * Get recent audit entries (for dashboard).
     */
    @Query("SELECT a FROM ActionAuditEntity a ORDER BY a.createdAt DESC")
    Page<ActionAuditEntity> findRecent(Pageable pageable);

    /**
     * Find failed actions for a user.
     */
    @Query("SELECT a FROM ActionAuditEntity a WHERE a.userId = :userId AND a.status = 'FAILED' ORDER BY a.createdAt DESC")
    List<ActionAuditEntity> findFailedByUser(@Param("userId") String userId, Pageable pageable);

    /**
     * Find expired actions.
     */
    @Query("SELECT a FROM ActionAuditEntity a WHERE a.status = 'EXPIRED' ORDER BY a.createdAt DESC")
    Page<ActionAuditEntity> findExpired(Pageable pageable);

    /**
     * Delete old audit entries for retention policy.
     */
    @Modifying
    @Query("DELETE FROM ActionAuditEntity a WHERE a.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    /**
     * Get success rate for a user.
     */
    @Query("SELECT COUNT(CASE WHEN a.status = 'EXECUTED' THEN 1 END) * 1.0 / COUNT(a) " +
           "FROM ActionAuditEntity a WHERE a.userId = :userId AND a.createdAt > :since")
    Double getSuccessRate(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Check if user has any pending staged actions.
     */
    @Query("SELECT COUNT(a) > 0 FROM ActionAuditEntity a WHERE a.userId = :userId AND a.status = 'STAGED'")
    boolean hasStaged(@Param("userId") String userId);

    /**
     * Get most active users.
     */
    @Query("SELECT a.userId, COUNT(a) as cnt FROM ActionAuditEntity a WHERE a.createdAt > :since GROUP BY a.userId ORDER BY cnt DESC")
    List<Object[]> getMostActiveUsers(@Param("since") Instant since, Pageable pageable);
}
