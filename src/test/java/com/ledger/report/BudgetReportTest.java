package com.ledger.report;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T21 — Budget Plan Report
 * Spec: 09-reporting.md Section 2.1, 13-api-design.md Section 10
 * Tests: T21-1 through T21-5
 */
@AutoConfigureMockMvc
class BudgetReportTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContractRepository contractRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;

    @Autowired private MilestoneService milestoneService;

    private Contract contract;
    private UUID janPeriodId;
    private UUID febPeriodId;

    @BeforeEach
    void setUp() {
        reconciliationRepository.deleteAll();
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
        milestoneVersionRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        contractRepository.deleteAll();

        contract = new Contract();
        contract.setName("Globant ADM");
        contract.setVendor("Globant");
        contract.setOwnerUser("Rob");
        contract.setStartDate(LocalDate.of(2025, 10, 1));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy("system");
        contractRepository.save(contract);

        Project project = new Project();
        project.setProjectId("PR13752");
        project.setContract(contract);
        project.setWbse("1174905.SU.ES");
        project.setName("DPI Photopass");
        project.setFundingSource(FundingSource.OPEX);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedBy("system");
        projectRepository.save(project);

        janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();

        milestoneService.createMilestone("PR13752", "January Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
        milestoneService.createMilestone("PR13752", "February Sustainment",
                null, new BigDecimal("20000.00"), febPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
    }

    // TEST T21-1: Budget report returns rows grouped by project with period amounts
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_groupByProject_returnsRowsWithPeriodAmounts() throws Exception {
        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY26")
                        .param("groupBy", "project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiscalYear").value("FY26"))
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].projectId").value("PR13752"))
                .andExpect(jsonPath("$.rows[0].projectName").value("DPI Photopass"))
                .andExpect(jsonPath("$.rows[0].periods['FY26-04-JAN']").value(25000.00))
                .andExpect(jsonPath("$.rows[0].periods['FY26-05-FEB']").value(20000.00))
                .andExpect(jsonPath("$.rows[0].total").value(45000.00))
                .andExpect(jsonPath("$.grandTotal").value(45000.00));
    }

    // TEST T21-2: Budget report filters by contract
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_filterByContract_returnsOnlyThatContract() throws Exception {
        // Add a second contract + project
        Contract otherContract = new Contract();
        otherContract.setName("Other Vendor");
        otherContract.setVendor("Other");
        otherContract.setOwnerUser("Alice");
        otherContract.setStartDate(LocalDate.of(2025, 10, 1));
        otherContract.setStatus(ContractStatus.ACTIVE);
        otherContract.setCreatedBy("system");
        contractRepository.save(otherContract);

        Project otherProject = new Project();
        otherProject.setProjectId("PR99999");
        otherProject.setContract(otherContract);
        otherProject.setWbse("9999999.SU.ES");
        otherProject.setName("Other Project");
        otherProject.setFundingSource(FundingSource.CAPEX);
        otherProject.setStatus(ProjectStatus.ACTIVE);
        otherProject.setCreatedBy("system");
        projectRepository.save(otherProject);

        milestoneService.createMilestone("PR99999", "CAPEX Milestone",
                null, new BigDecimal("50000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY26")
                        .param("contractId", contract.getContractId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].projectId").value("PR13752"));
    }

    // TEST T21-3: Budget report filters by funding source
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_filterByFundingSource_returnsOnlyMatching() throws Exception {
        Project capexProject = new Project();
        capexProject.setProjectId("PR99998");
        capexProject.setContract(contract);
        capexProject.setWbse("9999998.SU.ES");
        capexProject.setName("CAPEX Project");
        capexProject.setFundingSource(FundingSource.CAPEX);
        capexProject.setStatus(ProjectStatus.ACTIVE);
        capexProject.setCreatedBy("system");
        projectRepository.save(capexProject);

        milestoneService.createMilestone("PR99998", "CAPEX Milestone",
                null, new BigDecimal("30000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY26")
                        .param("fundingSource", "OPEX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].fundingSource").value("OPEX"));
    }

    // TEST T21-5: Grand total matches sum of all row totals
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_grandTotalMatchesSumOfRows() throws Exception {
        // With 2 projects (OPEX and CAPEX)
        Project capexProject = new Project();
        capexProject.setProjectId("PR99997");
        capexProject.setContract(contract);
        capexProject.setWbse("9999997.SU.ES");
        capexProject.setName("CAPEX Project");
        capexProject.setFundingSource(FundingSource.CAPEX);
        capexProject.setStatus(ProjectStatus.ACTIVE);
        capexProject.setCreatedBy("system");
        projectRepository.save(capexProject);

        milestoneService.createMilestone("PR99997", "CAPEX Milestone",
                null, new BigDecimal("30000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(2)))
                .andExpect(jsonPath("$.grandTotal").value(75000.00)); // 45000 + 30000
    }

    // TEST T21-4: Budget report with quarterly grouping sums 3 months into one quarter key
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_quarterlyGrouping_sumsThreeMonths() throws Exception {
        // Add OCT, NOV, DEC milestones (all FY26 Q1)
        UUID octPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-01-OCT").orElseThrow().getPeriodId();
        UUID novPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-02-NOV").orElseThrow().getPeriodId();
        UUID decPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-03-DEC").orElseThrow().getPeriodId();

        milestoneService.createMilestone("PR13752", "October Sustainment",
                null, new BigDecimal("10000.00"), octPeriodId,
                LocalDate.of(2025, 9, 1), "Initial", "system");
        milestoneService.createMilestone("PR13752", "November Sustainment",
                null, new BigDecimal("12000.00"), novPeriodId,
                LocalDate.of(2025, 9, 1), "Initial", "system");
        milestoneService.createMilestone("PR13752", "December Sustainment",
                null, new BigDecimal("8000.00"), decPeriodId,
                LocalDate.of(2025, 9, 1), "Initial", "system");

        // Request quarterly grouping
        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY26")
                        .param("groupBy", "quarter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                // Q1 (OCT+NOV+DEC) = 10000+12000+8000 = 30000; Q2 (JAN+FEB) = 25000+20000 = 45000
                .andExpect(jsonPath("$.rows[0].periods['FY26-Q1']").value(30000.00))
                .andExpect(jsonPath("$.rows[0].periods['FY26-Q2']").value(45000.00));
    }

    // TEST T21-fiscal-year-filter: Milestones in other fiscal years excluded
    // Spec: 09-reporting.md Section 2.1
    @Test
    void budgetReport_excludesMilestonesNotInRequestedFiscalYear() throws Exception {
        // Add a milestone in FY27 (doesn't exist in fiscal calendar, but let's use a period outside FY26)
        // The FY26 periods are FY26-01-OCT through FY26-13-SEP
        // All our test milestones are in FY26, so requesting FY25 should return empty
        mockMvc.perform(get("/api/v1/reports/budget")
                        .param("fiscalYear", "FY25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(0)))
                .andExpect(jsonPath("$.grandTotal").value(0.0));
    }
}
