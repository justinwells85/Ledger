package com.ledger.repository;

import com.ledger.entity.Contract;
import com.ledger.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Contract entity.
 * Spec: 01-domain-model.md Section 2.3
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    boolean existsByName(String name);

    List<Contract> findByStatus(ContractStatus status);
}
