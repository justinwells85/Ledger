package com.ledger.reconciliation;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T15 — Reconciliation Undo
 * T16 — Reconciliation Status Derivation
 * Spec: 06-reconciliation.md Sections 3.6, 4-5, BR-62
 */
@AutoConfigureMockMvc
class ReconciliationUndoAndStatusTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FiscalPeriodRepository fiscalPeriodRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private MilestoneVersionRepository milestoneVersionRepository;

    @Autowired
    private SapImportRepository sapImportRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private SapImportService sapImportService;

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

    private UUID uploadAndCommitActual(String docNum, String amount) throws Exception {
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n" +
                     docNum + ",2026-01-15," + amount + ",Globant,CC001,1174905.SU.ES,500000,Line\n";
        MockMultipartFile file = new MockMultipartFile("file", docNum + ".csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        return actualLineRepository.findAll().stream()
                .filter(l -> docNum.equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
    }

    // TEST T15-1: Undo reconciliation removes link and creates RECONCILE_UNDO journal
    // Spec: 06-reconciliation.md Section 3.6, R-04, R-05
    @Test
    void undoReconciliation_removesLinkAndCreatesUndoJournal() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC1", "25000.00");
        Reconciliation rec = reconciliationService.createReconciliation(
                actualId, milestone.getMilestoneId(), ReconciliationCategory.INVOICE, null, "system");

        mockMvc.perform(delete("/api/v1/reconciliation/{id}", rec.getReconciliationId())
                        .param("reason", "Wrong milestone"))
                .andExpect(status().isNoContent());

        assertThat(reconciliationRepository.findByActualId(actualId)).isEmpty();

        long undoEntries = journalEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == JournalEntryType.RECONCILE_UNDO)
                .count();
        assertThat(undoEntries).isEqualTo(1);
    }

    // TEST T15-2: Undo requires reason — BR-62
    // Spec: 06-reconciliation.md Section 3.6
    @Test
    void undoReconciliation_noReason_returns400() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC2", "25000.00");
        Reconciliation rec = reconciliationService.createReconciliation(
                actualId, milestone.getMilestoneId(), ReconciliationCategory.INVOICE, null, "system");

        mockMvc.perform(delete("/api/v1/reconciliation/{id}", rec.getReconciliationId()))
                .andExpect(status().isBadRequest());
    }

    // TEST T15-3: After undo, actual can be re-reconciled to different milestone
    // Spec: 06-reconciliation.md R-04
    @Test
    void undoReconciliation_actualCanBeRereconciled() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC3", "25000.00");
        Reconciliation rec = reconciliationService.createReconciliation(
                actualId, milestone.getMilestoneId(), ReconciliationCategory.INVOICE, null, "system");

        reconciliationService.undoReconciliation(rec.getReconciliationId(), "Wrong milestone", "system");

        // Re-reconcile to same milestone (different reconciliation)
        Reconciliation reRec = reconciliationService.createReconciliation(
                actualId, milestone.getMilestoneId(), ReconciliationCategory.ACCRUAL, null, "system");
        assertThat(reRec.getReconciliationId()).isNotEqualTo(rec.getReconciliationId());
    }

    // TEST T16-1: UNMATCHED — no actuals reconciled
    // Spec: 06-reconciliation.md Section 4
    @Test
    void status_unmatched_whenNoActuals() {
        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("UNMATCHED");
        assertThat(status.reconciledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // TEST T16-2: PARTIALLY_MATCHED — some actuals reconciled
    // Spec: 06-reconciliation.md Section 4
    @Test
    void status_partiallyMatched() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC4", "15000.00");
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("PARTIALLY_MATCHED");
        assertThat(status.reconciledAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(status.remaining()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    // TEST T16-3: FULLY_RECONCILED — exact match
    // Spec: 06-reconciliation.md Section 4
    @Test
    void status_fullyReconciled_exactMatch() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC5", "25000.00");
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("FULLY_RECONCILED");
    }

    // TEST T16-4: FULLY_RECONCILED — within tolerance (percentage)
    // Spec: 06-reconciliation.md Section 5
    @Test
    void status_fullyReconciled_withinTolerancePct() throws Exception {
        // $24,600 reconciled on $25,000 planned → remaining $400 = 1.6% < 2% tolerance
        UUID actualId = uploadAndCommitActual("DOC6", "24600.00");
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("FULLY_RECONCILED");
    }

    // TEST T16-5: FULLY_RECONCILED — within absolute tolerance
    // Spec: 06-reconciliation.md Section 5
    @Test
    void status_fullyReconciled_withinAbsoluteTolerance() throws Exception {
        // Milestone at $1000, reconcile $960 → remaining $40 < $50 tolerance_absolute
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("1000.00"),
                janPeriodId, LocalDate.of(2025, 12, 1), "reduce for test", "system");

        UUID actualId = uploadAndCommitActual("DOC7", "960.00");
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("FULLY_RECONCILED");
    }

    // TEST T16-6: OVER_BUDGET
    // Spec: 06-reconciliation.md Section 4
    @Test
    void status_overBudget() throws Exception {
        UUID actualId = uploadAndCommitActual("DOC8", "27000.00");
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.status()).isEqualTo("OVER_BUDGET");
        assertThat(status.remaining()).isEqualByComparingTo(new BigDecimal("-2000.00"));
    }

    // TEST T16-7: Category breakdown
    // Spec: 06-reconciliation.md Section 4
    @Test
    void status_categoryBreakdown() throws Exception {
        UUID inv = uploadAndCommitActual("DOC9", "15000.00");
        reconciliationService.createReconciliation(inv, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        UUID acc = uploadAndCommitActual("DOC10", "10000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        UUID rev = uploadAndCommitActual("DOC11", "-10000.00");
        reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(status.invoiceTotal()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(status.accrualNet()).isEqualByComparingTo(BigDecimal.ZERO); // 10000 + (-10000)
        assertThat(status.reconciledAmount()).isEqualByComparingTo(new BigDecimal("15000.00")); // 15000 + 10000 - 10000
    }
}
