package com.ledger.dto;

import com.ledger.entity.User;
import com.ledger.entity.UserRole;

import java.util.UUID;

/**
 * DTO response for user data.
 * Spec: 18-admin-configuration.md, BR-80
 */
public record UserResponse(
        UUID userId,
        String username,
        String displayName,
        String email,
        UserRole role,
        boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.isActive()
        );
    }
}
