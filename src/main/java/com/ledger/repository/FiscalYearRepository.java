package com.ledger.repository;

import com.ledger.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for FiscalYear entities.
 * Spec: 01-domain-model.md Section 2.1
 */
@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, String> {

    List<FiscalYear> findAllByOrderByStartDateAsc();
}
