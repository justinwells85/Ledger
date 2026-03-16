package com.ledger.user;

import com.ledger.BaseIntegrationTest;
import com.ledger.dto.CreateUserRequest;
import com.ledger.entity.User;
import com.ledger.entity.UserRole;
import com.ledger.repository.UserRepository;
import com.ledger.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for user management business rules.
 * Spec: 18-admin-configuration.md, BR-81, BR-84
 */
class UserTest extends BaseIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void deactivating_last_admin_throws_400() {
        // Spec: 18-admin-configuration.md, BR-84
        User admin = userService.createUser(new CreateUserRequest(
                "admin1", "Admin One", "admin1@test.com", UserRole.ADMIN, "password123"));

        assertThatThrownBy(() -> userService.deactivate(admin.getUserId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot deactivate the last active ADMIN");
    }

    @Test
    void deactivating_one_of_two_admins_succeeds() {
        // Spec: 18-admin-configuration.md, BR-84
        User admin1 = userService.createUser(new CreateUserRequest(
                "admin1", "Admin One", "admin1@test.com", UserRole.ADMIN, "password123"));
        userService.createUser(new CreateUserRequest(
                "admin2", "Admin Two", "admin2@test.com", UserRole.ADMIN, "password456"));

        User deactivated = userService.deactivate(admin1.getUserId());

        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    void verifying_password_for_inactive_user_returns_false() {
        // Spec: 18-admin-configuration.md, BR-81
        // Need two admins so we can deactivate one
        userService.createUser(new CreateUserRequest(
                "admin2", "Admin Two", "admin2@test.com", UserRole.ADMIN, "otherpass"));
        User admin = userService.createUser(new CreateUserRequest(
                "admin1", "Admin One", "admin1@test.com", UserRole.ADMIN, "correctpass"));

        userService.deactivate(admin.getUserId());

        boolean result = userService.verifyPassword("admin1", "correctpass");

        assertThat(result).isFalse();
    }
}
