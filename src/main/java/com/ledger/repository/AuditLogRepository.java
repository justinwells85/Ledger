package com.ledger.repository;

import com.ledger.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AuditLog entity.
 * Spec: 11-change-management.md Section 3
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
