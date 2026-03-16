package com.ledger.controller;

import com.ledger.config.SecurityUtils;
import com.ledger.dto.ActualLineResponse;
import com.ledger.dto.ReconciliationCreateRequest;
import com.ledger.dto.ReconciliationResponse;
import com.ledger.entity.Reconciliation;
import com.ledger.entity.ReconciliationCategory;
import com.ledger.repository.ActualLineRepository;
import com.ledger.service.AlreadyReconciledException;
import com.ledger.service.ReconciliationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final ActualLineRepository actualLineRepository;

    public ReconciliationController(ReconciliationService reconciliationService,
                                     ActualLineRepository actualLineRepository) {
        this.reconciliationService = reconciliationService;
        this.actualLineRepository = actualLineRepository;
    }

    /**
     * GET /api/v1/reconciliation/unreconciled
     * Return all non-duplicate actual lines that have no reconciliation.
     * Spec: 06-reconciliation.md Section 3
     */
    @GetMapping("/unreconciled")
    public List<ActualLineResponse> getUnreconciled() {
        return actualLineRepository.findUnreconciled()
                .stream().map(ActualLineResponse::from).toList();
    }

    /**
     * POST /api/v1/reconciliation
     * Create a reconciliation between an actual and a milestone.
     * Spec: 06-reconciliation.md Section 3.4-3.5
     */
    @PostMapping
    public ResponseEntity<?> createReconciliation(@Valid @RequestBody ReconciliationCreateRequest request) {
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
                    SecurityUtils.currentUsername()
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
            reconciliationService.undoReconciliation(id, reason, SecurityUtils.currentUsername());
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

    /**
     * GET /api/v1/reconciliation/candidates/{actualId}
     * Return ranked milestone candidates for reconciling the given actual.
     * WBSE-matching milestones appear first with higher relevance scores.
     * Spec: 06-reconciliation.md Section 3.3
     */
    @GetMapping("/candidates/{actualId}")
    public ResponseEntity<?> getCandidates(@PathVariable UUID actualId) {
        try {
            return ResponseEntity.ok(reconciliationService.getCandidates(actualId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
