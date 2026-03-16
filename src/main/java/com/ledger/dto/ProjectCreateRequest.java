package com.ledger.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new project under a contract.
 * Spec: 13-api-design.md Section 4
 */
public record ProjectCreateRequest(
        @NotBlank String projectId,
        @NotBlank String wbse,
        @NotBlank String name,
        @NotBlank String fundingSource
) {
}
