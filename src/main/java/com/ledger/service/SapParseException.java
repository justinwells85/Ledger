package com.ledger.service;

/**
 * Thrown when a SAP file cannot be parsed at all (corrupt, wrong format, etc.).
 * Spec: 05-sap-ingestion.md Section 4
 */
public class SapParseException extends RuntimeException {

    public SapParseException(String message) {
        super(message);
    }

    public SapParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
