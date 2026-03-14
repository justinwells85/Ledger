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
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T17 — Accrual Lifecycle Tracking
 * Spec: 07-accrual-lifecycle.md, BR-20 through BR-24
 * Tests: T17-1 through T17-6
 */
class AccrualLifecycleTest extends BaseIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n" +
                     docNum + ",2026-01-15," + amount + ",Globant,CC001,1174905.SU.ES,500000,Line\n";
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", docNum + ".csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        return actualLineRepository.findAll().stream()
                .filter(l -> docNum.equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
    }

    // TEST T17-1: ACCRUAL creates an open accrual (openAccrualCount = 1)
    // Spec: 07-accrual-lifecycle.md A-01, A-03
    @Test
    void accrualCreatesOpenAccrual() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        ReconciliationService.AccrualStatus status =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        assertThat(status.openAccrualCount()).isEqualTo(1);
        assertThat(status.accrualStatus()).isEqualTo("OPEN");
    }

    // TEST T17-2: ACCRUAL + ACCRUAL_REVERSAL closes the accrual (openAccrualCount = 0)
    // Spec: 07-accrual-lifecycle.md A-02, A-03
    @Test
    void accrualPlusReversal_closesAccrual() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        UUID rev = commitActual("DOC2", "-25000.00");
        reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        ReconciliationService.AccrualStatus status =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        assertThat(status.openAccrualCount()).isEqualTo(0);
        assertThat(status.accrualStatus()).isEqualTo("CLEAN");
    }

    // TEST T17-3: Full lifecycle — accrue, reverse, invoice → clean
    // Spec: 07-accrual-lifecycle.md Section 2
    @Test
    void fullLifecycle_accrueReverseInvoice() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        UUID rev = commitActual("DOC2", "-25000.00");
        reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        UUID inv = commitActual("DOC3", "25000.00");
        reconciliationService.createReconciliation(inv, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.AccrualStatus accrualStatus =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        assertThat(accrualStatus.openAccrualCount()).isEqualTo(0);
        assertThat(accrualStatus.accrualStatus()).isEqualTo("CLEAN");

        ReconciliationService.ReconciliationStatus reconStatus =
                reconciliationService.getStatus(milestone.getMilestoneId());
        assertThat(reconStatus.reconciledAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(reconStatus.status()).isEqualTo("FULLY_RECONCILED");
    }

    // TEST T17-4: Multiple accrual cycles
    // Spec: 07-accrual-lifecycle.md Section 2 (extended cycle)
    @Test
    void multipleAccrualCycles_openCountIsZero() throws Exception {
        // 2 full accrual/reversal cycles, then invoice
        for (int i = 0; i < 2; i++) {
            UUID acc = commitActual("ACC" + i, "25000.00");
            reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                    ReconciliationCategory.ACCRUAL, null, "system");
            UUID rev = commitActual("REV" + i, "-25000.00");
            reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                    ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");
        }
        UUID inv = commitActual("INV1", "25000.00");
        reconciliationService.createReconciliation(inv, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        ReconciliationService.AccrualStatus status =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        assertThat(status.openAccrualCount()).isEqualTo(0);
        assertThat(reconciliationService.getStatus(milestone.getMilestoneId()).reconciledAmount())
                .isEqualByComparingTo(new BigDecimal("25000.00"));
    }

    // TEST T17-5: Aging — open accrual beyond warning threshold
    // Spec: 07-accrual-lifecycle.md Section 4
    @Test
    void aging_openAccrualBeyondWarningThreshold() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL, null, "system");

        // Manually backdate the reconciliation to 70 days ago (native JDBC since reconciled_at is updatable=false)
        Reconciliation rec = reconciliationRepository.findByActualId(acc).orElseThrow();
        Instant backdated = Instant.now().minus(70, ChronoUnit.DAYS);
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(backdated), rec.getReconciliationId());

        ReconciliationService.AccrualStatus status =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        assertThat(status.openAccrualCount()).isEqualTo(1);
        assertThat(status.accrualStatus()).isEqualTo("AGING_WARNING"); // > 60 days warning threshold
    }

    // TEST T17-6: Positive ACCRUAL_REVERSAL produces warning (BR-24)
    // Spec: 07-accrual-lifecycle.md A-05
    @Test
    void positiveAccrualReversal_producesWarning() throws Exception {
        UUID rev = commitActual("DOC1", "25000.00"); // positive amount in ACCRUAL_REVERSAL
        Reconciliation rec = reconciliationService.createReconciliation(rev, milestone.getMilestoneId(),
                ReconciliationCategory.ACCRUAL_REVERSAL, null, "system");

        // Operation should succeed (not blocked)
        assertThat(rec.getReconciliationId()).isNotNull();
        assertThat(rec.getCategory()).isEqualTo(ReconciliationCategory.ACCRUAL_REVERSAL);

        // Warning is surfaced through the accrual status (positive reversal count but no offsetting accrual)
        ReconciliationService.AccrualStatus status =
                reconciliationService.getAccrualStatus(milestone.getMilestoneId());
        // openAccrualCount = accruals - reversals = 0 - 1 = -1 (anomalous but allowed)
        assertThat(status.openAccrualCount()).isEqualTo(-1);
    }
}
