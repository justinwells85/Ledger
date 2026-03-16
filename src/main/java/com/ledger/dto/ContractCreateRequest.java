package com.ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO for creating a new contract.
 * Spec: 13-api-design.md Section 3
 */
public record ContractCreateRequest(
        @NotBlank String name,
        @NotBlank String vendor,
        String description,
        @NotBlank String ownerUser,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
}
