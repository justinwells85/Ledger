package com.ledger.dto;

import java.time.LocalDate;

/**
 * DTO for updating a contract. Includes required reason field for audit.
 * Spec: 13-api-design.md Section 3, 11-change-management.md Section 4
 */
public record ContractUpdateRequest(
        String name,
        String vendor,
        String description,
        String ownerUser,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        String reason
) {
}
