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
 * T24 — Open Accruals Report
 * Spec: 07-accrual-lifecycle.md Section 6, 09-reporting.md Section 2.6
 * Tests: T24-1 through T24-3
 */
@AutoConfigureMockMvc
class OpenAccrualsReportTest extends BaseIntegrationTest {

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

    // TEST T24-1: Report shows open accruals sorted by age (oldest first)
    // Spec: 07-accrual-lifecycle.md Section 6
    @Test
    void openAccrualsReport_showsOpenAccrualsSortedByAge() throws Exception {
        UUID acc1 = commitActual("DOC1", "25000.00");
        Reconciliation rec1 = reconciliationService.createReconciliation(acc1, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");
        // Backdate to 30 days ago
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS)), rec1.getReconciliationId());

        mockMvc.perform(get("/api/v1/reports/open-accruals")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].milestoneName").value("January Sustainment"))
                .andExpect(jsonPath("$.rows[0].openAccrualCount").value(1))
                .andExpect(jsonPath("$.rows[0].ageDays").value(greaterThanOrEqualTo(29)))
                .andExpect(jsonPath("$.rows[0].accrualStatus").value("OPEN"));
    }

    // TEST T24-2: Status reflects WARNING and CRITICAL thresholds
    // Spec: 07-accrual-lifecycle.md Section 4
    @Test
    void openAccrualsReport_agingWarningThreshold() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        Reconciliation rec = reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");
        // Backdate to 70 days ago (> 60-day warning threshold)
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(Instant.now().minus(70, ChronoUnit.DAYS)), rec.getReconciliationId());

        mockMvc.perform(get("/api/v1/reports/open-accruals")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].accrualStatus").value("AGING_WARNING"));
    }

    // TEST T24-3: Resolved accruals (with reversal) do not appear
    // Spec: 07-accrual-lifecycle.md BR-20
    @Test
    void openAccrualsReport_resolvedAccruals_notShown() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        UUID rev = commitActual("DOC2", "-25000.00");
        reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        // openAccrualCount = 0, so milestone should not appear in open accruals report
        mockMvc.perform(get("/api/v1/reports/open-accruals")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(0)));
    }
}
