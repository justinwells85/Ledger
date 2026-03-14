package com.ledger.entity;

/**
 * Chart of accounts for the double-entry journal.
 * Spec: 02-journal-ledger.md Section 2
 */
public enum AccountType {
    PLANNED,
    ACTUAL,
    VARIANCE_RESERVE
}
