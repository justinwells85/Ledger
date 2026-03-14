package com.ledger.controller;

import com.ledger.dto.MilestoneCancelRequest;
import com.ledger.dto.MilestoneCreateRequest;
import com.ledger.dto.MilestoneResponse;
import com.ledger.dto.MilestoneVersionAdjustRequest;
import com.ledger.dto.MilestoneVersionResponse;
import com.ledger.entity.Milestone;
import com.ledger.entity.MilestoneVersion;
import com.ledger.repository.MilestoneVersionRepository;
import com.ledger.service.MilestoneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
     * GET /api/v1/projects/{projectId}/milestones?asOfDate=...
     * Return milestones for a project, each with the version effective as of the given date.
     * Milestones with no version as of the date are excluded.
     * Spec: 08-time-machine.md Section 2-4, BR-41
     */
    @GetMapping("/projects/{projectId}/milestones")
    public ResponseEntity<List<MilestoneResponse>> getMilestonesAsOf(
            @PathVariable String projectId,
            @RequestParam(required = false) LocalDate asOfDate) {
        List<MilestoneResponse> result = milestoneService.getMilestonesAsOf(projectId, asOfDate)
                .stream()
                .map(mao -> MilestoneResponse.from(mao.milestone(), mao.version()))
                .toList();
        return ResponseEntity.ok(result);
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

    /**
     * POST /api/v1/milestones/{milestoneId}/versions
     * Create a new version with PLAN_ADJUST journal entry.
     * Spec: 13-api-design.md Section 5
     */
    @PostMapping("/milestones/{milestoneId}/versions")
    public ResponseEntity<?> createVersion(@PathVariable UUID milestoneId,
                                            @RequestBody MilestoneVersionAdjustRequest request) {
        try {
            MilestoneVersion version = milestoneService.createVersion(
                    milestoneId,
                    request.plannedAmount(),
                    request.fiscalPeriodId(),
                    request.effectiveDate(),
                    request.reason(),
                    "system"
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(MilestoneVersionResponse.from(version));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/v1/milestones/{milestoneId}/versions
     * Return all versions for a milestone ordered by version_number ascending.
     * Spec: 13-api-design.md Section 5
     */
    @GetMapping("/milestones/{milestoneId}/versions")
    public ResponseEntity<List<MilestoneVersionResponse>> getVersionHistory(@PathVariable UUID milestoneId) {
        List<MilestoneVersionResponse> history = milestoneVersionRepository
                .findVersionHistory(milestoneId)
                .stream()
                .map(MilestoneVersionResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }

    /**
     * POST /api/v1/milestones/{milestoneId}/cancel
     * Cancel a milestone by creating a v with planned_amount = 0.
     * Spec: 04-milestone-versioning.md Section 4 (Cancelling), V-09
     */
    @PostMapping("/milestones/{milestoneId}/cancel")
    public ResponseEntity<?> cancelMilestone(@PathVariable UUID milestoneId,
                                              @RequestBody MilestoneCancelRequest request) {
        try {
            java.time.LocalDate effectiveDate = request.effectiveDate() != null
                    ? request.effectiveDate()
                    : java.time.LocalDate.now();
            MilestoneVersion cancelled = milestoneService.cancelMilestone(
                    milestoneId, effectiveDate, request.reason(), "system");
            return ResponseEntity.status(HttpStatus.CREATED).body(MilestoneVersionResponse.from(cancelled));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
