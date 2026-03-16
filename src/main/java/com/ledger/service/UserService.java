package com.ledger.service;

import com.ledger.dto.CreateUserRequest;
import com.ledger.entity.User;
import com.ledger.entity.UserRole;
import com.ledger.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for user management operations.
 * Spec: 18-admin-configuration.md, BR-80 through BR-84
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Create a new user with BCrypt-hashed password.
     * BR-80: Users must have unique username and email.
     */
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        return userRepository.save(user);
    }

    /**
     * Find a user by username.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Verify a user's password. Returns false if user not found or inactive.
     * BR-81: Inactive users cannot authenticate.
     */
    @Transactional(readOnly = true)
    public boolean verifyPassword(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .map(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .orElse(false);
    }

    /**
     * List all users.
     */
    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    /**
     * Update the role of a user.
     * BR-83: Only admins can change roles.
     */
    public User updateRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setRole(newRole);
        return userRepository.save(user);
    }

    /**
     * Deactivate a user.
     * BR-84: Cannot deactivate the last active ADMIN.
     */
    public User deactivate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (user.getRole() == UserRole.ADMIN && user.isActive()) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(UserRole.ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalArgumentException("Cannot deactivate the last active ADMIN");
            }
        }

        user.setActive(false);
        return userRepository.save(user);
    }

    /**
     * Reactivate a deactivated user.
     */
    public User reactivate(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setActive(true);
        return userRepository.save(user);
    }
}
