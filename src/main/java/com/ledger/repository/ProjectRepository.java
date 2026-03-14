package com.ledger.repository;

import com.ledger.entity.FundingSource;
import com.ledger.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Project entity.
 * Spec: 01-domain-model.md Section 2.4
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    List<Project> findByContractContractId(UUID contractId);

    List<Project> findByContractContractIdAndFundingSource(UUID contractId, FundingSource fundingSource);

    boolean existsByProjectId(String projectId);
}
