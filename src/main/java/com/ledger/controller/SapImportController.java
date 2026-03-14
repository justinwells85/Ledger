package com.ledger.controller;

import com.ledger.dto.SapImportResponse;
import com.ledger.entity.SapImport;
import com.ledger.service.SapImportService;
import com.ledger.service.SapParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for SAP import pipeline.
 * Spec: 13-api-design.md Section 7, 05-sap-ingestion.md
 */
@RestController
@RequestMapping("/api/v1/imports")
public class SapImportController {

    private final SapImportService sapImportService;

    public SapImportController(SapImportService sapImportService) {
        this.sapImportService = sapImportService;
    }

    /**
     * POST /api/v1/imports/upload
     * Upload a SAP file (CSV or Excel), parse, dedup, and stage.
     * Spec: 05-sap-ingestion.md Steps 1-4
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        try {
            SapImport staged = sapImportService.uploadAndStage(file, "system");
            return ResponseEntity.status(HttpStatus.CREATED).body(SapImportResponse.from(staged));
        } catch (SapParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot parse file: " + e.getMessage()));
        }
    }

    /**
     * POST /api/v1/imports/{id}/commit
     * Commit a staged import — creates ACTUAL_IMPORT journal entries for new lines.
     * Spec: 05-sap-ingestion.md Step 6
     */
    @PostMapping("/{id}/commit")
    public ResponseEntity<?> commit(@PathVariable UUID id) {
        try {
            SapImport committed = sapImportService.commitImport(id, "system");
            return ResponseEntity.ok(SapImportResponse.from(committed));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/imports/{id}/reject
     * Reject a staged import — no journal entries created.
     * Spec: 05-sap-ingestion.md Section 5
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable UUID id) {
        try {
            SapImport rejected = sapImportService.rejectImport(id);
            return ResponseEntity.ok(SapImportResponse.from(rejected));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
