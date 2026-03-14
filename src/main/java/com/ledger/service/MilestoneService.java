package com.ledger.service;

import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for milestone lifecycle operations.
 * Spec: 04-milestone-versioning.md, 02-journal-ledger.md Sections 5.1–5.4
 */
@Service
@Transactional
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneVersionRepository milestoneVersionRepository;
    private final ProjectRepository projectRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalService journalService;

    public MilestoneService(MilestoneRepository milestoneRepository,
                            MilestoneVersionRepository milestoneVersionRepository,
                            ProjectRepository projectRepository,
                            FiscalPeriodRepository fiscalPeriodRepository,
                            JournalService journalService) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneVersionRepository = milestoneVersionRepository;
        this.projectRepository = projectRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.journalService = journalService;
    }

    /**
     * Create a milestone with v1 and a PLAN_CREATE journal entry.
     * Spec: 04-milestone-versioning.md Section 4, 02-journal-ledger.md Section 5.1
     *
     * @throws IllegalArgumentException if plannedAmount < 0, project not found, or period not found
     */
    public Milestone createMilestone(String projectId,
                                     String name,
                                     String description,
                                     BigDecimal plannedAmount,
                                     UUID fiscalPeriodId,
                                     LocalDate effectiveDate,
                                     String reason,
                                     String createdBy) {
        if (plannedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Milestone planned amount must be >= 0, got: " + plannedAmount);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + fiscalPeriodId));

        // Create Milestone
        Milestone milestone = new Milestone();
        milestone.setProject(project);
        milestone.setName(name);
        milestone.setDescription(description);
        milestone.setCreatedBy(createdBy);
        milestoneRepository.save(milestone);

        // Create MilestoneVersion v1
        MilestoneVersion v1 = new MilestoneVersion();
        v1.setMilestone(milestone);
        v1.setVersionNumber(1);
        v1.setPlannedAmount(plannedAmount);
        v1.setFiscalPeriod(fiscalPeriod);
        v1.setEffectiveDate(effectiveDate);
        v1.setReason(reason);
        v1.setCreatedBy(createdBy);
        milestoneVersionRepository.save(v1);

        // Create PLAN_CREATE journal entry — 2 lines: debit PLANNED, credit VARIANCE_RESERVE
        // Spec: 02-journal-ledger.md Section 5.1
        UUID contractId = project.getContract().getContractId();
        List<JournalLineRequest> lines = List.of(
                new JournalLineRequest(
                        AccountType.PLANNED,
                        contractId,
                        project.getProjectId(),
                        milestone.getMilestoneId(),
                        fiscalPeriodId,
                        plannedAmount,
                        BigDecimal.ZERO,
                        "MILESTONE_VERSION",
                        v1.getVersionId()
                ),
                new JournalLineRequest(
                        AccountType.VARIANCE_RESERVE,
                        contractId,
                        project.getProjectId(),
                        milestone.getMilestoneId(),
                        fiscalPeriodId,
                        BigDecimal.ZERO,
                        plannedAmount,
                        "MILESTONE_VERSION",
                        v1.getVersionId()
                )
        );

        journalService.createEntry(
                JournalEntryType.PLAN_CREATE,
                effectiveDate,
                "Plan created: " + name + " (v1, $" + plannedAmount + ")",
                createdBy,
                lines
        );

        return milestone;
    }

    /**
     * Create a new version of a milestone with a PLAN_ADJUST journal entry.
     * Same-period adjustment: 2-line journal (debit/credit delta).
     * Period shift: 4-line journal (remove old period, add new period).
     * Spec: 04-milestone-versioning.md Section 4 (Adjusting), 02-journal-ledger.md 5.2–5.4
     *
     * @throws IllegalArgumentException if milestone not found, reason blank, or effectiveDate before prior version
     */
    public MilestoneVersion createVersion(UUID milestoneId,
                                          BigDecimal newPlannedAmount,
                                          UUID newFiscalPeriodId,
                                          LocalDate effectiveDate,
                                          String reason,
                                          String createdBy) {
        // BR-42: reason is required
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required for milestone version changes (BR-42)");
        }

        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId));

        MilestoneVersion currentVersion = milestoneVersionRepository
                .findCurrentVersion(milestoneId)
                .orElseThrow(() -> new IllegalStateException("No versions found for milestone: " + milestoneId));

        // V-03 / BR-05: effective_date must be >= prior version's effective_date
        if (effectiveDate.isBefore(currentVersion.getEffectiveDate())) {
            throw new IllegalArgumentException(
                    "New version effective_date " + effectiveDate +
                    " must be >= prior version effective_date " + currentVersion.getEffectiveDate() +
                    " (BR-05)");
        }

        FiscalPeriod newFiscalPeriod = fiscalPeriodRepository.findById(newFiscalPeriodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + newFiscalPeriodId));

        // Create the new version
        MilestoneVersion newVersion = new MilestoneVersion();
        newVersion.setMilestone(milestone);
        newVersion.setVersionNumber(currentVersion.getVersionNumber() + 1);
        newVersion.setPlannedAmount(newPlannedAmount);
        newVersion.setFiscalPeriod(newFiscalPeriod);
        newVersion.setEffectiveDate(effectiveDate);
        newVersion.setReason(reason);
        newVersion.setCreatedBy(createdBy);
        milestoneVersionRepository.save(newVersion);

        Project project = milestone.getProject();
        UUID contractId = project.getContract().getContractId();
        String projectId = project.getProjectId();
        boolean isPeriodShift = !newFiscalPeriodId.equals(currentVersion.getFiscalPeriod().getPeriodId());

        List<JournalLineRequest> lines = new ArrayList<>();

        if (isPeriodShift) {
            // 4-line journal: remove from old period, add to new period
            // Spec: 04-milestone-versioning.md Section 4 (period shift)
            UUID oldPeriodId = currentVersion.getFiscalPeriod().getPeriodId();
            BigDecimal oldAmount = currentVersion.getPlannedAmount();

            // Remove from old period
            lines.add(new JournalLineRequest(AccountType.VARIANCE_RESERVE, contractId, projectId,
                    milestoneId, oldPeriodId, oldAmount, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
            lines.add(new JournalLineRequest(AccountType.PLANNED, contractId, projectId,
                    milestoneId, oldPeriodId, BigDecimal.ZERO, oldAmount, "MILESTONE_VERSION", newVersion.getVersionId()));
            // Add to new period
            lines.add(new JournalLineRequest(AccountType.PLANNED, contractId, projectId,
                    milestoneId, newFiscalPeriodId, newPlannedAmount, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
            lines.add(new JournalLineRequest(AccountType.VARIANCE_RESERVE, contractId, projectId,
                    milestoneId, newFiscalPeriodId, BigDecimal.ZERO, newPlannedAmount, "MILESTONE_VERSION", newVersion.getVersionId()));
        } else {
            // 2-line journal: record the delta
            // Spec: 04-milestone-versioning.md Section 4 (same-period)
            BigDecimal delta = newPlannedAmount.subtract(currentVersion.getPlannedAmount());
            BigDecimal absDelta = delta.abs();

            if (delta.compareTo(BigDecimal.ZERO) > 0) {
                // Increase: debit PLANNED, credit VARIANCE_RESERVE
                lines.add(new JournalLineRequest(AccountType.PLANNED, contractId, projectId,
                        milestoneId, newFiscalPeriodId, absDelta, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
                lines.add(new JournalLineRequest(AccountType.VARIANCE_RESERVE, contractId, projectId,
                        milestoneId, newFiscalPeriodId, BigDecimal.ZERO, absDelta, "MILESTONE_VERSION", newVersion.getVersionId()));
            } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
                // Decrease: debit VARIANCE_RESERVE, credit PLANNED
                lines.add(new JournalLineRequest(AccountType.VARIANCE_RESERVE, contractId, projectId,
                        milestoneId, newFiscalPeriodId, absDelta, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
                lines.add(new JournalLineRequest(AccountType.PLANNED, contractId, projectId,
                        milestoneId, newFiscalPeriodId, BigDecimal.ZERO, absDelta, "MILESTONE_VERSION", newVersion.getVersionId()));
            } else {
                // No amount change, no-op journal — create a zero-delta balanced entry anyway
                lines.add(new JournalLineRequest(AccountType.PLANNED, contractId, projectId,
                        milestoneId, newFiscalPeriodId, BigDecimal.ZERO, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
                lines.add(new JournalLineRequest(AccountType.VARIANCE_RESERVE, contractId, projectId,
                        milestoneId, newFiscalPeriodId, BigDecimal.ZERO, BigDecimal.ZERO, "MILESTONE_VERSION", newVersion.getVersionId()));
            }
        }

        journalService.createEntry(
                JournalEntryType.PLAN_ADJUST,
                effectiveDate,
                "Plan adjusted: " + milestone.getName() + " (v" + newVersion.getVersionNumber() + ", reason: " + reason + ")",
                createdBy,
                lines
        );

        return newVersion;
    }

    /**
     * Cancel a milestone by creating a version with planned_amount = 0.
     * Spec: 04-milestone-versioning.md Section 4 (Cancelling), V-09
     */
    public MilestoneVersion cancelMilestone(UUID milestoneId,
                                             LocalDate effectiveDate,
                                             String reason,
                                             String createdBy) {
        return createVersion(milestoneId, BigDecimal.ZERO,
                milestoneVersionRepository.findCurrentVersion(milestoneId)
                        .orElseThrow(() -> new IllegalArgumentException("Milestone not found: " + milestoneId))
                        .getFiscalPeriod().getPeriodId(),
                effectiveDate, reason, createdBy);
    }

    /**
     * Get the net planned balance for a milestone as of a given date.
     * Delegates to JournalService.
     */
    @Transactional(readOnly = true)
    public BigDecimal getPlannedBalance(UUID milestoneId, LocalDate asOfDate) {
        return journalService.getPlannedBalance(milestoneId, asOfDate);
    }
}
