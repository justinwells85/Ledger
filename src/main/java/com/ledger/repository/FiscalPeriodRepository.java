package com.ledger.repository;

import com.ledger.entity.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for FiscalPeriod entities.
 * Spec: 01-domain-model.md Section 2.2, 03-fiscal-calendar.md Section 6
 */
@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {

    List<FiscalPeriod> findByFiscalYearFiscalYearOrderBySortOrderAsc(String fiscalYear);

    /**
     * Resolve a posting date to a fiscal period by matching the first-of-month date.
     * Spec: 03-fiscal-calendar.md Section 6
     */
    Optional<FiscalPeriod> findByCalendarMonth(LocalDate calendarMonth);
}
