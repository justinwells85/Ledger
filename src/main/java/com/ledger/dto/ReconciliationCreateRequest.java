package com.ledger.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for creating a reconciliation.
 * Spec: 06-reconciliation.md Section 3.4
 */
public record ReconciliationCreateRequest(
    @NotNull UUID actualId,
    @NotNull UUID milestoneId,
    @NotNull String category,
    String matchNotes
) {}
