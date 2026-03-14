package com.ledger.repository;

import com.ledger.entity.ActualLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
