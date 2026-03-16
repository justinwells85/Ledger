package com.ledger.dto;

import com.ledger.entity.JournalEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a JournalEntry.
 * Spec: 02-journal-ledger.md Section 3
 */
public record JournalEntryResponse(
        UUID entryId,
        Instant entryDate,
        LocalDate effectiveDate,
        String entryType,
        String description,
        Instant createdAt,
        String createdBy
) {
    public static JournalEntryResponse from(JournalEntry entry) {
        return new JournalEntryResponse(
                entry.getEntryId(),
                entry.getEntryDate(),
                entry.getEffectiveDate(),
                entry.getEntryType().name(),
                entry.getDescription(),
                entry.getCreatedAt(),
                entry.getCreatedBy()
        );
    }
}
