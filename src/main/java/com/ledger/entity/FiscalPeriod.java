package com.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A single month within a fiscal year.
 * Spec: 01-domain-model.md Section 2.2, 03-fiscal-calendar.md
 *
 * calendar_month is stored as a DATE (first of month) in the DB, e.g., 2025-10-01.
 */
@Entity
@Table(name = "fiscal_period")
public class FiscalPeriod {

    @Id
    @Column(name = "period_id")
    private UUID periodId;

    @ManyToOne
    @JoinColumn(name = "fiscal_year", nullable = false)
    private FiscalYear fiscalYear;

    @Column(name = "period_key", nullable = false, unique = true, length = 20)
    private String periodKey;

    @Column(name = "quarter", nullable = false, length = 2)
    private String quarter;

    /**
     * Stored as DATE (first of month) in the DB, e.g., 2025-10-01 for October 2025.
     */
    @Column(name = "calendar_month", nullable = false)
    private LocalDate calendarMonth;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected FiscalPeriod() {
    }

    public UUID getPeriodId() {
        return periodId;
    }

    public FiscalYear getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(FiscalYear fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getQuarter() {
        return quarter;
    }

    public void setQuarter(String quarter) {
        this.quarter = quarter;
    }

    public LocalDate getCalendarMonth() {
        return calendarMonth;
    }

    public void setCalendarMonth(LocalDate calendarMonth) {
        this.calendarMonth = calendarMonth;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
