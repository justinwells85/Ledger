package com.ledger.reconciliation;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T14 — Reconciliation Create
 * Spec: 06-reconciliation.md Section 3, BR-06, BR-07
 * Tests: T14-1 through T14-6 (T14-6 is candidate sorting, tested separately)
 */
@AutoConfigureMockMvc
class ReconciliationCreateTest extends BaseIntegrationTest {

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
    private SapImportService sapImportService;

    private Milestone milestone;
    private UUID actualId;
    private UUID duplicateActualId;

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

        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        milestone = milestoneService.createMilestone("PR13752", "January Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        // Stage and commit a real import so we have actual lines to reconcile
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n" +
                     "DOC1,2026-01-15,25000.00,Globant,CC001,1174905.SU.ES,500000,January\n" +
                     "DOC2,2026-01-15,10000.00,Globant,CC001,1174905.SU.ES,500000,Extra\n";
        MockMultipartFile file = new MockMultipartFile("file", "sap.csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");

        // Get the actual lines
        var lines = actualLineRepository.findAll();
        actualId = lines.stream().filter(l -> l.getSapDocumentNumber().equals("DOC1"))
                .findFirst().orElseThrow().getActualId();

        // Create a duplicate actual line for T14-5
        ActualLine dupLine = new ActualLine();
        dupLine.setSapImport(staged);
        dupLine.setSapDocumentNumber("DUP1");
        dupLine.setPostingDate(LocalDate.of(2026, 1, 15));
        dupLine.setAmount(new BigDecimal("5000.00"));
        dupLine.setLineHash("fakehash-dup-test");
        dupLine.setDuplicate(true);
        dupLine.setFiscalPeriodId(janPeriodId);
        actualLineRepository.save(dupLine);
        duplicateActualId = dupLine.getActualId();
    }

    // TEST T14-1: Reconcile actual to milestone creates reconciliation and RECONCILE journal
    // Spec: 06-reconciliation.md Section 3.4-3.5, BR-06, BR-07
    @Test
    void reconcile_createsReconciliationAndJournalEntry() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualId": "%s",
                                  "milestoneId": "%s",
                                  "category": "INVOICE",
                                  "matchNotes": "Confirmed with Rob"
                                }
                                """.formatted(actualId, milestone.getMilestoneId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reconciliationId").isNotEmpty())
                .andExpect(jsonPath("$.category").value("INVOICE"));

        assertThat(reconciliationRepository.findByActualId(actualId)).isPresent();

        long reconcileEntries = journalEntryRepository.findAll().stream()
                .filter(e -> e.getEntryType() == JournalEntryType.RECONCILE)
                .count();
        assertThat(reconcileEntries).isEqualTo(1);
    }

    // TEST T14-2: Cannot reconcile same actual twice — BR-06
    // Spec: 06-reconciliation.md R-01, BR-06
    @Test
    void reconcile_sameActualTwice_returns409() throws Exception {
        String body = """
                {"actualId": "%s", "milestoneId": "%s", "category": "INVOICE"}
                """.formatted(actualId, milestone.getMilestoneId());

        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        // Second attempt with same actual
        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // TEST T14-3: Category is required — BR-07
    // Spec: 06-reconciliation.md R-03
    @Test
    void reconcile_missingCategory_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualId": "%s", "milestoneId": "%s"}
                                """.formatted(actualId, milestone.getMilestoneId())))
                .andExpect(status().isBadRequest());
    }

    // TEST T14-4: Invalid category rejected
    // Spec: 06-reconciliation.md BR-07
    @Test
    void reconcile_invalidCategory_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualId": "%s", "milestoneId": "%s", "category": "INVALID"}
                                """.formatted(actualId, milestone.getMilestoneId())))
                .andExpect(status().isBadRequest());
    }

    // TEST T14-5: Cannot reconcile a duplicate actual — R-06
    // Spec: 06-reconciliation.md R-06
    @Test
    void reconcile_duplicateActual_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/reconciliation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actualId": "%s", "milestoneId": "%s", "category": "INVOICE"}
                                """.formatted(duplicateActualId, milestone.getMilestoneId())))
                .andExpect(status().isBadRequest());
    }
}
