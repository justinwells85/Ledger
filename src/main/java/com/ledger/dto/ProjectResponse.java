package com.ledger.dto;

import com.ledger.entity.Project;

import java.time.Instant;

/**
 * DTO for project API responses.
 * Spec: 13-api-design.md Section 4
 */
public record ProjectResponse(
        String projectId,
        String contractId,
        String wbse,
        String name,
        String fundingSource,
        String status,
        Instant createdAt,
        String createdBy
) {
    public static ProjectResponse from(Project entity) {
        return new ProjectResponse(
                entity.getProjectId(),
                entity.getContract().getContractId().toString(),
                entity.getWbse(),
                entity.getName(),
                entity.getFundingSource().name(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}
