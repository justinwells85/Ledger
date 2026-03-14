package com.ledger.repository;

import com.ledger.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Milestone entity.
 * Spec: 01-domain-model.md Section 2.5
 */
@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {

    List<Milestone> findByProjectProjectId(String projectId);
}
