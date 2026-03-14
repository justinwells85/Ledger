package com.ledger.entity;

/**
 * Types of journal entries in the double-entry ledger.
 * Spec: 02-journal-ledger.md Section 4
 */
public enum JournalEntryType {
    PLAN_CREATE,
    PLAN_ADJUST,
    ACTUAL_IMPORT,
    RECONCILE,
    RECONCILE_UNDO
}
