package com.ledger.dto;

import com.ledger.entity.FiscalYear;

import java.time.LocalDate;

/**
 * DTO for fiscal year API responses.
 * Spec: 13-api-design.md Section 2
 */
public record FiscalYearResponse(
        String fiscalYear,
        LocalDate startDate,
        LocalDate endDate
) {
    public static FiscalYearResponse from(FiscalYear entity) {
        return new FiscalYearResponse(
                entity.getFiscalYear(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
