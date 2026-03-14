package com.ledger.repository;

import com.ledger.entity.AccountType;
import com.ledger.entity.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Repository for JournalLine entity with balance query support.
 * Spec: 02-journal-ledger.md Section 6
 */
@Repository
public interface JournalLineRepository extends JpaRepository<JournalLine, UUID> {

    /**
     * Get balance (SUM(debit) - SUM(credit)) for a given account type and milestone,
     * filtered by effective_date <= asOfDate.
     * Spec: 02-journal-ledger.md Section 6
     */
    @Query("SELECT COALESCE(SUM(jl.debit), 0) - COALESCE(SUM(jl.credit), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account = :account " +
           "AND jl.milestoneId = :milestoneId " +
           "AND je.effectiveDate <= :asOfDate")
    BigDecimal getBalanceByAccountAndMilestone(
            @Param("account") AccountType account,
            @Param("milestoneId") UUID milestoneId,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * Get balance (SUM(debit) - SUM(credit)) for a given account type, contract, and period,
     * filtered by effective_date <= asOfDate.
     * Spec: 02-journal-ledger.md Section 6
     */
    @Query("SELECT COALESCE(SUM(jl.debit), 0) - COALESCE(SUM(jl.credit), 0) " +
           "FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account = :account " +
           "AND jl.contractId = :contractId " +
           "AND jl.fiscalPeriodId = :periodId " +
           "AND je.effectiveDate <= :asOfDate")
    BigDecimal getBalanceByAccountContractAndPeriod(
            @Param("account") AccountType account,
            @Param("contractId") UUID contractId,
            @Param("periodId") UUID periodId,
            @Param("asOfDate") LocalDate asOfDate);
}
