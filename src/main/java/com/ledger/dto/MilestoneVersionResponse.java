package com.ledger.dto;

import com.ledger.entity.MilestoneVersion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a MilestoneVersion.
 * Spec: 13-api-design.md Section 5
 */
public record MilestoneVersionResponse(
    UUID versionId,
    int versionNumber,
    BigDecimal plannedAmount,
    UUID fiscalPeriodId,
    String fiscalPeriodKey,
    LocalDate effectiveDate,
    String reason
) {
    public static MilestoneVersionResponse from(MilestoneVersion v) {
        return new MilestoneVersionResponse(
                v.getVersionId(),
                v.getVersionNumber(),
                v.getPlannedAmount(),
                v.getFiscalPeriod().getPeriodId(),
                v.getFiscalPeriod().getPeriodKey(),
                v.getEffectiveDate(),
                v.getReason()
        );
    }
}
