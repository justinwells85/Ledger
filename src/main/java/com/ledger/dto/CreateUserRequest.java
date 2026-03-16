package com.ledger.dto;

import com.ledger.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a new user.
 * Spec: 18-admin-configuration.md, BR-80
 */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String displayName,
        @NotBlank String email,
        @NotNull UserRole role,
        @NotBlank String password
) {}
