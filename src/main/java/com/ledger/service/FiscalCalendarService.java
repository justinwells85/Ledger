package com.ledger.service;

import com.ledger.entity.FiscalPeriod;
import com.ledger.entity.FiscalYear;
import com.ledger.repository.FiscalPeriodRepository;
import com.ledger.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for fiscal calendar operations.
 * Spec: 03-fiscal-calendar.md, 13-api-design.md Section 2
 */
@Service
@Transactional(readOnly = true)
public class FiscalCalendarService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public FiscalCalendarService(FiscalYearRepository fiscalYearRepository,
                                  FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalYearRepository = fiscalYearRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Returns all fiscal years ordered by start_date ascending.
     */
    public List<FiscalYear> getAllFiscalYears() {
        return fiscalYearRepository.findAllByOrderByStartDateAsc();
    }

    /**
     * Returns all fiscal periods for the given fiscal year, ordered by sort_order.
     */
    public List<FiscalPeriod> getPeriodsForYear(String fiscalYear) {
        return fiscalPeriodRepository.findByFiscalYearFiscalYearOrderBySortOrderAsc(fiscalYear);
    }

    /**
     * Resolves a posting date to its fiscal period.
     * The posting date's year-month determines the fiscal period.
     * Find the fiscal_period where calendar_month = first day of posting date's month.
     *
     * Spec: 03-fiscal-calendar.md Section 6
     *
     * @param postingDate the posting date to resolve
     * @return the matching fiscal period, or empty if no matching period exists
     */
    public Optional<FiscalPeriod> resolvePeriod(LocalDate postingDate) {
        LocalDate firstOfMonth = postingDate.withDayOfMonth(1);
        return fiscalPeriodRepository.findByCalendarMonth(firstOfMonth);
    }
}
