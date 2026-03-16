package com.ledger.controller;

import com.ledger.dto.CreateUserRequest;
import com.ledger.dto.UpdateUserRoleRequest;
import com.ledger.dto.UserResponse;
import com.ledger.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user management (admin only).
 * Spec: 18-admin-configuration.md, BR-80 through BR-84
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return userService.listUsers().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userService.createUser(request)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable UUID userId,
                                                    @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.updateRole(userId, request.role())));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivate(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserResponse.from(userService.deactivate(userId)));
    }

    @PostMapping("/{userId}/reactivate")
    public ResponseEntity<UserResponse> reactivate(@PathVariable UUID userId) {
        return ResponseEntity.ok(UserResponse.from(userService.reactivate(userId)));
    }
}
