package com.ledger.controller;

import com.ledger.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for reporting endpoints.
 * Spec: 09-reporting.md, 13-api-design.md Section 10
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * GET /api/v1/reports/budget
     * Budget plan report grouped by project.
     * Spec: 09-reporting.md Section 2.1, 13-api-design.md Section 10
     */
    @GetMapping("/budget")
    public ResponseEntity<?> getBudgetReport(
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) String projectId,
            @RequestParam String fiscalYear,
            @RequestParam(required = false) String fundingSource,
            @RequestParam(required = false) String groupBy,
            @RequestParam(required = false) LocalDate asOfDate) {
        try {
            ReportService.BudgetReport report = reportService.getBudgetReport(
                    contractId, projectId, fiscalYear, fundingSource, asOfDate);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
