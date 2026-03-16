package com.ledger.repository;

import com.ledger.entity.RefReconciliationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefReconciliationCategoryRepository extends JpaRepository<RefReconciliationCategory, String> {
    List<RefReconciliationCategory> findAllByOrderBySortOrderAsc();
}
