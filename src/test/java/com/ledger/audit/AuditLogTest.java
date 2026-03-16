package com.ledger.audit;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T25 — Entity Audit Log
 * Spec: 11-change-management.md Sections 2.2 and 3
 * Tests: T25-1 through T25-3
 */
@AutoConfigureMockMvc
class AuditLogTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContractRepository contractRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
    @Autowired private MilestoneRepository milestoneRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;

    @Autowired private MilestoneService milestoneService;

    private Contract contract;

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
    }

    // TEST T25-1: Contract update creates audit log with before/after values
    // Spec: 11-change-management.md Sections 2.2 and 3
    @Test
    void contractUpdate_createsAuditLog() throws Exception {
        String body = """
                {
                    "ownerUser": "Alice",
                    "reason": "Owner transferred"
                }
                """;

        mockMvc.perform(patch("/api/v1/contracts/{id}", contract.getContractId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                "CONTRACT", contract.getContractId().toString());
        assertThat(auditLogs).isNotEmpty();
        var updateLogs = auditLogs.stream()
                .filter(a -> "UPDATE".equals(a.getAction()))
                .toList();
        assertThat(updateLogs).hasSize(1);
        assertThat(updateLogs.get(0).getReason()).isEqualTo("Owner transferred");
    }

    // TEST T25-2: Project update creates audit log
    // Spec: 11-change-management.md
    @Test
    void projectUpdate_createsAuditLog() throws Exception {
        String body = """
                {
                    "name": "DPI Photopass Updated",
                    "reason": "Name correction"
                }
                """;

        mockMvc.perform(patch("/api/v1/projects/{id}", "PR13752")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var auditLogs = auditLogRepository.findByEntityTypeAndEntityId("PROJECT", "PR13752");
        assertThat(auditLogs).isNotEmpty();
        var updateLogs = auditLogs.stream()
                .filter(a -> "UPDATE".equals(a.getAction()))
                .toList();
        assertThat(updateLogs).hasSize(1);
        assertThat(updateLogs.get(0).getReason()).isEqualTo("Name correction");
    }

    // TEST T25-3: Audit log captures change details as JSON
    // Spec: 11-change-management.md Section 3
    @Test
    void auditLog_capturesChangesAsJson() throws Exception {
        String body = """
                {
                    "ownerUser": "Bob",
                    "reason": "Changed owner"
                }
                """;

        mockMvc.perform(patch("/api/v1/contracts/{id}", contract.getContractId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        var auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                "CONTRACT", contract.getContractId().toString());
        var updateLog = auditLogs.stream()
                .filter(a -> "UPDATE".equals(a.getAction()))
                .findFirst().orElseThrow();

        // Changes should contain ownerUser before/after
        assertThat(updateLog.getChanges()).isNotNull();
        assertThat(updateLog.getChanges()).containsKey("ownerUser");
        assertThat(updateLog.getChanges().get("ownerUser").get("before")).isEqualTo("Rob");
        assertThat(updateLog.getChanges().get("ownerUser").get("after")).isEqualTo("Bob");
    }
}
