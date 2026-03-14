package com.ledger.controller;

import com.ledger.dto.MilestoneCreateRequest;
import com.ledger.dto.MilestoneResponse;
import com.ledger.entity.Milestone;
import com.ledger.entity.MilestoneVersion;
import com.ledger.repository.MilestoneVersionRepository;
import com.ledger.service.MilestoneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for milestone creation and versioning.
 * Spec: 13-api-design.md Section 5
 */
@RestController
@RequestMapping("/api/v1")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final MilestoneVersionRepository milestoneVersionRepository;

    public MilestoneController(MilestoneService milestoneService,
                                MilestoneVersionRepository milestoneVersionRepository) {
        this.milestoneService = milestoneService;
        this.milestoneVersionRepository = milestoneVersionRepository;
    }

    /**
     * POST /api/v1/projects/{projectId}/milestones
     * Create a milestone with v1 and PLAN_CREATE journal entry.
     * Spec: 13-api-design.md Section 5
     */
    @PostMapping("/projects/{projectId}/milestones")
    public ResponseEntity<?> createMilestone(@PathVariable String projectId,
                                              @RequestBody MilestoneCreateRequest request) {
        try {
            Milestone milestone = milestoneService.createMilestone(
                    projectId,
                    request.name(),
                    request.description(),
                    request.plannedAmount(),
                    request.fiscalPeriodId(),
                    request.effectiveDate(),
                    request.reason(),
                    "system"
            );

            MilestoneVersion currentVersion = milestoneVersionRepository
                    .findCurrentVersion(milestone.getMilestoneId())
                    .orElseThrow();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MilestoneResponse.from(milestone, currentVersion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
