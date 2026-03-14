package com.ledger.timemachine;

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
 * T19 — Time Machine: Actuals + Reconciliation Queries
 * Spec: 08-time-machine.md Section 2, BR-41, BR-52
 * Tests: T19-1 through T19-3
 */
class TimeMachineActualsTest extends BaseIntegrationTest {

    @Autowired private ContractRepository contractRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private SapImportRepository sapImportRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalLineRepository journalLineRepository;

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
        milestone = milestoneService.createMilestone("PR13752", "Jan Sustainment",
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

    // TEST T19-1: Only reconciliations made on or before asOfDate are counted
    // Spec: 08-time-machine.md Section 2 (reconciliation.reconciled_at)
    @Test
    void status_withAsOfDate_onlyCountsReconciliationsMadeByThatDate() throws Exception {
        UUID acc1 = commitActual("DOC1", "15000.00");
        Reconciliation rec1 = reconciliationService.createReconciliation(acc1, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // Backdate rec1 to 14 days ago (before asOfDate)
        Instant earlyDate = Instant.now().minus(14, ChronoUnit.DAYS);
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(earlyDate), rec1.getReconciliationId());

        UUID acc2 = commitActual("DOC2", "10000.00");
        reconciliationService.createReconciliation(acc2, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // rec2 reconciled_at = now (after asOfDate)

        // asOfDate = 7 days ago: only rec1 (14 days ago) is visible, not rec2 (now)
        LocalDate asOfDate = LocalDate.now().minusDays(7);
        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId(), asOfDate);

        assertThat(status.reconciledAmount()).isEqualByComparingTo(new BigDecimal("15000.00"));
        assertThat(status.status()).isEqualTo("PARTIALLY_MATCHED");
    }

    // TEST T19-2: Reconciliation made after asOfDate — actual shows as unreconciled
    // Spec: 08-time-machine.md Section 2 (reconciliation.reconciled_at)
    @Test
    void status_withAsOfDate_reconciliationAfterDate_appearsUnreconciled() throws Exception {
        UUID acc = commitActual("DOC1", "25000.00");
        reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // reconciliation was just made (now), but asOfDate is in the past → not visible

        LocalDate asOfDate = LocalDate.now().minusDays(1);
        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId(), asOfDate);

        assertThat(status.reconciledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(status.status()).isEqualTo("UNMATCHED");
    }

    // TEST T19-3: Variance report respects asOfDate for both planned and reconciled amounts
    // Spec: 08-time-machine.md Section 3, BR-52
    @Test
    void varianceReport_withAsOfDate_respectsBothPlanAndActual() throws Exception {
        // v1: $25K effective 2025-11-01
        // Actual reconciled 14 days ago ($20K)
        UUID acc = commitActual("DOC1", "20000.00");
        Reconciliation rec = reconciliationService.createReconciliation(acc, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");
        // Backdate reconciliation to 14 days ago
        jdbcTemplate.update("UPDATE reconciliation SET reconciled_at = ? WHERE reconciliation_id = ?",
                Timestamp.from(Instant.now().minus(14, ChronoUnit.DAYS)), rec.getReconciliationId());

        // v2: $22K effective 3 days ago (AFTER our asOfDate of 7 days ago)
        UUID marPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-06-MAR").orElseThrow().getPeriodId();
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("22000.00"),
                marPeriodId, LocalDate.now().minusDays(3), "Reduce", "system");

        // asOfDate = 7 days ago: v2 isn't effective yet, only v1 applies
        LocalDate asOfDate = LocalDate.now().minusDays(7);
        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId(), asOfDate);

        // planned = $25K (v1, since v2 effective 3 days ago > asOfDate 7 days ago)
        assertThat(status.plannedAmount()).isEqualByComparingTo(new BigDecimal("25000.00"));
        // reconciled = $20K (reconciled 14 days ago <= asOfDate 7 days ago)
        assertThat(status.reconciledAmount()).isEqualByComparingTo(new BigDecimal("20000.00"));
        // remaining = $5K (variance)
        assertThat(status.remaining()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }
}
