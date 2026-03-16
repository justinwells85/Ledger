package com.ledger.controller;

import com.ledger.config.JwtUtil;
import com.ledger.entity.User;
import com.ledger.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for authentication.
 * Spec: 18-admin-configuration.md, BR-81
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (userService.verifyPassword(req.username(), req.password())) {
            User user = userService.findByUsername(req.username())
                    .orElseThrow(() -> new IllegalStateException("User not found after successful verification"));
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getDisplayName());
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole().name(),
                    "displayName", user.getDisplayName()
            ));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
}
