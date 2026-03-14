package com.ledger.service;

import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Reconciliation lifecycle: create, undo, status derivation.
 * Spec: 06-reconciliation.md, BR-06, BR-07, BR-30-32, 07-accrual-lifecycle.md
 */
@Service
@Transactional
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final ActualLineRepository actualLineRepository;
    private final MilestoneRepository milestoneRepository;
    private final MilestoneVersionRepository milestoneVersionRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final JournalService journalService;

    public ReconciliationService(ReconciliationRepository reconciliationRepository,
                                  ActualLineRepository actualLineRepository,
                                  MilestoneRepository milestoneRepository,
                                  MilestoneVersionRepository milestoneVersionRepository,
                                  SystemConfigRepository systemConfigRepository,
                                  JournalService journalService) {
        this.reconciliationRepository = reconciliationRepository;
        this.actualLineRepository = actualLineRepository;
        this.milestoneRepository = milestoneRepository;
        this.milestoneVersionRepository = milestoneVersionRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.journalService = journalService;
    }

    /**
     * Create a reconciliation linking an actual to a milestone.
     * Spec: 06-reconciliation.md Section 3.4-3.5, BR-06, BR-07
     *
     * @throws AlreadyReconciledExceptions if actual already has a reconciliation (BR-06)
     * @throws IllegalArgumentException for invalid inputs
     */
    public Reconciliation createReconciliation(UUID actualId,
                                                UUID milestoneId,
                                                ReconciliationCategory category,
                                                String matchNotes,
                                                String reconciledBy) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required (BR-07)");
        }

        ActualLine actual = actualLineRepository.findById(actualId)
                .orElseThrow(() -> new IllegalArgumentException("Actual line not found: " + actualId));

        // R-06: Cannot reconcile duplicates
        if (actual.isDuplicate()) {
            throw new IllegalArgumentException("Cannot reconcile a duplicate actual line (R-06)");
        }

        // BR-06: An actual can be reconciled to at most one milestone
        if (reconciliationRepository.findByActualId(actualId).isPresent()) {
            throw new AlreadyReconciledException("Actual " + actualId + " is already reconciled (BR-06)");
        }

        milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        Reconciliation rec = new Reconciliation();
        rec.setActualId(actualId);
        rec.setMilestoneId(milestoneId);
        rec.setCategory(category);
        rec.setMatchNotes(matchNotes);
        rec.setReconciledBy(reconciledBy);
        reconciliationRepository.save(rec);

        // Create informational RECONCILE journal entry
        UUID periodId = actual.getFiscalPeriodId();
        if (periodId != null) {
            List<JournalLineRequest> lines = List.of(
                    new JournalLineRequest(AccountType.ACTUAL, null, null,
                            milestoneId, periodId, BigDecimal.ZERO, BigDecimal.ZERO,
                            "RECONCILIATION", rec.getReconciliationId()),
                    new JournalLineRequest(AccountType.VARIANCE_RESERVE, null, null,
                            milestoneId, periodId, BigDecimal.ZERO, BigDecimal.ZERO,
                            "RECONCILIATION", rec.getReconciliationId())
            );
            journalService.createEntry(JournalEntryType.RECONCILE,
                    actual.getPostingDate(),
                    "Reconciled: actual " + actualId + " → milestone " + milestoneId + " [" + category + "]",
                    reconciledBy, lines);
        }

        return rec;
    }

    /**
     * Undo a reconciliation. Requires a reason. Creates RECONCILE_UNDO journal.
     * Spec: 06-reconciliation.md Section 3.6, R-04, R-05, BR-62
     */
    public void undoReconciliation(UUID reconciliationId, String reason, String undoneBy) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required to undo a reconciliation (BR-62)");
        }

        Reconciliation rec = reconciliationRepository.findById(reconciliationId)
                .orElseThrow(() -> new IllegalArgumentException("Reconciliation not found: " + reconciliationId));

        ActualLine actual = actualLineRepository.findById(rec.getActualId()).orElseThrow();
        UUID periodId = actual.getFiscalPeriodId();

        if (periodId != null) {
            List<JournalLineRequest> lines = List.of(
                    new JournalLineRequest(AccountType.ACTUAL, null, null,
                            rec.getMilestoneId(), periodId, BigDecimal.ZERO, BigDecimal.ZERO,
                            "RECONCILIATION", reconciliationId),
                    new JournalLineRequest(AccountType.VARIANCE_RESERVE, null, null,
                            rec.getMilestoneId(), periodId, BigDecimal.ZERO, BigDecimal.ZERO,
                            "RECONCILIATION", reconciliationId)
            );
            journalService.createEntry(JournalEntryType.RECONCILE_UNDO,
                    LocalDate.now(),
                    "Reconciliation undone: " + reconciliationId + " reason: " + reason,
                    undoneBy, lines);
        }

        reconciliationRepository.delete(rec);
    }

    /**
     * Get derived reconciliation status for a milestone.
     * Spec: 06-reconciliation.md Section 4-5
     */
    @Transactional(readOnly = true)
    public ReconciliationStatus getStatus(UUID milestoneId) {
        MilestoneVersion current = milestoneVersionRepository.findCurrentVersion(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        BigDecimal planned = current.getPlannedAmount();
        BigDecimal reconciled = reconciliationRepository.sumReconciledAmount(milestoneId);
        BigDecimal remaining = planned.subtract(reconciled);

        // Load tolerance config
        BigDecimal tolerancePct = getConfig("tolerance_percent", new BigDecimal("0.02"));
        BigDecimal toleranceAbs = getConfig("tolerance_absolute", new BigDecimal("50.00"));

        String statusStr;
        if (reconciled.compareTo(BigDecimal.ZERO) == 0) {
            statusStr = "UNMATCHED";
        } else {
            BigDecimal absRemaining = remaining.abs();
            boolean withinTolerance = false;
            if (planned.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal pctRemaining = absRemaining.divide(planned, 6, RoundingMode.HALF_UP);
                withinTolerance = pctRemaining.compareTo(tolerancePct) <= 0;
            }
            if (!withinTolerance) {
                withinTolerance = absRemaining.compareTo(toleranceAbs) <= 0;
            }

            if (withinTolerance) {
                statusStr = "FULLY_RECONCILED";
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                statusStr = "OVER_BUDGET";
            } else {
                statusStr = "PARTIALLY_MATCHED";
            }
        }

        BigDecimal invoiceTotal = reconciliationRepository
                .sumReconciledAmountByCategory(milestoneId, ReconciliationCategory.INVOICE);
        BigDecimal accrualTotal = reconciliationRepository
                .sumReconciledAmountByCategory(milestoneId, ReconciliationCategory.ACCRUAL);
        BigDecimal reversalTotal = reconciliationRepository
                .sumReconciledAmountByCategory(milestoneId, ReconciliationCategory.ACCRUAL_REVERSAL);
        BigDecimal accrualNet = accrualTotal.add(reversalTotal);

        return new ReconciliationStatus(milestoneId, planned, reconciled, remaining,
                statusStr, invoiceTotal, accrualNet);
    }

    private BigDecimal getConfig(String key, BigDecimal defaultValue) {
        return systemConfigRepository.findById(key)
                .map(c -> new BigDecimal(c.getConfigValue()))
                .orElse(defaultValue);
    }

    /**
     * Get derived accrual lifecycle status for a milestone.
     * Spec: 07-accrual-lifecycle.md Section 5, A-01 through A-05
     */
    @Transactional(readOnly = true)
    public AccrualStatus getAccrualStatus(UUID milestoneId) {
        List<Reconciliation> accruals = reconciliationRepository
                .findByMilestoneIdAndCategory(milestoneId, ReconciliationCategory.ACCRUAL);
        List<Reconciliation> reversals = reconciliationRepository
                .findByMilestoneIdAndCategory(milestoneId, ReconciliationCategory.ACCRUAL_REVERSAL);

        int openAccrualCount = accruals.size() - reversals.size();

        int warningDays = getConfigInt("accrual_aging_warning_days", 60);
        int criticalDays = getConfigInt("accrual_aging_critical_days", 90);

        String accrualStatusStr;
        if (openAccrualCount <= 0) {
            accrualStatusStr = "CLEAN";
        } else {
            // Find oldest open accrual
            Instant oldest = accruals.stream()
                    .map(Reconciliation::getReconciledAt)
                    .min(Instant::compareTo)
                    .orElse(Instant.now());
            long ageDays = ChronoUnit.DAYS.between(oldest, Instant.now());

            if (ageDays >= criticalDays) {
                accrualStatusStr = "AGING_CRITICAL";
            } else if (ageDays >= warningDays) {
                accrualStatusStr = "AGING_WARNING";
            } else {
                accrualStatusStr = "OPEN";
            }
        }

        return new AccrualStatus(milestoneId, openAccrualCount, accrualStatusStr);
    }

    private int getConfigInt(String key, int defaultValue) {
        return systemConfigRepository.findById(key)
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(defaultValue);
    }

    /** Derived reconciliation status for a milestone. */
    public record ReconciliationStatus(
            UUID milestoneId,
            BigDecimal plannedAmount,
            BigDecimal reconciledAmount,
            BigDecimal remaining,
            String status,
            BigDecimal invoiceTotal,
            BigDecimal accrualNet
    ) {}

    /** Derived accrual lifecycle status for a milestone. */
    public record AccrualStatus(
            UUID milestoneId,
            int openAccrualCount,
            String accrualStatus
    ) {}
}
