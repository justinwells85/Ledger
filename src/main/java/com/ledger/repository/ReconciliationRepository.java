package com.ledger.repository;

import com.ledger.entity.Reconciliation;
import com.ledger.entity.ReconciliationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Reconciliation entity.
 * Spec: 06-reconciliation.md, BR-06, BR-07
 */
@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    Optional<Reconciliation> findByActualId(UUID actualId);

    List<Reconciliation> findByMilestoneId(UUID milestoneId);

    List<Reconciliation> findByMilestoneIdAndCategory(UUID milestoneId, ReconciliationCategory category);

    /**
     * Sum of actual amounts reconciled to a milestone by category.
     */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Reconciliation r " +
           "JOIN ActualLine a ON a.actualId = r.actualId " +
           "WHERE r.milestoneId = :milestoneId")
    BigDecimal sumReconciledAmount(@Param("milestoneId") UUID milestoneId);

    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM Reconciliation r " +
           "JOIN ActualLine a ON a.actualId = r.actualId " +
           "WHERE r.milestoneId = :milestoneId AND r.category = :category")
    BigDecimal sumReconciledAmountByCategory(@Param("milestoneId") UUID milestoneId,
                                              @Param("category") ReconciliationCategory category);

    /** Backdates reconciled_at for testing aging logic. */
    @Modifying
    @Query(value = "UPDATE reconciliation SET reconciled_at = :ts WHERE reconciliation_id = :id", nativeQuery = true)
    void backdateReconciledAt(@Param("id") UUID id, @Param("ts") Instant ts);
}
