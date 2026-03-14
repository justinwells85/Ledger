package com.ledger.dto;

import com.ledger.entity.Milestone;
import com.ledger.entity.MilestoneVersion;

import java.util.UUID;

/**
 * Response DTO for a Milestone with its current version.
 * Spec: 13-api-design.md Section 5
 */
public record MilestoneResponse(
    UUID milestoneId,
    String projectId,
    String name,
    String description,
    MilestoneVersionResponse currentVersion
) {
    public static MilestoneResponse from(Milestone milestone, MilestoneVersion currentVersion) {
        return new MilestoneResponse(
                milestone.getMilestoneId(),
                milestone.getProject().getProjectId(),
                milestone.getName(),
                milestone.getDescription(),
                MilestoneVersionResponse.from(currentVersion)
        );
    }
}
