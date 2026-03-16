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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T14-6 — Reconciliation Candidates Endpoint
 * Spec: 06-reconciliation.md Section 3.3, 13-api-design.md Section 8
 */
@AutoConfigureMockMvc
class ReconciliationCandidatesTest extends BaseIntegrationTest {

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
    @Autowired private SapImportService sapImportService;

    private UUID actualId;

    @BeforeEach
    void setUp() throws Exception {
        reconciliationRepository.deleteAll();
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
        actualLineRepository.deleteAll();
        sapImportRepository.deleteAll();
        milestoneVersionRepository.deleteAll();
        milestoneRepository.deleteAll();
        projectRepository.deleteAll();
        contractRepository.deleteAll();

        // Contract
        Contract contract = new Contract();
        contract.setName("Globant ADM");
        contract.setVendor("Globant");
        contract.setOwnerUser("Rob");
        contract.setStartDate(LocalDate.of(2025, 10, 1));
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCreatedBy("system");
        contractRepository.save(contract);

        // Matching project (same WBSE as actual)
        Project matchingProject = new Project();
        matchingProject.setProjectId("PR13752");
        matchingProject.setContract(contract);
        matchingProject.setWbse("1174905.SU.ES");
        matchingProject.setName("DPI Photopass");
        matchingProject.setFundingSource(FundingSource.OPEX);
        matchingProject.setStatus(ProjectStatus.ACTIVE);
        matchingProject.setCreatedBy("system");
        projectRepository.save(matchingProject);

        // Non-matching project (different WBSE)
        Project otherProject = new Project();
        otherProject.setProjectId("PR99999");
        otherProject.setContract(contract);
        otherProject.setWbse("9999999.SU.ES");
        otherProject.setName("Other Project");
        otherProject.setFundingSource(FundingSource.CAPEX);
        otherProject.setStatus(ProjectStatus.ACTIVE);
        otherProject.setCreatedBy("system");
        projectRepository.save(otherProject);

        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();

        // Milestone under matching project
        milestoneService.createMilestone("PR13752", "January Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        // Milestone under non-matching project
        milestoneService.createMilestone("PR99999", "Other Milestone",
                null, new BigDecimal("10000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        // Commit an actual with WBSE matching PR13752
        String csv = "Document Number,Posting Date,Amount,Vendor Name,Cost Center,WBS Element,GL Account,Description\n"
                   + "DOC1,2026-01-15,25000.00,Globant,CC001,1174905.SU.ES,500000,January\n";
        MockMultipartFile file = new MockMultipartFile("file", "sap.csv", "text/csv", csv.getBytes());
        SapImport staged = sapImportService.uploadAndStage(file, "system");
        sapImportService.commitImport(staged.getImportId(), "system");
        actualId = actualLineRepository.findAll().stream()
                .filter(l -> "DOC1".equals(l.getSapDocumentNumber()))
                .findFirst().orElseThrow().getActualId();
    }

    // TEST T14-6: Candidates endpoint returns sorted candidates with WBSE-matching first
    // Spec: 06-reconciliation.md Section 3.3
    @Test
    void candidates_returnsMatchingWbseMilestonesFirst() throws Exception {
        mockMvc.perform(get("/api/v1/reconciliation/candidates/{actualId}", actualId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // First result should be WBSE-matching milestone (higher relevance)
                .andExpect(jsonPath("$[0].milestoneName").value("January Sustainment"))
                .andExpect(jsonPath("$[0].relevanceScore").value(greaterThan(0)))
                // Second result is non-matching (lower relevance)
                .andExpect(jsonPath("$[1].milestoneName").value("Other Milestone"))
                .andExpect(jsonPath("$[1].relevanceScore").value(0));
    }
}
