package com.ledger.dto;

import com.ledger.entity.Reconciliation;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a Reconciliation.
 * Spec: 06-reconciliation.md Section 3.5
 */
public record ReconciliationResponse(
    UUID reconciliationId,
    UUID actualId,
    UUID milestoneId,
    String category,
    String matchNotes,
    Instant reconciledAt,
    String reconciledBy
) {
    public static ReconciliationResponse from(Reconciliation rec) {
        return new ReconciliationResponse(
                rec.getReconciliationId(),
                rec.getActualId(),
                rec.getMilestoneId(),
                rec.getCategory().name(),
                rec.getMatchNotes(),
                rec.getReconciledAt(),
                rec.getReconciledBy()
        );
    }
}
