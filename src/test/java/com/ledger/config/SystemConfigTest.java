package com.ledger.config;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import com.ledger.service.ReconciliationService;
import com.ledger.service.SapImportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T27 — System Config CRUD
 * Spec: 10-business-rules.md BR-30, BR-31, BR-32
 * Tests: T27-1 through T27-4
 */
@AutoConfigureMockMvc
class SystemConfigTest extends BaseIntegrationTest {

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
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SystemConfigRepository systemConfigRepository;

    @Autowired private MilestoneService milestoneService;
    @Autowired private ReconciliationService reconciliationService;
    @Autowired private SapImportService sapImportService;

    @AfterEach
    void tearDown() {
        resetConfig("tolerance_percent", "0.02");
        resetConfig("tolerance_absolute", "50.00");
        resetConfig("accrual_aging_warning_days", "60");
        resetConfig("accrual_aging_critical_days", "90");
    }

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        reconciliationRepository.deleteAll();
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
        milestoneVersionRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        contractRepository.deleteAll();

        // Reset config to known defaults
        resetConfig("tolerance_percent", "0.02");
        resetConfig("tolerance_absolute", "50.00");
        resetConfig("accrual_aging_warning_days", "60");
        resetConfig("accrual_aging_critical_days", "90");
    }

    private void resetConfig(String key, String value) {
        systemConfigRepository.findById(key).ifPresent(c -> {
            c.setConfigValue(value);
            c.setUpdatedBy("system");
            c.setUpdatedAt(java.time.Instant.now());
            systemConfigRepository.save(c);
        });
    }

    // TEST T27-1: GET /api/v1/config returns all configuration values
    // Spec: 10-business-rules.md BR-31
    @Test
    void getConfig_returnsAllValues() throws Exception {
        mockMvc.perform(get("/api/v1/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[*].configKey", hasItem("tolerance_percent")))
                .andExpect(jsonPath("$[*].configKey", hasItem("tolerance_absolute")))
                .andExpect(jsonPath("$[*].configKey", hasItem("accrual_aging_warning_days")))
                .andExpect(jsonPath("$[*].configKey", hasItem("accrual_aging_critical_days")));
    }

    // TEST T27-2: PUT /api/v1/config/{key} updates a configuration value
    // Spec: 10-business-rules.md BR-31
    @Test
    void updateConfig_updatesValue() throws Exception {
        String body = """
                {
                    "value": "0.05",
                    "reason": "Increasing tolerance to 5%"
                }
                """;

        mockMvc.perform(put("/api/v1/config/{key}", "tolerance_percent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configKey").value("tolerance_percent"))
                .andExpect(jsonPath("$.configValue").value("0.05"));
    }

    // TEST T27-3: Config update creates audit log entry
    // Spec: 11-change-management.md Section 2.2
    @Test
    void updateConfig_createsAuditLog() throws Exception {
        String body = """
                {
                    "value": "100.00",
                    "reason": "Raising absolute tolerance"
                }
                """;

        mockMvc.perform(put("/api/v1/config/{key}", "tolerance_absolute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var auditLogs = auditLogRepository.findByEntityTypeAndEntityId("CONFIGURATION", "tolerance_absolute");
        org.assertj.core.api.Assertions.assertThat(auditLogs).isNotEmpty();
        var updateLog = auditLogs.stream()
                .filter(a -> "UPDATE".equals(a.getAction()))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updateLog.getReason()).isEqualTo("Raising absolute tolerance");
        org.assertj.core.api.Assertions.assertThat(updateLog.getChanges().get("configValue").get("before")).isEqualTo("50.00");
        org.assertj.core.api.Assertions.assertThat(updateLog.getChanges().get("configValue").get("after")).isEqualTo("100.00");
    }

    // TEST T27-4: Updated tolerance is immediately reflected in reconciliation status
    // Spec: 10-business-rules.md BR-30, BR-32
    @Test
    void updateTolerance_affectsReconciliationStatus() throws Exception {
        // Set up a milestone with $25000 planned; apply $24800 actual (remaining = $200)
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

        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        Milestone milestone = milestoneService.createMilestone("PR13752", "January Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        // Commit $24800 actual and reconcile it (remaining = $200 = 0.8% < default 2% tolerance)
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n"
                   + "DOC1,2026-01-15,24800.00,Globant,CC001,1174905.SU.ES,500000,Line\n";
        var file = new org.springframework.mock.web.MockMultipartFile(
                "file", "DOC1.csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        UUID actualId = actualLineRepository.findAll().stream()
                .filter(l -> "DOC1".equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        // Default tolerance = 2%, remaining = $200/$25000 = 0.8% → FULLY_RECONCILED
        ReconciliationService.ReconciliationStatus status =
                reconciliationService.getStatus(milestone.getMilestoneId());
        org.assertj.core.api.Assertions.assertThat(status.status()).isEqualTo("FULLY_RECONCILED");

        // Tighten tolerance to 0.1% — remaining $200 now exceeds it → PARTIALLY_MATCHED
        String body = """
                {
                    "value": "0.001",
                    "reason": "Tightening tolerance"
                }
                """;
        mockMvc.perform(put("/api/v1/config/{key}", "tolerance_percent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Also update absolute tolerance to $1 to ensure neither threshold passes
        String body2 = """
                {
                    "value": "1.00",
                    "reason": "Tightening absolute tolerance"
                }
                """;
        mockMvc.perform(put("/api/v1/config/{key}", "tolerance_absolute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk());

        // After tightening — status should now be PARTIALLY_MATCHED
        ReconciliationService.ReconciliationStatus statusAfter =
                reconciliationService.getStatus(milestone.getMilestoneId());
        org.assertj.core.api.Assertions.assertThat(statusAfter.status()).isEqualTo("PARTIALLY_MATCHED");
    }
}
