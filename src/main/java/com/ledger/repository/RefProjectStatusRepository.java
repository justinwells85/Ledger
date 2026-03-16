package com.ledger.repository;

import com.ledger.entity.RefProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefProjectStatusRepository extends JpaRepository<RefProjectStatus, String> {
    List<RefProjectStatus> findAllByOrderBySortOrderAsc();
}
