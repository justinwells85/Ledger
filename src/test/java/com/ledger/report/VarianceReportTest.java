package com.ledger.report;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import com.ledger.service.ReconciliationService;
import com.ledger.service.SapImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T22 — Variance Report (Plan vs. Actual)
 * Spec: 09-reporting.md Section 2.3, 13-api-design.md Section 10
 * Tests: T22-1 through T22-4
 */
@AutoConfigureMockMvc
class VarianceReportTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContractRepository contractRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;

    @Autowired private MilestoneService milestoneService;
    @Autowired private ReconciliationService reconciliationService;
    @Autowired private SapImportService sapImportService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Milestone milestone;
    private UUID janPeriodId;

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

        Contract contract = new Contract();
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
        milestone = milestoneService.createMilestone("PR13752", "January Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
    }

    private UUID commitActual(String docNum, String amount) throws Exception {
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n"
                   + docNum + ",2026-01-15," + amount + ",Globant,CC001,1174905.SU.ES,500000,Line\n";
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", docNum + ".csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        return actualLineRepository.findAll().stream()
                .filter(l -> docNum.equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
    }

    // TEST T22-1: Variance = planned - actual per project
    // Spec: 09-reporting.md Section 2.3, BR-51
    @Test
    void varianceReport_returnsPlannedActualVariance() throws Exception {
        UUID acc = commitActual("DOC1", "20000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        mockMvc.perform(get("/api/v1/reports/variance")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].projectId").value("PR13752"))
                .andExpect(jsonPath("$.rows[0].totalPlanned").value(25000.00))
                .andExpect(jsonPath("$.rows[0].totalActual").value(20000.00))
                .andExpect(jsonPath("$.rows[0].totalVariance").value(5000.00));
    }

    // TEST T22-2: Status reflects UNDER_BUDGET, OVER_BUDGET
    // Spec: 09-reporting.md Section 2.3
    @Test
    void varianceReport_statusReflectsOverBudget() throws Exception {
        UUID acc = commitActual("DOC1", "27000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        mockMvc.perform(get("/api/v1/reports/variance")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].totalStatus").value("OVER_BUDGET"));
    }

    // TEST T22-3: Variance report with asOfDate shows historical variance
    // Spec: 09-reporting.md Section 2.3, BR-52
    @Test
    void varianceReport_withAsOfDate_showsHistoricalVariance() throws Exception {
        UUID acc = commitActual("DOC1", "20000.00");
        Reconciliation rec = reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // Backdate reconciliation to 14 days ago
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(Instant.now().minus(14, ChronoUnit.DAYS)), rec.getReconciliationId());

        // asOfDate = 7 days ago: reconciliation (14 days ago) is visible, planned = $25K
        LocalDate asOfDate = LocalDate.now().minusDays(7);
        mockMvc.perform(get("/api/v1/reports/variance")
                        .param("fiscalYear", "FY26")
                        .param("asOfDate", asOfDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].totalPlanned").value(25000.00))
                .andExpect(jsonPath("$.rows[0].totalActual").value(20000.00))
                .andExpect(jsonPath("$.rows[0].totalVariance").value(5000.00));
    }

    // TEST T22-4: Unreconciled actuals do not appear in variance
    // Spec: 09-reporting.md Section 2.3
    @Test
    void varianceReport_unreconciledActuals_notCounted() throws Exception {
        // Import an actual but DON'T reconcile it
        commitActual("DOC1", "20000.00");

        mockMvc.perform(get("/api/v1/reports/variance")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].totalActual").value(0.00))
                .andExpect(jsonPath("$.rows[0].totalVariance").value(25000.00));
    }
}
