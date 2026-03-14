package com.ledger.service;

/**
 * Thrown when attempting to reconcile an actual that already has a reconciliation (BR-06).
 */
public class AlreadyReconciledException extends RuntimeException {
    public AlreadyReconciledException(String message) {
        super(message);
    }
}
