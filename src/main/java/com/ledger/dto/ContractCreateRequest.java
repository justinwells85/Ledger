package com.ledger.dto;

import java.time.LocalDate;

/**
 * DTO for creating a new contract.
 * Spec: 13-api-design.md Section 3
 */
public record ContractCreateRequest(
        String name,
        String vendor,
        String description,
        String ownerUser,
        LocalDate startDate,
        LocalDate endDate
) {
}
