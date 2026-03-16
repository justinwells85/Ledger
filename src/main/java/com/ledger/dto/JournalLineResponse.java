package com.ledger.dto;

import com.ledger.entity.JournalLine;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for a JournalLine.
 * Spec: 02-journal-ledger.md Section 3
 */
public record JournalLineResponse(
        UUID lineId,
        UUID entryId,
        String account,
        UUID contractId,
        String projectId,
        UUID milestoneId,
        UUID fiscalPeriodId,
        BigDecimal debit,
        BigDecimal credit,
        String referenceType,
        UUID referenceId
) {
    public static JournalLineResponse from(JournalLine line) {
        return new JournalLineResponse(
                line.getLineId(),
                line.getJournalEntry().getEntryId(),
                line.getAccount().name(),
                line.getContractId(),
                line.getProjectId(),
                line.getMilestoneId(),
                line.getFiscalPeriodId(),
                line.getDebit(),
                line.getCredit(),
                line.getReferenceType(),
                line.getReferenceId()
        );
    }
}
