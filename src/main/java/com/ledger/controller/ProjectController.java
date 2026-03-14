package com.ledger.controller;

import com.ledger.dto.ProjectCreateRequest;
import com.ledger.dto.ProjectResponse;
import com.ledger.entity.FundingSource;
import com.ledger.service.DuplicateProjectIdException;
import com.ledger.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for project operations.
 * Spec: 13-api-design.md Section 4
 */
@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * GET /api/v1/contracts/{contractId}/projects
     * List projects for a contract, optionally filtered by funding source.
     */
    @GetMapping("/api/v1/contracts/{contractId}/projects")
    public List<ProjectResponse> listProjects(
            @PathVariable UUID contractId,
            @RequestParam(required = false) FundingSource fundingSource) {
        return projectService.listProjects(contractId, fundingSource).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /**
     * POST /api/v1/contracts/{contractId}/projects
     * Create a project under a contract.
     */
    @PostMapping("/api/v1/contracts/{contractId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID contractId,
            @RequestBody ProjectCreateRequest request) {
        var project = projectService.createProject(contractId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    /**
     * GET /api/v1/projects/{projectId}
     * Get a single project.
     */
    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectResponse getProject(@PathVariable String projectId) {
        return ProjectResponse.from(projectService.getProject(projectId));
    }

    @ExceptionHandler(DuplicateProjectIdException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateProjectId(DuplicateProjectIdException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
