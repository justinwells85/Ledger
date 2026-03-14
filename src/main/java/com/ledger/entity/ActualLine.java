package com.ledger.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Individual line item from a SAP export. Deduplicated by line_hash (SHA-256).
 * Spec: 01-domain-model.md Section 2.8, 05-sap-ingestion.md
 */
@Entity
@Table(name = "actual_line")
public class ActualLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "actual_id")
    private UUID actualId;

    @ManyToOne
    @JoinColumn(name = "import_id", nullable = false)
    private SapImport sapImport;

    @Column(name = "sap_document_number", length = 50)
    private String sapDocumentNumber;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "fiscal_period_id")
    private UUID fiscalPeriodId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "vendor_name", length = 300)
    private String vendorName;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Column(name = "wbse", length = 50)
    private String wbse;

    @Column(name = "gl_account", length = 50)
    private String glAccount;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "line_hash", nullable = false, length = 64)
    private String lineHash;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public ActualLine() {
    }

    public UUID getActualId() { return actualId; }
    public void setActualId(UUID actualId) { this.actualId = actualId; }

    public SapImport getSapImport() { return sapImport; }
    public void setSapImport(SapImport sapImport) { this.sapImport = sapImport; }

    public String getSapDocumentNumber() { return sapDocumentNumber; }
    public void setSapDocumentNumber(String sapDocumentNumber) { this.sapDocumentNumber = sapDocumentNumber; }

    public LocalDate getPostingDate() { return postingDate; }
    public void setPostingDate(LocalDate postingDate) { this.postingDate = postingDate; }

    public UUID getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(UUID fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public String getWbse() { return wbse; }
    public void setWbse(String wbse) { this.wbse = wbse; }

    public String getGlAccount() { return glAccount; }
    public void setGlAccount(String glAccount) { this.glAccount = glAccount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLineHash() { return lineHash; }
    public void setLineHash(String lineHash) { this.lineHash = lineHash; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
