package com.ledger.repository;

import com.ledger.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for JournalEntry entity.
 * Spec: 02-journal-ledger.md
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    /**
     * Returns all journal entries that have at least one line referencing the given milestone.
     * Spec: 11-change-management.md Section 5
     */
    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN je.lines l WHERE l.milestoneId = :milestoneId")
    List<JournalEntry> findByMilestoneId(@Param("milestoneId") UUID milestoneId);
}
