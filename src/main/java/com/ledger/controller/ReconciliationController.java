package com.ledger.controller;

import com.ledger.dto.ReconciliationCreateRequest;
import com.ledger.dto.ReconciliationResponse;
import com.ledger.entity.Reconciliation;
import com.ledger.entity.ReconciliationCategory;
import com.ledger.service.AlreadyReconciledException;
import com.ledger.service.ReconciliationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for reconciliation lifecycle.
 * Spec: 13-api-design.md Section 8, 06-reconciliation.md
 */
@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /**
     * POST /api/v1/reconciliation
     * Create a reconciliation between an actual and a milestone.
     * Spec: 06-reconciliation.md Section 3.4-3.5
     */
    @PostMapping
    public ResponseEntity<?> createReconciliation(@RequestBody ReconciliationCreateRequest request) {
        if (request.category() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Category is required"));
        }

        ReconciliationCategory category;
        try {
            category = ReconciliationCategory.valueOf(request.category());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid category: " + request.category() +
                            ". Must be one of: INVOICE, ACCRUAL, ACCRUAL_REVERSAL, ALLOCATION"));
        }

        try {
            Reconciliation rec = reconciliationService.createReconciliation(
                    request.actualId(),
                    request.milestoneId(),
                    category,
                    request.matchNotes(),
                    "system"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(ReconciliationResponse.from(rec));
        } catch (AlreadyReconciledException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/reconciliation/{id}?reason=...
     * Undo a reconciliation. Reason is required.
     * Spec: 06-reconciliation.md Section 3.6
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> undoReconciliation(@PathVariable UUID id,
                                                  @RequestParam(required = false) String reason) {
        try {
            reconciliationService.undoReconciliation(id, reason, "system");
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/v1/reconciliation/status/{milestoneId}
     * Get derived reconciliation status for a milestone.
     * Spec: 06-reconciliation.md Section 4
     */
    @GetMapping("/status/{milestoneId}")
    public ResponseEntity<?> getStatus(@PathVariable UUID milestoneId) {
        try {
            return ResponseEntity.ok(reconciliationService.getStatus(milestoneId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
