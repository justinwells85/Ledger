package com.ledger.dto;

import com.ledger.entity.ActualLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for an ActualLine.
 * Spec: 05-sap-ingestion.md
 */
public record ActualLineResponse(
        UUID actualId,
        UUID importId,
        String sapDocumentNumber,
        LocalDate postingDate,
        UUID fiscalPeriodId,
        BigDecimal amount,
        String vendorName,
        String costCenter,
        String wbse,
        String glAccount,
        String description,
        boolean duplicate,
        Instant createdAt
) {
    public static ActualLineResponse from(ActualLine line) {
        return new ActualLineResponse(
                line.getActualId(),
                line.getSapImport().getImportId(),
                line.getSapDocumentNumber(),
                line.getPostingDate(),
                line.getFiscalPeriodId(),
                line.getAmount(),
                line.getVendorName(),
                line.getCostCenter(),
                line.getWbse(),
                line.getGlAccount(),
                line.getDescription(),
                line.isDuplicate(),
                line.getCreatedAt()
        );
    }
}
