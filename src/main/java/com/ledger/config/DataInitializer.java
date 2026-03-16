package com.ledger.config;

import com.ledger.entity.User;
import com.ledger.entity.UserRole;
import com.ledger.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default data on application start.
 * Creates a default admin user if no users exist.
 * Spec: 18-admin-configuration.md, BR-80
 */
@Component
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setDisplayName("System Admin");
            admin.setEmail("admin@ledger.local");
            admin.setRole(UserRole.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("admin"));
            admin.setActive(true);
            userRepository.save(admin);
        }
    }
}
