package com.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger.security")
public record LedgerSecurityProperties(
        String username,
        String password,
        String jwtSecret,
        int jwtExpiryHours
) {}
