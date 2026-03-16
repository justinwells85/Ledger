package com.ledger.audit;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.AuditService;
import com.ledger.service.MilestoneService;
import com.ledger.service.ReconciliationService;
import com.ledger.service.SapImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T26 — Audit Query APIs
 * Spec: 11-change-management.md Section 5
 * Tests: T26-1 through T26-3
 */
@AutoConfigureMockMvc
class AuditQueryTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ContractRepository contractRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ReconciliationRepository reconciliationRepository;
    @Autowired private JournalLineRepository journalLineRepository;
    @Autowired private JournalEntryRepository journalEntryRepository;
    @Autowired private ActualLineRepository actualLineRepository;
    @Autowired private SapImportRepository sapImportRepository;
    @Autowired private MilestoneVersionRepository milestoneVersionRepository;
    @Autowired private MilestoneRepository milestoneRepository;

    @Autowired private AuditService auditService;
    @Autowired private MilestoneService milestoneService;
    @Autowired private ReconciliationService reconciliationService;
    @Autowired private SapImportService sapImportService;
    @Autowired private FiscalPeriodRepository fiscalPeriodRepository;

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
        auditLogRepository.deleteAll();

        contract = new Contract();
        contract.setName("Globant ADM");
        contract.setVendor("Globant");
        contract.setOwnerUser("Rob");
        contract.setStartDate(LocalDate.of(2025, 10, 1));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy("system");
        contractRepository.save(contract);
    }

    // TEST T26-1: GET /api/v1/audit/contract/{id} returns audit logs for that contract
    // Spec: 11-change-management.md Section 5
    @Test
    void getAuditLogsForContract_returnsMatchingEntries() throws Exception {
        auditService.log("CONTRACT", contract.getContractId().toString(), "UPDATE",
                Map.of("ownerUser", Map.of("before", "Rob", "after", "Alice")),
                "Owner transferred", "system");

        mockMvc.perform(get("/api/v1/audit/contract/{id}", contract.getContractId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entityType").value("CONTRACT"))
                .andExpect(jsonPath("$[0].entityId").value(contract.getContractId().toString()))
                .andExpect(jsonPath("$[0].action").value("UPDATE"))
                .andExpect(jsonPath("$[0].reason").value("Owner transferred"));
    }

    // TEST T26-2: GET /api/v1/audit/user/{username} returns audit logs created by that user
    // Spec: 11-change-management.md Section 5
    @Test
    void getAuditLogsForUser_returnsEntriesCreatedByUser() throws Exception {
        auditService.log("CONTRACT", contract.getContractId().toString(), "UPDATE",
                Map.of("ownerUser", Map.of("before", "Rob", "after", "Alice")),
                "Owner transferred", "alice");

        auditService.log("CONTRACT", contract.getContractId().toString(), "UPDATE",
                Map.of("ownerUser", Map.of("before", "Alice", "after", "Bob")),
                "Another change", "bob");

        mockMvc.perform(get("/api/v1/audit/user/{username}", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].createdBy").value("alice"));
    }

    // TEST T26-3: GET /api/v1/audit/changes?from=...&to=... returns logs within date range
    // Spec: 11-change-management.md Section 5
    @Test
    void getAuditLogsByDateRange_returnsEntriesWithinRange() throws Exception {
        auditService.log("CONTRACT", contract.getContractId().toString(), "UPDATE",
                Map.of("ownerUser", Map.of("before", "Rob", "after", "Alice")),
                "Change", "system");

        String from = "2026-01-01";
        String to = "2026-12-31";

        mockMvc.perform(get("/api/v1/audit/changes")
                        .param("from", from)
                        .param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].entityType").value("CONTRACT"));
    }

    // TEST T26-2: GET /api/v1/audit/milestone/{id} returns milestone versions + reconciliations + journal entries
    // Spec: 11-change-management.md Section 5
    @Test
    void getAuditHistoryForMilestone_returnsVersionsReconciliationsAndJournal() throws Exception {
        // Create a project and milestone
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

        // Commit an actual and reconcile it
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n"
                   + "DOC1,2026-01-15,25000.00,Globant,CC001,1174905.SU.ES,500000,Line\n";
        MockMultipartFile file = new MockMultipartFile("file", "sap.csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        UUID actualId = actualLineRepository.findAll().stream()
                .filter(l -> "DOC1".equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
        reconciliationService.createReconciliation(actualId, milestone.getMilestoneId(),
                ReconciliationCategory.INVOICE, null, "system");

        UUID milestoneId = milestone.getMilestoneId();

        mockMvc.perform(get("/api/v1/audit/milestone/{id}", milestoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.milestoneId").value(milestoneId.toString()))
                .andExpect(jsonPath("$.versions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.reconciliations", hasSize(1)))
                .andExpect(jsonPath("$.journalEntries", hasSize(greaterThanOrEqualTo(1))));
    }
}
