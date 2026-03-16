package com.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new milestone with v1.
 * Spec: 13-api-design.md Section 5, 04-milestone-versioning.md
 */
public record MilestoneCreateRequest(
    @NotBlank String name,
    String description,
    @NotNull BigDecimal plannedAmount,
    @NotNull UUID fiscalPeriodId,
    @NotNull LocalDate effectiveDate,
    @NotBlank String reason
) {}
