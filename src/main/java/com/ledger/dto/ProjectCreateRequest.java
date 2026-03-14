package com.ledger.dto;

/**
 * DTO for creating a new project under a contract.
 * Spec: 13-api-design.md Section 4
 */
public record ProjectCreateRequest(
        String projectId,
        String wbse,
        String name,
        String fundingSource
) {
}
