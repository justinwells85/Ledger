package com.ledger.repository;

import com.ledger.entity.User;
import com.ledger.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity.
 * Spec: 18-admin-configuration.md, BR-80 through BR-84
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    long countByRoleAndActiveTrue(UserRole role);

    long count();
}
