package com.ledger.entity;

import jakarta.persistence.*;

/**
 * Database-managed reconciliation category reference data.
 * Spec: 18-admin-configuration.md Section 3.5, BR-96 through BR-97
 */
@Entity
@Table(name = "ref_reconciliation_category")
public class RefReconciliationCategory {

    @Id
    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "affects_accrual_lifecycle", nullable = false)
    private boolean affectsAccrualLifecycle = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isAffectsAccrualLifecycle() { return affectsAccrualLifecycle; }
    public void setAffectsAccrualLifecycle(boolean affectsAccrualLifecycle) {
        this.affectsAccrualLifecycle = affectsAccrualLifecycle;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
