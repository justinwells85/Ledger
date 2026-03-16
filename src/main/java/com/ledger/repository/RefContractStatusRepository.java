package com.ledger.repository;

import com.ledger.entity.RefContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefContractStatusRepository extends JpaRepository<RefContractStatus, String> {
    List<RefContractStatus> findAllByOrderBySortOrderAsc();
}
