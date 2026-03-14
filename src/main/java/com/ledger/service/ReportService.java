package com.ledger.service;

import com.ledger.entity.*;
import com.ledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for generating derived reports from the journal + reconciliation data.
 * Spec: 09-reporting.md, BR-52
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ProjectRepository projectRepository;
    private final MilestoneService milestoneService;

    public ReportService(ProjectRepository projectRepository,
                         MilestoneService milestoneService) {
        this.projectRepository = projectRepository;
        this.milestoneService = milestoneService;
    }

    /**
     * Generate a budget plan report.
     * Spec: 09-reporting.md Section 2.1, 13-api-design.md Section 10
     *
     * @param contractId    optional — filter by contract
     * @param projectId     optional — filter by project
     * @param fiscalYear    required — e.g. "FY26"
     * @param fundingSource optional — filter by funding source
     * @param asOfDate      optional — time machine date (BR-52, BR-53)
     */
    public BudgetReport getBudgetReport(UUID contractId,
                                        String projectId,
                                        String fiscalYear,
                                        String fundingSource,
                                        LocalDate asOfDate) {
        if (fiscalYear == null || fiscalYear.isBlank()) {
            throw new IllegalArgumentException("fiscalYear is required");
        }

        // BR-53: future asOfDate rejected (delegated to MilestoneService)

        // 1. Resolve projects matching the filters
        List<Project> projects = resolveProjects(contractId, projectId, fundingSource);

        // 2. Build rows: one per project
        List<BudgetReportRow> rows = new ArrayList<>();
        for (Project project : projects) {
            List<MilestoneService.MilestoneAsOf> milestones =
                    milestoneService.getMilestonesAsOf(project.getProjectId(), asOfDate);

            Map<String, BigDecimal> periods = new TreeMap<>();
            BigDecimal projectTotal = BigDecimal.ZERO;

            for (MilestoneService.MilestoneAsOf mao : milestones) {
                MilestoneVersion version = mao.version();
                String periodKey = version.getFiscalPeriod().getPeriodKey();

                // Only include milestones whose fiscal period belongs to the requested fiscal year
                if (!periodKey.startsWith(fiscalYear + "-")) {
                    continue;
                }

                BigDecimal amount = version.getPlannedAmount();
                periods.merge(periodKey, amount, BigDecimal::add);
                projectTotal = projectTotal.add(amount);
            }

            if (!periods.isEmpty()) {
                rows.add(new BudgetReportRow(
                        project.getContract().getContractId(),
                        project.getContract().getName(),
                        project.getProjectId(),
                        project.getName(),
                        project.getFundingSource().name(),
                        periods,
                        projectTotal
                ));
            }
        }

        // 3. Compute grand total
        BigDecimal grandTotal = rows.stream()
                .map(BudgetReportRow::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BudgetReport(fiscalYear, asOfDate, rows, grandTotal);
    }

    private List<Project> resolveProjects(UUID contractId, String projectId, String fundingSource) {
        // Single project filter
        if (projectId != null) {
            return projectRepository.findById(projectId)
                    .map(List::of)
                    .orElse(List.of());
        }

        FundingSource fs = fundingSource != null ? FundingSource.valueOf(fundingSource) : null;

        if (contractId != null && fs != null) {
            return projectRepository.findByContractContractIdAndFundingSource(contractId, fs);
        } else if (contractId != null) {
            return projectRepository.findByContractContractId(contractId);
        } else if (fs != null) {
            return projectRepository.findByFundingSource(fs);
        } else {
            return projectRepository.findAll();
        }
    }

    /** Budget report response. */
    public record BudgetReport(
            String fiscalYear,
            LocalDate asOfDate,
            List<BudgetReportRow> rows,
            BigDecimal grandTotal
    ) {}

    /** One row in the budget report (one per project when groupBy=project). */
    public record BudgetReportRow(
            UUID contractId,
            String contractName,
            String projectId,
            String projectName,
            String fundingSource,
            Map<String, BigDecimal> periods,
            BigDecimal total
    ) {}
}
