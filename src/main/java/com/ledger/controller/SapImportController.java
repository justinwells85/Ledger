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
}
