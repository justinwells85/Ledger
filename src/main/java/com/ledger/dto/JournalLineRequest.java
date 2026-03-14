package com.ledger.dto;

import com.ledger.entity.AccountType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating journal lines as part of a journal entry.
 * Spec: 02-journal-ledger.md Section 3
 */
public record JournalLineRequest(
    AccountType account,
    UUID contractId,
    String projectId,
    UUID milestoneId,
    UUID fiscalPeriodId,
    BigDecimal debit,
    BigDecimal credit,
    String referenceType,
    UUID referenceId
) {}
