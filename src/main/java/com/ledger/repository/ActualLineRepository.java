package com.ledger.repository;

import com.ledger.entity.ActualLine;
import com.ledger.entity.SapImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ActualLine entity.
 * Spec: 05-sap-ingestion.md, BR-08 (dedup by hash)
 */
@Repository
public interface ActualLineRepository extends JpaRepository<ActualLine, UUID> {

    /**
     * Check for an existing non-duplicate line with the same hash (BR-08).
     */
    Optional<ActualLine> findByLineHashAndDuplicateFalse(String lineHash);

    /**
     * Find all lines for a given import.
     */
    List<ActualLine> findBySapImport(SapImport sapImport);

    /**
     * Find all non-duplicate actual lines that have no reconciliation record.
     * Spec: 06-reconciliation.md Section 3
     */
    @Query("SELECT a FROM ActualLine a WHERE a.duplicate = false AND a.actualId NOT IN (SELECT r.actualId FROM Reconciliation r) ORDER BY a.postingDate DESC")
    List<ActualLine> findUnreconciled();

    /**
     * Find all lines for a given import with the specified duplicate status.
     */
    List<ActualLine> findBySapImportAndDuplicate(SapImport sapImport, boolean duplicate);
}
