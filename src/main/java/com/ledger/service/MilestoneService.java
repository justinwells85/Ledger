package com.ledger.service;

import com.ledger.dto.JournalLineRequest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
