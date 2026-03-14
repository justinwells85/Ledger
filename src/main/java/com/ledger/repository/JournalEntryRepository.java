package com.ledger.repository;

import com.ledger.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for JournalEntry entity.
 * Spec: 02-journal-ledger.md
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
}
