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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T20 — Time Machine: Cross-cutting
 * Spec: 08-time-machine.md, 10-business-rules.md BR-52, BR-53
 * Tests: T20-1 through T20-4
 */
@AutoConfigureMockMvc
class TimeMachineCrossTest extends BaseIntegrationTest {

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
        milestone = milestoneService.createMilestone("PR13752", "Jan Sustainment",
                null, new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
    }

    // TEST T20-1: Project milestone list with asOfDate=null returns current state
    // Spec: 08-time-machine.md Section 2
    @Test
    void projectMilestones_withNullAsOfDate_returnsCurrentState() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", "PR13752"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].currentVersion.plannedAmount").value(25000.00));
    }

    // TEST T20-2: Project milestones filtered by asOfDate (already covered in T18, sanity check here)
    // Spec: 08-time-machine.md Section 2, BR-41
    @Test
    void projectMilestones_withPastAsOfDate_respectsVersion() throws Exception {
        UUID febPeriodId = fiscalPeriodRepository.findByPeriodKey("FY26-05-FEB").orElseThrow().getPeriodId();
        milestoneService.createVersion(milestone.getMilestoneId(), new BigDecimal("18000.00"),
                febPeriodId, LocalDate.of(2026, 2, 1), "Reduce", "system");

        // asOfDate before v2 effective date → should show v1 ($25K)
        mockMvc.perform(get("/api/v1/projects/{projectId}/milestones", "PR13752")
                        .param("asOfDate", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentVersion.plannedAmount").value(25000.00));
    }

    // TEST T20-3: Future asOfDate rejected — BR-53
    // Spec: 10-business-rules.md BR-53
    @Test
    void getStatus_withFutureAsOfDate_throwsException() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        assertThatThrownBy(() ->
                reconciliationService.getStatus(milestone.getMilestoneId(), futureDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    // TEST T20-4: Reconciliation status with current-date asOfDate returns same as no-date
    // Spec: 08-time-machine.md Section 2, BR-52
    @Test
    void reconciliationStatus_withTodayAsOfDate_matchesCurrentState() {
        ReconciliationService.ReconciliationStatus withDate =
                reconciliationService.getStatus(milestone.getMilestoneId(), LocalDate.now());
        ReconciliationService.ReconciliationStatus current =
                reconciliationService.getStatus(milestone.getMilestoneId());

        assertThat(withDate.plannedAmount()).isEqualByComparingTo(current.plannedAmount());
        assertThat(withDate.reconciledAmount()).isEqualByComparingTo(current.reconciledAmount());
        assertThat(withDate.status()).isEqualTo(current.status());
    }
}
