package com.ledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new milestone version (adjustment).
 * Spec: 13-api-design.md Section 5, 04-milestone-versioning.md
 */
public record MilestoneVersionAdjustRequest(
    BigDecimal plannedAmount,
    UUID fiscalPeriodId,
    LocalDate effectiveDate,
    String reason
) {}
