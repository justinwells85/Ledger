package com.ledger.dto;

import java.util.UUID;

/**
 * Request DTO for creating a reconciliation.
 * Spec: 06-reconciliation.md Section 3.4
 */
public record ReconciliationCreateRequest(
    UUID actualId,
    UUID milestoneId,
    String category,
    String matchNotes
) {}
