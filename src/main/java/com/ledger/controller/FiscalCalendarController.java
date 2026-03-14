package com.ledger.controller;

import com.ledger.dto.FiscalPeriodResponse;
import com.ledger.dto.FiscalYearResponse;
import com.ledger.service.FiscalCalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for fiscal calendar operations.
 * Spec: 13-api-design.md Section 2
 */
@RestController
@RequestMapping("/api/v1/fiscal-years")
public class FiscalCalendarController {

    private final FiscalCalendarService fiscalCalendarService;

    public FiscalCalendarController(FiscalCalendarService fiscalCalendarService) {
        this.fiscalCalendarService = fiscalCalendarService;
    }

    /**
     * GET /api/v1/fiscal-years
     * List all fiscal years ordered by start_date.
     */
    @GetMapping
    public List<FiscalYearResponse> listFiscalYears() {
        return fiscalCalendarService.getAllFiscalYears().stream()
                .map(FiscalYearResponse::from)
                .toList();
    }

    /**
     * GET /api/v1/fiscal-years/{fiscalYear}/periods
     * List all periods for a fiscal year, sorted by sort_order.
     */
    @GetMapping("/{fiscalYear}/periods")
    public List<FiscalPeriodResponse> listPeriods(@PathVariable String fiscalYear) {
        return fiscalCalendarService.getPeriodsForYear(fiscalYear).stream()
                .map(FiscalPeriodResponse::from)
                .toList();
    }

    /**
     * GET /api/v1/fiscal-years/resolve-period?postingDate=YYYY-MM-DD
     * Resolve a posting date to its fiscal period.
     * Spec: 03-fiscal-calendar.md Section 6
     */
    @GetMapping("/resolve-period")
    public ResponseEntity<FiscalPeriodResponse> resolvePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate postingDate) {
        return fiscalCalendarService.resolvePeriod(postingDate)
                .map(FiscalPeriodResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
