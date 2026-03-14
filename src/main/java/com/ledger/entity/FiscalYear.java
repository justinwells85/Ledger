package com.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Disney fiscal year (October through September).
 * Spec: 01-domain-model.md Section 2.1, 03-fiscal-calendar.md
 */
@Entity
@Table(name = "fiscal_year")
public class FiscalYear {

    @Id
    @Column(name = "fiscal_year", length = 10)
    private String fiscalYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "fiscalYear")
    @OrderBy("sortOrder ASC")
    private List<FiscalPeriod> periods = new ArrayList<>();

    protected FiscalYear() {
    }

    public String getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<FiscalPeriod> getPeriods() {
        return periods;
    }
}
