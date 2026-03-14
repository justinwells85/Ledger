package com.ledger.timemachine;

import com.ledger.BaseIntegrationTest;
import com.ledger.entity.*;
import com.ledger.repository.*;
import com.ledger.service.MilestoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T18 — Time Machine: Milestone Version Queries
 * Spec: 08-time-machine.md, 04-milestone-versioning.md Section 5, BR-41
 * Tests: T18-1 through T18-4
 */
@AutoConfigureMockMvc
class TimeMachineMilestoneTest extends BaseIntegrationTest {

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
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalLineRepository journalLineRepository;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private ReconciliationRepository reconciliationRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private SapImportRepository sapImportRepository;

    private String projectId;

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
        projectId = "PR13752";
    }

    // TEST T18-1: Plan as of date before v2 returns v1 amount
    // Spec: 08-time-machine.md Section 2, BR-41
    @Test
    void planAsOfDate_beforeV2_returnsV1Amount() throws Exception {
        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        UUID febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();

        var milestone = milestoneService.createMilestone(projectId, "Jan Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                febPeriodId, LocalDate.of(2026, 2, 15), "Reduction", "system");

        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", projectId)
                        .param("asOfDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currentVersion.plannedAmount").value(25000.00));
    }

    // TEST T18-2: Plan as of date after v2 returns v2 amount
    // Spec: 08-time-machine.md Section 2, BR-41
    @Test
    void planAsOfDate_afterV2_returnsV2Amount() throws Exception {
        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();
        UUID febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();

        var milestone = milestoneService.createMilestone(projectId, "Jan Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("20000.00"),
                febPeriodId, LocalDate.of(2026, 2, 15), "Reduction", "system");

        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", projectId)
                        .param("asOfDate", "2026-03-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currentVersion.plannedAmount").value(20000.00));
    }

    // TEST T18-3: Milestone not visible before first version effective_date
    // Spec: 08-time-machine.md Section 4
    @Test
    void milestoneNotVisible_beforeFirstVersionEffectiveDate() throws Exception {
        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();

        milestoneService.createMilestone(projectId, "Jan Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");

        // asOfDate is before the first version's effective_date (2025-11-01)
        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", projectId)
                        .param("asOfDate", "2025-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // TEST T18-4: Cancelled milestone shows $0 after cancellation date
    // Spec: 08-time-machine.md Section 4
    @Test
    void cancelledMilestone_showsZeroAfterCancellationDate() throws Exception {
        UUID janPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-04-JAN").orElseThrow().getPeriodId();

        var milestone = milestoneService.createMilestone(projectId, "Jan Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
        milestoneService.cancelMilestone(milestone.getMilestoneId(),
                LocalDate.of(2026, 3, 1), "Cancelled project", "system");

        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", projectId)
                        .param("asOfDate", "2026-03-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currentVersion.plannedAmount").value(0.00));
    }
}
