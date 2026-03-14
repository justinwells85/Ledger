package com.ledger.entity;

/**
 * Status of a SAP import through the pipeline.
 * Spec: 05-sap-ingestion.md Section 5
 */
public enum SapImportStatus {
    STAGED,
    COMMITTED,
    REJECTED
}
