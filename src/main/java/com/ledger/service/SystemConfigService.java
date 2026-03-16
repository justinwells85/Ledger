package com.ledger.service;

import com.ledger.config.SecurityUtils;
import com.ledger.dto.ConfigUpdateRequest;
import com.ledger.entity.SystemConfig;
import com.ledger.repository.SystemConfigRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for system configuration operations.
 * Spec: 10-business-rules.md BR-31, BR-32
 */
@Service
public class SystemConfigService {

    private final SystemConfigRepository repo;
    private final AuditService auditService;

    public SystemConfigService(SystemConfigRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    public List<SystemConfig> findAll() {
        Sort sort = Sort.by(Sort.Direction.ASC, "configKey");
        return repo.findAll(PageRequest.of(0, 200, sort)).getContent();
    }

    @Transactional
    public SystemConfig update(String key, ConfigUpdateRequest request) {
        SystemConfig config = repo.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Config key not found: " + key));
        String oldValue = config.getConfigValue();
        config.setConfigValue(request.value());
        config.setUpdatedAt(Instant.now());
        config.setUpdatedBy(SecurityUtils.currentUsername());
        repo.save(config);
        auditService.log("CONFIGURATION", key, "UPDATE",
                Map.of("configValue", Map.of("before", oldValue, "after", request.value())),
                request.reason(), SecurityUtils.currentUsername());
        return config;
    }
}
