package com.ledger.dto;

import java.time.LocalDate;

/**
 * Request DTO for cancelling a milestone.
 * Spec: 04-milestone-versioning.md Section 4 (Cancelling)
 */
public record MilestoneCancelRequest(
    String reason,
    LocalDate effectiveDate
) {}
