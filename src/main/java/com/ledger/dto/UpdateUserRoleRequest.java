package com.ledger.dto;

import com.ledger.entity.UserRole;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a user's role.
 * Spec: 18-admin-configuration.md, BR-83
 */
public record UpdateUserRoleRequest(
        @NotNull UserRole role
) {}
