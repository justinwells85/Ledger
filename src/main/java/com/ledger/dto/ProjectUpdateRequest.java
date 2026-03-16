package com.ledger.dto;

/**
 * DTO for updating a project. Includes required reason field for audit.
 * Spec: 13-api-design.md Section 4, 11-change-management.md Section 4
 */
public record ProjectUpdateRequest(
        String name,
        String wbse,
        String status,
        String reason
) {
}
