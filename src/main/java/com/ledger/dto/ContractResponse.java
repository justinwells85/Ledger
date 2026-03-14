package com.ledger.dto;

import com.ledger.entity.Contract;

import java.time.Instant;
import java.time.LocalDate;

/**
 * DTO for contract API responses.
 * Spec: 13-api-design.md Section 3
 */
public record ContractResponse(
        String contractId,
        String name,
        String vendor,
        String description,
        String ownerUser,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant createdAt,
        String createdBy
) {
    public static ContractResponse from(Contract entity) {
        return new ContractResponse(
                entity.getContractId().toString(),
                entity.getName(),
                entity.getVendor(),
                entity.getDescription(),
                entity.getOwnerUser(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }
}
