package com.ledger.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Links an SAP actual line to a planned milestone with a category classification.
 * An actual can map to at most one milestone (BR-06). Category is required (BR-07).
 * Spec: 01-domain-model.md Section 2.9, 06-reconciliation.md
 */
@Entity
@Table(name = "reconciliation")
public class Reconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reconciliation_id")
    private UUID reconciliationId;

    @Column(name = "actual_id", nullable = false, unique = true)
    private UUID actualId;

    @Column(name = "milestone_id", nullable = false)
    private UUID milestoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ReconciliationCategory category;

    @Column(name = "match_notes", columnDefinition = "TEXT")
    private String matchNotes;

    @Column(name = "reconciled_at", nullable = false, updatable = false)
    private Instant reconciledAt;

    @Column(name = "reconciled_by", nullable = false, length = 100)
    private String reconciledBy;

    @PrePersist
    void prePersist() {
        if (reconciledAt == null) {
            reconciledAt = Instant.now();
        }
    }

    public Reconciliation() {
    }

    public UUID getReconciliationId() { return reconciliationId; }
    public void setReconciliationId(UUID reconciliationId) { this.reconciliationId = reconciliationId; }

    public UUID getActualId() { return actualId; }
    public void setActualId(UUID actualId) { this.actualId = actualId; }

    public UUID getMilestoneId() { return milestoneId; }
    public void setMilestoneId(UUID milestoneId) { this.milestoneId = milestoneId; }

    public ReconciliationCategory getCategory() { return category; }
    public void setCategory(ReconciliationCategory category) { this.category = category; }

    public String getMatchNotes() { return matchNotes; }
    public void setMatchNotes(String matchNotes) { this.matchNotes = matchNotes; }

    public Instant getReconciledAt() { return reconciledAt; }
    public void setReconciledAt(Instant reconciledAt) { this.reconciledAt = reconciledAt; }

    public String getReconciledBy() { return reconciledBy; }
    public void setReconciledBy(String reconciledBy) { this.reconciledBy = reconciledBy; }
}
