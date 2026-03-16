package com.ledger.controller;

import com.ledger.dto.ConfigUpdateRequest;
import com.ledger.entity.SystemConfig;
import com.ledger.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for system configuration CRUD.
 * Spec: 10-business-rules.md BR-31, BR-32
 */
@RestController
@RequestMapping("/api/v1/config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * GET /api/v1/config
     * Return all configuration values.
     * Spec: 10-business-rules.md BR-31
     */
    @GetMapping
    public List<SystemConfig> getAllConfig() {
        return systemConfigService.findAll();
    }

    /**
     * PUT /api/v1/config/{key}
     * Update a configuration value. Requires reason for audit.
     * Spec: 10-business-rules.md BR-31, 11-change-management.md Section 2.2
     */
    @PutMapping("/{key}")
    public ResponseEntity<?> updateConfig(
            @PathVariable String key,
            @RequestBody ConfigUpdateRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason is required"));
        }

        try {
            SystemConfig config = systemConfigService.update(key, request);
            return ResponseEntity.ok(config);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
