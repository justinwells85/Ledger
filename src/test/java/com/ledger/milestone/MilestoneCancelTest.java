package com.ledger.milestone;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T10 — Milestone Cancel
 * Spec: 04-milestone-versioning.md Section 4 (Cancelling)
 * Tests: T10-1, T10-2
 */
@AutoConfigureMockMvc
class MilestoneCancelTest extends BaseIntegrationTest {

    @Autowired
    private MilestoneService milestoneService;

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

    private Milestone milestone;
    private UUID janPeriodId;

    @BeforeEach
    void setUp() {
        journalLineRepository.deleteAll();
        journalEntryRepository.deleteAll();
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

        milestone = milestoneService.createMilestone(
                "PR13752", "January Sustainment", null,
                new BigDecimal("25000.00"), janPeriodId,
                LocalDate.of(2025, 11, 1), "Initial", "system");
    }

    // TEST T10-1: Cancel milestone creates version with amount = 0 and reversal journal
    // Spec: 04-milestone-versioning.md Section 4 (Cancelling)
    @Test
    void cancelMilestone_createsZeroVersionAndReversalJournal() throws Exception {
        mockMvc.perform(post("/api/v1/milestones/{id}/cancel", milestone.getMilestoneId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Project descoped"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(2))
                .andExpect(jsonPath("$.plannedAmount").value(0.0));

        MilestoneVersion cancelled = milestoneVersionRepository
                .findCurrentVersion(milestone.getMilestoneId()).orElseThrow();
        assertThat(cancelled.getPlannedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cancelled.getReason()).isEqualTo("Project descoped");
    }

    // TEST T10-2: Cancelled milestone shows $0 in budget queries
    // Spec: 02-journal-ledger.md Section 6
    @Test
    void cancelMilestone_plannedBalanceIsZero() {
        milestoneService.cancelMilestone(
                milestone.getMilestoneId(),
                LocalDate.of(2026, 2, 1),
                "Project descoped",
                "system"
        );

        BigDecimal balance = milestoneService.getPlannedBalance(
                milestone.getMilestoneId(), LocalDate.now());
        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
