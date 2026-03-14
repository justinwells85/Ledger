package com.ledger.repository;

import com.ledger.entity.SapImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for SapImport entity.
 * Spec: 05-sap-ingestion.md
 */
@Repository
public interface SapImportRepository extends JpaRepository<SapImport, UUID> {
}
