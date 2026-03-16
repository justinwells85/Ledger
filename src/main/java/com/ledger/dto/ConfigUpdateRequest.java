package com.ledger.dto;

/**
 * DTO for updating a system configuration value.
 * Spec: 10-business-rules.md BR-31, 11-change-management.md Section 2.2
 */
public record ConfigUpdateRequest(
        String value,
        String reason
) {
}
