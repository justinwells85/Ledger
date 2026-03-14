package com.ledger.repository;

import com.ledger.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for SystemConfig.
 * Spec: 10-business-rules.md BR-31, BR-32
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
