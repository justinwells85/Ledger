package com.ledger.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Record of a SAP file upload. Tracks the staging → commit/reject workflow.
 * Spec: 01-domain-model.md Section 2.7, 05-sap-ingestion.md
 */
@Entity
@Table(name = "sap_import")
public class SapImport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "import_id")
    private UUID importId;

    @Column(name = "filename", nullable = false, length = 500)
    private String filename;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @Column(name = "imported_by", nullable = false, length = 100)
    private String importedBy;

    @Column(name = "fiscal_year", length = 10)
    private String fiscalYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SapImportStatus status = SapImportStatus.STAGED;

    @Column(name = "total_lines", nullable = false)
    private int totalLines = 0;

    @Column(name = "new_lines", nullable = false)
    private int newLines = 0;

    @Column(name = "duplicate_lines", nullable = false)
    private int duplicateLines = 0;

    @Column(name = "error_lines", nullable = false)
    private int errorLines = 0;

    @PrePersist
    void prePersist() {
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }

    public SapImport() {
    }

    public UUID getImportId() { return importId; }
    public void setImportId(UUID importId) { this.importId = importId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }

    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public SapImportStatus getStatus() { return status; }
    public void setStatus(SapImportStatus status) { this.status = status; }

    public int getTotalLines() { return totalLines; }
    public void setTotalLines(int totalLines) { this.totalLines = totalLines; }

    public int getNewLines() { return newLines; }
    public void setNewLines(int newLines) { this.newLines = newLines; }

    public int getDuplicateLines() { return duplicateLines; }
    public void setDuplicateLines(int duplicateLines) { this.duplicateLines = duplicateLines; }

    public int getErrorLines() { return errorLines; }
    public void setErrorLines(int errorLines) { this.errorLines = errorLines; }
}
