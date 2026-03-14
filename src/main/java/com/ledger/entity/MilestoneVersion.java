package com.ledger.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A point-in-time snapshot of a milestone's planned amount and target period.
 * Independently versioned per milestone. effective_date drives time machine queries.
 * Spec: 01-domain-model.md Section 2.6, 04-milestone-versioning.md
 */
@Entity
@Table(name = "milestone_version")
public class MilestoneVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id")
    private UUID versionId;

    @ManyToOne
    @JoinColumn(name = "milestone_id", nullable = false)
    private Milestone milestone;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    @ManyToOne
    @JoinColumn(name = "fiscal_period_id", nullable = false)
    private FiscalPeriod fiscalPeriod;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public MilestoneVersion() {
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
    }

    public Milestone getMilestone() {
        return milestone;
    }

    public void setMilestone(Milestone milestone) {
        this.milestone = milestone;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public FiscalPeriod getFiscalPeriod() {
        return fiscalPeriod;
    }

    public void setFiscalPeriod(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriod = fiscalPeriod;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
