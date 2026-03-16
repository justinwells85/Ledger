package com.ledger.dto;

import com.ledger.entity.AuditLog;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for an AuditLog entry.
 * Spec: 11-change-management.md Section 5
 */
public record AuditLogResponse(
        UUID auditId,
        String entityType,
        String entityId,
        String action,
        Map<String, Map<String, String>> changes,
        String reason,
        Instant createdAt,
        String createdBy
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getAuditId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getChanges(),
                log.getReason(),
                log.getCreatedAt(),
                log.getCreatedBy()
        );
    }
}
