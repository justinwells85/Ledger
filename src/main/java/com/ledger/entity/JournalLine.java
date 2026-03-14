package com.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detail line for a journal entry. SUM(debit) must equal SUM(credit) per entry.
 * Spec: 02-journal-ledger.md Section 3
 */
@Entity
@Table(name = "journal_line")
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "line_id")
    private UUID lineId;

    @ManyToOne
    @JoinColumn(name = "entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "account", nullable = false, length = 20)
    private AccountType account;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "project_id", length = 20)
    private String projectId;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "fiscal_period_id", nullable = false)
    private UUID fiscalPeriodId;

    @Column(name = "debit", nullable = false, precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "credit", nullable = false, precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    public JournalLine() {
    }

    public UUID getLineId() {
        return lineId;
    }

    public void setLineId(UUID lineId) {
        this.lineId = lineId;
    }

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
    }

    public AccountType getAccount() {
        return account;
    }

    public void setAccount(AccountType account) {
        this.account = account;
    }

    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(UUID milestoneId) {
        this.milestoneId = milestoneId;
    }

    public UUID getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(UUID fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }
}
