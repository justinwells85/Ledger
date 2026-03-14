package com.ledger.entity;

/**
 * Classification for a reconciliation record.
 * Spec: 06-reconciliation.md Section 2, BR-07
 */
public enum ReconciliationCategory {
    INVOICE,
    ACCRUAL,
    ACCRUAL_REVERSAL,
    ALLOCATION
}
