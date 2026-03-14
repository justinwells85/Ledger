package com.ledger.dto;

import com.ledger.entity.SapImport;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a staged SAP import.
 * Spec: 05-sap-ingestion.md Step 4
 */
public record SapImportResponse(
    UUID importId,
    String filename,
    Instant importedAt,
    String importedBy,
    String status,
    int totalLines,
    int newLines,
    int duplicateLines,
    int errorLines
) {
    public static SapImportResponse from(SapImport imp) {
        return new SapImportResponse(
                imp.getImportId(),
                imp.getFilename(),
                imp.getImportedAt(),
                imp.getImportedBy(),
                imp.getStatus().name(),
                imp.getTotalLines(),
                imp.getNewLines(),
                imp.getDuplicateLines(),
                imp.getErrorLines()
        );
    }
}
