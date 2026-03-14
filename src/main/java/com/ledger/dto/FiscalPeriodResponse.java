package com.ledger.dto;

import com.ledger.entity.FiscalPeriod;

import java.time.format.DateTimeFormatter;

/**
 * DTO for fiscal period API responses.
 * Spec: 13-api-design.md Section 2
 *
 * calendarMonth is formatted as "YYYY-MM" (e.g., "2025-10") per the API spec.
 */
public record FiscalPeriodResponse(
        String periodId,
        String fiscalYear,
        String periodKey,
        String quarter,
        String calendarMonth,
        String displayName,
        int sortOrder
) {
    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public static FiscalPeriodResponse from(FiscalPeriod entity) {
        return new FiscalPeriodResponse(
                entity.getPeriodId().toString(),
                entity.getFiscalYear().getFiscalYear(),
                entity.getPeriodKey(),
                entity.getQuarter(),
                entity.getCalendarMonth().format(YEAR_MONTH_FMT),
                entity.getDisplayName(),
                entity.getSortOrder()
        );
    }
}
