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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T23 — Reconciliation Status Report
 * Spec: 09-reporting.md Section 2.4, 13-api-design.md Section 10
 * Tests: T23-1, T23-2
 */
@AutoConfigureMockMvc
class ReconciliationStatusReportTest extends BaseIntegrationTest {

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

    // TEST T23-1: Report shows category breakdown per milestone
    // Spec: 09-reporting.md Section 2.4
    @Test
    void reconciliationStatusReport_showsCategoryBreakdown() throws Exception {
        UUID inv = commitActual("DOC1", "15000.00");
        reconciliationService.createReconciliation(inv, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        UUID acc = commitActual("DOC2", "10000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        UUID rev = commitActual("DOC3", "-10000.00");
        reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        mockMvc.perform(get("/api/v1/reports/reconciliation-status")
                        .param("fiscalYear", "FY26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].milestoneName").value("January Sustainment"))
                .andExpect(jsonPath("$.rows[0].planned").value(25000.00))
                .andExpect(jsonPath("$.rows[0].invoiceTotal").value(15000.00))
                .andExpect(jsonPath("$.rows[0].accrualNet").value(0.00)) // 10000 + (-10000)
                .andExpect(jsonPath("$.rows[0].totalActual").value(15000.00)) // 15000 + 0
                .andExpect(jsonPath("$.rows[0].remaining").value(10000.00))
                .andExpect(jsonPath("$.rows[0].status").value("PARTIALLY_MATCHED"));
    }

    // TEST T23-2: Filter by status returns only matching milestones
    // Spec: 09-reporting.md Section 2.4
    @Test
    void reconciliationStatusReport_filterByStatus_returnsOnlyMatching() throws Exception {
        // Create second milestone — fully reconciled
        UUID febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();
        Milestone febMilestone = milestoneService.createMilestone("PR13752", "Feb Sustainment",
                null, new BigDecimal("20000.00"), febPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        UUID inv = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(inv, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // febMilestone is UNMATCHED

        // Filter by FULLY_RECONCILED — should return only the jan milestone
        mockMvc.perform(get("/api/v1/reports/reconciliation-status")
                        .param("fiscalYear", "FY26")
                        .param("status", "FULLY_RECONCILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].milestoneName").value("January Sustainment"));

        // Filter by UNMATCHED — should return only feb milestone
        mockMvc.perform(get("/api/v1/reports/reconciliation-status")
                        .param("fiscalYear", "FY26")
                        .param("status", "UNMATCHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].milestoneName").value("Feb Sustainment"));
    }
}
