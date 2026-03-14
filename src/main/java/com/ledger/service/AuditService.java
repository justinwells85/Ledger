package com.ledger.service;

import com.ledger.entity.AuditLog;
import com.ledger.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Reusable audit logging service.
 * Spec: 11-change-management.md Sections 2.2 and 3
 */
@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an entity change to the audit_log table.
     *
     * @param entityType  e.g., CONTRACT, PROJECT, CONFIGURATION
     * @param entityId    the ID of the changed entity
     * @param action      e.g., CREATE, UPDATE, STATUS_CHANGE
     * @param changes     map of field -> {old, new} values (nullable for CREATE)
     * @param reason      reason for the change (required for UPDATE/STATUS_CHANGE)
     * @param createdBy   user who made the change
     */
    public void log(String entityType, String entityId, String action,
                    Map<String, Map<String, String>> changes, String reason, String createdBy) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setChanges(changes);
        auditLog.setReason(reason);
        auditLog.setCreatedBy(createdBy);
        auditLogRepository.save(auditLog);
    }
}
