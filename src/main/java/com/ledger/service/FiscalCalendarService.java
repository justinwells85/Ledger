package com.ledger.service;

import com.ledger.entity.FiscalPeriod;
import com.ledger.entity.FiscalYear;
import com.ledger.repository.FiscalPeriodRepository;
import com.ledger.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
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
     * Creates a new fiscal year with 12 auto-generated periods.
     * Spec: 18-admin-configuration.md Section 2, BR-85 through BR-90
     */
    @Transactional
    public FiscalYear createFiscalYear(String label) {
        // BR-85: validate format FY## (exactly 2 digits)
        if (label == null || !label.matches("FY\\d{2}")) {
            throw new IllegalArgumentException("Fiscal year label must match FY## format (e.g., FY28)");
        }

        // Duplicate check
        if (fiscalYearRepository.findById(label).isPresent()) {
            throw new IllegalStateException("Fiscal year " + label + " already exists");
        }

        // BR-90: must be sequential — no gaps allowed
        List<FiscalYear> existing = fiscalYearRepository.findAllByOrderByStartDateAsc();
        if (!existing.isEmpty()) {
            FiscalYear last = existing.get(existing.size() - 1);
            int lastYearNum = Integer.parseInt(last.getFiscalYear().substring(2));
            int requestedYearNum = Integer.parseInt(label.substring(2));
            if (requestedYearNum != lastYearNum + 1) {
                throw new IllegalStateException(
                        "Fiscal year must be sequential. Expected FY" + String.format("%02d", lastYearNum + 1));
            }
        }

        int yearNum = Integer.parseInt(label.substring(2));
        int calendarYear = 2000 + yearNum;

        FiscalYear fiscalYear = new FiscalYear();
        fiscalYear.setFiscalYear(label);
        fiscalYear.setStartDate(LocalDate.of(calendarYear - 1, Month.OCTOBER, 1));
        fiscalYear.setEndDate(LocalDate.of(calendarYear, Month.SEPTEMBER, 30));
        FiscalYear savedFiscalYear = fiscalYearRepository.save(fiscalYear);

        // BR-87: generate all 12 periods; BR-88: period key format {FY}-{NN}-{MMM}
        record PeriodDef(String num, String quarter, Month month, String abbr) {}
        List<PeriodDef> defs = List.of(
                new PeriodDef("01", "Q1", Month.OCTOBER,   "OCT"),
                new PeriodDef("02", "Q1", Month.NOVEMBER,  "NOV"),
                new PeriodDef("03", "Q1", Month.DECEMBER,  "DEC"),
                new PeriodDef("04", "Q2", Month.JANUARY,   "JAN"),
                new PeriodDef("05", "Q2", Month.FEBRUARY,  "FEB"),
                new PeriodDef("06", "Q2", Month.MARCH,     "MAR"),
                new PeriodDef("07", "Q3", Month.APRIL,     "APR"),
                new PeriodDef("08", "Q3", Month.MAY,       "MAY"),
                new PeriodDef("09", "Q3", Month.JUNE,      "JUN"),
                new PeriodDef("10", "Q4", Month.JULY,      "JUL"),
                new PeriodDef("11", "Q4", Month.AUGUST,    "AUG"),
                new PeriodDef("12", "Q4", Month.SEPTEMBER, "SEP")
        );

        for (int i = 0; i < defs.size(); i++) {
            PeriodDef def = defs.get(i);
            // Oct-Dec belong to the prior calendar year
            int periodCalendarYear = (def.month().getValue() >= Month.OCTOBER.getValue())
                    ? calendarYear - 1 : calendarYear;

            FiscalPeriod period = new FiscalPeriod();
            period.setFiscalYear(savedFiscalYear);
            period.setPeriodKey(label + "-" + def.num() + "-" + def.abbr());
            period.setQuarter(def.quarter());
            period.setCalendarMonth(LocalDate.of(periodCalendarYear, def.month(), 1));
            period.setDisplayName(def.month().getDisplayName(
                    java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + periodCalendarYear);
            period.setSortOrder(i + 1);
            fiscalPeriodRepository.save(period);
        }

        return savedFiscalYear;
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
