package com.ledger.service;

import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.AccountType;
import com.ledger.entity.JournalEntry;
import com.ledger.entity.JournalEntryType;
import com.ledger.entity.JournalLine;
import com.ledger.repository.JournalEntryRepository;
import com.ledger.repository.JournalLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Core financial engine — all financial data flows through the journal.
 * Spec: 02-journal-ledger.md, BR-01, BR-02
 */
@Service
@Transactional
public class JournalService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;

    public JournalService(JournalEntryRepository journalEntryRepository,
                          JournalLineRepository journalLineRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
    }

    /**
     * Validate journal entry lines before persisting.
     * Extracted as package-private static method for unit testing without DB.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateEntry(List<JournalLineRequest> lines) {
        // BR-02: Every journal entry must have at least 2 lines
        if (lines == null || lines.size() < 2) {
            throw new IllegalArgumentException(
                    "Journal entry must have at least 2 lines (BR-02). Got: " +
                    (lines == null ? 0 : lines.size()));
        }

        // Validate no line has both debit > 0 and credit > 0
        for (JournalLineRequest line : lines) {
            if (line.debit().compareTo(BigDecimal.ZERO) > 0 &&
                line.credit().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException(
                        "A journal line cannot have both debit > 0 and credit > 0");
            }
        }

        // BR-01: Every journal entry must balance — SUM(debit) = SUM(credit)
        BigDecimal totalDebit = lines.stream()
                .map(JournalLineRequest::debit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = lines.stream()
                .map(JournalLineRequest::credit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException(
                    "Journal entry is unbalanced (BR-01): debits=" + totalDebit +
                    " credits=" + totalCredit);
        }
    }

    /**
     * Create a journal entry with lines.
     * Validates balance (BR-01), minimum 2 lines (BR-02), and no dual-sided lines.
     *
     * @return the persisted JournalEntry with all lines
     */
    public JournalEntry createEntry(JournalEntryType type, LocalDate effectiveDate,
                                     String description, String createdBy,
                                     List<JournalLineRequest> lines) {
        validateEntry(lines);

        JournalEntry entry = new JournalEntry();
        entry.setEntryType(type);
        entry.setEffectiveDate(effectiveDate);
        entry.setDescription(description);
        entry.setCreatedBy(createdBy);
        entry.setEntryDate(Instant.now());

        for (JournalLineRequest lineReq : lines) {
            JournalLine line = new JournalLine();
            line.setAccount(lineReq.account());
            line.setContractId(lineReq.contractId());
            line.setProjectId(lineReq.projectId());
            line.setMilestoneId(lineReq.milestoneId());
            line.setFiscalPeriodId(lineReq.fiscalPeriodId());
            line.setDebit(lineReq.debit());
            line.setCredit(lineReq.credit());
            line.setReferenceType(lineReq.referenceType());
            line.setReferenceId(lineReq.referenceId());
            entry.addLine(line);
        }

        return journalEntryRepository.save(entry);
    }

    /**
     * Get the planned balance for a milestone as of a given date.
     * SUM(debit) - SUM(credit) for PLANNED account lines.
     * Spec: 02-journal-ledger.md Section 6
     */
    @Transactional(readOnly = true)
    public BigDecimal getPlannedBalance(UUID milestoneId, LocalDate asOfDate) {
        return journalLineRepository.getBalanceByAccountAndMilestone(
                AccountType.PLANNED, milestoneId, asOfDate);
    }

    /**
     * Get the actual balance for a milestone as of a given date.
     * SUM(debit) - SUM(credit) for ACTUAL account lines.
     * Spec: 02-journal-ledger.md Section 6
     */
    @Transactional(readOnly = true)
    public BigDecimal getActualBalance(UUID milestoneId, LocalDate asOfDate) {
        return journalLineRepository.getBalanceByAccountAndMilestone(
                AccountType.ACTUAL, milestoneId, asOfDate);
    }

    /**
     * Get the planned balance for a contract in a fiscal period as of a given date.
     * Spec: 02-journal-ledger.md Section 6
     */
    @Transactional(readOnly = true)
    public BigDecimal getPlannedBalanceForContract(UUID contractId, UUID periodId,
                                                    LocalDate asOfDate) {
        return journalLineRepository.getBalanceByAccountContractAndPeriod(
                AccountType.PLANNED, contractId, periodId, asOfDate);
    }
}
